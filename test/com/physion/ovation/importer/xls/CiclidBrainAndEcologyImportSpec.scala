package com.physion.ovation.importer.xls

import org.specs2._
import execute.Result
import org.joda.time.DateTime
import java.io.FileInputStream
import scala.collection.JavaConversions._
import org.apache.poi.ss.usermodel.{Row,Cell}
import org.apache.log4j.{ConsoleAppender, Logger}
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFRow, XSSFWorkbook}
import ovation._
import scala.collection.{Seq, Map}
import specification.Step
import java.net.URI

class CiclidBrainAndEcologyImportSpec extends SpecificationWithJUnit with dbconfig { def is =


    "The Ciclid Brain and Ecology importer should" ^ Step(setupDB) ^ Step(importData)
        "import genus=>species Source hierarchy" ! xls().speciesSourceHierarchy ^
        "import site Source hierarchy" ! failure ^
        "import a fish Source" ^
            "with Source parent hierarchy specifying genus and species" ! xls().fishSourceParent ^
            "with lake, run#, sex, source and museum owner properties" ! xls().fishOwnerProperties ^
        "import one EpochGroup('anatomy') per fish Source" ! failure ^
            "with one trial" ! xls().countEpochs ^
                "with one floating point Response('measurement name') with floatingPointData[0] the measurement value" ! xls().epochAnatomyResponses ^
    Step(cleanupDB) ^
    end

    //TODO strip string values in genus/species/etc.

    var expUri: URI = null
    var workbook: XSSFWorkbook = null
    var projectUUID: String = null
    var dsc: DataStoreCoordinator = null

    val ANATOMY_XLSX_FIXTURE_PATH = "fixtures/Ciclid Brain and Social Data with Legend.xlsx"

    def setupDB {
        Ovation.enableLogging(LogLevel.DEBUG)
        Ovation.getLogger.debug("Setting up test database...")

        TestDBSetup.setupTestDB(connectionFile, institution_name, lab_name, licenseCode, username, password)
        dsc = DataStoreCoordinator.coordinatorWithConnectionFile(connectionFile)

        val ctx = dsc.getContext
        ctx.authenticateUser(username,password)


        val project = ctx.getProjects("test-project")
            .headOption
            .getOrElse(ctx.insertProject("test-project", "test-project", new DateTime()))

        projectUUID = project.getUuid
    }

    def importXLS(ctx: DataContext) =
    {
        val project = ctx.objectWithUUID(projectUUID).asInstanceOf[Project]
        val exp = project.insertExperiment("xls-import-test", new DateTime())

        // Import the XLS
        val xlsPath = ANATOMY_XLSX_FIXTURE_PATH
        new CiclidXlsImporter().importXLS(ctx, exp, xlsPath)

        (exp.getURI, new XSSFWorkbook(new FileInputStream(xlsPath)))
    }

    def importData {
        Ovation.getLogger.debug("Starting test data import")

        val ctx = dsc.getContext

        ctx.authenticateUser(username,password)

        val (uri, wb) = importXLS(ctx)
        expUri = uri
        workbook = wb

        Ovation.getLogger.debug("Finished test data import")
    }

    def cleanupDB {
        Ovation.getLogger.debug("Cleaning up test database")

        val ctx = dsc.getContext
        ctx.authenticateUser(username,password)

        TestDBSetup.cleanupDB(ctx)
    }


    case class xls() extends ovdbinit {


        def epochAnatomyResponses = {
            val exp = getContext.objectWithURI(expUri).asInstanceOf[Experiment]

            val combinedSheet = workbook.getSheet("Combined")

            val epochs = exp.getEpochsIterable

            epochs.map { epoch =>
                {
                    val row = epoch.getEpochGroup.getSource.getOwnerProperty("_xls-row").asInstanceOf[Int]
                    val measurements = Map[Int,String](8->"body volume",
                        9->"body length",
                        10->"gonad weight",
                        11->"GSI",
                        12->"brain length",
                        13->"brain mass",
                        14->"brain size",
                        15->"olfactory nerve", //TODO measurement, units?
                        16->"olfactory bulb", //TODO measurement, units?
                        17->"telencephalon", //TODO measurement, units?
                        18->"optic tectum", //TODO measurement, units?
                        19->"hypothalamus", //TODO measurement, units?
                        20->"inferior hypothalamus", //TODO measurement, units?
                        21->"hypophysis", //TODO meas/units?
                        22->"cerebellum", //TODO meas/units?
                        23->"dorsal medulla"//TODO meas/units?
                    )

                    val expectedUnits = Map[String,String](
                        "body length"->"?",
                        "gonad weight"->"?",
                        "GSI"->"?",
                        "brain length"->"?",
                        "brain mass"->"?",
                        "brain size"->"?",
                        "olfactory nerve"->"?",
                        "olfactory bulb"->"?",
                        "telencephalon"->"?",
                        "optic tectum"->"?",
                        "hypothalamus"->"?",
                        "inferior hypothalamus"->"?",
                        "hypophysis"->"?",
                        "cerebellum"->"?",
                        "dorsal medulla"->"?"
                    )

                    def checkMeasurement(e: Epoch, responseName: String, value: Double) = {
                        val r = e.getResponse(responseName)
                        val data = r.getFloatingPointData

                        (r.getUTI must beEqualTo(IResponseData.NUMERIC_DATA_UTI)) and
                            (r.getUnits must beEqualTo (expectedUnits(r.getExternalDevice.getName))) and
                            (data must have size(1)) and
                            (data(0) must beEqualTo(value))
                    }

                    def getDoubleCellValue(cellNum: Int) = combinedSheet.getRow(row).getCell(cellNum).getNumericCellValue

                    measurements.map {
                        case (cellNum, name) => checkMeasurement(epoch, name, getDoubleCellValue(cellNum))
                    }
                    .reduce { (r1 ,r2) => r1 and r2 }
                }
            } reduce { (r1,r2) => r1 and r2 }

        }

        def countEpochs = {
            val exp = getContext.objectWithURI(expUri).asInstanceOf[Experiment]
            val epochGroups: Iterable[EpochGroup] = exp.getEpochGroupsWithLabel("anatomy")

            def checkSize(itr: Iterable[Epoch]): Result = {
                itr must have size(1)
            }
            epochGroups
                .map(eg => checkSize(eg.getEpochsIterable))
                .reduce { (r1,r2) => r1 and r2 }
        }

        def fishSourceParent = {
            val combinedSheet = workbook.getSheet("Combined")

            val fish = getContext.getSourcesWithLabel("fish")

            def checkParents(src: Source): Result = {
                val species = src.getParent
                val genus = species.getParent

                val expectedGenus = combinedSheet.getRow(src.getOwnerProperty("_xls-row").asInstanceOf[Int]).getCell(1).getStringCellValue
                val expectedSpecies = combinedSheet.getRow(src.getOwnerProperty("_xls-row").asInstanceOf[Int]).getCell(2).getStringCellValue

                (genus.getOwnerProperty("genus-name") must beEqualTo (expectedGenus)) and
                (species.getOwnerProperty("genus-name") must beEqualTo(expectedSpecies))
            }

            fish map { src => checkParents(src) } reduce { (r1,r2) => r1 and r2 }
        }

        def fishOwnerProperties = {
            val combinedSheet = workbook.getSheet("Combined")

            val fish = getContext.getSourcesWithLabel("fish")

            def checkSourceProperties(src: Source) = {
                val properties = Map[Int,String](3->"lake", 4->"run#", 5->"sex", 6->"sample-origin", 7->"museum")

                properties.map
                { case (cellNumber,  prop) => {
                    checkOwnerProperty(src, prop, combinedSheet, src.getOwnerProperty("_xls-row").asInstanceOf[Int], cellNumber) }
                }
            }

            fish flatMap { src => checkSourceProperties(src) } reduce { (m1,m2) => m1 and m2 }
        }

        def checkOwnerProperty(entity: IEntityBase, prop: String, sheet: XSSFSheet, row: Int, cellNumber: Int) = {
            val expected = sheet.getRow(row).getCell(cellNumber)
            expected match {
                case null => entity.getOwnerProperty(prop) must beNull
                case _ => entity.getOwnerProperty(prop) must beEqualTo(expected)
            }
        }

        def speciesSourceHierarchy = {
            val ctx = getContext

            val combinedSheet = workbook.getSheet("Combined")


            //Collect (genus,species) tuples
            case class SourceInfo(genus: String,
                                  species: String){} //TODO should be genus=>species=>fish

            println("Collecting genus/species tuples (" + combinedSheet.getLastRowNum + " rows)")
            val genusSpeciesTuples = (1 to combinedSheet.getLastRowNum).map(rowNumber => {

                val row = combinedSheet.getRow(rowNumber)
                val genusCell = row.getCell(1, Row.RETURN_BLANK_AS_NULL)
                val speciesCell = row.getCell(2, Row.RETURN_BLANK_AS_NULL)

                val genus = genusCell.getStringCellValue.replace("'","")

                val species = speciesCell.getStringCellValue.replace("'","")

                SourceInfo(genus, species)
            })

            println("Verifying genus/species sources")
            genusSpeciesTuples.map((srcInfo) => {
                val (genus,species) = (srcInfo.genus, srcInfo.species)
                val genusSources = ctx.getSourcesWithLabel("genus").filter( src => src.getOwnerProperty("genus-name").equals(genus) )
                val speciesSources = genusSources.flatMap((gs) => gs.getSourcesWithLabel("species").filter( src => src.getOwnerProperty("species-name").equals(species) ))

                val matchers = Seq(speciesSources must have size(1),
                    speciesSources must have((src) => src.getOwnerProperty("species-name").equals(species)),
                    speciesSources must have((src) => src.getParent.getOwnerProperty("genus-name").equals(genus)))

                matchers.reduce( (m1,m2) => m1 and m2 )
            })

        }
    }
}
