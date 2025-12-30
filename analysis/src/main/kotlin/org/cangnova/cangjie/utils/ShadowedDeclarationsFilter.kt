/*
 * Copyright 2024 LinQingYing. and contributors.
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

import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.createExpressionByPattern
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.context.CheckArgumentTypesMode
import org.cangnova.cangjie.resolve.calls.context.ContextDependency
import org.cangnova.cangjie.resolve.scopes.ExplicitImportsScope
import org.cangnova.cangjie.resolve.scopes.addImportingScope
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.Receiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTraceFilter.Companion.NO_DIAGNOSTICS
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.binding.getDataFlowInfoBefore
import org.cangnova.cangjie.resolve.calls.util.CallTypeAndReceiver
import org.cangnova.cangjie.types.TypeUtils
import kotlin.reflect.KProperty

/**
 * 被遮蔽声明过滤器
 *
 * 用于过滤代码补全中被其他声明遮蔽（shadowed）的符号。
 * 在仓颉语言中，如果多个同名符号在同一作用域内可见，遵循以下遮蔽规则：
 * - 更近作用域的声明遮蔽外层作用域的声明
 * - 精确导入的声明遮蔽通配符导入的声明
 * - 本地声明遮蔽导入的声明
 *
 * **工作原理**：
 * 1. 将声明按签名分组（相同签名的声明可能相互遮蔽）
 * 2. 对每组声明，模拟实际的调用解析过程
 * 3. 只保留解析成功的声明，过滤掉被遮蔽的声明
 *
 * **使用场景**：
 * - 代码补全：避免显示被遮蔽的符号
 * - 自动导入：选择正确的符号进行导入
 * - 符号引用解析：确定引用指向哪个声明
 *
 * **性能考虑**：
 * 过滤过程涉及完整的解析，开销较大。
 * 因此会进行多种优化：
 * - 提前检测结构等价的声明（classpath 中的重复库）
 * - 缓存虚拟表达式（dummy expressions）
 * - 单个声明组无需过滤
 *
 * @property bindingContext 绑定上下文，包含已解析的符号信息
 * @property resolutionFacade 解析门面，提供解析服务
 * @property context 当前 PSI 上下文元素（用于确定作用域）
 * @property explicitReceiverValue 显式接收器值（如 `foo.bar()` 中的 `foo`），null 表示无接收器
 */
class ShadowedDeclarationsFilter(
    private val bindingContext: BindingContext,
    private val resolutionFacade: ResolutionFacade,
    private val context: PsiElement,
    private val explicitReceiverValue: ReceiverValue?
) {
    companion object {
        /**
         * 创建被遮蔽声明过滤器
         *
         * 根据调用类型和接收器，创建合适的过滤器实例。
         *
         * **支持的调用类型**：
         * - DEFAULT - 默认调用（无接收器）：`foo()`
         * - DOT - 点调用（有接收器）：`obj.foo()`
         * - SAFE - 安全调用：`obj?.foo()`
         *
         * **不支持的调用类型**（返回 null）：
         * - 可调用引用（Callable References）：`::foo`
         * - 其他特殊调用类型
         *
         * @param bindingContext 绑定上下文
         * @param resolutionFacade 解析门面
         * @param context 当前 PSI 上下文元素
         * @param callTypeAndReceiver 调用类型和接收器信息
         * @return ShadowedDeclarationsFilter? 过滤器实例，如果不支持该调用类型则返回 null
         */
        fun create(
            bindingContext: BindingContext,
            resolutionFacade: ResolutionFacade,
            context: PsiElement,
            callTypeAndReceiver: CallTypeAndReceiver<*, *>
        ): ShadowedDeclarationsFilter? {
            val receiverExpression = when (callTypeAndReceiver) {
                is CallTypeAndReceiver.DEFAULT -> null
                is CallTypeAndReceiver.DOT -> callTypeAndReceiver.receiver
                is CallTypeAndReceiver.SAFE -> callTypeAndReceiver.receiver
//                is CallTypeAndReceiver.SUPER_MEMBERS -> callTypeAndReceiver.receiver
//                is CallTypeAndReceiver.INFIX -> callTypeAndReceiver.receiver
//                is CallTypeAndReceiver.TYPE, is CallTypeAndReceiver.ANNOTATION -> null // need filtering of classes with the same FQ-name
                else -> return null // TODO: support shadowed declarations filtering for callable references
            }

            val explicitReceiverValue = receiverExpression?.let {
                val type = bindingContext.getType(it) ?: return null
                ExpressionReceiver.create(it, type, bindingContext)
            }
            return ShadowedDeclarationsFilter(bindingContext, resolutionFacade, context, explicitReceiverValue)
        }
    }

    private val psiFactory = CjPsiFactory(resolutionFacade.project)
    private val dummyExpressionFactory = DummyExpressionFactory(psiFactory)

    /**
     * 过滤被遮蔽的声明
     *
     * 对声明集合进行过滤，移除被其他声明遮蔽的符号。
     * 这是过滤器的主入口方法，用于代码补全、自动导入等场景。
     *
     * **工作流程**：
     * 1. 按签名（Signature）对声明分组 - 相同签名的声明可能相互遮蔽
     * 2. 对每个签名组调用 [filterEqualSignatureGroup] 进行解析过滤
     * 3. 合并所有组的过滤结果
     *
     * **性能优化**：
     * - 使用签名分组避免不必要的解析（不同签名的声明不会相互遮蔽）
     * - 单个声明的组会被快速处理，无需完整解析
     *
     * **使用示例**：
     * ```kotlin
     * val candidates = listOf(localFoo, importedFoo, wildcardImportedFoo)
     * val visible = filter.filter(candidates)
     * // 结果可能只包含 localFoo（本地声明遮蔽导入的声明）
     * ```
     *
     * @param TDescriptor 声明描述符的类型（如 FunctionDescriptor, VariableDescriptor）
     * @param declarations 待过滤的声明集合
     * @return Collection<TDescriptor> 过滤后的声明集合（不包含被遮蔽的声明）
     */
    fun <TDescriptor : DeclarationDescriptor> filter(declarations: Collection<TDescriptor>): Collection<TDescriptor> =
        declarations.groupBy { signature(it) }.values.flatMap { group -> filterEqualSignatureGroup(group) }

    /**
     * 创建非导入声明过滤器
     *
     * 创建一个高阶函数，用于过滤未导入的声明，同时考虑被遮蔽关系。
     * 此方法主要用于自动导入功能，判断哪些符号需要显示在"导入建议"中。
     *
     * **核心逻辑**：
     * - 已导入的声明直接保留（无需再次导入）
     * - 未导入的声明需要检查是否被已导入的声明遮蔽
     * - 不同包中的同签名声明不会相互遮蔽（可以共存）
     *
     * **使用场景**：
     * - 代码补全：只显示可见的符号（已导入 + 可导入的未遮蔽符号）
     * - 自动导入建议：显示需要导入的符号
     * - 导入优化：移除被遮蔽的导入
     *
     * **allowExpectedDeclarations 参数**：
     * - 当前版本未启用 expect/actual 机制（注释代码）
     * - 保留此参数以备未来支持 expect/actual 声明
     *
     * **性能优化**：
     * - 提前过滤：如果没有导入声明，或只有一个未遮蔽的声明，直接返回
     * - 按包分组：同包符号分组处理，避免重复解析
     *
     * **使用示例**：
     * ```kotlin
     * val importedFunctions = file.getStrictParentOfType<CjImportDirective>()sItem.mapNotNull { it.resolveToDescriptor() }
     * val filter = shadowedFilter.createNonImportedDeclarationsFilter(importedFunctions, false)
     *
     * val allCandidates = findAllFunctionsNamed("foo")
     * val visibleCandidates = filter(allCandidates)
     * // visibleCandidates 只包含未被遮蔽的符号
     * ```
     *
     * @param TDescriptor 声明描述符的类型
     * @param importedDeclarations 已导入的声明集合
     * @param allowExpectedDeclarations 是否允许 expected 声明（当前未使用）
     * @return 过滤函数，接受声明集合并返回过滤后的集合
     */
    fun <TDescriptor : DeclarationDescriptor> createNonImportedDeclarationsFilter(
        importedDeclarations: Collection<DeclarationDescriptor>,
        allowExpectedDeclarations: Boolean,
    ): (Collection<TDescriptor>) -> Collection<TDescriptor> {
        val importedDeclarationsSet = if (allowExpectedDeclarations) {
            importedDeclarations.asSequence().flatMap {
//                if (it is MemberDescriptor && it.isActual) {
//                    sequenceOf(it) + it.findExpects().asSequence()
//                } else {
                sequenceOf(it)
//                }
            }.toSet()
        } else {
            importedDeclarations.toSet()
        }

        val importedDeclarationsBySignature = importedDeclarationsSet.groupBy { signature(it) }

        return filter@{ declarations ->
            // optimization
            if (declarations.isEmpty() || declarations.size == 1 && importedDeclarationsBySignature[signature(
                    declarations.single()
                )] == null
            ) return@filter declarations

            val nonImportedDeclarations = declarations.filter { it !in importedDeclarationsSet }

            val notShadowed = HashSet<DeclarationDescriptor>()
            // same signature non-imported declarations from different packages do not shadow each other
            for ((pair, group) in nonImportedDeclarations.groupBy { signature(it) to packageName(it) }) {
                val imported = importedDeclarationsBySignature[pair.first]
                val all = if (imported != null) group + imported else group
                notShadowed.addAll(filterEqualSignatureGroup(all, descriptorsToImport = group))
            }
            declarations.filter { it in notShadowed }
        }
    }

    /**
     * 生成声明的签名
     *
     * 为声明生成一个签名对象，用于判断两个声明是否可能相互遮蔽。
     * 只有签名相同的声明才可能相互遮蔽，因此签名用于分组和去重。
     *
     * **签名规则**：
     * - **SimpleFunctionDescriptor** - 使用 [FunctionSignature]（包含名称、参数类型、类型参数）
     * - **VariableDescriptor** - 使用变量名（变量不关心类型）
     * - **ClassDescriptor** - 使用全限定名（类名必须全局唯一）
     * - **其他描述符** - 使用描述符本身（如包、模块等）
     *
     * **为什么函数需要特殊处理**：
     * 函数支持重载，同名函数可以有不同的参数。
     * 因此函数签名必须包含参数信息，而不仅仅是名称。
     *
     * **为什么变量只用名称**：
     * 仓颉语言中，同一作用域内不能有同名变量（即使类型不同）。
     * 因此变量签名只需要名称即可。
     *
     * @param descriptor 声明描述符
     * @return Any 签名对象（可能是 FunctionSignature, Name, FqName 或描述符本身）
     */
    private fun signature(descriptor: DeclarationDescriptor): Any = when (descriptor) {
        is SimpleFunctionDescriptor -> FunctionSignature(descriptor)
        is VariableDescriptor -> descriptor.name
        is ClassDescriptor -> descriptor.importableFqName ?: descriptor
        else -> descriptor
    }

    /**
     * 获取声明的包名
     *
     * 返回声明所在的包的全限定名。
     * 用于判断两个声明是否来自同一个包，辅助遮蔽规则判断。
     *
     * **包名的作用**：
     * - 同包中的声明可能有不同的导入优先级
     * - 不同包的同签名声明不会相互遮蔽（需要全限定名区分）
     *
     * **实现细节**：
     * - [DeclarationDescriptor.importableFqName] 返回可导入的全限定名（如 `std.collection.ArrayList`）
     * - [FqName.parent] 返回父级包名（如 `std.collection`）
     *
     * @param descriptor 声明描述符
     * @return FqName? 包名，如果声明没有包名则返回 null
     */
    private fun packageName(descriptor: DeclarationDescriptor) = descriptor.importableFqName?.parent()

    /**
     * 过滤相同签名组中的声明
     *
     * 对具有相同签名的声明进行过滤，移除被遮蔽的声明。
     * 这是遮蔽过滤的核心实现，通过模拟实际的调用解析过程来判断哪些声明可见。
     *
     * **工作原理**：
     * 1. **快速路径**：如果只有一个声明，直接返回（无遮蔽可能）
     * 2. **结构等价检查**：如果所有声明结构等价（classpath 重复库），只保留第一个
     * 3. **类名冲突处理**：相同全限定名的类，只保留第一个
     * 4. **完整解析**：创建虚拟调用，执行真实的解析过程，找出哪些声明被解析成功
     *
     * **完整解析流程**：
     * - 构造虚拟参数表达式（dummy expressions）
     * - 创建虚拟调用（Call）对象，模拟实际的函数/变量调用
     * - 使用 [CallResolver] 执行解析
     * - 解析成功的声明即为可见的声明（未被遮蔽）
     *
     * **descriptorsToImport 参数**：
     * 当此方法用于"自动导入"场景时，`descriptorsToImport` 指定需要导入的声明。
     * 这些声明会被临时添加到作用域中，以模拟"导入后"的解析结果。
     *
     * **性能优化**：
     * - 缓存虚拟表达式：[DummyExpressionFactory] 复用 PSI 节点，避免重复创建
     * - 提前返回：单声明组、结构等价组无需完整解析
     * - 使用无诊断的 BindingTrace：避免记录错误信息
     *
     * **虚拟调用的构造**：
     * ```kotlin
     * // 模拟调用 foo(arg1, arg2, ...)
     * val call = object : Call {
     *     override val calleeExpression = createExpression("foo")
     *     override val valueArguments = listOf(DummyArgument(0), DummyArgument(1), ...)
     *     override val explicitReceiver = this@ShadowedDeclarationsFilter.explicitReceiverValue
     * }
     * ```
     *
     * **为什么使用完整解析**：
     * 遮蔽规则非常复杂，涉及：
     * - 作用域优先级（本地 > 导入 > 外层）
     * - 导入优先级（精确导入 > 通配符导入）
     * - 类型兼容性检查
     * - 重载解析（参数匹配）
     *
     * 直接使用编译器的解析器可以确保规则的一致性，避免重复实现和错误。
     *
     * **使用示例**：
     * ```kotlin
     * // 场景：本地变量 foo, 导入的函数 foo, 通配符导入的 foo
     * val group = listOf(localFoo, importedFoo, wildcardFoo)
     * val visible = filterEqualSignatureGroup(group)
     * // 结果：只有 localFoo（本地声明遮蔽其他）
     * ```
     *
     * @param TDescriptor 声明描述符的类型
     * @param descriptors 相同签名的声明集合
     * @param descriptorsToImport 需要临时导入的声明（用于自动导入场景）
     * @return Collection<TDescriptor> 过滤后的声明集合（解析成功的声明）
     */
    private fun <TDescriptor : DeclarationDescriptor> filterEqualSignatureGroup(
        descriptors: Collection<TDescriptor>,
        descriptorsToImport: Collection<TDescriptor> = emptyList()
    ): Collection<TDescriptor> {
        if (descriptors.size == 1) return descriptors

        val first = descriptors.firstOrNull {
            it is ClassDescriptor || it is ConstructorDescriptor || it is CallableDescriptor && !it.name.isSpecial
        } ?: return descriptors

        if (first is ClassDescriptor) { // for classes with the same FQ-name we simply take the first one
            return listOf(first)
        }

        // Optimization: if the descriptors are structurally equivalent then there is no need to run resolve.
        // This can happen when the classpath contains multiple copies of the same library.
        if (descriptors.all {
                DescriptorEquivalenceForOverrides.areEquivalent(
                    first,
                    it,
                    allowCopiesFromTheSameDeclaration = true
                )
            }) {
            return listOf(first)
        }

        val isFunction = first is FunctionDescriptor
        val name = when (first) {
            is ConstructorDescriptor -> first.constructedClass.name
            else -> first.name
        }
        val parameters = (first as CallableDescriptor).valueParameters

        val dummyArgumentExpressions = dummyExpressionFactory.createDummyExpressions(parameters.size)

        val bindingTrace = DelegatingBindingTrace(
            bindingContext, "Temporary trace for filtering shadowed declarations",
            filter = NO_DIAGNOSTICS
        )
        for ((expression, parameter) in dummyArgumentExpressions.zip(parameters)) {
            bindingTrace.recordType(expression, /*parameter.varargElementType ?:*/ parameter.type)
            bindingTrace.record(BindingContext.PROCESSED, expression, true)
        }

//        val firstVarargIndex = parameters.withIndex().firstOrNull { it.value.varargElementType != null }?.index
        val useNamedFromIndex =/*
            if (firstVarargIndex != null && firstVarargIndex != parameters.lastIndex) firstVarargIndex else*/
            parameters.size

        class DummyArgument(val index: Int) : ValueArgument {
            private val expression = dummyArgumentExpressions[index]

            private val argumentName: ValueArgumentName? = if (isNamed()) {
                object : ValueArgumentName {
                    override val asName = parameters[index].name
                    override val referenceExpression = null
                }
            } else {
                null
            }

            override fun getArgumentExpression() = expression
            override fun isNamed() = index >= useNamedFromIndex
            override fun getArgumentName() = argumentName
            override fun asElement() = expression
            override fun getSpreadElement() = null
            override fun isExternal() = false
        }

        val arguments = ArrayList<DummyArgument>()
        for (i in parameters.indices) {
            arguments.add(DummyArgument(i))
        }

        val newCall = object : Call {

            //val arguments = parameters.indices.map { DummyArgument(it) }
            val callee = psiFactory.createExpressionByPattern("$0", name, reformat = false)
            override var noValueArgument: Boolean  = false
            override var noTypeParameter: Boolean = false

            override val callOperationNode: ASTNode? = null
            override val explicitReceiver: Receiver? = explicitReceiverValue
            override val dispatchReceiver: ReceiverValue? = null
            override val calleeExpression: CjExpression = callee
            override val valueArgumentList: CjValueArgumentList? = null
            override val valueArguments: List<ValueArgument> = arguments
            override val functionLiteralArguments = emptyList<LambdaArgument>()
            override val typeArguments = emptyList<CjTypeProjection>()
            override val typeArgumentList: CjTypeArgumentList? = null
            override val callElement: CjElement = callee
            override val callType: Call.CallType = Call.CallType.DEFAULT
        }

        var scope = context.getResolutionScope(bindingContext, resolutionFacade)

        if (descriptorsToImport.isNotEmpty()) {
            scope = scope.addImportingScope(ExplicitImportsScope(descriptorsToImport))
        }

        val dataFlowInfo = bindingContext.getDataFlowInfoBefore(context)
        val context = BasicCallResolutionContext.create(
            bindingTrace, scope, newCall, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo,
            ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            false, resolutionFacade.languageVersionSettings,
            resolutionFacade.dataFlowValueFactory
        )

        @OptIn(FrontendInternals::class)
        val callResolver = resolutionFacade.frontendService<CallResolver>()
        val results =
            if (isFunction) callResolver.resolveFunctionCall(context) else callResolver.resolveSimpleVariable(context)
        val resultingDescriptors = results.resultingCalls.map { it.resultingDescriptor }
        val resultingOriginals = resultingDescriptors.mapTo(HashSet<DeclarationDescriptor>()) {
            it.original

        }
        val filtered = descriptors.filter { candidateDescriptor ->
            candidateDescriptor.original in resultingOriginals /* optimization */ && resultingDescriptors.any {
                descriptorsEqualWithSubstitution(
                    it,
                    candidateDescriptor
                )
            }
        }

        // Something went wrong, none of our declarations among resolve candidates, let's not filter anything
        return filtered.ifEmpty { descriptors }
    }

    /**
     * 虚拟表达式工厂
     *
     * 用于创建和缓存虚拟表达式（dummy expressions），提高遮蔽过滤的性能。
     *
     * **虚拟表达式的作用**：
     * 在模拟函数调用时，需要为每个参数创建一个表达式对象。
     * 这些表达式不需要实际的语法内容，只需要是有效的 PSI 节点即可。
     *
     * **为什么缓存**：
     * - 创建 PSI 节点有一定开销（解析、树构建）
     * - 虚拟表达式的内容是固定的（都是 "dummy"）
     * - 同一次过滤操作可能需要多次模拟调用，共享虚拟表达式可以减少开销
     *
     * **实现细节**：
     * - 使用 [ArrayList] 存储已创建的表达式
     * - 当请求 N 个表达式时，复用已有的表达式，不足的部分创建新的
     * - 表达式内容为 "dummy" 字符串（任意有效的标识符即可）
     *
     * **使用示例**：
     * ```kotlin
     * val factory = DummyExpressionFactory(psiFactory)
     * val args = factory.createDummyExpressions(3)
     * // 第一次调用：创建 3 个表达式
     * val moreArgs = factory.createDummyExpressions(5)
     * // 第二次调用：复用前 3 个，新创建 2 个
     * ```
     *
     * @property factory PSI 工厂，用于创建表达式
     */
    private class DummyExpressionFactory(val factory: CjPsiFactory) {
        /**
         * 已创建的虚拟表达式缓存
         */
        private val expressions = ArrayList<CjExpression>()

        /**
         * 创建指定数量的虚拟表达式
         *
         * 返回 N 个虚拟表达式，复用已缓存的表达式，不足的部分创建新的。
         *
         * @param count 需要的表达式数量
         * @return List<CjExpression> 虚拟表达式列表（大小为 count）
         */
        fun createDummyExpressions(count: Int): List<CjExpression> {
            while (expressions.size < count) {
                expressions.add(factory.createExpression("dummy"))
            }
            return expressions.take(count)
        }
    }

    /**
     * 函数签名
     *
     * 表示函数的签名信息，用于判断两个函数是否可能相互遮蔽。
     * 实现 [equals] 和 [hashCode]，支持作为 HashMap/HashSet 的键。
     *
     * **签名包含的信息**：
     * - 函数名称（[FunctionDescriptor.name]）
     * - 值参数类型和数量（[FunctionDescriptor.valueParameters]）
     * - 类型参数及其约束（[FunctionDescriptor.typeParameters]）
     *
     * **不包含的信息**：
     * - 返回类型（不影响重载解析中的候选筛选）
     * - 函数体（签名只关心声明）
     * - 修饰符（如 public, protected, inline 等）
     *
     * **为什么需要签名类**：
     * 直接比较 FunctionDescriptor 对象使用的是引用相等（===），无法判断结构等价性。
     * 签名类通过值相等（==）比较函数的核心特征，用于：
     * - 分组：将可能冲突的函数分到同一组
     * - 去重：检测 classpath 中的重复函数定义
     *
     * **仓颉语言的函数重载规则**：
     * 两个函数可以同名，当且仅当：
     * - 参数数量不同，或
     * - 参数类型不同（位置对应），或
     * - 类型参数的约束不同
     *
     * **equals 实现逻辑**：
     * 1. 检查函数名是否相同
     * 2. 检查参数数量是否相同
     * 3. 逐一比较参数类型（不比较 vararg，当前版本已注释）
     * 4. 检查类型参数数量是否相同
     * 5. 逐一比较类型参数的上界（约束）
     *
     * **hashCode 实现**：
     * 使用函数名和参数数量计算哈希值，平衡性能和碰撞率：
     * - `hashCode = name.hashCode() * 17 + parameterCount`
     * - 相同签名的函数必然有相同的哈希值
     * - 不同签名的函数可能有相同的哈希值（允许少量碰撞）
     *
     * **使用示例**：
     * ```kotlin
     * // foo(x: Int, y: String) 和 foo(a: Int, b: String) 有相同签名
     * val sig1 = FunctionSignature(foo1)
     * val sig2 = FunctionSignature(foo2)
     * sig1 == sig2  // true（参数类型相同）
     *
     * // foo(x: Int) 和 foo(x: Int, y: Int) 签名不同
     * val sig3 = FunctionSignature(foo3)
     * sig1 == sig3  // false（参数数量不同）
     * ```
     *
     * @property function 函数描述符
     */
    private class FunctionSignature(val function: FunctionDescriptor) {
        /**
         * 判断两个函数签名是否相等
         *
         * 签名相等的条件：
         * - 函数名相同
         * - 参数数量相同，且每个位置的参数类型相同
         * - 类型参数数量相同，且每个位置的类型参数约束相同
         *
         * @param other 另一个对象
         * @return Boolean true 表示签名相等
         */
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is FunctionSignature) return false
            if (function.name != other.function.name) return false
            val parameters1 = function.valueParameters
            val parameters2 = other.function.valueParameters
            if (parameters1.size != parameters2.size) return false
            for (i in parameters1.indices) {
                val p1 = parameters1[i]
                val p2 = parameters2[i]
//                if (p1.varargElementType != p2.varargElementType) return false // both should be vararg or or both not
                if (p1.type != p2.type) return false
            }

            val typeParameters1 = function.typeParameters
            val typeParameters2 = other.function.typeParameters
            if (typeParameters1.size != typeParameters2.size) return false
            for (i in typeParameters1.indices) {
                val t1 = typeParameters1[i]
                val t2 = typeParameters2[i]
                if (t1.upperBounds != t2.upperBounds) return false
            }
            return true
        }

        /**
         * 计算函数签名的哈希值
         *
         * 使用函数名和参数数量计算哈希值，用于 HashMap/HashSet 的快速查找。
         *
         * **实现策略**：
         * - 乘以质数 17 以分散哈希值，减少碰撞
         * - 只使用函数名和参数数量，计算成本低
         * - 允许少量碰撞（相同哈希值的签名会进一步用 equals 比较）
         *
         * **为什么不使用参数类型**：
         * - 类型对象的 hashCode 计算成本较高
         * - 函数名和参数数量已经能提供足够的区分度
         * - 碰撞后的 equals 比较会检查类型，不影响正确性
         *
         * @return Int 哈希值
         */
        override fun hashCode() = function.name.hashCode() * 17 + function.valueParameters.size
    }
}
