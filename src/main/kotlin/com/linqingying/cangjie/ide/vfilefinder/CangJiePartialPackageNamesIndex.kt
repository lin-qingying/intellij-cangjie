package com.linqingying.cangjie.ide.vfilefinder


import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.lang.declarations.CangJieDeclarationsFileType
import com.linqingying.cangjie.lang.declarations.CjDeclarationsFile
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.parentOrNull
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.utils.safeAs
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import java.io.DataInput
import java.io.DataOutput

private val LOG = logger<CangJiePartialPackageNamesIndex>()
val NAME: ID<FqName, Name?> = ID.create(CangJiePartialPackageNamesIndex::class.java.canonicalName)

data class NameIsRoot(
    val name: Name,
    val isRoot: Boolean
) {
    fun asString() = "$name:$isRoot"
}

class CangJiePartialPackageNamesIndex : FileBasedIndexExtension<FqName, Name?>() {


    private object NullableNameExternalizer : DataExternalizer<Name?> {
        override fun save(out: DataOutput, value: Name?) {
            out.writeBoolean(value == null)
            if (value != null) {
                IOUtil.writeUTF(out, value.asString())
            }
        }

        override fun read(input: DataInput): Name? =
            if (input.readBoolean()) null else Name.guessByFirstCharacter(IOUtil.readUTF(input))


        fun guessByFirstCharacter(s: String): NameIsRoot {
            val (name, isRoot) = s.split(":")

            return NameIsRoot(Name.guessByFirstCharacter(name), isRoot.toBoolean())

        }
    }


    override fun getName() = NAME

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = FqNameKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<Name?> = NullableNameExternalizer

    override fun getInputFilter(): DefaultFileTypeSpecificInputFilter =
        DefaultFileTypeSpecificInputFilter(
//            JavaClassFileType.INSTANCE,
            CangJieFileType.INSTANCE,
            CangJieDeclarationsFileType

//            CangJieJavaScriptMetaFileType,

//            KlibMetaFileType,
        )

    override fun getVersion() = 3

    override fun traceKeyHashToVirtualFileMapping(): Boolean = true
    private fun FileContent.getBuiltInFilePackage(): FqName? {
        assert(this.fileType == CangJieDeclarationsFileType)
        val virtualFile =
            FileTypeIndex.getFiles(fileType, GlobalSearchScope.projectScope(project)).firstOrNull()
        val psiFIle = virtualFile?.let { PsiManager.getInstance(project).findFile(it) }
        return psiFIle.safeAs<CjDeclarationsFile>()?.packageFqName
    }

    private fun FileContent.toPackageFqName(): FqName? =
        when (this.fileType) {
            CangJieFileType.INSTANCE -> this.psiFile.safeAs<CjFile>()?.packageFqName

            CangJieDeclarationsFileType ->/* this.getBuiltInFilePackage()*/this.psiFile.safeAs<CjDeclarationsFile>()?.packageFqName

            else -> null
        }


    override fun getIndexer() = DataIndexer<FqName, Name?, FileContent> { fileContent ->
        try {
            val packageFqName = fileContent.toPackageFqName() ?: return@DataIndexer emptyMap<FqName, Name?>()
//
//            generateSequence(packageFqName) {
//                it.parentOrNull()
//            }.filterNot { it.isRoot }.associateBy({ it.parent() }, { it.shortName() }) + mapOf(packageFqName to null)
//
            val a = generateSequence(packageFqName) {
                it.parentOrNull()
            }
            val b = a.filterNot { it.isRoot }
            val c = b.associateBy({ it.parent() }, {
//                NameIsRoot(
                it.shortName().apply {
                    isRoot = it.parent().isRoot
                }
//                ,
//                    it.parent().isRoot
//                )
            })
            val d = c + mapOf(packageFqName to null)
            d

        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            LOG.warn("Error `(${e.javaClass.simpleName}: ${e.message})` while indexing file ${fileContent.fileName} using $name index. Probably the file is broken.")
            emptyMap()
        }
    }
}

