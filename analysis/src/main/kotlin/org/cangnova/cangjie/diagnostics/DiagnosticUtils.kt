/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.diagnostics


import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjReferenceExpression

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.diagnostics.PsiDiagnosticUtils.Companion.offsetToLineAndColumn
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.resolve.DescriptorUtils.getContainingClass
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.checker.isCaptured
import org.cangnova.cangjie.resolve.calls.context.CallPosition
import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.model.VariableAsFunctionResolvedCall
import org.cangnova.cangjie.resolve.calls.util.getEffectiveExpectedType
import org.cangnova.cangjie.resolve.calls.util.getResolvedCall
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.SubstitutionOptions
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.TypeUtils.noExpectedType
import org.cangnova.cangjie.types.isAny
import org.cangnova.cangjie.types.isFunctionType
import org.cangnova.cangjie.types.isNothing

/**
 * 在声明位置报告诊断信息
 *
 * 根据描述符查找对应的 PSI 元素，并在该位置报告诊断信息。
 * 如果找不到对应的声明位置，不会报告任何错误。
 *
 * @param trace 绑定跟踪器，用于记录诊断信息
 * @param descriptor 声明描述符
 * @param what 根据 PSI 元素生成诊断信息的函数
 */
inline fun reportOnDeclaration(
    trace: BindingTrace,
    descriptor: DeclarationDescriptor,
    what: (PsiElement) -> Diagnostic
) {
    DescriptorToSourceUtils.descriptorToDeclaration(descriptor)?.let { psiElement ->
        trace.report(what(psiElement))
    }
}

/**
 * 诊断工具类
 *
 * 提供各种诊断相关的辅助功能。
 */
object DiagnosticUtils {
    /**
     * 获取文件中指定范围的行号和列号
     *
     * @param file PSI 文件
     * @param range 文本范围
     * @return 行号和列号信息
     */
    fun getLineAndColumnInPsiFile(
        file: PsiFile,
        range: TextRange
    ): PsiDiagnosticUtils.LineAndColumn {
        val document = file.viewProvider.document
        return offsetToLineAndColumn(document, range.startOffset)
    }

    /**
     * 如果在服务器模式下运行，抛出异常
     *
     * Web Demo 服务器需要记录来自分析器的异常，
     * 而不是在编辑器中显示它们。在单元测试模式下也会抛出异常。
     *
     * @param e 要处理的异常
     * @throws RuntimeException 如果在服务器模式或单元测试模式下运行
     */
    fun throwIfRunningOnServer(e: Throwable?) {
        // 检查是否在服务器模式或单元测试模式下运行
        if (System.getProperty(
                "cangjie.running.in.server.mode",
                "false"
            ) == "true" || ApplicationManager.getApplication().isUnitTestMode
        ) {
            // 保持原始异常类型
            if (e is RuntimeException) {
                throw (e as RuntimeException?) ?: return
            }
            if (e is Error) {
                throw (e as Error?) ?: return
            }
            // 包装为 RuntimeException
            throw RuntimeException(e)
        }
    }

}

/**
 * 在声明位置报告诊断信息或失败
 *
 * 与 [reportOnDeclaration] 类似，但如果找不到声明位置会抛出异常。
 * 用于必须能找到声明位置的场景。
 *
 * @param trace 绑定跟踪器
 * @param descriptor 声明描述符
 * @param what 生成诊断信息的函数
 * @throws AssertionError 如果找不到对应的声明位置
 */
inline fun reportOnDeclarationOrFail(
    trace: BindingTrace,
    descriptor: DeclarationDescriptor,
    what: (PsiElement) -> Diagnostic
) {
    DescriptorToSourceUtils.descriptorToDeclaration(descriptor)?.let { psiElement ->
        trace.report(what(psiElement))
    } ?: throw AssertionError("No declaration for $descriptor")
}

/**
 * 仅报告一次诊断信息
 *
 * 检查相同类型的诊断信息是否已经在同一元素上报告过。
 * 如果已经报告过，则跳过；否则报告新的诊断信息。
 *
 * 这避免了在同一位置重复显示相同的错误消息。
 *
 * @receiver 绑定跟踪器
 * @param diagnostic 要报告的诊断信息
 */
fun BindingTrace.reportDiagnosticOnce(diagnostic: Diagnostic) {
    // 检查是否已经报告过相同工厂的诊断信息
    if (bindingContext.diagnostics.noSuppression().forElement(diagnostic.psiElement)
            .any { it.factory == diagnostic.factory }
    ) return

    report(diagnostic)
}

/**
 * 报告由于类型投影导致的类型不匹配错误
 *
 * 当类型投影（type projection）导致类型不匹配时，报告特定的错误信息。
 * 类型投影通常发生在泛型类型的协变和逆变场景中。
 *
 * ## 处理场景
 * 1. **值参数位置**: 函数调用时的参数类型不匹配
 * 2. **变量赋值**: 赋值语句右侧的类型不匹配
 *
 * ## 检查逻辑
 * 1. 验证期望类型中是否包含 Any 或 Nothing 类型
 * 2. 获取已解析的调用和接收者类型
 * 3. 创建不进行近似的类型替换器
 * 4. 检查替换后的类型是否包含捕获类型
 * 5. 报告相应的错误信息
 *
 * @receiver 解析上下文
 * @param expression 表达式
 * @param expectedType 期望的类型
 * @param expressionType 表达式的实际类型
 * @return 如果报告了错误返回 true，否则返回 false
 */
fun ResolutionContext<*>.reportTypeMismatchDueToTypeProjection(
    expression: CjElement,
    expectedType: CangJieType,
    expressionType: CangJieType?
): Boolean {
    // 检查期望类型是否包含 Any 或 Nothing
    if (!TypeUtils.contains(expectedType) {
            // 确保期望类型可用，否则会抛出异常
            !noExpectedType(it) && (it.isAny() || it.isNothing())
        }
    ) return false

    // 根据调用位置获取已解析的调用和期望类型
    val (resolvedCall, correspondingNotApproximatedTypeByDescriptor: (CallableDescriptor) -> CangJieType?) = when (callPosition) {
        // 值参数位置：函数调用的参数
        is CallPosition.ValueArgumentPosition ->
            callPosition.resolvedCall to { f: CallableDescriptor ->
                getEffectiveExpectedType(
                    f.valueParameters[callPosition.valueParameter.index],
                    callPosition.valueArgument,
                    this
                )
            }

        // 变量赋值位置
        is CallPosition.VariableAssignment -> {
            // 如果是赋值左侧，不处理
            if (callPosition.isLeft) return false
            val resolvedCall = callPosition.leftPart.getResolvedCall(trace.bindingContext) ?: return false
            resolvedCall to { f: CallableDescriptor -> null }
        }

        // 属性赋值位置（已注释，暂未启用）
//        is CallPosition.PropertyAssignment -> {
//            if (callPosition.isLeft) return false
//            val resolvedCall = callPosition.leftPart.getResolvedCall(trace.bindingContext) ?: return false
//            resolvedCall to { f: CallableDescriptor -> (f as? PropertyDescriptor)?.setter?.valueParameters?.get(0)?.type }
//        }

        // 未知位置或其他位置，不处理
        is CallPosition.Unknown/*, is CallPosition.CallableReferenceRhs*/ -> return false
    }

    // 获取接收者类型（优先使用智能转换后的类型）
    val receiverType = resolvedCall.smartCastDispatchReceiverType
        ?: (resolvedCall.dispatchReceiver ?: return false).type

    // 获取原始的可调用描述符
    val callableDescriptor = resolvedCall.resultingDescriptor.original

    // 创建不进行捕获类型近似的替换器
    val substitutedDescriptor =
        ComposableTypeSubstitutor.create(
            receiverType,
            SubstitutionOptions.DEFAULT.copy(approximateCapturedTypes = false)
        ).let { callableDescriptor.substitute(it) } ?: return false

    // 获取非近似的期望类型
    val nonApproximatedExpectedType =
        correspondingNotApproximatedTypeByDescriptor(substitutedDescriptor) ?: return false

    // 检查是否包含捕获类型
    if (!TypeUtils.contains(nonApproximatedExpectedType) { it.isCaptured() }) return false

    // 根据期望类型报告不同的错误
    if (expectedType.isNothing()) {
        // 属性赋值的情况（已注释）
        /*        if (callPosition is CallPosition.PropertyAssignment) {
                    trace.report(
                         SETTER_PROJECTED_OUT.on(
                            callPosition.leftPart ?: return false,
                            resolvedCall.resultingDescriptor
                        )
                    )
                } else {*/

        val call = resolvedCall.call
        // 确定报告位置（变量作为函数调用时使用变量表达式）
        val reportOn =
            if (resolvedCall is VariableAsFunctionResolvedCall)
                resolvedCall.variableCall.call.calleeExpression
            else
                call.calleeExpression

        // 报告成员被投影的错误
        trace.reportDiagnosticOnce(
            MEMBER_PROJECTED.on(
                reportOn ?: call.callElement,
                callableDescriptor,
                receiverType
            )
        )
//        }
    } else {
        // expressionType 在报告 CONSTANT_EXPECTED_TYPE_MISMATCH 时可能为 null
        // （参见 addAll.kt 测试）
        expressionType ?: return false

        // 报告由于类型投影导致的类型不匹配
        trace.report(
            TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS.on(
                expression, TypeMismatchDueToTypeProjectionsData(
                    expectedType, expressionType, receiverType, callableDescriptor
                )
            )
        )
    }

    return true
}

/**
 * 报告由于类 Scala 的命名函数语法导致的类型不匹配
 *
 * 检测用户是否在命名函数中使用了 `= { ... }` 语法（类似 Scala），
 * 这会导致返回类型为函数类型而不是期望的类型。
 *
 * ## 示例
 * ```kotlin
 * fun foo(): Int = {  // 错误：返回了 lambda 而不是 Int
 *     42
 * }
 * ```
 *
 * 正确写法应该是：
 * ```kotlin
 * fun foo(): Int {
 *     return 42
 * }
 * ```
 *
 * @receiver 解析上下文
 * @param expression 表达式
 * @param expectedType 期望的类型
 * @param expressionType 表达式的实际类型
 * @return 如果报告了错误返回 true，否则返回 false
 */
fun ResolutionContext<*>.reportTypeMismatchDueToScalaLikeNamedFunctionSyntax(
    expression: CjElement,
    expectedType: CangJieType,
    expressionType: CangJieType?
): Boolean {
    if (expressionType == null) return false

    // 检查是否是函数类型不匹配且使用了 = lambda 语法
    if (expressionType.isFunctionType && !expectedType.isFunctionType && isScalaLikeEqualsBlock(expression)) {
        trace.report(TYPE_MISMATCH_DUE_TO_EQUALS_LAMBDA_IN_FUN.on(expression, expectedType))
        return true
    }

    return false
}

/**
 * 检查是否是类 Scala 的等号代码块语法
 *
 * 判断表达式是否是命名函数中使用 `=` 后跟 lambda 表达式的情况。
 *
 * @param expression 要检查的表达式
 * @return 如果是类 Scala 语法返回 true，否则返回 false
 */
private fun isScalaLikeEqualsBlock(expression: CjElement): Boolean =
    expression is CjLambdaExpression &&
            expression.parent.let { it is CjNamedFunction && it.equalsToken != null }

/**
 * 类型不匹配数据（由于类型投影）
 *
 * 封装由于类型投影导致的类型不匹配的相关信息。
 *
 * @property expectedType 期望的类型
 * @property expressionType 表达式的实际类型
 * @property receiverType 接收者类型
 * @property callableDescriptor 可调用描述符
 */
class TypeMismatchDueToTypeProjectionsData(
    val expectedType: CangJieType,
    val expressionType: CangJieType,
    val receiverType: CangJieType,
    val callableDescriptor: CallableDescriptor
)

/**
 * 无效的二元操作数据
 *
 * 封装二元操作符错误使用的相关信息。
 *
 * @property operatorString 操作符字符串
 * @property leftType 左操作数类型
 * @property rightType 右操作数类型
 */
class InvalidBinaryData(
    val operatorString: String,
    val leftType: CangJieType,
    val rightType: CangJieType
)

/**
 * 检查成员描述符是否实际上是外部的
 *
 * 判断一个成员是否应该被视为外部（external）成员。
 * 这包括直接标记为 external 的成员，以及其包含类为 external 的成员。
 *
 * ## 检查规则
 * 1. 如果成员本身标记为 external，返回 true（已注释）
 * 2. 如果成员是属性访问器，检查对应的属性（已注释）
 * 3. 如果成员是属性且其 getter/setter 为 external，返回 true（已注释）
 * 4. 如果包含类为 external，返回 true
 *
 * @receiver 成员描述符
 * @return 如果实际上是外部成员返回 true，否则返回 false
 */
fun MemberDescriptor.isEffectivelyExternal(): Boolean {
    // 以下检查已被注释，当前仅检查包含类
//    if (isExternal) return true
//
//    if (this is PropertyAccessorDescriptor) {
//        val variableDescriptor = correspondingProperty
//        if (variableDescriptor.isEffectivelyExternal()) return true
//    }

//    if (this is PropertyDescriptor) {
//        if (getter?.isExternal == true &&
//            (!isVar || setter?.isExternal == true)
//        ) return true
//    }

    // 检查包含类是否为 external
    val containingClass = getContainingClass(this)
    return containingClass != null && containingClass.isEffectivelyExternal()
}

// ==================== 错误报告扩展方法 ====================

/**
 * 报告未解析引用错误 (UNRESOLVED_REFERENCE)
 *
 * **重要**: 这是报告 UNRESOLVED_REFERENCE 的唯一入口。
 * 所有需要报告未解析引用错误的地方都必须使用此方法，
 * 而不是直接调用 `trace.report(UNRESOLVED_REFERENCE.on(...))`。
 *
 * ## 设计意图
 * 此方法确保:
 * 1. 错误报告的一致性 - 所有未解析引用使用统一的报告方式
 * 2. 便于后续添加额外的错误处理逻辑（如日志记录、统计等）
 * 3. 便于全局搜索和重构 - 只需查找此方法的调用位置
 *
 * ## 使用场景
 * - 无法解析的变量引用
 * - 无法解析的函数调用
 * - 无法解析的类型引用
 * - 其他所有无法解析的符号引用
 *
 * @receiver 绑定跟踪器
 * @param expression 未解析的引用表达式
 *
 * @see BindingTrace.reportInvisibleReference 报告不可见引用错误
 */
fun BindingTrace.reportUnresolvedReference(expression: CjReferenceExpression) {
    report(UNRESOLVED_REFERENCE.on(expression, expression))
}

/**
 * 报告不可见引用错误 (INVISIBLE_REFERENCE)
 *
 * **重要**: 这是报告 INVISIBLE_REFERENCE 的唯一入口。
 * 所有需要报告不可见引用错误的地方都必须使用此方法。
 *
 * ## 使用场景
 * 当符号可以被解析，但由于可见性限制无法访问时使用此方法：
 * - 访问私有成员
 * - 访问保护成员
 * - 访问内部成员（跨模块）
 * - 访问其他受限制的成员
 *
 * ## 与 UNRESOLVED_REFERENCE 的区别
 * - UNRESOLVED_REFERENCE: 完全找不到符号
 * - INVISIBLE_REFERENCE: 能找到符号但无权访问
 *
 * @receiver 绑定跟踪器
 * @param expression 引用表达式
 * @param descriptor 不可见的声明描述符（包含可见性信息）
 *
 * @see BindingTrace.reportUnresolvedReference 报告未解析引用错误
 */
fun BindingTrace.reportInvisibleReference(
    expression: CjSimpleNameExpression,
    descriptor: DeclarationDescriptorWithVisibility
) {
    report(
        INVISIBLE_REFERENCE.on(
            expression,
            descriptor,
            descriptor.visibility,
            descriptor
        )
    )
}