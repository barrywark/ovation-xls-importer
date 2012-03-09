package com.physion.ovation.importer.xls

import scala.collection.JavaConversions._
import java.io.FileInputStream
import ovation.DataContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook

/**
 * Created by IntelliJ IDEA.
 * User: barry
 * Date: 3/8/12
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */

class XLSImporter {

    def importXLS(ctx: DataContext, xlsPath: String) {
        val input = new FileInputStream(xlsPath);
        val workbook = new XSSFWorkbook(input)

        val sheet = workbook.getSheetAt(0)
        sheet.foreach(row => {
            row.foreach(cell => {
                //System.out.println(cell)
            })
        })
//        for (row <- sheet) {
//            for (cell: HSSFCell <- row) {
//
//            }
//        }
        /*
        Sheet sheet1 = wb.getSheetAt(0);
    for (Row row : sheet1) {
        for (Cell cell : row) {
            CellReference cellRef = new CellReference(row.getRowNum(), cell.getColumnIndex());
            System.out.print(cellRef.formatAsString());
            System.out.print(" - ");

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
        }
         */
    }
}
