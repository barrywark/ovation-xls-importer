package com.physion.ovation.importer.xls

import java.io.FileInputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scala.collection.JavaConversions._
import org.apache.poi.ss.usermodel.{Cell, Row}
import org.joda.time.DateTime
import ovation._
import scala.collection.{Seq, Map}
import scala.Tuple2._

class OvationXLSImportException(msg: String, cause: Throwable) extends OvationException(msg, cause) {

    def this(msg: String) = this(msg, null)
}

class CiclidXlsImporter {

    val GENUS_COLUMN = 1 //"B"
    val SPECIES_COLUMN = 2 //"C"
    val GENUS_NAME_KEY = "genus-name"
    val SPECIES_NAME_KEY = "species-name"
    val XLS_ROW_KEY = "_xls-row"
    val ANATOMY_GROUP_LABEL = "anatomy"
    val ANATOMY_PROTOCOL = "edu.texas.hofmann.brain-anatomy"
    val ANATOMY_DEVICE_MANUFACTURER = "Measurement" //TODO is this what we want?
    val NOT_APPLICABLE = "n/a"

    val anatomyMeasurementColumns = Map[Int,String](8->"body volume",
        9->"body length",
        10->"gonad weight",
        11->"GSI",
        12->"brain length",
        13->"brain mass",
        14->"brain size",
        15->"olfactory nerve", //TODO measurement, units?
        16->"olfactory bulb", //TODO measurement, units?
        17->"telencephalon", //TODO measurement, units?
        18->"optic tectum", //TODO measurement, units?
        19->"hypothalamus", //TODO measurement, units?
        20->"inferior hypothalamus", //TODO measurement, units?
        21->"hypophysis", //TODO meas/units?
        22->"cerebellum", //TODO meas/units?
        23->"dorsal medulla"//TODO meas/units?
    )

    val anatomyMeasurementUnits = Map[String,String](
        "body length"->"<TODO>",
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

    def anatomyRowPhylogeny(row: Row) = {
        val genus = row.getCell(GENUS_COLUMN).getStringCellValue.replace("'", "")
        val species = row.getCell(SPECIES_COLUMN).getStringCellValue.replace("'", "")
        (genus, species)
    }


    def rowSource(ctx: DataContext, genus:String, species: String) = {
        ctx.sourceForInsertion(Seq("genus", "species").toArray,
        Seq(GENUS_NAME_KEY, SPECIES_NAME_KEY).toArray,
        Seq(genus, species).toArray)
    }


    def importXLS(ctx: DataContext, exp: Experiment, xlsPath: String) {

        val input = new FileInputStream(xlsPath);
        val workbook = new XSSFWorkbook(input)

        val sheet = workbook.getSheet("combined")

        val log = Ovation.getLogger

        log.info("Updating anatomy Sources")
        sheet.foreach(row => {

            val (genus,species) = anatomyRowPhylogeny(row)

            val srcInsertion = rowSource(ctx, genus, species)

            if(srcInsertion.isNew) {
                val source = srcInsertion.getSource
                source.addProperty(XLS_ROW_KEY, row.getRowNum)

                log.info("Inserted source " + source.getParent.getLabel + "(" + genus + ")" + " => " + source.getLabel + "(" + species + ")")
            }

        })

        log.info("Importing anatomy Epochs")
        sheet.foreach(row => {
            val (genus,species) = anatomyRowPhylogeny(row)

            val src = rowSource(ctx, genus, species).getSource.getChildren
                .filter((s:Source) => s.getOwnerProperty(XLS_ROW_KEY).asInstanceOf[Int] == row.getRowNum)
                .head

            val group = exp.insertEpochGroup(src, ANATOMY_GROUP_LABEL, new DateTime()) //TODO experiment dates?

            val parameters = Map[String,Object]()

            val epoch = group.insertEpoch(new DateTime(), //TODO epoch date?
                new DateTime(), //TODO end date?
                ANATOMY_PROTOCOL,
                parameters //TODO protocol parameters for anatomy?
            )

            anatomyMeasurementColumns.foreach { case (cellNum, name) => {
                    val value = row.getCell(cellNum, Row.RETURN_BLANK_AS_NULL)
                    if(value != null) {
                        value.getCellType match {
                            case Cell.CELL_TYPE_NUMERIC => epoch.insertResponse(exp.externalDevice(name, ANATOMY_DEVICE_MANUFACTURER),
                                Map[String, Object](),//TODO device parameters?
                                new NumericData(Seq(value.getNumericCellValue).toArray),
                                anatomyMeasurementUnits(name),
                                name,
                                0,
                                NOT_APPLICABLE,
                                IResponseData.NUMERIC_DATA_UTI
                            )
                            case _ => throw new OvationXLSImportException("Unexpected measurement type: " + value.getCellType)
                        }

                    }
                }
            }
        })

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
