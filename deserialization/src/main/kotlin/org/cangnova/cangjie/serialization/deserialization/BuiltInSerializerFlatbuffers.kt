package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.state.ToolchainSettingsState
import org.cangnova.cangjie.utils.getArchName
import org.cangnova.cangjie.utils.getOsName
import java.io.File
import kotlin.io.path.Path

object BuiltInSerializerFlatbuffers : SerializerExtensionFlatbuffers() {
    const val BUILTINS_FILE_EXTENSION = "cjo"
    const val DOT_DEFAULT_EXTENSION = ".$BUILTINS_FILE_EXTENSION"


    private fun getPrefix(): String {
        val sdkPath = Path(ToolchainSettingsState.getInstance().path)
        val modulePath = sdkPath.resolve("modules")

//        根据系统类型获取不同的路径
        val arch = getArchName()
        val osName = getOsName()
        return modulePath.resolve("${osName}_${arch}_llvm").toString() + File.separator

    }

    fun getBuiltInsFilePath(fqName: FqName): String {

//        前缀路径  系统 sdk
        val prefix = getPrefix()

        return prefix + fqName.parent().asString() + File.separator + getBuiltInsFileName(fqName)


    }

    fun getBuiltInsFileName(fqName: FqName): String =
        fqName.asString() + DOT_DEFAULT_EXTENSION

}