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

package org.cangnova.cangjie.decompiler.psi.compiled

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager

/**
 * 类文件反编译器扩展点 API
 *
 * 该服务提供了扩展 IntelliJ IDEA 默认 .class 文件反编译器的能力，
 * 用于处理从 Java 以外的语言编译生成的类文件。
 *
 * ## 设计思路
 *
 * 此 API 允许插件通过实现不同级别的反编译器来增强或替换默认的 .class 文件处理逻辑：
 *
 * 1. **Light 反编译器**: 轻量级扩展，用于在标准 IDEA 反编译器基础上添加额外信息
 * 2. **Full 反编译器**: 完整实现，提供对不同于 Java 的语言的完整支持
 *
 * ## 使用场景
 *
 * - 为编译后的代码添加额外的元数据注释
 * - 为非 Java 语言（如 Kotlin、Scala、CangJie）提供自定义反编译支持
 * - 构建自定义的文件 Stub 以支持更好的索引和导航
 */
@Service
class ClassFileDecompilers private constructor() {
    /**
     * 反编译器基接口
     *
     * 实际实现应该继承 [Light] 或 [Full] 类，不符合要求的实现会被静默忽略。
     */
    interface Decompiler {
        /**
         * 判断该反编译器是否接受处理指定的文件
         *
         * @param file 虚拟文件
         * @return 如果接受处理返回 true
         */
        fun accepts(file: VirtualFile): Boolean
    }

    /**
     * 轻量级反编译器
     *
     * "Light" 反编译器用于增强标准 IDEA 反编译器生成的文件内容，而不改变其结构。
     * 例如：
     * - 在注释中提供额外信息
     * - 用更有意义的内容替换标准的 "compiled code" 方法体注释
     *
     * ## 异常处理
     *
     * 如果插件因某种原因无法反编译文件，可以抛出 [CannotDecompileException]，
     * 这会使 IDEA 回退到内置的反编译器实现。
     *
     * ## 注册建议
     *
     * 注册此类型扩展的插件通常应该：
     * - 接受所有文件
     * - 使用 `order="last"` 属性以避免干扰其他反编译器
     */
    abstract class Light : Decompiler {
        /**
         * 无法反编译异常
         *
         * 当反编译器无法处理文件时抛出，触发回退到默认实现。
         */
        class CannotDecompileException(message: String?, cause: Throwable?) :
            RuntimeException(message, cause)

        /**
         * 获取反编译后的文本
         *
         * @param file 要反编译的虚拟文件
         * @return 反编译后的文本内容
         * @throws CannotDecompileException 如果无法反编译
         */
        @Throws(CannotDecompileException::class)
        abstract fun getText(file: VirtualFile): CharSequence
    }

    /**
     * 完整反编译器
     *
     * "Full" 反编译器设计用于为与 Java 显著不同的语言提供扩展支持。
     * 此类型的扩展需要负责：
     * - 构建文件 Stub
     * - 正确地索引 Stub
     *
     * 作为回报，它们能够以更贴近原始语言的方式表示反编译后的文件。
     *
     * ## 实现示例
     *
     * Kotlin、Scala、CangJie 等语言都通过实现此接口提供自定义反编译支持。
     */
    abstract class Full : Decompiler {
        /**
         * Stub 构建器
         *
         * 用于从编译后的文件构建轻量级的 Stub 索引结构。
         */
        abstract val stubBuilder: ClsStubBuilder

        /**
         * 创建文件视图提供器
         *
         * ## 实现者注意事项
         *
         * 1. **语言设置**: 从 [FileViewProvider.getBaseLanguage] 返回正确的语言
         *
         * 2. **调用场景**: 此方法会在两种情况下被调用：
         *    - 构建 PSI 文件
         *    - 获取文档文本（此时 PsiManager 基于默认项目，只会调用 [FileViewProvider.getContents]）
         *
         * 3. **辅助类文件处理**: 语言编译器可能会生成应作为父类一部分处理的辅助 .class 文件。
         *    标准做法是从 [FileViewProvider.getPsi] 返回 `null` 来隐藏这些文件。
         *
         * @param file 虚拟文件
         * @param manager PSI 管理器
         * @param physical 是否为物理文件
         * @return 文件视图提供器
         */
        abstract fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider
    }

    /**
     * 反编译器扩展点名称
     */
    val EP_NAME: ExtensionPointName<Decompiler> = ExtensionPointName("org.cangnova.cangjie.classFileDecompiler")

    init {
        // 监听扩展点变化，通知二进制文件类型反编译器集合已改变
        EP_NAME.addChangeListener({ BinaryFileTypeDecompilers.getInstance().notifyDecompilerSetChange() }, null)
    }

    /**
     * 查找能够处理指定文件的反编译器
     *
     * @param file 要处理的虚拟文件
     * @param decompilerClass 反编译器类型
     * @return 找到的反编译器实例
     */
    fun <D : Decompiler> find(file: VirtualFile, decompilerClass: Class<D>): D {
        return EP_NAME.findFirstSafe { d: Decompiler -> decompilerClass.isInstance(d) && d.accepts(file) } as D
    }

    companion object {
        /**
         * 获取类文件反编译器服务实例
         */
        val instance: ClassFileDecompilers
            get() = ApplicationManager.getApplication().getService(
                ClassFileDecompilers::class.java,
            )
    }
}
