package com.physion.ovation.importer.xls

import org.specs2._
import org.joda.time.DateTime
import java.io.FileInputStream
import scala.collection.JavaConversions._
import org.apache.poi.xssf.usermodel.{XSSFRow, XSSFWorkbook}
import org.apache.poi.ss.usermodel.{Row,Cell}
import ovation.{Project, DataContext}
import org.apache.log4j.{ConsoleAppender, Logger}

class CiclidBrainAndEcologyImportSpec extends SpecificationWithJUnit with ovdbinit { def is =

    //sequential ^
    "The Ciclid Brain and Ecology importer should" ^
      "import genus=>species Source hierarchy" ! xls().speciesSourceHierarchy ^
      "import site Source hierarchy" ! pending


    case class xls() {


        def importXLS(ctx: DataContext) =
        {
            val project = ctx.objectWithUUID(projectUUID).asInstanceOf[Project]
            val exp = project.insertExperiment("xls-import-test", new DateTime())

            // Import the XLS
            val xlsPath = "fixtures/Ciclid Brain and Social Data with Legend.xlsx"
            val importer = new CiclidXlsImporter().importXLS(ctx, xlsPath)

            (exp, new XSSFWorkbook(new FileInputStream(xlsPath)))
        }

        def speciesSourceHierarchy = {
            val ctx = getContext
            val (exp, workbook) = importXLS(ctx)
            val combinedSheet = workbook.getSheet("Combined")


            //Collect (genus,species) tuples
            case class SourceInfo(genus: String,
                                  species: String,
                                  lake: Option[String],
                                  run: Option[Double]){} //TODO should be genus=>species=>fish

            println("Collecting genus/species tuples (" + combinedSheet.getLastRowNum + " rows)")
            val genusSpeciesTuples = (1 to combinedSheet.getLastRowNum).map(rowNumber => {

                val row = combinedSheet.getRow(rowNumber)
                val genusCell = row.getCell(1, Row.RETURN_BLANK_AS_NULL)
                val speciesCell = row.getCell(2, Row.RETURN_BLANK_AS_NULL)

                val genus = genusCell.getStringCellValue.replace("'","")

                val species = speciesCell.getStringCellValue.replace("'","")

                val lake = row.getCell(3, Row.RETURN_BLANK_AS_NULL).getStringCellValue match {
                    case "" => None
                    case _ => Some(_)
                }

                val run = row.getCell(3, Row.RETURN_BLANK_AS_NULL) match {
                    case null => None
                    case c: Cell => Some(c.getNumericCellValue)
                }

                SourceInfo(genus, species, lake, run)
            })

            println("Verifying genus/species sources")
            genusSpeciesTuples.map((srcInfo) => {
                val (genus,species) = (srcInfo.genus, srcInfo.species)
                val lake = srcInfo.lake
                val genusSources = ctx.getSourcesWithLabel(genus)
                val speciesSources = genusSources.flatMap((gs) => gs.getSourcesWithLabel(species))

                Seq(speciesSources must have size(1),
                  speciesSources must have((src) => src.getOwnerProperty("species-name").equals(species)),
                  speciesSources must have((src) => src.getParent.getOwnerProperty("genus-name").equals(genus)),
                  speciesSources must have((src) => (lake.isEmpty || src.getOwnerProperty("lake").equals(lake.get)),
                  speciesSources must have((src) => (src.getOwnerProperty("run") == null || src.getOwnerProperty("run") == srcInfo.run.get))
                  ).reduce((m1,m2) => m1 and m2)

            })

        }
    }

}
