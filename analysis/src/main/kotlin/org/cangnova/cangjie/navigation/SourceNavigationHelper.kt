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

package org.cangnova.cangjie.navigation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import org.cangnova.cangjie.decompiler.psi.file.CjDecompiledFile
import org.cangnova.cangjie.lang.declarations.CjDeclarationsFile
import org.cangnova.cangjie.projectStructure.RootKindFilter
import org.cangnova.cangjie.projectStructure.matches
import org.cangnova.cangjie.psi.*

/**
 * 源码导航辅助工具
 *
 * 该对象提供了在源码文件和编译文件（反编译文件）之间进行双向导航的功能。
 * 它是 IDE 中"跳转到声明"、"跳转到实现"等导航功能的核心组件。
 *
 * ## 核心功能
 *
 * 1. **源码到编译文件**: 从库的源码跳转到对应的反编译文件
 * 2. **编译文件到源码**: 从反编译的类文件跳转到对应的源码（如果存在）
 *
 * ## 导航场景
 *
 * ### 场景 1: 库源码导航
 * ```
 * 用户在库源码中点击某个类
 *   ↓
 * getOriginalElement() - 获取编译后的声明
 *   ↓
 * 返回反编译文件中的对应声明
 * ```
 *
 * ### 场景 2: 反编译文件导航
 * ```
 * 用户在反编译文件中点击某个类
 *   ↓
 * getNavigationElement() - 尝试查找源码
 *   ↓
 * 如果存在源码，返回源码中的声明
 * 如果不存在，返回反编译声明本身
 * ```
 *
 * ## 使用场景
 *
 * - **跳转到声明**: 从引用跳转到实际的声明位置
 * - **查看实现**: 查看库类型的实现细节
 * - **源码优先**: 优先显示可读的源码，而非反编译代码
 * - **调试**: 在调试时定位到正确的源码位置
 *
 * ## 设计考虑
 *
 * - **性能优化**: 在索引构建期间（Dumb Mode）不执行导航，避免性能问题
 * - **本地声明过滤**: 不导航局部变量、局部函数等本地声明
 * - **库源码识别**: 只对库源码执行源码到编译文件的导航
 * - **有效性检查**: 导航前检查 PSI 元素的有效性
 *
 * @see NavigationKind
 * @see CjDecompiledFile
 * @see CjDeclarationsFile
 */
object SourceNavigationHelper {
    private val LOG = Logger.getInstance(SourceNavigationHelper::class.java)

    /**
     * 获取声明的原始元素
     *
     * 该方法用于从库的源码声明导航到对应的编译文件声明。
     * 当用户在库源码中查看某个类时，IDE 可能需要获取其编译后的版本以获取完整的元数据。
     *
     * ## 导航方向
     *
     * 源码（库源文件） → 编译文件（反编译文件）
     *
     * ## 使用场景
     *
     * - **类型推导**: 从源码声明获取编译后的类型信息
     * - **元数据访问**: 访问只在编译文件中存在的元数据
     * - **实现查找**: 查找接口或抽象类的实际实现
     * - **跨模块引用**: 解析跨模块的符号引用
     *
     * ## 返回值
     *
     * - 如果是库源码声明，返回对应的反编译声明
     * - 如果已经是编译文件声明，返回自身
     * - 如果导航失败，返回原声明
     *
     * ## 示例
     *
     * ```kotlin
     * // 用户在库源码中的类声明
     * // file: library-sources/std/collection/ArrayList.cj
     * class ArrayList<T> { ... }
     *
     * // 调用 getOriginalElement
     * val original = SourceNavigationHelper.getOriginalElement(arrayListClass)
     *
     * // 返回反编译文件中的声明
     * // file: library-classes/std/collection/ArrayList.cjo (反编译)
     * class ArrayList<T> { ... }
     * ```
     *
     * @param declaration 源码中的声明
     * @return 对应的编译文件声明，或原声明（如果导航失败）
     */
    fun getOriginalElement(declaration: CjDeclaration): CjElement {
        return navigateToDeclaration(declaration, NavigationKind.SOURCES_TO_CLASS_FILES)
    }

    /**
     * 获取声明的导航元素
     *
     * 该方法用于从编译文件（反编译文件）导航到对应的源码声明。
     * 这是 IDE 中"跳转到源码"功能的核心实现，优先显示可读的源码而非反编译代码。
     *
     * ## 导航方向
     *
     * 编译文件（反编译文件） → 源码（如果存在）
     *
     * ## 使用场景
     *
     * - **跳转到源码**: 从库引用跳转到可读的源代码
     * - **代码浏览**: 查看库的源码实现而非反编译代码
     * - **调试**: 在调试时显示源码位置
     * - **文档查看**: 查看源码中的注释和文档
     *
     * ## 返回值
     *
     * - 如果存在对应的源码，返回源码声明
     * - 如果不存在源码，返回原反编译声明
     * - 如果导航失败，返回原声明
     *
     * ## 示例
     *
     * ```kotlin
     * // 用户在反编译文件中的类声明
     * // file: library-classes/std/collection/ArrayList.cjo (反编译)
     * class ArrayList<T> { ... }
     *
     * // 调用 getNavigationElement
     * val navigation = SourceNavigationHelper.getNavigationElement(arrayListClass)
     *
     * // 如果存在源码，返回源码中的声明
     * // file: library-sources/std/collection/ArrayList.cj
     * class ArrayList<T> { ... }
     * ```
     *
     * @param declaration 编译文件中的声明
     * @return 对应的源码声明，或原声明（如果源码不存在或导航失败）
     */
    fun getNavigationElement(declaration: CjDeclaration): CjElement {
        return navigateToDeclaration(declaration, NavigationKind.CLASS_FILES_TO_SOURCES)
    }

    /**
     * 导航类型
     *
     * 定义了两种导航方向，用于在源码和编译文件之间进行双向导航。
     */
    enum class NavigationKind {
        /**
         * 从编译文件导航到源码
         *
         * 用于"跳转到源码"功能，优先显示可读的源码。
         */
        CLASS_FILES_TO_SOURCES,

        /**
         * 从源码导航到编译文件
         *
         * 用于获取编译后的元数据和类型信息。
         */
        SOURCES_TO_CLASS_FILES
    }

    /**
     * 执行声明的导航
     *
     * 这是导航的核心实现方法，根据导航类型在源码和编译文件之间进行转换。
     *
     * ## 导航前检查
     *
     * 在执行导航前，会进行以下检查：
     * 1. **有效性检查**: PSI 元素必须有效
     * 2. **索引检查**: 不在 Dumb Mode 下执行（避免性能问题）
     * 3. **文件类型检查**: 确保文件类型符合导航方向
     * 4. **本地声明过滤**: 跳过局部变量、局部函数等
     *
     * ## 导航逻辑
     *
     * ### CLASS_FILES_TO_SOURCES（编译文件 → 源码）
     * - 只处理编译文件（`isCompiled = true`）
     * - 如果不是编译文件，直接返回原声明
     *
     * ### SOURCES_TO_CLASS_FILES（源码 → 编译文件）
     * - 跳过反编译文件（已经是编译文件）
     * - 跳过声明文件（已经是编译文件）
     * - 只处理库源码文件（`RootKindFilter.librarySources`）
     * - 跳过本地声明（局部变量、嵌套函数等）
     *
     * ## 访问者模式
     *
     * 使用 [SourceAndDecompiledConversionVisitor] 遍历 PSI 树，
     * 查找对应的声明。访问者会处理不同类型的声明（类、函数、属性等）。
     *
     * @param from 源声明
     * @param navigationKind 导航类型
     * @return 导航后的声明，或原声明（如果导航失败）
     */
    private fun navigateToDeclaration(
        from: CjDeclaration,
        navigationKind: NavigationKind
    ): CjDeclaration {
        // 1. 检查 PSI 元素有效性和索引状态
        if (!from.isValid || DumbService.isDumb(from.project)) return from

        // 2. 根据导航类型进行预检查
        when (navigationKind) {
            // 编译文件到源码：只处理编译文件
            NavigationKind.CLASS_FILES_TO_SOURCES -> if (!from.getContainingCjFile().isCompiled) return from

            // 源码到编译文件：只处理库源码
            NavigationKind.SOURCES_TO_CLASS_FILES -> {
                val file = from.containingFile

                // 如果已经是反编译文件，直接返回
                if (file is CjDecompiledFile && file.isCompiled) return from

                // 如果已经是声明文件，直接返回
                if (file is CjDeclarationsFile /*&& file.isCompiled*/) return from

                // 只处理库源码文件
                if (!RootKindFilter.librarySources.matches(from)) return from

                // 跳过本地声明（局部变量、嵌套函数等）
                if (CjPsiUtil.isLocal(from)) return from
            }
        }

        // 3. 使用访问者模式执行实际的导航
        return from.accept(SourceAndDecompiledConversionVisitor(navigationKind), Unit) ?: from
    }

    /**
     * 源码和反编译文件转换访问者
     *
     * 该访问者实现了在源码声明和反编译声明之间进行转换的逻辑。
     * 它会遍历 PSI 树，根据导航类型查找对应的声明。
     *
     * ## 访问者模式
     *
     * 使用访问者模式可以：
     * - 为不同类型的声明提供专门的处理逻辑
     * - 保持导航代码的可扩展性
     * - 避免类型判断和强制转换
     *
     * ## 实现说明
     *
     * 具体的访问方法（如 `visitClass`、`visitFunction` 等）需要在子类中实现，
     * 用于处理不同类型声明的导航逻辑。
     *
     * @param navigationKind 导航类型（源码到编译文件，或编译文件到源码）
     */
    private class SourceAndDecompiledConversionVisitor(private val navigationKind: NavigationKind) :
        CjVisitor<CjDeclaration?, Unit>()
}
