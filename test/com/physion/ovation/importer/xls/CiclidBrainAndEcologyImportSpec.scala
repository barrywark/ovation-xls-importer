package com.physion.ovation.importer.xls

import org.specs2._
import execute.Result
import org.joda.time.{DateTime,DateTimeZone}
import java.io.FileInputStream
import scala.collection.JavaConversions._
import org.apache.log4j.{ConsoleAppender, Logger}
import org.apache.poi.xssf.usermodel.{XSSFSheet, XSSFRow, XSSFWorkbook}
import ovation._
import scala.collection.{Seq, Map}
import specification.Step
import java.net.URI
import scala.Predef._
import org.apache.poi.ss.usermodel.{Sheet, Row, Cell}
import com.google.common.collect.Iterables


object CiclidBrainAndEcologyFixture {
    val ANATOMY_XLSX_FIXTURE_PATH = "fixtures/Ciclid Brain and Social Data with Legend.xlsx"
    val ECOLOGY_XLSX_FIXTURE_PATH = "fixtures/Data Consilidation.xlsx"
}

class CiclidBrainAndEcologyFixture extends SpecificationWithJUnit with testconfig { def is =

    "Setting up the test fixture" ^ Step(setupDB) ^ Step(importData) ^
        new CiclidBrainAndEcologyImportSpec() ^
        Step(cleanupDB) ^
        end

    var dsc: DataStoreCoordinator = null

    var projectUri: URI = null

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

        projectUri = project.getURI
    }

    def importData {
        Ovation.getLogger.debug("Starting test data import")

        val ctx = dsc.getContext

        ctx.authenticateUser(username,password)

        importXLS(ctx)

        Ovation.getLogger.debug("Finished test data import")
    }

    def cleanupDB {
        Ovation.getLogger.debug("Cleaning up test database")

        val ctx = dsc.getContext
        ctx.authenticateUser(username,password)

        TestDBSetup.cleanupDB(ctx)
    }

    def importXLS(ctx: DataContext)
    {
        val project = ctx.objectWithURI(projectUri).asInstanceOf[Project]
        val exp = project.insertExperiment("xls-import-anatomy-test", new DateTime())
        val ecologyExp = project.insertExperiment("xls-import-ecology-test", new DateTime())

        // Import the XLS
        val xlsPath = CiclidBrainAndEcologyFixture.ANATOMY_XLSX_FIXTURE_PATH
        val ecologyXLSPath = CiclidBrainAndEcologyFixture.ECOLOGY_XLSX_FIXTURE_PATH
        new CiclidXlsImporter().importXLS(ctx,
                                          exp,
                                          new XSSFWorkbook(new FileInputStream(xlsPath)),
                                          DateTimeZone.forID("UTC+3"),
                                          ecologyExp,
                                          new XSSFWorkbook(new FileInputStream(ecologyXLSPath)),
                                          DateTimeZone.forID("UTC+3"))

    }
}

class CiclidBrainAndEcologyImportSpec extends SpecificationWithJUnit with testconfig { def is =

    "The Ciclid Brain and Ecology importer should" ^ Step(enableLogging) ^
        //"import genus=>species Source hierarchy" ! xls().speciesSourceHierarchy ^ //TODO re-enable
        "import site Source hierarchy" ! xls().siteSourceHierarchy ^
        "import a fish anatomy Source for each speices" ^
            "with Source parent hierarchy specifying genus and species" ! xls().fishSourceParent ^
            "with lake, run#, sex, source and museum owner properties" ! xls().fishOwnerProperties ^
        "import one EpochGroup('anatomy') per fish anatomy Source" ^
            "with one trial" ! xls().countEpochs ^
                "with one floating point Response('measurement name') with floatingPointData[0] the measurement value" ! xls().epochAnatomyResponses

    var workbook: XSSFWorkbook = null
    var dsc = DataStoreCoordinator.coordinatorWithConnectionFile(connectionFile)


    def enableLogging() {
        Ovation.enableLogging(LogLevel.INFO)
    }

    case class xls() extends testcontext {


        val expUri = getContext.query(classOf[Experiment], "").asInstanceOf[java.util.Iterator[Experiment]].next.getURI
        val anatomyWorkbook = new XSSFWorkbook(new FileInputStream(CiclidBrainAndEcologyFixture.ANATOMY_XLSX_FIXTURE_PATH))

        def siteSourceHierarchy = {
            println("siteSourceHierarchy")
            val ecologyWorkbook = new XSSFWorkbook(new FileInputStream(CiclidBrainAndEcologyFixture.ECOLOGY_XLSX_FIXTURE_PATH))

            val ctx = getContext
            val ecologySheet2003 = ecologyWorkbook.getSheet("2003")

            val siteRows = ecologySiteRows(ecologySheet2003)

            val sources = ctx.getSourcesWithLabel("ecology-site")

            sources.size must beEqualTo (siteRows.size)

        }

        def ecologySiteRows(sheet: Sheet) = {
            sheet.drop(1).map { row => siteRowOption(row) }
        }

        def siteRowOption(row: Row) = {
            val cell = row.getCell(0, Row.RETURN_BLANK_AS_NULL)
            cell match {
                case null => None
                case _ => Some(cell)
            }
        }

        def epochAnatomyResponses = {
            println("epochAnatomyResponses")
            val exp = getContext.objectWithURI(expUri).asInstanceOf[Experiment]

            val combinedSheet = anatomyWorkbook.getSheet("Combined")

            val epochs = exp.getEpochsIterable

            epochs.map { epoch =>
                {
                    val row = epoch.getEpochGroup.getSource.getOwnerProperty("_xls-row").asInstanceOf[Long].toInt
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
                        "body volume"->"?",
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

                    def checkMeasurement(e: Epoch, responseName: String, value: Option[Double]) = {
                        val r = e.getResponse(responseName)
                        value match {
                            case None => r must beNull
                            case Some(value) => {
                                val data = r.getFloatingPointData

                                ((data must not beNull) and
                                    (r.getUTI must beEqualTo(IResponseData.NUMERIC_DATA_UTI)) and
                                    //(r.getUnits must beEqualTo (expectedUnits(r.getExternalDevice.getName))) and //TODO re-enable
                                    (data must have size(1)) and
                                    (data(0) must beEqualTo(value)))
                            }
                        }

                    }

                    def getDoubleCellValue(cellNum: Int) = {
                        val cell = combinedSheet.getRow(row).getCell(cellNum, Row.RETURN_BLANK_AS_NULL)
                        cell match {
                            case null => None
                            case _ => cell.getCellType match {
                                case Cell.CELL_TYPE_NUMERIC => Some(cell.getNumericCellValue)
                                case Cell.CELL_TYPE_ERROR => None
                            }
                        }
                    }

                    measurements.map {
                        case (cellNum, name) => checkMeasurement(epoch, name, getDoubleCellValue(cellNum))
                    }
                    .reduce { (r1 ,r2) => r1 and r2 }
                }
            } reduce { (r1,r2) => r1 and r2 }

        }

        def countEpochs = {
            println("countEpochs")
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
            println("fishSourceParent")
            val combinedSheet = anatomyWorkbook.getSheet("Combined")

            val fish = getContext.getSourcesWithLabel("fish")

            def checkParents(src: Source): Result = {
                val species = src.getParent
                val genus = species.getParent

                val expectedGenus = combinedSheet.getRow(src.getOwnerProperty("_xls-row").asInstanceOf[Long].toInt).getCell(1).getStringCellValue.trim
                val expectedSpecies = combinedSheet.getRow(src.getOwnerProperty("_xls-row").asInstanceOf[Long].toInt).getCell(2).getStringCellValue.trim

                (genus.getOwnerProperty("genus-name") must beEqualTo (expectedGenus)) and
                (species.getOwnerProperty("species-name") must beEqualTo(expectedSpecies))
            }

            fish map { src => checkParents(src) } reduce { (r1,r2) => r1 and r2 }
        }

        def fishOwnerProperties = {
            println("fishOwnerProperties")
            val combinedSheet = anatomyWorkbook.getSheet("Combined")

            val fish = getContext.getSourcesWithLabel("fish")

            def checkSourceProperties(src: Source) = {
                val properties = Map[Int,String](3->"lake",
                                                 4->"run-number",
                                                 5->"sex",
                                                 6->"sample-origin",
                                                 7->"museum-animal-ID")

                properties.map
                { case (cellNumber,  prop) => {
                    checkOwnerProperty(src, prop, combinedSheet, src.getOwnerProperty("_xls-row").asInstanceOf[Long].toInt, cellNumber) }
                }
            }

            fish flatMap { src => checkSourceProperties(src) } reduce { (m1,m2) => m1 and m2 }
        }

        def checkOwnerProperty(entity: IEntityBase, prop: String, sheet: XSSFSheet, row: Int, cellNumber: Int) = {
            val expected = sheet.getRow(row).getCell(cellNumber, Row.RETURN_BLANK_AS_NULL)
            expected match {
                case null => entity.getOwnerProperty(prop) must beNull
                case _ => expected.getCellType match {
                    case Cell.CELL_TYPE_NUMERIC => entity.getOwnerProperty(prop) must beEqualTo(expected.getNumericCellValue)
                    case Cell.CELL_TYPE_STRING => entity.getOwnerProperty(prop) must beEqualTo(expected.getStringCellValue)
                }
            }
        }

        def speciesSourceHierarchy = {
            val ctx = getContext

            val combinedSheet = anatomyWorkbook.getSheet("Combined")


            //Collect (genus,species) tuples
            case class SourceInfo(genus: String,
                                  species: String){} //TODO should be genus=>species=>fish

            println("Collecting genus/species tuples (" + combinedSheet.getLastRowNum + " rows)")
            val genusSpeciesTuples = (1 to combinedSheet.getLastRowNum).map(rowNumber => {

                val row = combinedSheet.getRow(rowNumber)
                val genusCell = row.getCell(1, Row.RETURN_BLANK_AS_NULL)
                val speciesCell = row.getCell(2, Row.RETURN_BLANK_AS_NULL)

                val genus = genusCell.getStringCellValue.replace("'","").trim

                val species = speciesCell.getStringCellValue.replace("'","").trim

                SourceInfo(genus, species)
            })

            println("Verifying genus/species sources")
            genusSpeciesTuples.map((srcInfo) => {
                val (genus,species) = (srcInfo.genus, srcInfo.species)
                println("  " + genus + " " + species)
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
