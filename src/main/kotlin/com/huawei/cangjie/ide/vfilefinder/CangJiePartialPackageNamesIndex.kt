package com.huawei.cangjie.ide.vfilefinder


import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.parentOrNull
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.utils.safeAs
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import java.io.DataInput
import java.io.DataOutput

private val LOG = logger<CangJiePartialPackageNamesIndex>()
val NAME: ID<FqName, Name?> = ID.create(CangJiePartialPackageNamesIndex::class.java.canonicalName)

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
    }

    override fun getName() = NAME

    override fun dependsOnFileContent() = true

    override fun getKeyDescriptor() = FqNameKeyDescriptor

    override fun getValueExternalizer(): DataExternalizer<Name?> = NullableNameExternalizer

    override fun getInputFilter(): DefaultFileTypeSpecificInputFilter =
        DefaultFileTypeSpecificInputFilter(
//            JavaClassFileType.INSTANCE,
            CangJieFileType,
//            CangJieJavaScriptMetaFileType,
//            CangJieBuiltInFileType,
//            KlibMetaFileType,
        )

    override fun getVersion() = 1

    override fun traceKeyHashToVirtualFileMapping(): Boolean = true

    private fun FileContent.toPackageFqName(): FqName? =
        when (this.fileType) {
            CangJieFileType -> this.psiFile.safeAs<CjFile>()?.packageFqName
//            JavaClassFileType.INSTANCE -> ClsCangJieBinaryClassCache.getInstance()
//                .getCangJieBinaryClassHeaderData(this.file, this.content)?.packageNameWithFallback
//            CangJieJavaScriptMetaFileType -> this.fqNameFromJsMetadata()
//            CangJieBuiltInFileType -> this.classIdFromCangJieMetadata()?.packageFqName
//            KlibMetaFileType -> KlibLoadingMetadataCache.getInstance().getCachedPackageFragment(file)
//                ?.getExtension(KlibMetadataProtoBuf.fqName)?.let(::FqName)
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
            val c = b.associateBy({ it.parent() }, { it.shortName() })
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

