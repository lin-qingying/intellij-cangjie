package com.linqingying.cangjie.ide.vfilefinder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ID
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.metadata.CangJieBinaryClassCache
import com.linqingying.cangjie.metadata.decompiler.CangJieClassFinder
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.serialization.CangJieMetadataVersion
import org.jetbrains.annotations.TestOnly
import java.io.FileNotFoundException
import java.io.InputStream


abstract class VirtualFileFinder : CangJieClassFinder {
    abstract fun findVirtualFileWithHeader(classId: ClassId): VirtualFile?

    abstract fun findSourceOrBinaryVirtualFile(classId: ClassId): VirtualFile?

    override fun findCangJieClassOrContent(classId: ClassId, metadataVersion: CangJieMetadataVersion): CangJieClassFinder.Result? {
        val file = findVirtualFileWithHeader(classId) ?: return null
        return CangJieBinaryClassCache.getCangJieBinaryClassOrClassFileContent(file, metadataVersion)
    }

//    override fun findCangJieClassOrContent(javaClass: JavaClass, jvmMetadataVersion: CangJieMetadataVersion): CangJieClassFinder.Result? {
//        var file = (javaClass as? VirtualFileBoundJavaClass)?.virtualFile ?: return null
//
//        if (javaClass.outerClass != null) {
//            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
//            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
//            file = file.parent!!.findChild(classFileName(javaClass) + ".class").sure { "Virtual file not found for $javaClass" }
//        }
//
//        return CangJieBinaryClassCache.getCangJieBinaryClassOrClassFileContent(file, jvmMetadataVersion)
//    }

//    private fun classFileName(jClass: JavaClass): String {
//        val simpleName = jClass.name.asString()
//        val outerClass = jClass.outerClass ?: return simpleName
//        return classFileName(outerClass) + "$" + simpleName
//    }

    companion object SERVICE {
        fun getInstance(project: Project, module: ModuleDescriptor): VirtualFileFinder =
            VirtualFileFinderFactory.getInstance(project).create(project, module)

        @TestOnly
        fun getInstance(project: Project): VirtualFileFinder =
            VirtualFileFinderFactory.getInstance(project).create(GlobalSearchScope.allScope(project))
    }
}


class IdeVirtualFileFinder(private val scope: GlobalSearchScope) : VirtualFileFinder() {
    override fun findMetadata(classId: ClassId): InputStream? {
        val file = findVirtualFileWithHeader(classId.asSingleFqName(), CangJieMetadataFileIndex.NAME)?.takeIf { it.exists() } ?: return null

        return try {
            file.inputStream
        } catch (e: FileNotFoundException) {
            null
        }
    }

    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? = null

    override fun hasMetadataPackage(fqName: FqName): Boolean = hasSomethingInPackage(CangJieMetadataFilePackageIndex.NAME, fqName, scope)

    override fun findBuiltInsData(packageFqName: FqName): InputStream? =
        findVirtualFileWithHeader(packageFqName, CangJieBuiltInsMetadataIndex.NAME)?.inputStream

    override fun findSourceOrBinaryVirtualFile(classId: ClassId) = findVirtualFileWithHeader(classId)

    init {
        if (scope != GlobalSearchScope.EMPTY_SCOPE && scope.project == null) {
            LOG.warn("Scope with null project $scope")
        }
    }

    override fun findVirtualFileWithHeader(classId: ClassId): VirtualFile? = null
//        findVirtualFileWithHeader(classId.asSingleFqName(), CangJieClassFileIndex.NAME)

    private fun findVirtualFileWithHeader(fqName: FqName, key: ID<FqName, Void>): VirtualFile? {
        val iterator = FileBasedIndex.getInstance().getContainingFilesIterator(key, fqName, scope)
        return if (iterator.hasNext()) {
            iterator.next()
        } else {
            null
        }
    }

    companion object {
        private val LOG = Logger.getInstance(IdeVirtualFileFinder::class.java)
    }
}
