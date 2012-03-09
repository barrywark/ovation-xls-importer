package com.physion.ovation.importer.xls

import org.specs2._

class CiclidBrainAndEcologyImportSpec extends mutable.SpecificationWithJUnit {

    "The Ciclid Brain and Ecology importer" should {

        sequential

        "import genus=>species Source hierarchy" in new xls {
            ctx.currentAuthenticatedUser() must not beNull
        }

        "import site Source hierarchy" in new xls {
            ctx.currentAuthenticatedUser() must beNull
        }
    }
}


trait xls extends ovdbinit {

    // Import the XLS
    val importer = XLSImporter().importXLS(ctx, xlsPath)
}
