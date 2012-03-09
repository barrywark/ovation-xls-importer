package com.physion.ovation.importer.xls

import org.specs2._
import org.joda.time.DateTime
import ovation.TestDBSetup

class CiclidBrainAndEcologyImportSpec extends mutable.SpecificationWithJUnit {

    "The Ciclid Brain and Ecology importer" should {

        "import genus=>species Source hierarchy" in new xls {
            ctx.currentAuthenticatedUser() must not beNull
        }

        "import site Source hierarchy" in new xls {
            ctx.currentAuthenticatedUser() must beNull
        }
    }
}


trait xls extends ovdbinit {

    val exp = project.insertExperiment("xls-import-test", new DateTime())

    // Import the XLS
    val xlsPath = "fixtures/Ciclid Brain and Social Data with Legend.xlsx"
    val importer = new XLSImporter().importXLS(ctx, xlsPath)

    override def after = deleteExp

    def deleteExp {
        ctx.beginTransaction()
        val db = exp.getContainer.getDB
        exp.delete()
        TestDBSetup.deleteDB(ctx, db)
        ctx.commitTransaction()
    }
}
