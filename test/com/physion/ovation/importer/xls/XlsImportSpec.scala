package com.physion.ovation.importer.xls

import org.specs2.{SpecificationWithJUnit}


class XlsImportSpec extends SpecificationWithJUnit {
  def is =
    "The XLS importer should" ^
      "exist" ! success
}

class CiclidBrainAndEcologyImpoter extends SpecificationWithJUnit { def is =

  "The Ciclid Brain and Ecology importer should" ^
    "import xls" ! failure
}
