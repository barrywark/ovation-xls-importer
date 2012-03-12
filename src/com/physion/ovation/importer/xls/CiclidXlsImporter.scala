package com.physion.ovation.importer.xls

import ovation.DataContext
import java.io.FileInputStream
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import scala.collection.JavaConversions._

class CiclidXlsImporter {
    def importXLS(ctx: DataContext, xlsPath: String) {
        val input = new FileInputStream(xlsPath);
        val workbook = new XSSFWorkbook(input)

        val sheet = workbook.getSheet("combined")

        val GENUS_COLUMN = "B"
        val SPECIES_COLUMN = "C"

        sheet.foreach(row => {
            row.foreach(cell => {
                //System.out.println(cell)
            })
        })
    }
}
