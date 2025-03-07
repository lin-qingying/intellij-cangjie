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

package cn.cangnova.cangjie.resolve.calls.tower

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import cn.cangnova.cangjie.resolve.calls.model.CangJieCallArgument
import cn.cangnova.cangjie.resolve.calls.model.CangJieCallDiagnostic
import cn.cangnova.cangjie.resolve.calls.model.DiagnosticReporter
import cn.cangnova.cangjie.resolve.scopes.LexicalScope
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.resolve.scopes.ResolutionScope
import cn.cangnova.cangjie.resolve.scopes.SyntheticScopes
import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import cn.cangnova.cangjie.resolve.scopes.util.parentsWithSelf
import cn.cangnova.cangjie.types.TypeApproximator

@JvmName("getResultApplicabilityForConstraintErrors")
fun getResultApplicability(diagnostics: Collection<ConstraintSystemError>): CandidateApplicability =
    diagnostics.minByOrNull { it.applicability }?.applicability ?: CandidateApplicability.RESOLVED

@JvmName("getResultApplicabilityForCallDiagnostics")
fun getResultApplicability(diagnostics: Collection<CangJieCallDiagnostic>): CandidateApplicability =
    diagnostics.minByOrNull { it.candidateApplicability }?.candidateApplicability ?: CandidateApplicability.RESOLVED


// todo error for this access from nested class
class VisibilityError(val invisibleMember: DeclarationDescriptorWithVisibility) : ResolutionDiagnostic(
    CandidateApplicability.RUNTIME_ERROR
) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

object HiddenExtensionRelatedToDynamicTypes : ResolutionDiagnostic(CandidateApplicability.HIDDEN)
object DeprecatedUnaryPlusAsPlus : ResolutionDiagnostic(CandidateApplicability.CONVENTION_ERROR)
object InvokeConventionCallNoOperatorModifier : ResolutionDiagnostic(CandidateApplicability.CONVENTION_ERROR)

class ContextReceiverAmbiguity : ResolutionDiagnostic(CandidateApplicability.RESOLVED_WITH_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

class NoMatchingContextReceiver : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCall(this)
    }
}

object HiddenDescriptor : ResolutionDiagnostic(CandidateApplicability.HIDDEN)

/**
 * 接口定义了在作用域塔中检索各种声明的函数
 * 主要用于在给定的作用域内查找变量、枚举条目、类类型、对象和函数
 */
interface ScopeTowerLevel {
    /**
     * 根据名称和扩展接收器获取变量集合
     *
     * @param name 变量的名称
     * @param extensionReceiver 扩展接收器，可能带有智能类型转换信息
     * @return 包含候选变量的集合，带有绑定的分发接收器
     */
    fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>

    /**
     * 根据名称和扩展接收器获取枚举条目集合默认为空列表
     *
     * @param name 枚举条目的名称
     * @param extensionReceiver 扩展接收器，可能带有智能类型转换信息
     * @return 包含候选枚举条目的集合，带有绑定的分发接收器
     */
    fun getEnumEntrys(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> = emptyList()

    /**
     * 根据名称、类型种类和扩展接收器获取枚举类型集合默认为空列表
     *
     * @param name 枚举类型的名称
     * @param kind 枚举类型的种类
     * @param extensionReceiver 扩展接收器，可能带有智能类型转换信息
     * @return 包含候选枚举类型的集合，带有绑定的分发接收器
     */
    fun getEnumTypeByKind(
        name: Name,
        kind: ClassKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> = emptyList()

    /**
     * 根据名称和扩展接收器获取类类型集合
     *
     * @param name 类类型的名称
     * @param extensionReceiver 扩展接收器，可能带有智能类型转换信息
     * @return 包含候选类类型的集合，带有绑定的分发接收器
     */
    fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>

    /**
     * 根据名称和扩展接收器获取对象集合
     *
     * @param name 对象的名称
     * @param extensionReceiver 扩展接收器，可能带有智能类型转换信息
     * @return 包含候选对象的集合，带有绑定的分发接收器
     */
    fun getObjects(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>

    /**
     * 根据名称和扩展接收器获取函数集合
     *
     * @param name 函数的名称
     * @param extensionReceiver 扩展接收器，可能带有智能类型转换信息
     * @return 包含候选函数的集合，带有绑定的分发接收器
     */
    fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver>

    /**
     * 记录一次名称查找操作
     *
     * @param name 被查找的名称
     */
    fun recordLookup(name: Name)
}


object UnstableSmartCastDiagnostic : ResolutionDiagnostic(CandidateApplicability.UNSTABLE_SMARTCAST)

interface ImplicitScopeTower {
    val lexicalScope: LexicalScope
    val areContextReceiversEnabled: Boolean
    val syntheticScopes: SyntheticScopes
    val dynamicScope: MemberScope

    val typeApproximator: TypeApproximator
    val isNewInferenceEnabled: Boolean
    fun getContextReceivers(scope: LexicalScope): List<ReceiverValueWithSmartCastInfo>

    fun interceptVariableCandidates(
        resolutionScope: ResolutionScope,
        name: Name,
        initialResults: Collection<VariableDescriptor>,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<VariableDescriptor>

    fun allScopesWithImplicitsResolutionInfo(): Sequence<ScopeWithImplicitsExtensionsResolutionInfo> =
        implicitsResolutionFilter.getScopesWithInfo(lexicalScope.parentsWithSelf)

    fun interceptFunctionCandidates(
        resolutionScope: ResolutionScope,
        name: Name,
        initialResults: Collection<FunctionDescriptor>,
        location: LookupLocation,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<FunctionDescriptor>

    //    val syntheticScopes: SyntheticScopes
    val location: LookupLocation
    val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter
    fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo?

}

abstract class ResolutionDiagnostic(candidateApplicability: CandidateApplicability) :
    CangJieCallDiagnostic(candidateApplicability) {
    override fun report(reporter: DiagnosticReporter) {
        // do nothing
    }
}

class CandidateWithBoundDispatchReceiver(
    val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    val descriptor: CallableDescriptor,
    val diagnostics: List<ResolutionDiagnostic>
)

object ErrorDescriptorDiagnostic :
    ResolutionDiagnostic(CandidateApplicability.RESOLVED) // todo discuss and change to INAPPLICABLE

object EmptyDiagnostic : ResolutionDiagnostic(CandidateApplicability.INAPPLICABLE)
object DynamicDescriptorDiagnostic : ResolutionDiagnostic(CandidateApplicability.RESOLVED_LOW_PRIORITY)

class ResolvedUsingDeprecatedVisibility(val baseSourceScope: ResolutionScope, val lookupLocation: LookupLocation) :
    ResolutionDiagnostic(
        CandidateApplicability.RESOLVED
    )

class VisibilityErrorOnArgument(
    val argument: CangJieCallArgument,
    val invisibleMember: DeclarationDescriptorWithVisibility
) : ResolutionDiagnostic(CandidateApplicability.RUNTIME_ERROR) {
    override fun report(reporter: DiagnosticReporter) {
        reporter.onCallArgument(argument, this)
    }
}
