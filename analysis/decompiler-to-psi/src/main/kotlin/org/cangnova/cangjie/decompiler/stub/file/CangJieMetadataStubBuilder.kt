/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.decompiler.stub.file

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.cangnova.cangjie.decompiler.psi.compiled.ClsStubBuilder
import org.cangnova.cangjie.decompiler.psi.compiled.impl.ClassFileStubBuilder
import org.cangnova.cangjie.decompiler.stub.*
import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.serialization.deserialization.BLACK_LIST
import org.cangnova.cangjie.serialization.deserialization.FlatBuffersBasedClassDataFinder

/**
 * 从 CangJie 的"元数据文件"中构建 PSI Stub，用于 IDE 在没有源码时进行代码结构索引、导航与反编译查看。
 *
 * 该构建器基于 Flatbuffers 序列化格式，解析仓颉编译器生成的元数据文件，
 * 并为 IDE 构建轻量级的 Stub 索引结构，支持快速的符号查找和代码导航。
 */
open class CangJieMetadataStubBuilder(
    private val version: Int,
    private val fileType: FileType,
    private val serializerFlatbuffers: () -> SerializerExtensionFlatbuffers,
    private val readFile: (Project, VirtualFile, ByteArray) -> FileWithMetadata?
) : ClsStubBuilder() {

    override val stubVersion: Int = ClassFileStubBuilder.STUB_VERSION + version

    /**
     * 检查文件类型是否被支持
     */
    protected fun isSupported(file: VirtualFile): Boolean {
        return file.extension == fileType.defaultExtension || file.fileType == fileType
    }

    override fun buildFileStub(fileContent: FileContent): PsiFileStub<*>? {
        val virtualFile = fileContent.file

        if (!isSupported(virtualFile)) {
            LOG.warn("Unexpected file type: ${virtualFile.path}")
            return null
        }

        // 从 FileContent 获取 project，而不是使用 ProjectUtil.getActiveProject()
        val project = fileContent.project
        val file = try {
            readFile(project, virtualFile, fileContent.content)
        } catch (e: Exception) {
            LOG.warn("Failed to read metadata file: ${virtualFile.path}", e)
            null
        } ?: return null

        return when (file) {
            is FileWithMetadata.Incompatible -> {
                createIncompatibleAbiVersionFileStub()
            }

            is FileWithMetadata.Compatible -> {
                buildCompatibleFileStub(file, virtualFile)
            }
        }
    }

    /**
     * 构建兼容版本的文件 Stub
     */
    private fun buildCompatibleFileStub(
        file: FileWithMetadata.Compatible,
        virtualFile: VirtualFile
    ): PsiFileStub<*> {
        val packageWrapper = file.`package`
        val packageFqName = file.packageFqName

        // 创建组件
        val components = ClsStubBuilderComponents(
            classDataFinder = FlatBuffersBasedClassDataFinder(
                packageWrapper,
                file.version
            ),
            virtualFileForDebug = virtualFile,
            declTable = packageWrapper.declTable,
            typeTable = packageWrapper.typeTable
        )

        // 创建上下文
        val context = components.createContext(packageFqName, packageWrapper.typeTable)

        // 创建文件 Stub
        val fileStub = createFileStub(packageFqName)

        // 创建包容器
        val protoContainer = ProtoContainer.Package(packageFqName, packageWrapper.typeTable)

        // 创建顶层函数和变量 Stubs
        createPackageDeclarationsStubs(
            fileStub,
            context,
            protoContainer,
            packageWrapper.functions,
            packageWrapper.variables
        )

        // 创建类声明 Stubs
        for (classDecl in file.classesToDecompile) {
            createClassStub(fileStub, classDecl, context)
        }

        // 创建扩展声明 Stubs
        for (extend in packageWrapper.extends) {
            ExtendClsStubBuilder(fileStub, context, extend).build()
        }

        // 创建类型别名 Stubs
        for (typeAlias in packageWrapper.typeAliass) {
            TypeAliasClsStubBuilder(fileStub, context, typeAlias).build()
        }

        return fileStub
    }

    /**
     * 用于保存从元数据文件读取的信息
     */
    sealed class FileWithMetadata {
        /**
         * 不兼容的元数据版本
         */
        class Incompatible(val version: BinaryVersion) : FileWithMetadata()

        /**
         * 兼容的元数据，包含包信息和版本
         */
        open class Compatible(
            val `package`: PackageWrapper,
            val version: BinaryVersion,
            val serializerProtocol: SerializerExtensionFlatbuffers
        ) : FileWithMetadata() {
            val packageFqName = `package`.packageName

            /**
             * 需要反编译的类列表，过滤掉嵌套类和黑名单中的类
             */
            open val classesToDecompile: List<ClassDeclWrapper> =
                `package`.allClassDecls.filter { decl ->
                    !decl.classId.isNestedClass && decl.classId !in BLACK_LIST
                }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(CangJieMetadataStubBuilder::class.java)
    }
}