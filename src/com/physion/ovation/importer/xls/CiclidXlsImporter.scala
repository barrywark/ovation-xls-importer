package com.physion.ovation.importer.xls

import java.io.FileInputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scala.collection.JavaConversions._
import ovation._
import scala.collection.{Seq, Map}
import scala.Predef._
import org.joda.time.format.DateTimeFormatterBuilder
import org.apache.poi.ss.usermodel.{DateUtil, Workbook, Cell, Row}
import org.joda.time.{LocalDate, DateTimeZone, DateTime}


class CiclidXlsImporter {

    val GENUS_COLUMN = 1 //"B"
    val SPECIES_COLUMN = 2 //"C"


    //TODO- we should define a map name => column number
    /*
    val anatomyColumns = Map[String,Int](
    "genus"->1,
    "species"->2
    )
    */

    val GENUS_NAME_KEY = "genus-name"
    val SPECIES_NAME_KEY = "species-name"
    val XLS_ROW_KEY = "_xls-row"
    val ANATOMY_GROUP_LABEL = "anatomy"
    val ANATOMY_PROTOCOL = "edu.texas.hofmann.brain-anatomy"
    val FISH_LABEL = "fish"

    val ANATOMY_DEVICE_MANUFACTURER = "Measurement" //TODO is this what we want?
    val OBSERVATION_SAMPLING_RATE = "single observation"

    val NUM_HEADER_ROWS = 1

    protected val anatomyMeasurementColumns = Map[Int,String](8->"body volume",
        9->"body length",
        10->"gonad weight",
        11->"GSI",
        12->"brain length",
        13->"brain mass",
        14->"brain size",
        15->"olfactory nerve", //TODO measurement
        16->"olfactory bulb", //TODO measurement
        17->"telencephalon", //TODO measurement
        18->"optic tectum", //TODO measurement
        19->"hypothalamus", //TODO measurement
        20->"inferior hypothalamus", //TODO measurement
        21->"hypophysis", //TODO measurement
        22->"cerebellum", //TODO measurement
        23->"dorsal medulla"//TODO measurement
    )


    protected val anatomyMeasurementUnits = Map[String,String](
        "body length"->"<TODO>",
        "body volume"->"<TODO>",
        "gonad weight"->"<TODO>",
        "GSI"->"<TODO>",
        "brain length"->"<TODO>",
        "brain mass"->"<TODO>",
        "brain size"->"<TODO>",
        "olfactory nerve"->"<TODO>",
        "olfactory bulb"->"<TODO>",
        "telencephalon"->"<TODO>",
        "optic tectum"->"<TODO>",
        "hypothalamus"->"<TODO>",
        "inferior hypothalamus"->"<TODO>",
        "hypophysis"->"<TODO>",
        "cerebellum"->"<TODO>",
        "dorsal medulla"->"<TODO>"
    )

    protected val log = Ovation.getLogger


    def importXLS(ctx: DataContext,
                  anatomyExperiment: Experiment,
                  anatomyWorkbook: Workbook,
                  anatomyTimeZone: DateTimeZone,
                  ecologyExperiment: Experiment,
                  ecologyWorkbook: Workbook,
                  ecologyTimeZone: DateTimeZone) {

        log.info("Importing Ciclid anatomy data")

        log.info("Importing anatomy Epochs")

        val anatomySheet = anatomyWorkbook.getSheet("Combined")
        anatomySheet.drop(NUM_HEADER_ROWS).foreach(row => {
            //importEpoch(ctx, anatomyExperiment, row)
        })

        log.info("Importing field ecology data")
        val ecologySheetNames = Seq("1998", "2003", "2004")
        ecologySheetNames foreach {
            sheetName => {
                val ecologySheet = ecologyWorkbook.getSheet(sheetName)

                println(sheetName)
                val startRows = ecologySheet.drop(NUM_HEADER_ROWS).filter { row => row.getCell(0, Row.RETURN_BLANK_AS_NULL) != null }

                log.info("  " + sheetName + "...")

                log.info ("    " + startRows.size + " sites...")
                startRows.sliding (2) foreach { case (row,next) => importEcologySite(ctx, ecologyExperiment, row, next, ecologyTimeZone) }
            }
        }


        log.info("Import complete")

    }

    //TODO: pass a sliding window of two rows so that we can find where to stop observations
    protected def importEcologySite(ctx: DataContext,
                                    exp: Experiment,
                                    row: Row,
                                    nextSiteRow: Row,
                                    timeZone: DateTimeZone)
    {
        val date = new LocalDate(DateUtil.getJavaDate(row.getCell(0).getNumericCellValue), timeZone)
        val site = row.getCell(1).getStringCellValue
        val location = row.getCell(2).getStringCellValue

        val startTime = date.toDateTimeAtStartOfDay(timeZone)
        val endTime = date.plusDays(1).toDateTimeAtStartOfDay(timeZone)
        val siteGroup = exp.insertEpochGroup("ecology-site",
                                        startTime,
                                        endTime)

        siteGroup.addProperty("site", site)
        siteGroup.addProperty("location", location)

        val gpsLattitude = row.getCell(3, Row.RETURN_BLANK_AS_NULL)
        if(gpsLattitude != null) {
            siteGroup.addProperty("gps-lattitude", gpsLattitude)
        }

        val gpsLongitude = row.getCell(4, Row.RETURN_BLANK_AS_NULL)
        if(gpsLongitude != null) {
            siteGroup.addProperty("gps-longitude", gpsLongitude)
        }

        val ecologyGroup = siteGroup.insertEpochGroup("ecology", startTime, endTime)
        val surveyGroup = siteGroup.insertEpochGroup("survey", startTime, endTime)


    }

    protected def importEpoch(ctx: DataContext, exp: Experiment, row: Row)
    {
        val (genus, species) = anatomyRowGenusAndSpecies(row)

        log.info("    Importing '" + genus + " " + species + "' (row " + row.getRowNum + ")")

        val rowSpecies: Source = rowSource(ctx, genus, species).getSource

        val src = rowSpecies.insertSource(FISH_LABEL)
        src.addProperty(XLS_ROW_KEY, row.getRowNum)

        addSourceProperties(src, row)


        val group = exp.insertEpochGroup(src, ANATOMY_GROUP_LABEL, new DateTime(), new DateTime()) //TODO experiment dates?

        val parameters = Map[String, Object]()

        val epoch = group.insertEpoch(new DateTime(), //TODO epoch date?
                                      new DateTime(), //TODO end date?
                                      ANATOMY_PROTOCOL,
                                      parameters //TODO protocol parameters for anatomy?
        )

        anatomyMeasurementColumns.foreach {
                                              case (cellNum, name) => {
                                                  val value = numericCellValue(row, cellNum)
                                                  value match {
                                                      case None => log.warn("      Missing anatomy measurement " + name)
                                                      case Some(measurement) => {
                                                          epoch.insertResponse(
                                                              exp.externalDevice(name, ANATOMY_DEVICE_MANUFACTURER),
                                                              Map[String, Object](), //TODO device parameters?
                                                              new NumericData(Seq(measurement).toArray),
                                                              anatomyMeasurementUnits(name),
                                                              name,
                                                              1,
                                                              OBSERVATION_SAMPLING_RATE,
                                                              IResponseData.NUMERIC_DATA_UTI
                                                          )
                                                      }

                                                  }
                                              }
                                          }

    }


    protected def anatomyRowGenusAndSpecies(row: Row) = {
        val genus = row.getCell(GENUS_COLUMN).getStringCellValue.replace("'", "").trim
        val species = row.getCell(SPECIES_COLUMN).getStringCellValue.replace("'", "").trim
        (genus, species)
    }

    protected def rowSource(ctx: DataContext, genus:String, species: String) = {
        ctx.sourceForInsertion(Seq("genus", "species").toArray,
                               Seq(GENUS_NAME_KEY, SPECIES_NAME_KEY).toArray,
                               Seq(genus, species).toArray)
    }

    protected def numericCellValue(row: Row, cellNum: Int): Option[Double] = {
        val cell = row.getCell(cellNum, Row.RETURN_BLANK_AS_NULL)

        cell match {
            case null => None
            case _ => cell.getCellType match {
                case Cell.CELL_TYPE_NUMERIC => Some(cell.getNumericCellValue)
                case _ => {
                    log.warn("      Non-numeric cell value " + cellValue(row, cellNum).getOrElse("<unknown>"))
                    None
                }
            }

        }
    }

    protected def cellValue(row: Row, cellNum: Int) =  {
        val cell = row.getCell(cellNum, Row.RETURN_BLANK_AS_NULL)
        
        cell match {
            case null => None
            case _ => Some(
                cell.getCellType match {
                    case Cell.CELL_TYPE_STRING => cell.getStringCellValue.trim
                    case Cell.CELL_TYPE_BOOLEAN => cell.getBooleanCellValue
                    case Cell.CELL_TYPE_NUMERIC => cell.getNumericCellValue
                    case Cell.CELL_TYPE_ERROR => "Error " + cell.getErrorCellValue
                    case _ => throw new OvationXLSImportException("      Cannot get cell value for cell of type " + cell.getCellType)
                }
            )
        }
    }

    private def addSourceProperties(src: Source, row: Row)
    {
        val properties = Map[Int,String](3->"lake",
                                         4->"run-number",
                                         5->"sex",
                                         6->"sample-origin",
                                         7->"museum-animal-ID")
        
        properties.map { case (cellNum, label) => {
            val value = cellValue(row, cellNum)
            value match {
                case Some(value) => src.addProperty(label, value)
                case None => log.warn("      Source is missing " + label)
            }
        } }
    }
}

/*
switch (cell.getCellType()) {
                case Cell.CELL_TYPE_STRING:
                    System.out.println(cell.getRichStringCellValue().getString());
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        System.out.println(cell.getDateCellValue());
                    } else {
                        System.out.println(cell.getNumericCellValue());
                    }
                    break;
                case Cell.CELL_TYPE_BOOLEAN:
                    System.out.println(cell.getBooleanCellValue());
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    System.out.println(cell.getCellFormula());
                    break;
                default:
                    System.out.println();
            }
            */
