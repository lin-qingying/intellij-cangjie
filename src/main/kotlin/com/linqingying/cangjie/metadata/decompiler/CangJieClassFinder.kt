package com.linqingying.cangjie.metadata.decompiler

import com.intellij.openapi.vfs.VirtualFile
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.serialization.CangJieMetadataVersion
import java.io.InputStream


interface CangJieClassFinder : CangJieMetadataFinder {
    fun findCangJieClassOrContent(classId: ClassId, metadataVersion: CangJieMetadataVersion): Result?

//    fun findCangJieClassOrContent(cangjieClass:  CangJieClass, jvmMetadataVersion: CangJieMetadataVersion): Result?

    sealed class Result {
//        fun toCangJieBinaryClass(): CangJieBinaryClass? = (this as? CangJieClass)?.kotlinJvmBinaryClass

//        class CangJieClass(val kotlinJvmBinaryClass: CangJieBinaryClass, val byteContent: ByteArray? = null) : Result() {
//            operator fun component1(): CangJieBinaryClass = kotlinJvmBinaryClass
//            operator fun component2(): ByteArray? = byteContent
//        }

        class ClassFileContent(val content: ByteArray) : Result()
    }
}
class DirectoryBasedClassFinder(
    val packageDirectory: VirtualFile,
    val directoryPackageFqName: FqName
) : CangJieClassFinder {
    override fun findCangJieClassOrContent(
        classId: ClassId,
         metadataVersion: CangJieMetadataVersion
    ): CangJieClassFinder.Result? {
//        if (classId.packageFqName != directoryPackageFqName) {
//            return null
//        }
//        val targetName = classId.relativeClassName.pathSegments().joinToString("$", postfix = ".class")
//        val virtualFile = packageDirectory.findChild(targetName)
//        if (virtualFile != null && isCangJieWithCompatibleAbiVersion(virtualFile, jvmMetadataVersion)) {
//            return ClsCangJieBinaryClassCache.getInstance().getKotlinBinaryClass(virtualFile)?.let(::KotlinClass)
//        }
        return null
    }


    // TODO
    override fun findMetadata(classId: ClassId): InputStream? = null

    // TODO
    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? = null

    // TODO
    override fun hasMetadataPackage(fqName: FqName): Boolean = false

    // TODO: load built-ins from packageDirectory?
    override fun findBuiltInsData(packageFqName: FqName): InputStream? = null
}
