package com.physion.ovation.importer.xls

import java.io.FileInputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scala.collection.JavaConversions._
import org.apache.poi.ss.usermodel.Row
import org.apache.log4j.Logger
import ovation.{Ovation, DataContext}

class CiclidXlsImporter {
    def importXLS(ctx: DataContext, xlsPath: String) {
        val input = new FileInputStream(xlsPath);
        val workbook = new XSSFWorkbook(input)

        val sheet = workbook.getSheet("combined")

        val GENUS_COLUMN = 1 //"B"
        val SPECIES_COLUMN = 2 //"C"
        val GENUS_NAME_KEY = "genus-name"
        val SPECIES_NAME_KEY = "species-name"

        val log = Ovation.getLogger

        log.info("Importing/updating genus=>species Sources")
        sheet.foreach(row => {
            
            val genus = row.getCell(GENUS_COLUMN).getStringCellValue.replace("'","")
            val species = row.getCell(SPECIES_COLUMN).getStringCellValue.replace("'","")

            val srcInsertion = ctx.sourceForInsertion(Seq(genus, species).toArray,
                                                      Seq(GENUS_NAME_KEY, SPECIES_NAME_KEY).toArray,
                                                      Seq(genus, species).toArray)

            if(srcInsertion.isNew) {
                val source = srcInsertion.getSource
                log.info("Inserted source " + source.getParent.getLabel + " => " + source.getLabel)
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
