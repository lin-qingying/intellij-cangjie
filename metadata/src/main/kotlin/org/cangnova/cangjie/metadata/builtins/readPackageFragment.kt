package org.cangnova.cangjie.metadata.builtins

import org.cangnova.cangjie.metadata.model.Package
import org.cangnova.cangjie.metadata.model.parser.toPackage
import java.io.InputStream

fun InputStream.readBuiltinsPackageFragment(): Pair<Package?, BuiltInsBinaryVersion> =
    use { stream ->
        val `package` = stream.toPackage()

        val version = `package`.cjoVersion
        `package` to version
    }
