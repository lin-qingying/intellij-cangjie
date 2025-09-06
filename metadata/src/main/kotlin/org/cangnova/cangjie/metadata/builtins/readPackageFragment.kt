package org.cangnova.cangjie.metadata.builtins

import org.cangnova.cangjie.metadata.model.fb.FbPackage
import org.cangnova.cangjie.metadata.model.fb.parser.toFbPackage
import java.io.InputStream

fun InputStream.readBuiltinsPackageFragment(): Pair<FbPackage?, BuiltInsBinaryVersion> =
    use { stream ->
        val `package` = stream.toFbPackage()

        val version = `package`.cjoVersion
        `package` to version
    }
