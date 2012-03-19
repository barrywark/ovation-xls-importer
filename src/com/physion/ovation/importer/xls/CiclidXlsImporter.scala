package com.physion.ovation.importer.xls

import scala.collection.JavaConversions._
import ovation._
import scala.Predef._
import org.joda.time.{LocalDate, DateTimeZone, DateTime}
import org.apache.poi.ss.usermodel._
import collection.{Seq, Map}
import collection.immutable.HashMap
import com.google.common.annotations.VisibleForTesting


class CiclidXlsImporter {

    implicit def cellToOption(cell: Cell): Option[Cell] = {
        cell match {
            case null => None
            case _ => Some(cell)
        }
    }

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

    val XLS_SITE_ID = "_xls_site_id"


    protected val log = Ovation.getLogger

    def importXLS(ctx: DataContext,
                  anatomyExperiment: Experiment,
                  anatomySheet: Sheet,
                  anatomyTimeZone: DateTimeZone,
                  ecologyExperiment: Experiment,
                  ecologyWorkbook: Workbook,
                  ecologyTimeZone: DateTimeZone) {

        log.info("Importing Ciclid anatomy data")

        log.info("Importing anatomy Epochs…")

        anatomySheet.drop(NUM_HEADER_ROWS).foreach(row => {
            importEpoch(ctx, anatomyExperiment, row)
        })

        log.info("Importing field ecology data")

        log.info("  Importing Site Sources…")
        val sitesSheet = ecologyWorkbook.getSheet("Site")
        val sites = sitesSheet.drop(NUM_HEADER_ROWS)
            .filter { row => row.getCell(0, Row.RETURN_BLANK_AS_NULL) != null }
            .map { row => importEcologySite(ctx, ecologyExperiment, row, ecologyTimeZone) }
            .toMap


        log.info("  Importing site ecology measurements…")
        Seq("Environment", "Rugosity", "Rock").map { sheetName =>
            val ecologyMeasurementsSheet = ecologyWorkbook.getSheet(sheetName)
            importEcologyMeasurements(ctx, sites, ecologyMeasurementsSheet)
        }

        log.info("  Importing site survey…")
        val speciesSurveySheet = ecologyWorkbook.getSheet("Survey")
        importSpeciesSurvey(sites, speciesSurveySheet)

        //TODO nest size/diameter

        log.info("Import complete")

    }

    val surveyProtocol = "edu.texas.hofmann.site-survey"
    def importSpeciesSurvey(sites: Map[Long, EpochGroup], sheet: Sheet) {
        log.info("    Importing site survey…")
        val siteMeasurements = sheet.drop(NUM_HEADER_ROWS)
            .filter { row => row.getCell(0, Row.RETURN_BLANK_AS_NULL) != null }
            .groupBy( row => row.getCell(0).getNumericCellValue.asInstanceOf[Long])

        siteMeasurements.foreach { case (siteID, rows) =>
            val siteGroup = sites(siteID)
            val surveyGroup: EpochGroup = siteGroup.getChildren("survey").length match {
                case 0 => siteGroup.insertEpochGroup("survey", siteGroup.getStartTime, siteGroup.getEndTime)
                case 1 => siteGroup.getChildren("survey")(0)
                case _ => {log.warn("Site EpochGroup group has more than \"survey\" child"); siteGroup.getChildren("ecology")(0)}
            }


            val surveyEpoch = surveyGroup.getEpochsIterable.headOption.getOrElse { surveyGroup.insertEpoch(surveyGroup.getStartTime,
                surveyGroup.getEndTime,
                surveyProtocol,
                new HashMap[String,Object]()) //TODO protocolParameters?
            }

            // insert a response for each survey
            rows.foreach { row => {
                val species = row.getCell(1).getStringCellValue
                val response = surveyEpoch.insertResponse(surveyGroup.getExperiment.externalDevice(species, "field survey"),
                    new HashMap[String,Object](), //TODO deviceParameters?
                    new NumericData(Seq(row.getCell(2).getNumericCellValue.asInstanceOf[Int]).toArray),
                    "count",
                    species + " count",
                    1,
                    "not applicable",
                    IResponseData.NUMERIC_DATA_UTI)

                row.getCell(3, Row.RETURN_BLANK_AS_NULL).foreach { cell => response.addNote(cell.getStringCellValue, "survey-notes")}

                surveyEpoch.addTag(species)
                surveyGroup.addTag(species)
                surveyGroup.getParent.addTag(species)
            }}
        }
    }

    /**
     * Imports Ecology site Source and EpochGroup
     *
     * @param ctx DataContext
     * @param exp Experiment to contain the inserted EpochGroup
     * @param row XLS Row with Site data
     * @param timeZone EpochGroup timezone
     * @return tuple (siteID, EpochGroup(site))
     */
    protected def importEcologySite(ctx: DataContext,
                                    exp: Experiment,
                                    row: Row,
    timeZone: DateTimeZone) = {
        val date = new LocalDate(DateUtil.getJavaDate(row.getCell(0).getNumericCellValue), timeZone)
        val siteName = row.getCell(2).getStringCellValue

        val location = row.getCell(3).getStringCellValue
        val startTime = date.toDateTimeAtStartOfDay(timeZone)
        val endTime = date.plusDays(1).toDateTimeAtStartOfDay(timeZone)

        val site = ctx.insertSource("ecology-site")

        site.addProperty("site-name", siteName)
        site.addProperty("location", location)
        val siteID = row.getCell(1).getNumericCellValue.asInstanceOf[Long]
        site.addProperty(XLS_SITE_ID, siteID)

        row.getCell(4, Row.RETURN_BLANK_AS_NULL).foreach { cell: Cell => site.addProperty("gps-lattitue", cell.getStringCellValue)}
        row.getCell(5, Row.RETURN_BLANK_AS_NULL).foreach { cell: Cell => site.addProperty("gps-longitude", cell.getStringCellValue)}

        siteID->exp.insertEpochGroup(site,
            "ecology-site",
            startTime,
            endTime)
    }

    val ecologyProtocol = "edu.texas.hofmann.site-ecology"

    protected def importEcologyMeasurements(ctx: DataContext, sites: Map[Long, EpochGroup], sheet: Sheet) {

        log.info("    Importing ecology measurements " + sheet.getSheetName + "…")
        val siteMeasurements = sheet.drop(NUM_HEADER_ROWS)
            .filter { row => row.getCell(0, Row.RETURN_BLANK_AS_NULL) != null }
            .groupBy( row => row.getCell(0).getNumericCellValue.asInstanceOf[Long])

        siteMeasurements.foreach { case (siteID, rows) =>
            val siteGroup = sites(siteID)
            val ecoGroup: EpochGroup = siteGroup.getChildren("ecology").length match {
                case 0 => siteGroup.insertEpochGroup("ecology", siteGroup.getStartTime, siteGroup.getEndTime)
                case 1 => siteGroup.getChildren("ecology")(0)
                case _ => {log.warn("Site EpochGroup group has more than \"ecology\" child"); siteGroup.getChildren("ecology")(0)}
            }


            val ecoEpoch = ecoGroup.getEpochsIterable.headOption.getOrElse { ecoGroup.insertEpoch(ecoGroup.getStartTime,
                ecoGroup.getEndTime,
                ecologyProtocol,
                new HashMap[String,Object]())
            }

            //add responeses per-column
            val header = sheet.getRow(0)
            header.drop(1).foreach { cell => {
                val units = columnUnits(header, cell.getColumnIndex)
                val label = columnLabel(header, cell.getColumnIndex)

                val measurements = columnMeasurements(sheet.drop(1), cell.getColumnIndex)

                log.info("      Inserting Response for " + label)
                ecoEpoch.insertResponse(ecoGroup.getExperiment.externalDevice(label, "field observation"),
                    new HashMap[String,Object](),
                    new NumericData(measurements.toArray),
                    units,
                    label,
                    1,
                    "not applicable",
                    IResponseData.NUMERIC_DATA_UTI
                )

            }}

        }
    }

    @VisibleForTesting
    def columnMeasurements( rows: Iterable[Row], colNum: Int) = {
        rows.map { row => cellToOption(row.getCell(colNum, Row.CREATE_NULL_AS_BLANK)) }
        .filter  { cell => cell.isDefined }
        .map { cell => cell.get.getCellType match {
            case Cell.CELL_TYPE_NUMERIC => cell.get.getNumericCellValue
            case _ => { println(cell.get.getRow.getSheet.getSheetName); println(cell.get.getRow.getRowNum); println(cell.get.getColumnIndex); throw new OvationXLSImportException("Unusppored measurement cell type " + cell.get.getCellType) }
        } }
    }

    @VisibleForTesting
    def columnUnits(row: Row, colNum: Int) = {
        val cell = row.getCell(colNum, Row.RETURN_BLANK_AS_NULL).map { cell => cell.getStringCellValue }
            .getOrElse({
            println(row.getSheet.getSheetName)
            println(row.getRowNum)
            println(colNum)
            throw new OvationException("Unable to parse units from column header")
        })

        cell.slice(cell.indexOf('(')+1, cell.indexOf(')')).trim

    }

    @VisibleForTesting
    def columnLabel(row: Row, colNum: Int) = {
        val cell = row.getCell(colNum, Row.RETURN_BLANK_AS_NULL).map { cell => cell.getStringCellValue }
            .getOrElse(throw new OvationException("Unable to parse units from column header"))

        cell.slice(0, cell.indexOf('(')).trim
    }

    protected def importEpoch(ctx: DataContext, exp: Experiment, row: Row) {

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
