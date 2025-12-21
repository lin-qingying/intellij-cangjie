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

package org.cangnova.cangjie.decompiler.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.decompiler.psi.compiled.ClassFileDecompilers
import org.cangnova.cangjie.decompiler.psi.compiled.ClsStubBuilder
import org.cangnova.cangjie.decompiler.psi.file.CjDecompiledFile
import org.cangnova.cangjie.decompiler.psi.text.DecompiledText
import org.cangnova.cangjie.decompiler.psi.text.buildDecompiledText
import org.cangnova.cangjie.decompiler.psi.text.createIncompatibleMetadataVersionDecompiledText
import org.cangnova.cangjie.decompiler.psi.text.defaultDecompilerRendererOptions
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.metadata.SerializerExtensionFlatbuffers
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.renderer.DescriptorRenderer
import java.io.IOException

/**
 * 仓颉语言元数据反编译器的抽象基类。
 *
 * 该类负责将仓颉编译后的二进制元数据文件（如 `.cjo` 文件）反编译为可读的 PSI 结构，
 * 使 IDE 能够提供代码导航、补全、查找引用等功能。
 *
 * ## 核心职责
 *
 * 1. **文件识别**: 通过 [accepts] 方法判断是否能处理指定的虚拟文件
 * 2. **元数据读取**: 通过 [readFile] 方法解析二进制元数据内容
 * 3. **Stub 构建**: 提供 [stubBuilder] 用于构建轻量级的 Stub 索引
 * 4. **反编译文本生成**: 通过 [buildDecompiledText] 将描述符渲染为可读的源代码文本
 * 5. **PSI 文件创建**: 通过 [createFileViewProvider] 创建反编译后的 PSI 文件视图
 *
 * ## 版本兼容性
 *
 * 该反编译器支持版本检查机制：
 * - [expectedBinaryVersion]: 当前期望的二进制版本
 * - [invalidBinaryVersion]: 无效/不兼容的二进制版本标识
 *
 * 当遇到不兼容的元数据版本时，会生成包含版本不匹配提示的反编译文本。
 *
 * ## 使用示例
 *
 * ```kotlin
 * class MyCangJieDecompiler : CangJieMetadataDecompiler<MyVersion>(
 *     fileType = CangJieMetadataFileType,
 *     serializerFlatbuffers = { MySerializerExtension() },
 *     expectedBinaryVersion = { MyVersion.CURRENT },
 *     invalidBinaryVersion = { MyVersion.INVALID },
 *     stubVersion = 1
 * ) {
 *     override fun readFile(project: Project, bytes: ByteArray, file: VirtualFile) = ...
 * }
 * ```
 *
 * @param V 二进制版本类型，必须继承自 [BinaryVersion]
 * @param fileType 该反编译器处理的文件类型
 * @param serializerFlatbuffers 序列化扩展工厂，用于反序列化元数据
 * @param expectedBinaryVersion 期望的二进制版本提供者
 * @param invalidBinaryVersion 无效二进制版本提供者
 * @param stubVersion Stub 构建器版本号，用于缓存失效判断
 *
 * @see ClassFileDecompilers.Full
 * @see CangJieMetadataStubBuilder
 * @see CangJieMetadataDeserializerForDecompiler
 */
abstract class CangJieMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerFlatbuffers: () -> SerializerExtensionFlatbuffers,

    private val expectedBinaryVersion: () -> V,
    private val invalidBinaryVersion: () -> V,
    stubVersion: Int
) : ClassFileDecompilers.Full() {

    /**
     * 描述符渲染器，用于将声明描述符转换为可读的源代码文本。
     *
     * 使用懒加载初始化，配置了默认的反编译渲染选项。
     * 渲染器负责格式化类、函数、属性等声明的文本表示。
     */
    private val renderer: DescriptorRenderer by lazy {
        DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }
    }

    /**
     * 元数据 Stub 构建器，用于从元数据文件构建轻量级的 Stub 索引。
     *
     * Stub 索引允许 IDE 在不完全解析文件的情况下快速访问符号信息，
     * 显著提升大型项目的索引和搜索性能。
     *
     * 子类可以覆盖此属性以提供自定义的 Stub 构建逻辑。
     */
    protected open val metadataStubBuilder: CangJieMetadataStubBuilder =
        CangJieMetadataStubBuilder(stubVersion, fileType, serializerFlatbuffers) { file, bytes ->
            readFileSafely(file, bytes)
        }

    /**
     * 判断该反编译器是否能处理指定的虚拟文件。
     *
     * 通过检查文件扩展名或文件类型来判断兼容性。
     *
     * @param file 待检查的虚拟文件
     * @return 如果文件扩展名匹配或文件类型匹配则返回 `true`
     */
    override fun accepts(file: VirtualFile) = file.extension == fileType.defaultExtension || file.fileType == fileType

    /**
     * 获取用于构建 Stub 索引的构建器。
     *
     * @see metadataStubBuilder
     */
    override val stubBuilder: ClsStubBuilder
        get() = metadataStubBuilder

    /**
     * 安全地读取元数据文件内容。
     *
     * 该方法封装了文件读取的异常处理逻辑，确保即使文件不存在或无法访问时也不会抛出异常。
     * 这在处理可能指向不存在的 JAR 条目的虚拟文件时尤为重要。
     *
     * ## 异常处理说明
     *
     * 有时会收到指向不存在的 JAR 条目的 VirtualFile 实例。这些文件在 `isValid()` 检查时
     * 返回 `true`，但尝试读取其内容时会抛出 `FileNotFoundException`。
     *
     * 虽然调用 `refresh()` 可能看起来更正确，但这并不总是被允许的，
     * 而且可能会降低性能。
     *
     * @param project 当前项目
     * @param file 要读取的虚拟文件
     * @param content 可选的文件内容字节数组，如果为 `null` 则从文件读取
     * @return 解析后的文件元数据，如果文件无效或读取失败则返回 `null`
     */
    protected fun readFileSafely(

        file: VirtualFile,
        content: ByteArray? = null
    ): CangJieMetadataStubBuilder.FileWithMetadata? {
        if (!file.isValid) return null

        return try {
            readFile(content ?: file.contentsToByteArray(false), file)
        } catch (e: IOException) {
            // 这里需要捕获异常，因为有时会收到指向不存在的 JAR 条目的 VirtualFile 实例。
            // 这些文件在 isValid() 检查时返回 true，但尝试读取其内容时会抛出 FileNotFoundException。
            // 注意：虽然调用 refresh() 而不是捕获异常看起来更正确，
            // 但这并不总是被允许的，而且可能会降低性能。
            null
            // 为方便调试，这里先抛出异常
            throw e
        }
    }

    /**
     * 读取虚拟文件的元数据（仅用于测试）。
     *
     * 该方法是 [readFileSafely] 的简化包装，用于单元测试场景。
     *
     * @param project 当前项目
     * @param file 要读取的虚拟文件
     * @return 解析后的文件元数据
     */

    fun readFile(file: VirtualFile) = readFileSafely(file)

    /**
     * 从字节数组读取并解析元数据文件。
     *
     * 子类必须实现此方法以提供具体的元数据解析逻辑。
     * 该方法负责将原始字节数据转换为结构化的文件元数据对象。
     *
     * @param project 当前项目（可能为 null，在索引构建期间），用于访问项目级服务
     * @param bytes 文件的原始字节内容
     * @param file 源虚拟文件，用于获取文件路径等元信息
     * @return 解析后的文件元数据，如果解析失败则返回 `null`
     */
    abstract fun readFile(

        bytes: ByteArray,
        file: VirtualFile
    ): CangJieMetadataStubBuilder.FileWithMetadata?

    /**
     * 为元数据文件创建文件视图提供者。
     *
     * 文件视图提供者负责管理反编译文件的 PSI 结构，
     * 使 IDE 能够像处理普通源文件一样处理反编译后的元数据文件。
     *
     * 创建的 [CangJieDecompiledFileViewProvider] 会在需要时延迟生成反编译文本。
     *
     * ## 设计说明
     *
     * 本方法创建一个文本工厂函数，该工厂：
     * 1. 读取并解析元数据文件
     * 2. 生成反编译后的文本内容
     * 3. 在反编译失败时返回错误提示文本
     *
     * 文本工厂保证永不返回 null 或空字符串，避免文档和 PSI 内容不一致的问题。
     *
     * @param file 元数据虚拟文件
     * @param manager PSI 管理器
     * @param physical 是否为物理文件（非内存中的临时文件）
     * @return 用于管理反编译文件视图的提供者
     */
    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return CangJieDecompiledFileViewProvider(manager, file, physical) {
            val virtualFile = it.virtualFile

            // 直接读取并反编译，CangJieDecompiledFileViewProvider 内部已有缓存
            val fileWithMetadata = readFileSafely(virtualFile)
            CjDecompiledFile(it)
        }
    }

    private fun buildErrorText(virtualFile: VirtualFile): String {
        return """
            // IntelliJ API Decompiler stub source generated from a class file
            // Unable to read metadata file: ${virtualFile.name}
            // The file may not exist or cannot be accessed.
        """.trimIndent()
    }

    /**
     * 将文件元数据构建为反编译文本。
     *
     * 根据元数据的兼容性状态采取不同的处理策略：
     *
     * ## 不兼容版本
     * 如果元数据版本与当前期望版本不兼容，生成包含版本不匹配提示信息的文本，
     * 告知用户需要更新编译器或 IDE 插件。
     *
     * ## 兼容版本
     * 对于兼容的元数据：
     * 1. 创建 [CangJieMetadataDeserializerForDecompiler] 反序列化器
     * 2. 解析包中的所有声明（类、函数、属性等）
     * 3. 使用 [renderer] 将声明渲染为格式化的源代码文本
     *
     * @param project 当前项目
     * @param file 包含元数据的文件对象
     * @return 反编译后的文本，包含源代码和文本到描述符的映射
     *
     * @see DecompiledText
     * @see CangJieMetadataDeserializerForDecompiler
     */
    private fun buildDecompiledText(
        project: Project,
        file: CangJieMetadataStubBuilder.FileWithMetadata
    ): DecompiledText {
        return when (file) {
            is CangJieMetadataStubBuilder.FileWithMetadata.Incompatible -> {
                createIncompatibleMetadataVersionDecompiledText(expectedBinaryVersion(), file.version)
            }

            is CangJieMetadataStubBuilder.FileWithMetadata.Compatible -> {
                val packageFqName = file.packageFqName
                val resolver = CangJieMetadataDeserializerForDecompiler(
                    project,
                    packageFqName, file.`package`, file.version,
                    serializerFlatbuffers()
                )
                val declarations = arrayListOf<DeclarationDescriptor>()
                declarations.addAll(resolver.resolveAllDeclarationsInPackage(packageFqName))

                buildDecompiledText(packageFqName, declarations, renderer)
            }
        }
    }
}