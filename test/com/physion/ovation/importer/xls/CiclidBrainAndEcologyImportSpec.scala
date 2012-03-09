package com.physion.ovation.importer.xls

import org.specs2._

class CiclidBrainAndEcologyImportSpec extends SpecificationWithJUnit { def is =

    "The Ciclid Brain and Ecology importer should" ^
        "import import source hierarchy" ^
            "genus and species for anatomy data" ! xls().speciesSources ^
            "site for ecology data" ! failure
}


case class xls() extends ovdbinit() {

    // this is equivalent to def speciesSources = this.apply {...}
    def speciesSources = { 1 must_== 1 }

}
