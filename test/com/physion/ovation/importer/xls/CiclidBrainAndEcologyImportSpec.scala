package com.physion.ovation.importer.xls

import org.specs2._
import org.joda.time.DateTime
import ovation.TestDBSetup

class CiclidBrainAndEcologyImportSpec extends SpecificationWithJUnit { def is =

    "The Ciclid Brain and Ecology importer should" ^
      "import genus=>species Source hierarchy" ! xls().speciesSourceHierarchy ^
      "import site Source hierarchy" ! xls().siteSourceHierarchy

    case class xls() extends ovdbinit {

        val exp = project.insertExperiment("xls-import-test", new DateTime())

        // Import the XLS
        val xlsPath = "fixtures/Ciclid Brain and Social Data with Legend.xlsx"
        val importer = new CiclidXlsImporter().importXLS(ctx, xlsPath)

        def speciesSourceHierarchy = {
            ctx.currentAuthenticatedUser() must not beNull
        }

        def siteSourceHierarchy = {
            ctx.currentAuthenticatedUser() must not beNull
        }
    }

}
