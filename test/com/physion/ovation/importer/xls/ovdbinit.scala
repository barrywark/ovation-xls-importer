package com.physion.ovation.importer.xls

import org.specs2._
import com.objy.db.app.ooDBObj
import org.apache.log4j.Level
import ovation.{DataStoreCoordinator, Ovation}
import com.google.common.collect.{Iterators, Sets}

/**
 * Created by IntelliJ IDEA.
 * User: barry
 * Date: 3/9/12
 * Time: 11:45 AM
 * To change this template use File | Settings | File Templates.
 */

case class ovdbinit() extends specification.After {
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

    val connectionFile = System.getProperty("OVATION_DEV_FD_BOOTFILE_PATH")

    Ovation.getLogger.setLevel(Level.DEBUG)

    val dsc = DataStoreCoordinator.coordinatorWithConnectionFile(connectionFile)

    val ctx = dsc.getContext
    ctx.authenticateUser(username,password)


    def after = {
        ctx.beginTransaction()
        try {
            val itr: java.util.Iterator[ooDBObj] = ctx.getSession().getFD().containedDBs().asInstanceOf[java.util.Iterator[ooDBObj]];

            val toDelete: Array[ooDBObj] = Iterators.toArray[ooDBObj](itr, classOf[ooDBObj])
            val keepers = Set("OVProjects", "OVDBUsersAndGroups")
            toDelete.map((db) => if (!keepers.contains(db.getName)) { db.delete() } )
        }
        finally {
            ctx.commitTransaction()
        }
    }
}
