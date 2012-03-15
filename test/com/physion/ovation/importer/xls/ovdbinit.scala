package com.physion.ovation.importer.xls

import org.specs2._
import org.joda.time.DateTime
import scala.collection.JavaConversions._
import ovation.{LogLevel, TestDBSetup, DataStoreCoordinator, Ovation}

trait dbconfig {
    val lab_name = "Lab";
    val institution_name = "Institution";
    val licenseCode = "QLbehF8zl4iCyCeRDzjo2s2/hynkX18TraxunvijO4aa0cZw4L5IVWO0PwVOk8cD\n" +
        "i2W6KLGhowvy1CoME+3CgHCvFgqE5bdjLWbqLdEt53PqnRClWKd+XIsdIQrEwd98\n" +
        "Yp5IR+Ctt1QN6BhNxFJURZEkAC7a7HDNc0wJw4hgKWq5hQCnrK16xCIb8ywKHuQA\n" +
        "XnB50d0QwSCrOzia3s//DMMfCORHlXE8gBQLLqUTLv/FF+BLC7v61FZ3J3qJ6jk2\n" +
        "13sIBtPvvHAdYG38966JlAm2vE5LdHMQBpyLY9FwsBBV4iS6GM1plOXZBIaLPv5f\n" +
        "CFcoFW2Dv4Lk3OjVTB6f4g==";
    val OBJY_TEST_DEFAULT_ENCRYPTION_KEY_ID = "Institution::Lab::Ovation";
    val username = "user"
    val password = "test"

    val connectionFile = System.getProperty("OVATION_TEST_FD_PATH")
}

trait ovdbinit extends dbconfig {

    val dsc = DataStoreCoordinator.coordinatorWithConnectionFile(connectionFile)


    def getContext = {
        val ctx = dsc.getContext
        ctx.authenticateUser(username, password)
        ctx
    }
}
