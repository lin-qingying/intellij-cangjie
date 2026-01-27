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

package org.cangnova.cangjie.resolve.calls.model

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.components.ReturnArgumentsInfo
import org.cangnova.cangjie.resolve.calls.components.TypeArgumentsToParametersMapper
import org.cangnova.cangjie.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import org.cangnova.cangjie.resolve.calls.components.extractInputOutputTypesFromCallableReferenceExpectedType
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystem
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintMismatch
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintWarning
import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableForLambdaReturnType
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.unCapture

import org.cangnova.cangjie.utils.addIfNotNull


/**
 * 解析原子（Resolution Atom）
 *
 * 表示需要进行类型解析的基本单元，包括：
 * - 调用表达式（Call）
 * - 可调用引用（Callable reference）
 * - Lambda 表达式和函数表达式
 * - 集合字面量（Collection literal）
 *
 * 未来可能会添加其他字面量类型，因为它们具有相似的生命周期。
 *
 * 注意：带类型的表达式也被视为原子。这是为了简化实现。TODO: 需要重新考虑这个设计
 */
interface ResolutionAtom

/**
 * 已解析原子的基类
 *
 * 表示一个已经过解析处理的原子，包含解析状态和子原子信息。
 * 所有解析结果都继承此类。
 */
sealed class ResolvedAtom {
    /**
     * 关联的解析原子
     *
     * 注意：CallResolutionResult 没有 ResolutionAtom，因此可能为 null
     */
    abstract val atom: ResolutionAtom?

    /**
     * 是否已分析完成
     *
     * 标记此原子及其子原子是否已经过完整的类型分析
     */
    var analyzed: Boolean = false
        private set

    /**
     * 子已解析原子列表
     *
     * 对于包含嵌套表达式的原子（如包含 lambda 的调用），
     * 此列表包含所有子表达式的解析结果
     */
    var subResolvedAtoms: List<ResolvedAtom>? = null
        private set

    /**
     * 设置分析结果
     *
     * 标记原子为已分析状态，并保存子原子的解析结果。
     * 此方法只能调用一次。
     *
     * @param subResolvedAtoms 子原子的解析结果列表
     * @throws AssertionError 如果原子已经被分析过
     */
    protected open fun setAnalyzedResults(subResolvedAtoms: List<ResolvedAtom>) {
        assert(!analyzed) {
            "Already analyzed: $this"
        }

        analyzed = true

        this.subResolvedAtoms = subResolvedAtoms
    }

    /**
     * 设置空的分析结果
     *
     * 用于 AllCandidates 模式，避免分析延迟参数。
     * 在收集所有候选项时使用，不需要完整分析子表达式。
     */
    fun setEmptyAnalyzedResults() {
        setAnalyzedResults(emptyList())
    }
}

/**
 * 带诊断信息的候选项
 *
 * 将解析候选项与其产生的诊断信息（错误、警告）配对。
 *
 * @property candidate 解析候选项
 * @property diagnostics 该候选项产生的诊断信息列表
 */
data class CandidateWithDiagnostics(val candidate: ResolutionCandidate, val diagnostics: List<CangJieCallDiagnostic>)

/**
 * 所有候选项解析结果
 *
 * 用于重载解析时收集所有可能的候选项及其诊断信息。
 * 这允许编译器提供更详细的错误信息，说明为什么每个候选项不适用。
 *
 * @property allCandidates 所有候选项及其诊断信息的集合
 * @property constraintSystem 约束系统（在此模式下通常为空或最小）
 */
class AllCandidatesResolutionResult(
    val allCandidates: Collection<CandidateWithDiagnostics>,
    constraintSystem: ConstraintSystem
) : CallResolutionResult(null, emptyList(), constraintSystem)

/**
 * 已解析的可调用引用原子标记接口
 *
 * 用于标识可调用引用的解析结果
 */
sealed interface ResolvedCallableReferenceAtom

/**
 * 已解析调用原子的抽象基类
 *
 * 表示一个已解析的函数调用，包含所有必要的信息：
 * - 候选函数描述符
 * - 参数映射
 * - 类型替换信息
 * - 各种类型转换的记录
 */
abstract class ResolvedCallAtom : ResolvedAtom() {
    /** 关联的调用表达式 */
    abstract override val atom: CangJieCall

    /** 候选函数描述符（可能在解析过程中更新） */
    abstract val candidateDescriptor: CallableDescriptor

    /** 显式接收者类型（如对象方法调用中的对象） */
    abstract val explicitReceiverKind: ExplicitReceiverKind

    /** 分发接收者参数 */
    abstract val dispatchReceiverArgument: SimpleCangJieCallArgument?

    /** 上下文接收者参数列表 */
    abstract var contextReceiversArguments: List<SimpleCangJieCallArgument>

    /** 类型参数到参数的映射（按原始定义） */
    abstract val typeArgumentMappingByOriginal: TypeArgumentsToParametersMapper.TypeArgumentsMapping

    /** 参数映射（按原始定义）：值参数描述符到已解析调用参数 */
    abstract val argumentMappingByOriginal: Map<ValueParameterDescriptor, ResolvedCallArgument>

    /** 新鲜类型变量替换器：用于类型推断 */
    abstract val freshVariablesSubstitutor: ComposableTypeSubstitutor

    /** 新鲜类型变量列表：在类型推断中创建的类型变量 */
    abstract val freshVariables: List<org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor>

    /** 需要进行挂起函数转换的参数映射 */
    abstract val argumentsWithSuspendConversion: Map<CangJieCallArgument, UnwrappedType>

    /** 已知参数替换器：基于已知类型信息的替换 */
    abstract val knownParametersSubstitutor: ComposableTypeSubstitutor

    /** 需要进行 SAM 转换的参数映射 */
    abstract val argumentsWithConversion: Map<CangJieCallArgument, SamConversionDescription>

    /** 需要进行 Unit 类型转换的参数映射 */
    abstract val argumentsWithUnitConversion: Map<CangJieCallArgument, UnwrappedType>

    /** 需要进行常量转换的参数映射（有符号/无符号整数） */
    abstract val argumentsWithConstantConversion: Map<CangJieCallArgument, IntegerValueTypeConstant>

    /**
     * 设置新的候选函数描述符
     *
     * @param newCandidateDescriptor 新的候选函数描述符
     */
    abstract fun setCandidateDescriptor(newCandidateDescriptor: CallableDescriptor)
}

/**
 * SAM（Single Abstract Method）转换描述
 *
 * 描述将 lambda 表达式转换为函数式接口所需的类型信息。
 *
 * @property convertedTypeByOriginParameter 按原始参数的转换类型
 * @property convertedTypeByCandidateParameter 按候选参数的转换类型（对应参数的期望类型）
 * @property originalParameterType 原始参数类型（用于解析继承的 SAM 接口的重载）
 */
class SamConversionDescription(
    val convertedTypeByOriginParameter: UnwrappedType,
    val convertedTypeByCandidateParameter: UnwrappedType,
    val originalParameterType: UnwrappedType
)

/**
 * 调用解析结果的基类
 *
 * 表示函数调用解析的最终或中间结果，包含：
 * - 诊断信息（错误、警告）
 * - 约束系统状态
 *
 * @property resultCallAtom 解析后的调用原子（某些子类中可能为 null）
 * @property diagnostics 解析过程中产生的诊断信息列表
 * @property constraintSystem 类型推导使用的约束系统
 */
sealed class CallResolutionResult(
    resultCallAtom: ResolvedCallAtom?,
    val diagnostics: List<CangJieCallDiagnostic>,
    val constraintSystem: ConstraintSystem
) : ResolvedAtom() {
    /** CallResolutionResult 没有关联的 ResolutionAtom */
    override val atom: ResolutionAtom? get() = null

    override fun toString(): String = "diagnostics: (${diagnostics.joinToString()})"

    /**
     * 生成完成的诊断信息
     *
     * 使用类型替换器更新诊断信息中的类型，
     * 将类型变量替换为其最终推断出的具体类型。
     *
     * @param substitutor 类型替换器
     * @return 更新后的诊断信息列表
     */
    fun completedDiagnostic(substitutor: ComposableTypeSubstitutor): List<CangJieCallDiagnostic> {
        return diagnostics.map {
            val error = it.constraintSystemError ?: return@map it
            if (error !is ConstraintMismatch) return@map it
            val lowerType = (error.lowerType as? CangJieType)?.unwrap() ?: return@map it
            val newLowerType = substitutor.safeSubstitute(lowerType.unCapture())
            when (error) {
                is ConstraintError -> ConstraintError(
                    newLowerType,
                    error.upperType,
                    error.position
                ).asDiagnostic()

                is ConstraintWarning -> ConstraintWarning(
                    newLowerType,
                    error.upperType,
                    error.position
                ).asDiagnostic()
            }
        }
    }
}

/**
 * 单次调用解析结果
 *
 * 表示单个候选函数调用的解析结果，包含：
 * - 解析后的调用原子（包含参数映射、类型替换等信息）
 * - 诊断信息（错误、警告等）
 * - 约束系统（类型推导过程中产生的约束）
 *
 * 这是最基础的调用解析结果类型，所有调用解析都会产生这个结果。
 * 当解析完全成功（所有类型变量都已固定）时，返回此类型。
 *
 * @property resultCallAtom 解析后的调用原子，包含：
 *   - 候选函数描述符
 *   - 参数到形参的映射
 *   - 类型替换信息
 *   - 解析后的类型参数
 * @property diagnostics 解析过程中产生的诊断信息列表
 *   - 类型不匹配错误
 *   - 参数数量错误
 *   - 其他调用相关的错误/警告
 * @property constraintSystem 类型推导使用的约束系统，包含：
 *   - 所有收集到的类型约束
 *   - 类型变量的固定状态
 *   - 约束求解的中间结果
 */
open class SingleCallResolutionResult(
    val resultCallAtom: ResolvedCallAtom,
    diagnostics: List<CangJieCallDiagnostic>,
    constraintSystem: ConstraintSystem
) : CallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

/**
 * 部分调用解析结果
 *
 * 表示调用解析部分完成，需要后续处理才能最终确定的结果。
 * 这通常发生在以下场景：
 *
 * ## 场景 1: 存在未固定的类型变量
 * ```kotlin
 * fun <T> foo(x: T, f: (T) -> Unit) {}
 * foo(42) { it.toString() }  // 第一阶段解析后 T 可能未固定
 * ```
 * 需要将此结果转发给推断会话（Inference Session）继续处理。
 *
 * ## 场景 2: 存在延迟参数（Postponed Arguments）
 * ```kotlin
 * listOf(1, 2, 3).map { it * 2 }
 *                      ^^^^^^^^ lambda 需要延迟分析
 * ```
 * Lambda 的分析需要等待其参数类型确定。
 *
 * ## 场景 3: 需要多轮迭代推导
 * ```kotlin
 * class A<T> {
 *     companion object { fun create(): Unit {} }
 * }
 * val x: A<Int> = A.create()  // 需要从上下文推导 T
 * ```
 * 静态调用的类型参数需要从上下文迭代推导。
 *
 * ## 与 SingleCallResolutionResult 的区别
 * - **SingleCallResolutionResult**: 解析完全完成，所有类型都已确定
 * - **PartialCallResolutionResult**: 解析部分完成，需要后续步骤
 *
 * ## 后续处理流程
 * 1. 如果 `forwardToInferenceSession = true`:
 *    - 将结果转发给 InferenceSession
 *    - 在推断会话中运行迭代推导引擎
 *    - 分析延迟参数（lambda 等）
 *    - 固定剩余的类型变量
 *
 * 2. 如果 `forwardToInferenceSession = false`:
 *    - 使用当前约束系统继续处理
 *    - 可能在 CallCompleter 中完成剩余推导
 *
 * @property resultCallAtom 部分解析后的调用原子（可能包含未固定的类型变量）
 * @property diagnostics 当前阶段产生的诊断信息
 * @property constraintSystem 当前的约束系统状态（包含部分解）
 * @property forwardToInferenceSession 是否需要转发到推断会话进行进一步处理
 *   - `true`: 转发到 InferenceSession，运行完整的迭代推导
 *   - `false`: 在当前上下文中继续处理（例如在 CallCompleter 中）
 *
 * @see SingleCallResolutionResult 完全解析的结果
 * @see InferenceSession 推断会话，处理复杂的类型推导场景
 */
class PartialCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<CangJieCallDiagnostic>,
    constraintSystem: ConstraintSystem,
    val forwardToInferenceSession: Boolean = false
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

/**
 * 部分调用容器
 *
 * 用于包装可能为 null 的部分调用解析结果。
 * 提供统一的接口来处理有/无部分调用结果的情况。
 *
 * @property result 部分调用解析结果（可能为 null）
 */
class PartialCallContainer(val result: PartialCallResolutionResult?) {
    companion object {
        /** 空容器：表示没有部分调用结果 */
        val empty = PartialCallContainer(null)
    }
}

/**
 * 延迟解析原子的基类
 *
 * 表示需要延迟分析的表达式（如 lambda、可调用引用）。
 * 这些表达式的类型分析依赖于上下文信息，需要在后续阶段处理。
 */
sealed class PostponedResolvedAtom : ResolvedAtom(), PostponedResolvedAtomMarker {
    /** 输入类型集合（参数类型） */
    abstract override val inputTypes: Collection<UnwrappedType>

    /** 输出类型（返回类型） */
    abstract override val outputType: UnwrappedType?

    /** 期望类型（从上下文推断的类型） */
    abstract override val expectedType: UnwrappedType?
}

/**
 * 可调用引用解析原子接口
 *
 * 表示可调用引用表达式（如 `::functionName`）的解析信息。
 */
sealed interface CallableReferenceResolutionAtom : ResolutionAtom {
    /** 左侧结果（接收者表达式的解析结果） */
    val lhsResult: LHSResult

    /** 右侧名称（被引用的函数或属性名） */
    val rhsName: Name

    /** 关联的调用表达式 */
    val call: CangJieCall
}

/**
 * 错误调用解析结果
 *
 * 表示调用解析失败的结果。
 * 包含导致失败的诊断信息和约束系统状态。
 *
 * @property resultCallAtom 失败的调用原子
 * @property diagnostics 错误诊断信息
 * @property constraintSystem 失败时的约束系统状态
 */
class ErrorCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<CangJieCallDiagnostic>,
    constraintSystem: ConstraintSystem
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

/**
 * 完成的调用解析结果
 *
 * 表示调用解析成功完成，所有类型变量都已固定。
 * 这是最终的、可以直接使用的解析结果。
 *
 * @property resultCallAtom 完全解析的调用原子
 * @property diagnostics 解析过程中的诊断信息（可能包含警告）
 * @property constraintSystem 最终的约束系统状态
 */
class CompletedCallResolutionResult(
    resultCallAtom: ResolvedCallAtom,
    diagnostics: List<CangJieCallDiagnostic>,
    constraintSystem: ConstraintSystem
) : SingleCallResolutionResult(resultCallAtom, diagnostics, constraintSystem)

/**
 * 已解析的可调用引用参数原子
 *
 * 表示作为参数传递的可调用引用（如 `foo(::bar)`）的解析结果。
 *
 * @property atom 可调用引用调用参数
 * @property expectedType 期望类型（从参数位置推断）
 */
abstract class ResolvedCallableReferenceArgumentAtom(
    override val atom: CallableReferenceCangJieCallArgument,
    override val expectedType: UnwrappedType?
) : PostponedResolvedAtom(), ResolvedCallableReferenceAtom {
    /**
     * 解析候选项
     *
     * 被引用的函数或属性的候选项
     */
    var candidate: CallableReferenceResolutionCandidate? = null
        private set

    /** 是否已完成解析 */
    var completed: Boolean = false

    /**
     * 设置分析结果
     *
     * @param candidate 解析的候选项
     * @param subResolvedAtoms 子原子列表
     */
    fun setAnalyzedResults(
        candidate: CallableReferenceResolutionCandidate?,
        subResolvedAtoms: List<ResolvedAtom>
    ) {
        this.candidate = candidate
        setAnalyzedResults(subResolvedAtoms)
    }

}

/**
 * 可调用引用仓颉调用
 *
 * 表示可调用引用表达式的调用信息。
 *
 * @property call 关联的调用表达式
 * @property lhsResult 左侧表达式的解析结果（接收者）
 * @property rhsName 右侧名称（被引用的名称）
 */
class CallableReferenceCangJieCall(
    override val call: CangJieCall,
    override val lhsResult: LHSResult,
    override val rhsName: Name,
) : CallableReferenceResolutionAtom

/**
 * 获取已解析调用的新鲜返回类型
 *
 * 使用新鲜类型变量替换器处理候选函数的返回类型。
 *
 * @return 替换后的返回类型，如果候选函数没有返回类型则返回 null
 */
val ResolvedCallAtom.freshReturnType: UnwrappedType?
    get() {
        val returnType = candidateDescriptor.returnType ?: return null
        return freshVariablesSubstitutor.safeSubstitute(returnType.unwrap())
    }

/**
 * 从调用解析结果中提取已解析调用原子
 *
 * @return 如果是 SingleCallResolutionResult 则返回其 resultCallAtom，否则返回 null
 */
fun CallResolutionResult.resultCallAtom(): ResolvedCallAtom? =
    if (this is SingleCallResolutionResult) resultCallAtom else null

/**
 * 桩已解析原子
 *
 * 用于代理属性的特殊情况，当有一个好的候选项和一个坏的候选项时使用。
 * 例如：`var x by lazy { "" }`
 *
 * @property typeVariable 类型变量的类型构造器
 */
class StubResolvedAtom(val typeVariable: TypeConstructor) : ResolvedAtom() {
    override val atom: ResolutionAtom? get() = null
}

/**
 * 已解析表达式原子
 *
 * 表示一个简单表达式参数的解析结果（不是 lambda 或可调用引用）。
 *
 * @property atom 表达式调用参数
 */
class ResolvedExpressionAtom(override val atom: ExpressionCangJieCallArgument) : ResolvedAtom() {
    init {
        // 表达式参数没有子原子
        setAnalyzedResults(listOf())
    }
}

/**
 * 已解析 Lambda 原子
 *
 * 表示 lambda 表达式的完整解析结果，包含：
 * - 参数类型
 * - 返回类型
 * - 接收者类型（如果是扩展 lambda）
 * - 返回参数信息
 *
 * @property atom Lambda 调用参数
 * @property receiver 接收者类型（对于扩展 lambda）
 * @property parameters 参数类型列表
 * @property returnType 返回类型
 * @property typeVariableForLambdaReturnType Lambda 返回类型的类型变量
 * @property expectedType 期望类型
 */
class ResolvedLambdaAtom(
    override val atom: LambdaCangJieCallArgument,
    val receiver: UnwrappedType?,
    val parameters: List<UnwrappedType>,
    val returnType: UnwrappedType,
    val typeVariableForLambdaReturnType: TypeVariableForLambdaReturnType?,
    override val expectedType: UnwrappedType?
) : PostponedResolvedAtom() {
    /**
     * 返回参数信息
     *
     * 只有在 lambda 被展开时才不为 null（参见 [unwrap]）。
     * 如果 lambda 是在 resolveName 的歧义解析过程中分析的，
     * 则可能为 null。
     */
    var resultArgumentsInfo: ReturnArgumentsInfo? = null
        private set

    /**
     * 设置分析结果
     *
     * @param resultArguments 返回参数信息
     * @param subResolvedAtoms 子原子列表
     */
    fun setAnalyzedResults(
        resultArguments: ReturnArgumentsInfo?,
        subResolvedAtoms: List<ResolvedAtom>
    ) {
        this.resultArgumentsInfo = resultArguments
        setAnalyzedResults(subResolvedAtoms)
    }

    /**
     * 输入类型集合
     *
     * 包含所有参数类型和接收者类型（如果存在）
     */
    override val inputTypes: Collection<UnwrappedType>
        get() {
            if (receiver == null ) return parameters
            return ArrayList<UnwrappedType>(parameters.size + 1).apply {
                addAll(parameters)
                addIfNotNull(receiver)
            }
        }

    /** 输出类型（返回类型） */
    override val outputType: UnwrappedType get() = returnType
}

/**
 * 抽象延迟可调用引用原子
 *
 * 延迟解析的可调用引用的基类。
 *
 * @param atom 可调用引用调用参数
 * @param expectedType 期望类型
 */
sealed class AbstractPostponedCallableReferenceAtom(
    atom: CallableReferenceCangJieCallArgument,
    expectedType: UnwrappedType?
) : ResolvedCallableReferenceArgumentAtom(atom, expectedType) {
    /**
     * 输入类型集合
     *
     * 从期望类型中提取输入类型，如果无法提取则使用期望类型本身
     */
    override val inputTypes: Collection<UnwrappedType>
        get() = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.inputTypes ?: listOfNotNull(
            expectedType
        )

    /**
     * 输出类型
     *
     * 从期望类型中提取输出类型
     */
    override val outputType: UnwrappedType?
        get() = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType)?.outputType
}

/**
 * 急切可调用引用原子
 *
 * 表示可以立即解析的可调用引用（不需要等待上下文类型）。
 * 可以转换为延迟可调用引用原子。
 *
 * @property atom 可调用引用调用参数
 * @property expectedType 期望类型
 */
class EagerCallableReferenceAtom(
    atom: CallableReferenceCangJieCallArgument,
    expectedType: UnwrappedType?
) : ResolvedCallableReferenceArgumentAtom(atom, expectedType) {
    /** 急切解析不需要输入类型 */
    override val inputTypes: Collection<UnwrappedType> get() = emptyList()

    /** 急切解析不需要输出类型 */
    override val outputType: UnwrappedType? get() = null

    /**
     * 转换为延迟可调用引用原子
     *
     * @return 延迟可调用引用原子
     */
    fun transformToPostponed(): PostponedCallableReferenceAtom = PostponedCallableReferenceAtom(this)
}

/**
 * 延迟可调用引用原子
 *
 * 表示需要延迟到有足够上下文信息时才能解析的可调用引用。
 *
 * @param eagerCallableReferenceAtom 转换前的急切可调用引用原子
 */
class PostponedCallableReferenceAtom(
    eagerCallableReferenceAtom: EagerCallableReferenceAtom
) : AbstractPostponedCallableReferenceAtom(eagerCallableReferenceAtom.atom, eagerCallableReferenceAtom.expectedType),
    PostponedCallableReferenceMarker {
    /**
     * 修订的期望类型
     *
     * 在类型推导过程中，期望类型可能会被更新
     */
    override var revisedExpectedType: UnwrappedType? = null
        private set

    /**
     * 修订期望类型
     *
     * @param expectedType 新的期望类型
     */
    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        require(expectedType is UnwrappedType)
        revisedExpectedType = expectedType
    }
}

/**
 * 带修订期望类型的可调用引用原子
 *
 * 表示期望类型已被更新的可调用引用。
 *
 * @property atom 可调用引用调用参数
 * @property expectedType 修订后的期望类型
 */
class CallableReferenceWithRevisedExpectedTypeAtom(
    atom: CallableReferenceCangJieCallArgument,
    expectedType: UnwrappedType?,
) : AbstractPostponedCallableReferenceAtom(atom, expectedType)

/**
 * 期望类型为类型变量的 Lambda 原子
 *
 * 表示期望类型是一个未固定的类型变量的 lambda 表达式。
 * 需要等待类型变量固定后才能完全解析。
 *
 * @property atom Lambda 调用参数
 * @property expectedType 期望类型（是一个类型变量）
 */
class LambdaWithTypeVariableAsExpectedTypeAtom(
    override val atom: LambdaCangJieCallArgument,
    override val expectedType: UnwrappedType
) : PostponedResolvedAtom(), LambdaWithTypeVariableAsExpectedTypeMarker {
    /** 输入类型（期望类型本身） */
    override val inputTypes: Collection<UnwrappedType> get() = listOf(expectedType)

    /** 输出类型（未知） */
    override val outputType: UnwrappedType? get() = null

    /**
     * 修订的期望类型
     *
     * 当类型变量被固定后，更新为具体类型
     */
    override var revisedExpectedType: UnwrappedType? = null
        private set

    /**
     * 从声明中提取的参数类型
     *
     * 如果 lambda 有显式参数类型声明，保存在这里
     */
    override var parameterTypesFromDeclaration: List<UnwrappedType?>? = null
        private set

    /**
     * 更新从声明中提取的参数类型
     *
     * @param types 参数类型列表
     */
    override fun updateParameterTypesFromDeclaration(types: List<CangJieTypeMarker?>?) {
        @Suppress("UNCHECKED_CAST")
        types as List<UnwrappedType?>?
        parameterTypesFromDeclaration = types
    }

    /**
     * 修订期望类型
     *
     * @param expectedType 新的期望类型（类型变量被固定后的类型）
     */
    override fun reviseExpectedType(expectedType: CangJieTypeMarker) {
        require(expectedType is UnwrappedType)
        revisedExpectedType = expectedType
    }

    /**
     * 设置为已分析状态
     *
     * @param resolvedLambdaAtom 解析后的 lambda 原子
     */
    fun setAnalyzed(resolvedLambdaAtom: ResolvedLambdaAtom) {
        setAnalyzedResults(listOf(resolvedLambdaAtom))
    }
}

/**
 * 已解析集合字面量原子
 *
 * 表示集合字面量（如 `[1, 2, 3]`）的解析结果。
 *
 * @property atom 集合字面量调用参数
 * @property expectedType 期望类型
 */
class ResolvedCollectionLiteralAtom(
    override val atom: CollectionLiteralCangJieCallArgument,
    val expectedType: UnwrappedType?
) : ResolvedAtom() {
    init {
        // 集合字面量没有子原子
        setAnalyzedResults(listOf())
    }
}

/**
 * 已解析子调用参数
 *
 * 表示嵌套调用参数的解析结果。
 *
 * @property atom 子调用参数
 * @param resolveIndependently 是否独立解析（不依赖父调用的结果）
 */
class ResolvedSubCallArgument(override val atom: SubCangJieCallArgument, resolveIndependently: Boolean) :
    ResolvedAtom() {
    init {
        if (resolveIndependently)
        // 独立解析：没有子原子
            setAnalyzedResults(listOf())
        else
        // 非独立解析：使用子调用的解析结果作为子原子
            setAnalyzedResults(listOf(atom.callResult))
    }
}