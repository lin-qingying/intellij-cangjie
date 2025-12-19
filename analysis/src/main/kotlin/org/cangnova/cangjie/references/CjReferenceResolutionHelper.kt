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

package org.cangnova.cangjie.references

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.resolve.binding.BindingContext

/**
 * 仓颉语言引用解析辅助服务
 *
 * 该接口定义了 IDE 中处理仓颉语言引用解析的核心服务。它提供了多种引用解析功能：
 * - 部分分析（partial analysis）：增量分析单个元素
 * - 导入引用解析：解析 import 语句中的符号
 * - PSI 声明查找：从 Descriptor 找到对应的 PSI 元素
 * - CDoc 链接解析：解析文档注释中的符号引用
 * - 反编译声明查找：从编译后的库文件中查找声明
 *
 * ## 服务架构
 *
 * 这是一个应用级（Application-level）服务，通过 IntelliJ Platform 的服务系统注册。
 * 实现类需要在 `cangjie-analysis.xml` 中注册为 `applicationService`。
 *
 * ## 使用方式
 *
 * ```kotlin
 * val helper = CjReferenceResolutionHelper.getInstance()
 * val context = helper.partialAnalyze(element)
 * ```
 *
 * ## 设计考虑
 *
 * - **性能优化**: `partialAnalyze` 仅分析必要的代码部分，避免全量分析
 * - **懒加载**: 反编译声明在需要时才查找和构建
 * - **缓存友好**: 解析结果可以被 IDE 的缓存系统复用
 */
interface CjReferenceResolutionHelper {
    /**
     * 对 CangJie 元素执行部分分析
     *
     * 部分分析是一种增量分析策略，只分析给定元素及其依赖，而不是整个文件或模块。
     * 这大大提高了 IDE 响应速度，特别是在大型项目中。
     *
     * ## 分析范围
     *
     * 部分分析会处理：
     * - 元素本身的类型推导
     * - 元素的直接依赖（如引用的声明）
     * - 必要的上下文信息（如包含它的类或函数）
     *
     * ## 使用场景
     *
     * - **代码补全**: 获取光标位置的类型信息
     * - **快速修复**: 分析单个错误位置
     * - **悬浮文档**: 显示符号的类型和文档
     * - **引用解析**: 查找符号的定义
     *
     * @param element 要分析的 CangJie PSI 元素
     * @return 包含分析结果的绑定上下文（BindingContext），可用于查询类型、解析引用等
     */
    fun partialAnalyze(element: CjElement): BindingContext

    /**
     * 解析 import 语句中的引用
     *
     * 该方法解析 `import` 语句中指定的完全限定名（FqName），
     * 返回所有匹配的声明描述符。这包括类、函数、变量、类型别名等。
     *
     * ## 解析逻辑
     *
     * 1. 在当前模块的依赖中搜索指定的 FqName
     * 2. 查找所有可见性允许的匹配声明
     * 3. 返回所有找到的声明描述符
     *
     * ## 使用场景
     *
     * - **导入语句高亮**: 判断导入是否有效
     * - **自动导入**: 找到要导入的符号
     * - **导航到导入**: 从 import 语句跳转到声明
     * - **优化导入**: 移除未使用的导入
     *
     * ## 示例
     *
     * ```kotlin
     * // 对于 import std.collection.ArrayList
     * resolveImportReference(file, FqName("std.collection.ArrayList"))
     * // 返回 ArrayList 类的 DeclarationDescriptor
     * ```
     *
     * @param file 包含 import 语句的文件
     * @param fqName 要解析的完全限定名
     * @return 匹配的声明描述符集合，可能为空（如果未找到）
     */
    fun resolveImportReference(file: CjFile, fqName: FqName): Collection<DeclarationDescriptor>

    /**
     * 从声明描述符查找对应的 PSI 声明元素
     *
     * 该方法是 Descriptor → PSI 转换的核心，它将编译器级别的描述符
     * 转换为 IDE 可以导航和显示的 PSI 元素。
     *
     * ## 查找范围
     *
     * 该方法会在以下位置查找：
     * - 项目源代码
     * - 依赖的库（通过反编译）
     * - 内置库（builtins）
     * - 指定的解析范围内的所有文件
     *
     * ## 处理多个声明
     *
     * 某些情况下，一个 Descriptor 可能对应多个 PSI 元素：
     * - 伪覆盖（fake override）方法有多个实际声明
     * - 同一符号在源码和反编译代码中都存在
     *
     * ## 使用场景
     *
     * - **跳转到声明**: 从引用跳转到定义位置
     * - **查找用法**: 找到符号的所有使用位置
     * - **重构**: 重命名、移动等需要找到所有相关 PSI 元素
     *
     * @param declaration 要查找的声明描述符
     * @param project 当前项目
     * @param resolveScope 查找范围，限制在哪些文件中搜索
     * @return 找到的所有 PSI 元素，可能包含源码和反编译的声明
     */
    fun findPsiDeclarations(
        declaration: DeclarationDescriptor,
        project: Project,
        resolveScope: GlobalSearchScope
    ): Collection<PsiElement>

    /**
     * 解析 CDoc 文档注释中的链接引用
     *
     * CDoc 是仓颉语言的文档注释系统（类似于 JavaDoc、KDoc）。
     * 该方法解析文档注释中的符号引用，例如 `@see` 标签或 `{@link}` 标签中的符号。
     *
     * ## 解析上下文
     *
     * CDoc 链接的解析需要考虑：
     * - 当前声明的作用域
     * - 可见性规则（private 成员在外部不可见）
     * - 导入的符号
     * - 完全限定名
     *
     * ## 使用场景
     *
     * - **文档链接导航**: 从文档注释跳转到引用的声明
     * - **文档生成**: 构建 API 文档时解析链接
     * - **文档验证**: 检查文档中的链接是否有效
     * - **快速文档**: 在悬浮窗口中显示链接的文档
     *
     * ## 示例
     *
     * ```kotlin
     * /**
     *  * 参见 {@link ArrayList} 获取更多信息
     *  */
     * // resolveCDocLink 会解析 "ArrayList" 到对应的类描述符
     * ```
     *
     * @param element CDoc 中的名称元素（如链接中的符号名）
     * @return 匹配的声明描述符集合，可能为空（如果链接无效）
     */
    fun resolveCDocLink(element: CDocName): Collection<DeclarationDescriptor>

    /**
     * 从反编译代码中查找声明
     *
     * 当引用指向编译后的库文件（.cjo 文件）时，需要通过反编译来获取对应的 PSI 声明。
     * 该方法负责从编译后的元数据中查找并构建 PSI 声明元素。
     *
     * ## 反编译流程
     *
     * 1. 根据 Descriptor 确定对应的编译文件位置
     * 2. 读取并解析元数据（使用 Flatbuffers 格式）
     * 3. 从元数据构建 PSI Stub 树
     * 4. 创建或获取对应的 PSI 声明元素
     *
     * ## 查找范围
     *
     * - 如果提供了 `builtInsSearchScope`，则只在内置库范围内查找
     * - 否则在所有库依赖中查找
     *
     * ## 使用场景
     *
     * - **库符号导航**: 跳转到依赖库中的类、函数定义
     * - **代码补全**: 显示库 API 的详细信息
     * - **查看源码**: 显示反编译后的库代码
     * - **类型推导**: 获取库类型的详细信息
     *
     * ## 性能考虑
     *
     * 反编译是相对耗时的操作，结果会被缓存。
     * 只有在需要导航或显示详细信息时才会触发反编译。
     *
     * @param project 当前项目
     * @param referencedDescriptor 被引用的声明描述符（来自编译后的库）
     * @param builtInsSearchScope 内置库的搜索范围（可选），用于限制只查找内置库
     * @return 反编译得到的 CangJie 声明 PSI 元素，如果未找到则返回 null
     */
    fun findDecompiledDeclaration(
        project: Project,
        referencedDescriptor: DeclarationDescriptor,
        builtInsSearchScope: GlobalSearchScope?
    ): CjDeclaration?

    companion object {
        /**
         * 获取引用解析辅助服务的单例实例
         *
         * 该方法通过 IntelliJ Platform 的应用级服务系统获取服务实例。
         * 服务在应用启动时自动初始化，并在整个 IDE 生命周期内保持单例。
         *
         * @return 引用解析辅助服务的实例
         */
        fun getInstance(): CjReferenceResolutionHelper =
            ApplicationManager.getApplication().getService(CjReferenceResolutionHelper::class.java)
    }
}
