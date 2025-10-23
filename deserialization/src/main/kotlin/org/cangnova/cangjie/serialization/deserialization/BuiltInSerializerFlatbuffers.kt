package org.cangnova.cangjie.serialization.deserialization

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import org.cangnova.cangjie.utils.getArchName
import org.cangnova.cangjie.utils.getOsName
import java.io.File

/**
 * 内置序列化器（Flatbuffers格式）
 *
 * 提供对CangJie标准库和内置类型的序列化数据访问
 */

object BuiltInSerializerFlatbuffers : SerializerExtensionFlatbuffers() {

    /**
     * 获取SDK模块路径前缀
     *
     * @return 模块路径前缀，如果SDK未配置则返回null
     */
    private fun getPrefix(sdk: CjSdk? = null): String? {


        val modulePath =
            sdk?.stdlibPath ?: CjSdkRegistry.getInstance().getAllSdks().firstOrNull()?.stdlibPath ?: return null


        // 根据系统类型获取不同的路径
        val arch = getArchName()
        val osName = getOsName()
        return modulePath.resolve("${osName}_${arch}_llvm").toString() + File.separator
    }

    /**
     * 获取内置类型的序列化文件路径
     *
     * @param fqName 完全限定名
     * @return 序列化文件路径，如果SDK未配置则返回null
     */
    fun getBuiltInsFilePath(fqName: FqName, sdk: CjSdk? ): String? {
        val prefix = getPrefix(sdk) ?: return null
        return prefix + fqName.parent().asString() + File.separator + getBuiltInsFileName(fqName)
    }

    /**
     * 获取内置类型的序列化文件名
     *
     * @param fqName 完全限定名
     * @return 序列化文件名
     */
    fun getBuiltInsFileName(fqName: FqName): String =
        fqName.asString() + DOT_DEFAULT_EXTENSION


    const val BUILTINS_FILE_EXTENSION = "cjo"
    const val DOT_DEFAULT_EXTENSION = ".$BUILTINS_FILE_EXTENSION"


}