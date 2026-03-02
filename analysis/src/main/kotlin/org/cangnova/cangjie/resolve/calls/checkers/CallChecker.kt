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

package org.cangnova.cangjie.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.resolve.MissingSupertypesResolver
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.model.CangJieCallComponents
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.DeferredType


/**
 * 检查器上下文接口
 *
 * 为调用检查器提供必要的上下文信息,包括绑定跟踪、废弃功能解析器、
 * 语言版本设置和模块描述符等核心组件。
 */
interface CheckerContext {
    /** 绑定跟踪器,用于记录类型解析和绑定信息 */
    val trace: BindingTrace

    /** 废弃功能解析器,用于检查和处理已废弃的API */
    val deprecationResolver: DeprecationResolver

    /** 语言版本设置,用于确定当前使用的语言特性和规则 */
    val languageVersionSettings: LanguageVersionSettings

    /** 模块描述符,表示当前正在处理的模块 */
    val moduleDescriptor: ModuleDescriptor
}


/**
 * 调用检查器接口
 *
 * 用于对已解析的函数或方法调用进行额外的语义检查,例如:
 * - 参数类型兼容性检查
 * - 废弃API使用检查
 * - 可见性检查
 * - 其他语言特定的规则验证
 */
interface CallChecker {
    /**
     * 执行调用检查
     *
     * 注意:[reportOn] 参数仅应用作检查器报告诊断信息的目标元素。
     * 检查器的逻辑不应依赖于诊断信息的目标元素是什么!
     *
     * @param resolvedCall 已解析的调用,包含调用目标和参数信息
     * @param reportOn 用于报告诊断信息的PSI元素
     * @param context 调用检查器上下文,提供检查所需的各种信息
     */
    fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext)
}

/**
 * 调用检查器上下文实现类
 *
 * 封装了执行调用检查所需的所有上下文信息,包括解析上下文、
 * 废弃功能解析器、模块描述符、缺失父类型解析器等。
 *
 * @param resolutionContext 解析上下文,包含作用域、数据流信息等
 * @param deprecationResolver 废弃功能解析器
 * @param moduleDescriptor 模块描述符
 * @param missingSupertypesResolver 缺失父类型解析器,用于处理类型层次结构中的缺失类型
 * @param callComponents 仓颉语言调用组件,提供调用解析相关的工具
 * @param trace 绑定跟踪器,默认使用解析上下文中的trace
 */
class CallCheckerContext @JvmOverloads constructor(
    val resolutionContext: ResolutionContext<*>,
    override val deprecationResolver: DeprecationResolver,
    override val moduleDescriptor: ModuleDescriptor,
    val missingSupertypesResolver: MissingSupertypesResolver,
    val callComponents: CangJieCallComponents,
    override val trace: BindingTrace = resolutionContext.trace
) : CheckerContext {

    /** 获取当前的词法作用域 */
    val scope: LexicalScope
        get() = resolutionContext.scope

    /** 获取数据流信息,用于跟踪变量的可空性、智能类型转换等信息 */
    val dataFlowInfo: DataFlowInfo
        get() = resolutionContext.dataFlowInfo

    /** 判断当前是否处于注解上下文中 */
    val isAnnotationContext: Boolean
        get() = resolutionContext.isAnnotationContext

    /** 获取语言版本设置 */
    override val languageVersionSettings: LanguageVersionSettings
        get() = resolutionContext.languageVersionSettings

    /** 获取数据流值工厂,用于创建数据流分析中的值表示 */
    val dataFlowValueFactory: DataFlowValueFactory
        get() = resolutionContext.dataFlowValueFactory
}

/**
 * 工具函数:避免过早计算已解析可调用描述符的延迟返回类型
 *
 * 在 CallChecker#check 方法中计算延迟类型是不可行的,因为这会触发
 * "类型检查遇到递归问题"的错误。
 *
 * 接收者参数的存在是为了强调这个函数理想情况下应该只从调用检查器中使用。
 *
 * @param type 要检查的类型
 * @return 如果类型是正在计算中的延迟类型则返回true
 */
@Suppress("unused")
fun CallChecker.isComputingDeferredType(type: CangJieType) =
    type is DeferredType && type.isComputing