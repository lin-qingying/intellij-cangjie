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

package org.cangnova.cangjie.utils

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.types.CangJieType
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import kotlin.reflect.KProperty

/**
 * 代码片段工具
 *
 * 提供代码片段（Code Fragment）相关的工具函数和属性。
 * 代码片段主要用于以下场景：
 * - 调试器中的表达式求值（Debugger Expression Evaluator）
 * - IDE 控制台中的代码执行
 * - 快速文档预览
 * - 代码模板实例化
 *
 * **代码片段特点**：
 * - 不是完整的 .cj 文件，而是一段独立的代码
 * - 可以访问外部作用域的声明（通过 [externalDescriptors]）
 * - 支持运行时类型求值（通过 [RUNTIME_TYPE_EVALUATOR]）
 */
object CodeFragmentUtils {
    /**
     * 运行时类型求值器的键
     *
     * 用于在调试器中对表达式进行运行时类型求值。
     * 求值器函数接收一个表达式，返回其在当前调试上下文中的运行时类型。
     *
     * **使用场景**：
     * - 调试器表达式求值时获取变量的实际类型（而非声明类型）
     * - 智能类型转换提示
     * - 类型推导优化
     *
     * @see CjCodeFragment.getCopyableUserData
     */
    val RUNTIME_TYPE_EVALUATOR: Key<Function1<CjExpression, CangJieType?>> = Key.create("RUNTIME_TYPE_EVALUATOR")

    /**
     * 标记代码片段是否用于 IR 求值器的编译
     *
     * 调试器求值器有两种模式：
     * - 解释执行模式：直接解释执行代码片段
     * - 编译执行模式：将代码片段编译为 IR 后执行（更快但启动慢）
     *
     * 此标记用于区分这两种模式，影响分析缓存策略。
     * 详见 [PerFileAnalysisCache.getAnalysisResults]。
     *
     * @see CjCodeFragment.getCopyableUserData
     */
    val USED_FOR_COMPILATION_IN_IR_EVALUATOR: Key<Boolean> = Key.create("USED_FOR_COMPILATION_IN_EVALUATOR")
}

/**
 * 代码片段的外部描述符列表
 *
 * 代码片段可以引用其外部作用域中的声明，例如：
 * - 调试器中当前方法的局部变量
 * - 当前类的成员变量和方法
 * - 外部导入的类型和函数
 *
 * 此属性存储所有可以被代码片段访问的外部声明描述符。
 * 分析引擎在解析代码片段时会将这些描述符加入到作用域中。
 *
 * **使用示例**：
 * ```kotlin
 * val codeFragment = CjPsiFactory(project).createCodeFragment("x + y", context)
 * codeFragment.externalDescriptors = listOf(xDescriptor, yDescriptor)
 * ```
 *
 * @receiver CjCodeFragment 代码片段
 * @see CopyablePsiUserDataProperty
 */
var CjCodeFragment.externalDescriptors: List<DeclarationDescriptor>? by CopyablePsiUserDataProperty(Key.create("EXTERNAL_DESCRIPTORS"))

/**
 * 可复制的 PSI 用户数据属性委托
 *
 * IntelliJ 的 PSI 元素支持两种用户数据存储：
 * - 普通用户数据（UserData）：PSI 元素复制时不会复制
 * - 可复制用户数据（CopyableUserData）：PSI 元素复制时会一起复制
 *
 * 此类提供了 Kotlin 属性委托的方式来访问可复制用户数据，
 * 使得代码更加简洁和类型安全。
 *
 * **使用示例**：
 * ```kotlin
 * var MyElement.myProperty: String? by CopyablePsiUserDataProperty(Key.create("MY_KEY"))
 *
 * element.myProperty = "value"
 * val copy = element.copy()
 * println(copy.myProperty)  // 输出 "value"（数据被复制了）
 * ```
 *
 * @param R PSI 元素类型（必须继承自 PsiElement）
 * @param T 存储的数据类型
 * @property key 用户数据的键
 */
class CopyablePsiUserDataProperty<in R : PsiElement, T : Any>(val key: Key<T>) {
    /**
     * 获取属性值
     *
     * @param thisRef PSI 元素实例
     * @param property 属性元数据（未使用）
     * @return T? 存储的数据，如果不存在则返回 null
     */
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getCopyableUserData(key)

    /**
     * 设置属性值
     *
     * @param thisRef PSI 元素实例
     * @param property 属性元数据（未使用）
     * @param value 要存储的数据，null 表示删除数据
     */
    operator fun setValue(thisRef: R, property: KProperty<*>, value: T?) = thisRef.putCopyableUserData(key, value)
}
