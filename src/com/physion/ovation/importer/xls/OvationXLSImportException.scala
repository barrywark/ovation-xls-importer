package com.physion.ovation.importer.xls

import ovation.OvationException

class OvationXLSImportException(msg: String, cause: Throwable) extends OvationException(msg, cause)
{
    def this(msg: String) = this(msg, null)
}
