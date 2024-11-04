package com.linqingying.cangjie.metadata

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.NameUtils
import com.linqingying.cangjie.psi.CjFile
import org.jetbrains.annotations.TestOnly


object PackagePartClassUtils {
    @JvmStatic
    fun getPathHashCode(file: VirtualFile): Int = file.path.lowercase().hashCode()

    private const val PART_CLASS_NAME_SUFFIX = "Cj"

    @JvmStatic
    private fun decapitalizeAsJavaClassName(str: String): String =
    // NB use Locale.ENGLISH so that build is locale-independent.
        // See Javadoc on java.lang.String.toUpperCase() for more details.
        when {
            Character.isJavaIdentifierStart(str[0]) -> str.substring(0, 1).lowercase() + str.substring(1)
            str[0] == '_' -> str.substring(1)
            else -> str
        }

    @TestOnly
    @JvmStatic
    fun getDefaultPartFqName(facadeClassFqName: FqName, file: VirtualFile): FqName =
        getPackagePartFqName(facadeClassFqName.parent(), file.name)

    @JvmStatic
    fun getPackagePartFqName(packageFqName: FqName, fileName: String): FqName {
        val partClassName = getFilePartShortName(fileName)
        return packageFqName.child(Name.identifier(partClassName))
    }

    @JvmStatic
    fun getFilesWithCallables(files: Collection<CjFile>): List<CjFile> =
        files.filter { it.hasTopLevelCallables() }

    @JvmStatic
    fun getFilePartShortName(fileName: String): String =
        NameUtils.getPackagePartClassNamePrefix(FileUtil.getNameWithoutExtension(fileName)) + PART_CLASS_NAME_SUFFIX

    @JvmStatic
    fun getFileNameByFacadeName(facadeClassName: String): String? {
        if (!facadeClassName.endsWith(PART_CLASS_NAME_SUFFIX)) return null
        val baseName = facadeClassName.substring(0, facadeClassName.length - PART_CLASS_NAME_SUFFIX.length)
        if (baseName == "_") return null
        return "${decapitalizeAsJavaClassName(baseName)}.${CangJieFileType.EXTENSION}"
    }
}
