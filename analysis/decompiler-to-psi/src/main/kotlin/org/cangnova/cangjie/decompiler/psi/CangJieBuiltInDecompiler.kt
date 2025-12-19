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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.decompiler.stub.file.CangJieMetadataStubBuilder
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import org.cangnova.cangjie.metadata.model.fb.parser.toFbPackage
import org.cangnova.cangjie.metadata.model.wrapper.ClassDeclWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.stubs.CangJieStubVersions
import org.cangnova.cangjie.serialization.deserialization.BuiltInSerializerFlatbuffers
import org.jetbrains.annotations.TestOnly
import java.io.ByteArrayInputStream

/**
 * 内置库 Stub 版本号
 *
 * 该版本号用于确保 Stub 缓存的有效性。当编译器版本或内置库发生变化时，
 * 版本号会改变，从而触发 Stub 重建。
 *
 * ## 版本计算
 *
 * 版本号由两部分组成：
 * - 基础版本: [CangJieStubVersions.BUILTIN_STUB_VERSION]
 * - 偏移量: [CangJieBuiltInStubVersionOffsetProvider.getVersionOffset]
 *
 * 偏移量允许在不改变基础版本的情况下强制重建 Stub。
 */
private val stubVersionForStubBuilderAndDecompiler: Int
    get() = CangJieStubVersions.BUILTIN_STUB_VERSION + _root_ide_package_.org.cangnova.cangjie.decompiler.psi.CangJieBuiltInStubVersionOffsetProvider.getVersionOffset()

/**
 * 仓颉内置库反编译器
 *
 * 该类负责将编译后的内置库文件（.cjb 格式）反编译为可在 IDE 中显示的 PSI 结构。
 * 内置库包含语言的基本类型（如 Int8, Bool, Unit 等）和核心接口（如 Equatable, Comparable）。
 *
 * ## 核心功能
 *
 * 1. **读取内置库文件**: 从 .cjb 文件中读取 Flatbuffers 格式的元数据
 * 2. **构建 Stub 树**: 为内置库声明创建轻量级的 Stub 表示
 * 3. **生成反编译文本**: 将内置库声明转换为可读的仓颉代码
 *
 * ## 工作流程
 *
 * ```
 * .cjb 文件
 *   ↓
 * readFile() - 读取 Flatbuffers 数据
 *   ↓
 * BuiltInDefinitionFile - 解析包和声明
 *   ↓
 * metadataStubBuilder - 构建 Stub 树
 *   ↓
 * CjDecompiledFile - 可在 IDE 中导航的 PSI 文件
 * ```
 *
 * ## 与 CangJieMetadataDecompiler 的关系
 *
 * 继承自 [CangJieMetadataDecompiler]，提供内置库特定的配置：
 * - 文件类型: [CangJieBuiltInFileType]
 * - 序列化器: [BuiltInSerializerFlatbuffers]
 * - 版本管理: [BuiltInsBinaryVersion]
 *
 * @see CangJieMetadataDecompiler
 * @see BuiltInDefinitionFile
 * @see CangJieBuiltInDecompilationInterceptor
 */
internal class CangJieBuiltInDecompiler :
    CangJieMetadataDecompiler<BuiltInsBinaryVersion>(
        CangJieBuiltInFileType,
        { BuiltInSerializerFlatbuffers },
        { BuiltInsBinaryVersion.INSTANCE },
        { BuiltInsBinaryVersion.INVALID_VERSION },
        stubVersionForStubBuilderAndDecompiler,
    ) {
    /**
     * 内置库的 Stub 构建器
     *
     * 使用 [CangJieBuiltInMetadataStubBuilder] 专门处理内置库文件的 Stub 构建。
     * 通过 `readFileSafely` 安全地读取文件内容，捕获并处理可能的解析错误。
     */
    override val metadataStubBuilder: CangJieMetadataStubBuilder =
        CangJieBuiltInMetadataStubBuilder { project, file, bytes ->
            readFileSafely(project, file, bytes)
        }

    /**
     * 读取内置库文件
     *
     * 该方法负责从字节数组中解析内置库元数据，创建 [BuiltInDefinitionFile] 对象。
     *
     * ## 读取策略
     *
     * 1. **拦截器优先**: 首先调用 [CangJieBuiltInDecompilationInterceptor.readFile]
     *    - 允许自定义内置库的读取逻辑
     *    - 可用于测试或特殊场景
     *
     * 2. **标准读取**: 如果拦截器返回 null，则使用 [BuiltInDefinitionFile.read]
     *    - 解析 Flatbuffers 格式的包数据
     *    - 创建包含所有声明的定义文件
     *
     * ## 返回值
     *
     * - 成功: 返回 [BuiltInDefinitionFile]，包含要反编译的类、函数、变量等
     * - 失败: 返回 null，文件将被跳过
     *
     * @param project 当前项目（可能为 null，在索引构建期间）
     * @param bytes 内置库文件的原始字节内容
     * @param file 虚拟文件对象，用于获取文件路径等信息
     * @return 解析后的文件元数据，如果解析失败则返回 null
     */
    override fun readFile(
        project: Project,
        bytes: ByteArray,
        file: VirtualFile
    ): CangJieMetadataStubBuilder.FileWithMetadata? {
        return CangJieBuiltInDecompilationInterceptor.readFile(project, bytes, file)
            ?: BuiltInDefinitionFile.read(
                bytes,
                file
            )
    }
}

/**
 * 内置库定义文件
 *
 * 该类表示一个已解析的内置库文件，包含了从 Flatbuffers 元数据中读取的所有声明信息。
 * 它决定了哪些类需要被反编译，哪些可以跳过（如果已有源文件）。
 *
 * ## 核心职责
 *
 * 1. **包装元数据**: 持有从 .cjb 文件解析的包和声明信息
 * 2. **过滤类**: 决定哪些类需要反编译，避免重复工作
 * 3. **版本管理**: 跟踪内置库的二进制版本
 *
 * ## 类过滤策略
 *
 * 为了优化性能和避免重复，内置库的某些类可能不需要反编译：
 * - 如果同一目录下已存在对应的 .cj 源文件，则不反编译该类
 * - 如果内置文件是"凭空创建"（packageDirectory 为 null），则反编译所有类
 *
 * ## 使用场景
 *
 * - **IDE 启动**: 加载内置库时创建此对象
 * - **Stub 构建**: 为内置库声明创建索引
 * - **代码补全**: 提供内置类型的成员信息
 * - **导航**: 从引用跳转到内置库定义
 *
 * @param package 包装的包信息，包含所有顶层声明
 * @param version 内置库的二进制版本
 * @param packageDirectory 包含该文件的目录，可能为 null（凭空创建的情况）
 * @param isMetadata 是否为纯元数据文件（vs. 包含实现的文件）
 * @param filterOutClassesExistingAsClassFiles 是否过滤掉已存在源文件的类
 */
class BuiltInDefinitionFile(
    `package`: PackageWrapper,
    version: BuiltInsBinaryVersion,
    /**
     * 包含该虚拟文件的目录
     *
     * 可能为 null，当内置文件是"凭空创建"时（例如在测试或特殊场景中）。
     * 如果为 null，表示没有对应的物理文件系统位置，需要反编译所有类。
     */
    val packageDirectory: VirtualFile?,
    /**
     * 是否为纯元数据文件
     *
     * - true: 文件只包含类型签名，没有实现
     * - false: 文件可能包含实现代码
     */
    val isMetadata: Boolean,
    /**
     * 是否过滤掉已存在源文件的类
     *
     * 如果设置为 true，并且在同一目录下找到了对应的 .cj 源文件，
     * 则不会反编译该类，而是使用源文件。
     */
    private val filterOutClassesExistingAsClassFiles: Boolean = true,
) : CangJieMetadataStubBuilder.FileWithMetadata.Compatible(`package`, version, BuiltInSerializerFlatbuffers) {
    /**
     * 需要反编译的类列表
     *
     * 该属性根据过滤策略决定哪些类需要反编译：
     *
     * ## 过滤逻辑
     *
     * 1. **凭空创建的文件** (packageDirectory == null):
     *    - 反编译所有类，因为没有源文件可替代
     *
     * 2. **纯元数据文件** (isMetadata == true):
     *    - 反编译所有类，元数据总是需要的
     *
     * 3. **禁用过滤** (FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES == false):
     *    - 反编译所有类（通常用于测试）
     *
     * 4. **标准过滤** (其他情况):
     *    - 只反编译那些在同一目录下没有对应 .cj 源文件的类
     *
     * ## 性能考虑
     *
     * 过滤可以显著减少反编译工作量，因为许多内置类型可能已有源文件表示。
     */
    override val classesToDecompile: List<ClassDeclWrapper>
        get() = super.classesToDecompile.let { classes ->
            if (packageDirectory == null) {
                // 如果内置文件是凭空创建的，
                // 意味着我们需要所有内置类，因为没有 .cj 源文件来替代它们
                return@let classes
            }
            if (isMetadata || !FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES || !filterOutClassesExistingAsClassFiles) classes
            else classes.filter { decl ->
                shouldDecompileBuiltInClass(decl.classId, packageDirectory)
            }
        }

    /**
     * 判断内置类是否应该被反编译
     *
     * 检查指定类是否在包目录中存在对应的 .cj 源文件。
     * 如果源文件存在，则不需要反编译；如果不存在，则需要反编译。
     *
     * ## 判断逻辑
     *
     * 构造源文件名: `类名短名.cj`（例如 `Int8.cj`）
     * 在包目录中查找该文件
     * - 找到: 返回 false（不反编译）
     * - 未找到: 返回 true（需要反编译）
     *
     * @param classId 类的标识符
     * @param packageDirectory 包所在的目录
     * @return true 如果应该反编译该类，false 如果可以跳过
     */
    private fun shouldDecompileBuiltInClass(classId: ClassId, packageDirectory: VirtualFile): Boolean {
        val realClassFileName = classId.shortClassName.asString() + "." + CangJieFileType.INSTANCE.defaultExtension
        return packageDirectory.findChild(realClassFileName) == null
    }

    companion object {
        /**
         * 全局开关：是否过滤掉已存在源文件的类
         *
         * 该标志控制是否跳过那些在包目录中已有对应 .cj 源文件的内置类。
         *
         * ## 使用场景
         *
         * - **生产环境** (true): 跳过已有源文件的类，优化性能
         * - **测试环境** (false): 强制反编译所有类，用于测试反编译逻辑
         *
         * ## 注意事项
         *
         * 该属性仅用于测试目的，生产环境应始终保持 true。
         * 使用 @TestOnly 注解限制 setter 只能在测试代码中调用。
         */
        var FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = true
            @TestOnly set

        /**
         * 从字节数组读取内置库定义文件
         *
         * 该方法是创建 [BuiltInDefinitionFile] 的工厂方法，负责：
         * 1. 解析 Flatbuffers 格式的包数据
         * 2. 检查版本兼容性（当前已注释）
         * 3. 创建定义文件对象
         * 4. 验证文件是否包含有效声明
         *
         * ## 解析流程
         *
         * ```
         * ByteArray
         *   ↓
         * ByteArrayInputStream
         *   ↓
         * toFbPackage() - Flatbuffers 解析
         *   ↓
         * PackageWrapper - 包装对象
         *   ↓
         * BuiltInDefinitionFile - 定义文件
         * ```
         *
         * ## 空文件处理
         *
         * 如果文件不包含任何声明（类、类型别名、函数、变量都为空），
         * 则返回 null，跳过该文件的索引构建。
         *
         * ## 版本检查
         *
         * 版本检查代码当前被注释掉，未来可能重新启用以确保兼容性。
         *
         * @param contents 内置库文件的字节内容
         * @param file 虚拟文件对象
         * @param filterOutClassesExistingAsClassFiles 是否过滤已存在源文件的类
         * @return 解析后的定义文件，如果文件为空或不兼容则返回 null
         */
        @JvmOverloads
        fun read(
            contents: ByteArray,
            file: VirtualFile,
            filterOutClassesExistingAsClassFiles: Boolean = true
        ): CangJieMetadataStubBuilder.FileWithMetadata? {
            val stream = ByteArrayInputStream(contents)

            // 从 Flatbuffers 格式解析包数据
            val `package` = stream.toFbPackage().packageWrapper
            val version = `package`.cjoVersion

            // 版本兼容性检查（当前未启用）
            // 未来可能需要验证内置库版本是否与当前编译器兼容
//            if (!version.isCompatibleWithCurrentCompilerVersion()) {
//                return Incompatible(version)
//            }

            val result = BuiltInDefinitionFile(
                `package`, version, file.parent,
                false,
                filterOutClassesExistingAsClassFiles
            )

            // 检查是否有任何需要反编译的声明
            // 如果所有声明列表都为空，则跳过该文件
            if (result.classesToDecompile.isEmpty() &&
                `package`.typeAliass.isEmpty() && `package`.functions.isEmpty() && `package`.variables.isEmpty()
            ) {
                // 没有任何声明需要反编译：应该跳过该文件
                return null
            }

            return result
        }
    }
}

/**
 * 内置库元数据 Stub 构建器
 *
 * 该类是 [CangJieMetadataStubBuilder] 的内置库特化版本，专门用于处理内置库文件的 Stub 构建。
 *
 * ## 职责
 *
 * - 为内置库文件创建轻量级的 Stub 树
 * - 使用内置库特定的版本号
 * - 使用内置库特定的文件类型
 * - 通过提供的 readFile 函数读取文件内容
 *
 * ## 与普通 Stub 构建器的区别
 *
 * - **文件类型**: 使用 [CangJieBuiltInFileType] 而不是普通的 .cjo 文件类型
 * - **序列化器**: 使用 [BuiltInSerializerFlatbuffers] 进行反序列化
 * - **版本管理**: 使用内置库专用的 Stub 版本号
 *
 * ## 使用方式
 *
 * 该类由 [CangJieBuiltInDecompiler] 内部使用，不应直接实例化。
 *
 * @param readFile 读取文件的函数，接受项目、虚拟文件和字节数组，返回文件元数据
 */
private class CangJieBuiltInMetadataStubBuilder(
    readFile: (Project, VirtualFile, ByteArray) -> FileWithMetadata?,
) : CangJieMetadataStubBuilder(
    stubVersionForStubBuilderAndDecompiler,
    CangJieBuiltInFileType,
    { BuiltInSerializerFlatbuffers },
    readFile
)
