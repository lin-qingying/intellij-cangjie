package org.cangnova.cangjie.metadata.builtins

import org.cangnova.cangjie.metadata.model.fb.FbPackage
import org.cangnova.cangjie.metadata.model.fb.parser.toFbPackage
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import java.io.InputStream

fun InputStream.readBuiltinsPackageFragment(): Pair<PackageWrapper?, BuiltInsBinaryVersion> =
    use { stream ->
        val `package` = stream.toFbPackage().packageWrapper

        val version = `package`.cjoVersion
        `package` to version
    }
