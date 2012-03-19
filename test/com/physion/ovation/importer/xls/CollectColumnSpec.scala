package com.physion.ovation.importer.xls

import org.specs2._
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import scala.Predef._
import org.apache.poi.ss.usermodel.Row
import scala.collection.JavaConversions._

class CollectColumnSpec extends mutable.SpecificationWithJUnit with testconfig {

    val workbook = new XSSFWorkbook(new FileInputStream(CiclidBrainAndEcologyFixture.ECOLOGY_XLSX_FIXTURE_PATH))
    val sheet = workbook.getSheet("Environment")

    "The column measurement collection" should {
        "parse units from row 0" in {
            val expected = Seq("C", "m", "m")
            val actual = (1 to 3).map {
                colNumber => new CiclidXlsImporter().columnUnits(sheet.getRow(0), colNumber)
            }
            actual must beEqualTo(expected)
        }
        "parse name from row 0" in {
            val expected = Seq("air temperature", "depth minimum", "depth maximum")
            val actual = (1 to 3).map {
                colNumber => new CiclidXlsImporter().columnLabel(sheet.getRow(0), colNumber)
            }
            actual must beEqualTo(expected)
        }
        "collect rows as Seq" in {
            val expected  = Seq(11.,13.,2.,3.,12.)

            val actual = new CiclidXlsImporter().columnMeasurements(sheet.drop(1), 2)

            actual must beEqualTo(expected)
        }
    }
}
