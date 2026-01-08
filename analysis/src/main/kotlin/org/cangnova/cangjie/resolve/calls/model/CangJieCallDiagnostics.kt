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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.NewConstraintError
import org.cangnova.cangjie.resolve.calls.inference.model.NewConstraintWarning
import org.cangnova.cangjie.resolve.calls.inference.model.transformToWarning
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.UnwrappedType


/**
 * 可转换为警告的诊断接口
 *
 * 某些错误级别的诊断信息可以降级为警告级别。
 * 实现此接口的诊断类可以提供转换逻辑。
 *
 * @param T 诊断类型
 */
interface TransformableToWarning<T : CangJieCallDiagnostic> {
    /**
     * 将当前诊断转换为警告级别
     *
     * @return 警告级别的诊断，如果无法转换则返回 null
     */
    fun transformToWarning(): T?
}


/**
 * 约束系统诊断
 *
 * 封装类型推断约束系统的错误信息。约束系统错误可能包括：
 * - 类型不匹配
 * - 类型参数推断失败
 * - 约束冲突
 *
 * @property error 约束系统错误详情
 */
class CangJieConstraintSystemDiagnostic(
    val error: ConstraintSystemError
) : CangJieCallDiagnostic(error.applicability), TransformableToWarning<CangJieConstraintSystemDiagnostic> {
    /**
     * 报告约束错误
     */
    override fun report(reporter: DiagnosticReporter) = reporter.constraintError(error)

    /**
     * 尝试将约束错误转换为警告
     *
     * 只有 NewConstraintError 类型的错误才能转换为警告
     */
    override fun transformToWarning(): CangJieConstraintSystemDiagnostic? =
        if (error is NewConstraintError) CangJieConstraintSystemDiagnostic(error.transformToWarning()) else null
}

/**
 * 获取诊断中的约束系统错误
 *
 * 如果诊断是 CangJieConstraintSystemDiagnostic 类型，返回其内部的约束错误
 */
val CangJieCallDiagnostic.constraintSystemError: ConstraintSystemError?
    get() = (this as? CangJieConstraintSystemDiagnostic)?.error

/**
 * 将约束系统错误转换为诊断
 */
fun ConstraintSystemError.asDiagnostic(): CangJieConstraintSystemDiagnostic = CangJieConstraintSystemDiagnostic(this)

/**
 * 将约束系统错误集合转换为诊断列表
 */
fun Collection<ConstraintSystemError>.asDiagnostics(): List<CangJieConstraintSystemDiagnostic> =
    map(ConstraintSystemError::asDiagnostic)

// 智能转换诊断

/**
 * 智能转换诊断
 *
 * 当参数通过智能转换到更具体的类型时生成此诊断。
 * 智能转换基于控制流分析，例如在 null 检查后将可空类型转换为非空类型。
 *
 * @property argument 被智能转换的表达式参数
 * @property smartCastType 智能转换后的目标类型
 * @property cangjieCall 相关的调用
 */
class SmartCastDiagnostic(
    val argument: ExpressionCangJieCallArgument,
    val smartCastType: UnwrappedType,
    val cangjieCall: CangJieCall?
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

/**
 * 不可调用的成员引用诊断
 *
 * 当可调用引用指向的成员无法被调用时生成。
 * 例如，引用指向了非函数/属性的成员。
 *
 * @property argument 可调用引用解析原子
 * @property candidate 被引用的候选描述符
 */
class NotCallableMemberReference(
    val argument: CallableReferenceResolutionAtom,
    val candidate: CallableDescriptor
) : CallableReferenceInapplicableDiagnostic(argument)

/**
 * 预期类型不可调用诊断
 *
 * 当可调用引用的预期类型不是函数类型时生成。
 * 例如，尝试将函数引用赋值给非函数类型的变量。
 *
 * @property argument 可调用引用参数
 * @property expectedType 预期的类型
 * @property notCallableTypeConstructor 不可调用的类型构造器
 */
class NotCallableExpectedType(
    val argument: CallableReferenceCangJieCallArgument,
    val expectedType: UnwrappedType,
    val notCallableTypeConstructor: TypeConstructor
) : CallableReferenceInapplicableDiagnostic(argument)

/**
 * 不安全调用错误
 *
 * 当在可空类型的接收者上调用成员而没有进行 null 检查时生成。
 * 例如：nullableObject.method() 而没有使用 ?. 或先检查 null。
 *
 * @property receiver 接收者参数
 * @property isForImplicitInvoke 是否用于隐式 invoke 调用
 */
class UnsafeCallError(
    val receiver: SimpleCangJieCallArgument,
    val isForImplicitInvoke: Boolean = false
) : CangJieCallDiagnostic(CandidateApplicability.UNSAFE_CALL) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

/**
 * 不稳定智能转换解析错误
 *
 * 当智能转换依赖于可变状态，在调用时无法保证安全时生成。
 * 例如，var 变量或可变属性的智能转换在并发环境中可能不安全。
 *
 * @param argument 表达式参数
 * @param targetType 目标类型
 */
class UnstableSmartCastResolutionError(
    argument: ExpressionCangJieCallArgument,
    targetType: UnwrappedType,
) : UnstableSmartCast(argument, targetType, CandidateApplicability.UNSTABLE_SMARTCAST)


/**
 * 不稳定智能转换基类
 *
 * 表示基于不稳定条件的智能转换，这些转换在某些情况下可能失效。
 * 不稳定智能转换的典型场景：
 * - var 变量的智能转换（值可能在检查后被修改）
 * - 可变属性的智能转换
 * - 自定义 getter 的属性智能转换
 *
 * @property argument 被智能转换的表达式参数
 * @property targetType 智能转换的目标类型
 * @param applicability 候选项适用性级别
 */
sealed class UnstableSmartCast(
    val argument: ExpressionCangJieCallArgument,
    val targetType: UnwrappedType,
    applicability: CandidateApplicability,
) : CangJieCallDiagnostic(applicability) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)

    companion object {
        /**
         * 创建不稳定智能转换诊断
         *
         * @param argument 表达式参数
         * @param targetType 目标类型
         * @param isReceiver 是否为接收者（保留以兼容旧推断）
         * @return 不稳定智能转换解析错误
         */
        operator fun invoke(
            argument: ExpressionCangJieCallArgument,
            targetType: UnwrappedType,
            @Suppress("UNUSED_PARAMETER") isReceiver: Boolean = false, // for reproducing OI behaviour
        ): UnstableSmartCast {
            return UnstableSmartCastResolutionError(argument, targetType)
        }
    }
}

/**
 * 参数传递两次诊断
 *
 * 当同一个参数被传递给函数多次时生成。
 * 例如：function(x = 1, x = 2) 或 function(1, x = 2) 当第一个位置参数对应 x。
 *
 * @property argument 重复传递的参数
 * @property parameterDescriptor 目标参数描述符
 * @property firstOccurrence 第一次传递的参数位置
 */
class ArgumentPassedTwice(
    val argument: CangJieCallArgument,
    val parameterDescriptor: ValueParameterDescriptor,
    val firstOccurrence: ResolvedCallArgument
) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

/**
 * 缺少命名参数前缀诊断
 *
 * 当参数需要命名但没有提供名称时生成。
 * 在某些上下文中，参数必须使用命名方式传递。
 *
 * @property argument 缺少名称的参数
 * @property names 可能的参数名称集合
 */
class MissingNamedArgumentPrefix(
    val argument: CangJieCallArgument,
    val names: Set<Name>

) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)

}

/**
 * 多个候选项调用诊断
 *
 * 当重载解析无法确定唯一的最佳候选项时生成。
 * 表示存在多个同样适用的函数重载，需要更多类型信息或更明确的调用。
 *
 * @property candidates 所有同等适用的候选项集合
 */
class ManyCandidatesCallDiagnostic(val candidates: Collection<ResolutionCandidate>) : CangJieCallDiagnostic(
    CandidateApplicability.INAPPLICABLE
) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

/**
 * 不允许命名参数诊断
 *
 * 当对不支持命名参数的函数使用命名参数时生成。
 * 某些函数（如操作符重载）不允许使用命名参数调用。
 *
 * @property argument 使用了名称的参数
 * @property descriptor 被调用的函数描述符
 */
class NamedArgumentNotAllowed(val argument: CangJieCallArgument, val descriptor: CallableDescriptor) :
    CangJieCallDiagnostic(
        CandidateApplicability.INAPPLICABLE
    ) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

// 候选项解析结果诊断

/**
 * 无候选项调用诊断
 *
 * 当没有找到任何适用的候选项时生成。
 * 表示调用无法解析到任何函数或属性。
 */
class NoneCandidatesCallDiagnostic : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

/**
 * 存根构建器推断接收者诊断
 *
 * 在使用存根类型进行类型推断时，标记推断的接收者。
 * 用于支持不完整代码的类型推断。
 *
 * @property receiver 推断的接收者参数
 */
class StubBuilderInferenceReceiver(
    val receiver: SimpleCangJieCallArgument,
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallReceiver(receiver, this)
}

/**
 * 无操作符函数诊断
 *
 * 当操作符调用找不到对应的操作符函数时生成。
 * 例如：left + right 但 left 类型没有定义 plus 操作符。
 *
 * @property left 左操作数类型
 * @property right 右操作数类型
 */
class NoneOperatorCallDiagnostic(val left: CangJieType, val right: CangJieType) :
    CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

/**
 * 过滤错误级别诊断
 *
 * 从诊断列表中过滤出错误级别的诊断，排除警告。
 * 约束系统中的 NewConstraintWarning 不被视为错误。
 *
 * @return 仅包含错误的诊断列表
 */
fun List<CangJieCallDiagnostic>.filterErrorDiagnostics() =
    filter { it !is CangJieConstraintSystemDiagnostic || it.error !is NewConstraintWarning }

// 参数到参数映射器诊断

/**
 * 参数过多诊断
 *
 * 当调用提供的参数数量超过函数定义的参数数量时生成。
 *
 * @property argument 多余的参数
 * @property descriptor 被调用的函数描述符
 */
class TooManyArguments(val argument: CangJieCallArgument, val descriptor: CallableDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

/**
 * 命名参数引用诊断
 *
 * 成功解析的命名参数引用，记录参数到参数描述符的映射。
 * 这是一个成功的诊断，用于跟踪参数绑定。
 *
 * @property argument 命名参数
 * @property parameterDescriptor 对应的参数描述符
 */
class NamedArgumentReference(
    val argument: CangJieCallArgument,
    val parameterDescriptor: ValueParameterDescriptor
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

/**
 * 混合命名和位置参数诊断
 *
 * 当在调用中混合使用命名参数和位置参数且违反规则时生成。
 * 某些语言规则要求所有命名参数必须出现在位置参数之后。
 */
class MixingNamedAndPositionArguments(override val argument: CangJieCallArgument) : InapplicableArgumentDiagnostic()

/**
 * 命名参数后跟位置参数诊断
 *
 * 当在命名参数之后又提供位置参数时生成。
 * 位置参数必须出现在所有命名参数之前。
 */
class PositionalAfierNamedArgument(override val argument: CangJieCallArgument) : InapplicableArgumentDiagnostic()

/**
 * 不适用参数诊断基类
 *
 * 所有与参数不适用相关的诊断的基类。
 * 子类表示各种参数无法正确映射到函数参数的情况。
 */
abstract class InapplicableArgumentDiagnostic : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    /**
     * 不适用的参数
     */
    abstract val argument: CangJieCallArgument

    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgument(argument, this)
}

/**
 * 参数名称未找到诊断
 *
 * 当使用的命名参数名称在函数定义中不存在时生成。
 *
 * @property argument 使用了未知名称的参数
 * @property descriptor 被调用的函数描述符
 */
class NameNotFound(val argument: CangJieCallArgument, val descriptor: CallableDescriptor) : CangJieCallDiagnostic(
    CandidateApplicability.INAPPLICABLE
) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

/**
 * 歧义参数名称诊断
 *
 * 当重写链中的参数名称不一致时生成约定错误。
 * 例如，基类的参数名为 x，子类重写时改为 y，使用命名参数调用时会产生歧义。
 *
 * @property argument 使用了歧义名称的参数
 * @property parameterDescriptor 当前函数的参数描述符
 * @property overriddenParameterWithOtherName 重写链中使用不同名称的参数描述符
 */
class NameForAmbiguousParameter(
    val argument: CangJieCallArgument,
    val parameterDescriptor: ValueParameterDescriptor,
    val overriddenParameterWithOtherName: ValueParameterDescriptor
) : CangJieCallDiagnostic(CandidateApplicability.CONVENTION_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentName(argument, this)
}

/**
 * 括号外的可变参数诊断
 *
 * 当可变参数（vararg）出现在调用括号之外时生成。
 * 可变参数必须在函数调用的括号内传递。
 *
 * @property argument 括号外的参数
 * @property parameterDescriptor 可变参数描述符
 */
class VarargArgumentOutsideParentheses(
    override val argument: CangJieCallArgument,
    val parameterDescriptor: ValueParameterDescriptor
) : InapplicableArgumentDiagnostic()

/**
 * 参数缺少值诊断
 *
 * 当必需的参数在调用中没有提供值且没有默认值时生成。
 *
 * @property parameterDescriptor 缺少值的参数描述符
 * @property descriptor 被调用的函数描述符
 */
class NoValueForParameter(
    val parameterDescriptor: ValueParameterDescriptor,
    val descriptor: CallableDescriptor
) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

// 类型参数到参数映射器诊断

/**
 * 类型参数数量错误诊断
 *
 * 当提供的类型参数数量与函数或类定义的类型参数数量不匹配时生成。
 *
 * @property descriptor 被调用的可调用描述符
 * @property currentCount 实际提供的类型参数数量
 */
class WrongCountOfTypeArguments(
    val descriptor: CallableDescriptor,
    val currentCount: Int
) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onTypeArguments(this)
}

/**
 * 枚举条目后的类型参数诊断
 *
 * 当在已经给定枚举类型的情况下，枚举构造器后又出现类型参数时生成。
 * 枚举类型的类型参数应该在枚举类型上指定，而不是在枚举条目上。
 *
 * @property enumEntry 枚举条目类描述符
 * @property enum 枚举类描述符
 */
class TypeArgumentsAfterEnumEntry(val enumEntry: ClassDescriptor, val enum: ClassDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onTypeArguments(this)
}

/**
 * 类型参数编译器错误
 *
 * 表示类型参数处理过程中发生的内部编译器错误。
 */
object TypeArgumentsCompilerError :
    CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onTypeArguments(this)
}

/**
 * 可调用引用候选项歧义诊断
 *
 * 当可调用引用解析到多个同等适用的候选项时生成。
 * 例如：::function 可能匹配多个重载。
 *
 * @property argument 可调用引用参数
 * @property candidates 所有歧义的候选项集合
 */
class CallableReferenceCallCandidatesAmbiguity(
    val argument: CallableReferenceCangJieCallArgument,
    val candidates: Collection<CallableReferenceResolutionCandidate>
) : CallableReferenceInapplicableDiagnostic(argument)

/**
 * 可调用引用不适用诊断基类
 *
 * 所有与可调用引用不适用相关的诊断的基类。
 *
 * @param argument 可调用引用解析原子
 * @param applicability 候选项适用性级别，默认为 INAPPLICABLE
 */
abstract class CallableReferenceInapplicableDiagnostic(
    private val argument: CallableReferenceResolutionAtom,
    applicability: CandidateApplicability = CandidateApplicability.INAPPLICABLE
) : CangJieCallDiagnostic(applicability) {
    override fun report(reporter: DiagnosticReporter) {
        when (argument) {
            is CallableReferenceCangJieCall -> reporter.onCall(this)
            is CallableReferenceCangJieCallArgument -> reporter.onCallArgument(argument, this)
        }
    }
}

/**
 * 兼容性警告
 *
 * 当使用已弃用或兼容性受限的候选项时生成警告。
 * 这是一个已解析的诊断，但会提示用户注意潜在的兼容性问题。
 *
 * @property candidate 有兼容性问题的候选描述符
 */
class CompatibilityWarning(val candidate: CallableDescriptor) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

/**
 * 参数上的兼容性警告
 *
 * 当参数使用已弃用或兼容性受限的候选项时生成。
 * 与 CompatibilityWarning 类似，但特定于某个参数。
 *
 * @property argument 使用了有兼容性问题候选项的参数
 * @property candidate 有兼容性问题的候选描述符
 */
class CompatibilityWarningOnArgument(
    val argument: CangJieCallArgument,
    val candidate: CallableDescriptor
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}

/**
 * 无可调用引用候选项诊断
 *
 * 当可调用引用无法解析到任何候选项时生成。
 *
 * @property argument 无法解析的可调用引用参数
 */
class NoneCallableReferenceCallCandidates(val argument: CallableReferenceCangJieCallArgument) :
    CallableReferenceInapplicableDiagnostic(argument)

/**
 * Lambda 参数信息不足诊断
 *
 * 当 Lambda 表达式的参数类型无法从上下文推断时生成。
 * 例如，Lambda 参数的类型需要显式指定或需要更多类型信息。
 *
 * @property lambdaArgument Lambda 参数
 * @property parameterIndex 信息不足的参数索引
 */
class NotEnoughInformationForLambdaParameter(
    val lambdaArgument: LambdaCangJieCallArgument,
    val parameterIndex: Int
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED_WITH_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(lambdaArgument, this)
    }
}

/**
 * 非可变参数展开诊断
 *
 * 当对非 vararg 参数使用展开操作符（spread operator）时生成。
 * 展开操作符只能用于可变参数。
 *
 * @property argument 使用了展开操作符的参数
 */
class NonVarargSpread(val argument: CangJieCallArgument) : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCallArgumentSpread(argument, this)
}

/**
 * 参数可空性不匹配诊断接口
 *
 * 标记所有与参数可空性不匹配相关的诊断。
 * 当可空类型的值传递给非空类型的参数时触发。
 */
sealed interface ArgumentNullabilityMismatchDiagnostic {
    /**
     * 预期的参数类型（通常是非空类型）
     */
    val expectedType: UnwrappedType

    /**
     * 实际的参数类型（通常是可空类型）
     */
    val actualType: UnwrappedType

    /**
     * 表达式参数
     */
    val expressionArgument: ExpressionCangJieCallArgument
}

/**
 * 类型检查器递归检测
 *
 * 当类型检查器在检查过程中检测到递归类型定义时生成。
 * 用于防止无限递归。
 */
object TypeCheckerHasRanIntoRecursion : CangJieCallDiagnostic(CandidateApplicability.INAPPLICABLE) {
    override fun report(reporter: DiagnosticReporter) = reporter.onCall(this)
}

/**
 * 参数可空性错误诊断
 *
 * 当可空类型的值传递给非空类型的参数时生成错误级别诊断。
 * 这是一个不安全的调用，可能导致运行时空指针异常。
 *
 * @property expectedType 预期的非空类型
 * @property actualType 实际的可空类型
 * @property expressionArgument 表达式参数
 */
class ArgumentNullabilityErrorDiagnostic(
    override val expectedType: UnwrappedType,
    override val actualType: UnwrappedType,
    override val expressionArgument: ExpressionCangJieCallArgument
) : CangJieCallDiagnostic(CandidateApplicability.UNSAFE_CALL),
    TransformableToWarning<ArgumentNullabilityWarningDiagnostic>, ArgumentNullabilityMismatchDiagnostic {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(expressionArgument, this)
    }

    /**
     * 将可空性错误转换为警告
     *
     * 在某些上下文中，可空性不匹配可以降级为警告而非错误
     */
    override fun transformToWarning() =
        ArgumentNullabilityWarningDiagnostic(expectedType, actualType, expressionArgument)
}

/**
 * 参数可空性警告诊断
 *
 * 可空性不匹配的警告级别诊断。
 * 在某些宽松的上下文中，可空性不匹配仅产生警告。
 *
 * @property expectedType 预期的非空类型
 * @property actualType 实际的可空类型
 * @property expressionArgument 表达式参数
 */
class ArgumentNullabilityWarningDiagnostic(
    override val expectedType: UnwrappedType,
    override val actualType: UnwrappedType,
    override val expressionArgument: ExpressionCangJieCallArgument
) : CangJieCallDiagnostic(CandidateApplicability.RESOLVED), ArgumentNullabilityMismatchDiagnostic {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(expressionArgument, this)
    }
}

/**
 * 非静态上下文访问静态成员诊断
 *
 * 当在非静态上下文（实例方法、实例属性等）中尝试访问静态成员时生成。
 * 例如：在实例方法中访问伴生对象的成员。
 *
 * @property kind 描述符类型（函数、属性等）
 * @property descriptor 被访问的静态成员描述符
 */
class NonStaticContextAccessStaticMemberDiagnostic(val kind: DescriptorKind, val descriptor: DeclarationDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {

        reporter.onCall(this)
    }
}

/**
 * 静态上下文访问非静态成员诊断
 *
 * 当在静态上下文（静态方法、伴生对象等）中尝试访问非静态成员时生成。
 * 例如：在伴生对象方法中访问实例成员。
 *
 * @property kind 描述符类型（函数、属性等）
 * @property descriptor 被访问的非静态成员描述符
 */
class StaticContextAccessNonStaticMemberDiagnostic(val kind: DescriptorKind, val descriptor: DeclarationDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)


    }
}

/**
 * 无调用操作符函数诊断
 *
 * 当尝试对没有定义调用操作符（invoke）的对象进行函数调用时生成。
 * 例如：someObject() 但 someObject 的类型没有 invoke 操作符。
 *
 * @property descriptor 被调用的对象描述符
 */
class NoCallOperatorFunction(val descriptor: DeclarationDescriptor) :
    CangJieCallDiagnostic(CandidateApplicability.UNSAFE_CALL) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

/**
 * 抽象伪重写的父类调用诊断
 *
 * 当通过 super 调用抽象的伪重写函数时生成。
 * 伪重写是编译器合成的重写，不应该通过 super 调用。
 */
object AbstractFakeOverrideSuperCall : CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

/**
 * 抽象父类调用诊断
 *
 * 当通过 super 调用抽象函数时生成。
 * 抽象函数没有实现，不能通过 super 调用。
 *
 * @property receiver 接收者参数（super）
 */
class AbstractSuperCall(val receiver: SimpleCangJieCallArgument) :
    CangJieCallDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}
