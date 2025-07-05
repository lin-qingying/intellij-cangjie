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
import cn.cangnova.cangjie.descriptors.enumd.EnumEntryDescriptor
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.resolve.calls.util.FakeCallableDescriptorForObject
import cn.cangnova.cangjie.resolve.hasClassValueDescriptor
import cn.cangnova.cangjie.resolve.lazy.descriptors.LazyClassMemberScope
import cn.cangnova.cangjie.resolve.scopes.*
import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.ErrorUtils
import cn.cangnova.cangjie.types.TypeSubstitutor
import com.intellij.util.SmartList
import com.intellij.util.containers.addIfNotNull

internal class ImportingScopeBasedTowerLevel(
    scopeTower: ImplicitScopeTower,
    importingScope: ImportingScope
) : ScopeBasedTowerLevel(scopeTower, importingScope)

internal abstract class AbstractScopeTowerLevel(
    protected val scopeTower: ImplicitScopeTower
) : ScopeTowerLevel {
    protected val location: LookupLocation get() = scopeTower.location

    protected fun createCandidateDescriptor(
        descriptor: CallableDescriptor,
        dispatchReceiver: ReceiverValueWithSmartCastInfo?,
        specialError: ResolutionDiagnostic? = null,
        dispatchReceiverSmartCastType: CangJieType? = null
    ): CandidateWithBoundDispatchReceiver {
        val diagnostics = SmartList<ResolutionDiagnostic>()
        diagnostics.addIfNotNull(specialError)

//        if (ErrorUtils.isError(descriptor)) {
//            diagnostics.add(ErrorDescriptorDiagnostic)
//        } else {
//            if (descriptor.hasLowPriorityInOverloadResolution() || descriptor.isLowPriorityFromStdlibJre7Or8()) {
//                diagnostics.add(LowPriorityDescriptorDiagnostic)
//            }
//            if (dispatchReceiverSmartCastType != null) diagnostics.add(UsedSmartCastForDispatchReceiver(dispatchReceiverSmartCastType))
//
//            val shouldSkipVisibilityCheck = scopeTower.isNewInferenceEnabled
//            if (!shouldSkipVisibilityCheck) {
//                DescriptorVisibilityUtils.findInvisibleMember(
//                    getReceiverValueWithSmartCast(dispatchReceiver?.receiverValue, dispatchReceiverSmartCastType),
//                    descriptor,
//                    scopeTower.lexicalScope.ownerDescriptor,
//                    scopeTower.languageVersionSettings
//                )?.let { diagnostics.add(VisibilityError(it)) }
//            }
//        }

        return CandidateWithBoundDispatchReceiver(dispatchReceiver, descriptor, diagnostics)


    }

}

/**
 * [ScopeBasedTowerLevel] 是一个开放类，提供了基于作用域的解析级别抽象。
 * 主要封装了在给定解析作用域内解析不同类型的符号（如枚举、变量、对象、类类型和函数）的逻辑，
 * 并能够处理弃用诊断信息。
 *
 * @param scopeTower 隐式作用域塔
 * @param resolutionScope 解析作用域
 */
internal open class ScopeBasedTowerLevel protected constructor(
    scopeTower: ImplicitScopeTower,
    private val resolutionScope: ResolutionScope
) : AbstractScopeTowerLevel(scopeTower) {

    /**
     * 如果解析作用域是弃用词法作用域，则包含弃用可见性诊断信息，否则为 null。
     */
    val deprecationDiagnosticOfThisScope: ResolutionDiagnostic? =
        if (resolutionScope is DeprecatedLexicalScope) ResolvedUsingDeprecatedVisibility(
            resolutionScope,
            location
        ) else null

    /**
     * 内部构造函数，接受隐式作用域塔和词法作用域，并将其转换为解析作用域。
     *
     * @param scopeTower 隐式作用域塔
     * @param lexicalScope 词法作用域
     */
    internal constructor(scopeTower: ImplicitScopeTower, lexicalScope: LexicalScope) : this(
        scopeTower,
        lexicalScope as ResolutionScope
    )

    /**
     * 获取指定名称的枚举条目。
     *
     * @param name 枚举名称
     * @param extensionReceiver 扩展接收者值及其智能转换信息
     * @return 包含候选描述符的集合
     */
    override fun getEnumEntrys(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return resolutionScope.getContributedEnumEntrys(name, location)
            .map {
                createCandidateDescriptor(
                    EnumClassCallableDescriptor(it),
                    dispatchReceiver = null,
                    specialError = ResolvedUsingDeprecatedVisibility(
                        resolutionScope,
                        location
                    )
                )
            }
    }

    /**
     * 获取指定名称的变量。
     *
     * @param name 变量名称
     * @param extensionReceiver 扩展接收者值及其智能转换信息
     * @return 包含候选描述符的集合
     */
    override fun getVariables(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return resolutionScope.getContributedVariablesAndIntercept(
            name,
            location,
            null,
            extensionReceiver,
            scopeTower
        ).map {
            createCandidateDescriptor(
                it,
                dispatchReceiver = null,
                specialError = deprecationDiagnosticOfThisScope
            )
        }
    }

    /**
     * 获取指定名称的对象。
     *
     * @param name 对象名称
     * @param extensionReceiver 扩展接收者值及其智能转换信息
     * @return 包含候选描述符的集合
     */
    override fun getObjects(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> =
        resolutionScope.getContributedObjectVariablesIncludeDeprecateds(name, location)
            .map { (classifier, isDeprecated) ->
                createCandidateDescriptor(
                    classifier,
                    dispatchReceiver = null,
                    specialError = if (isDeprecated) ResolvedUsingDeprecatedVisibility(
                        resolutionScope,
                        location
                    ) else null
                )
            }

    /**
     * 根据指定的名称和种类获取枚举类型。
     *
     * @param name 类型名称
     * @param kind 类的种类
     * @param extensionReceiver 扩展接收者值及其智能转换信息
     * @return 包含候选描述符的集合
     */
    override fun getEnumTypeByKind(
        name: Name,
        kind: ClassKind,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return resolutionScope.getContributedClassifiers(name, location).filter {
            (it as? ClassDescriptor)?.kind == kind
        }
            .map {
                createCandidateDescriptor(
                    EnumClassCallableDescriptor(it),
                    dispatchReceiver = null,
                    specialError = ResolvedUsingDeprecatedVisibility(
                        resolutionScope,
                        location
                    )
                )
            }
    }

    /**
     * 根据指定的名称获取类类型。
     *
     * @param name 类型名称
     * @param extensionReceiver 扩展接收者值及其智能转换信息
     * @return 包含候选描述符的集合
     */
    override fun getClassType(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        return resolutionScope.getContributedClassifiers(name, location)
            .map {
                createCandidateDescriptor(
                    ClassCallableDescriptor(it),
                    dispatchReceiver = null,
                    specialError = ResolvedUsingDeprecatedVisibility(
                        resolutionScope,
                        location
                    )
                )
            }
    }

    /**
     * 获取指定名称的函数。
     *
     * @param name 函数名称
     * @param extensionReceiver 扩展接收者值及其智能转换信息
     * @return 包含候选描述符的集合
     */
    override fun getFunctions(
        name: Name,
        extensionReceiver: ReceiverValueWithSmartCastInfo?
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val result: ArrayList<CandidateWithBoundDispatchReceiver> = ArrayList()

        resolutionScope.getContributedFunctionsAndConstructors(name, location, null, extensionReceiver, scopeTower)
            .mapTo(result) {
                createCandidateDescriptor(
                    it,
                    dispatchReceiver = null,
                    specialError = deprecationDiagnosticOfThisScope
                )
            }

        // 添加弃用分类器的构造函数，并附加诊断信息
        val descriptorWithDeprecation = resolutionScope.getContributedClassifierIncludeDeprecated(name, location)
        if (descriptorWithDeprecation != null && descriptorWithDeprecation.isDeprecated) {
            getConstructorsOfClassifier(descriptorWithDeprecation.descriptor).mapTo(result) {
                createCandidateDescriptor(
                    it,
                    dispatchReceiver = null,
                    specialError = ResolvedUsingDeprecatedVisibility(resolutionScope, location)
                )
            }
        }

        return result
    }

    /**
     * 记录查找操作。
     *
     * @param name 查找的名称
     */
    override fun recordLookup(name: Name) {
        resolutionScope.recordLookup(name, location)
    }
}

fun ResolutionScope.getContributedVariablesAndIntercept(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    extensionReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<VariableDescriptor> {
    val variables = getContributedVariables(name, location)
    val propertys = getContributedPropertys(name, location)
    val result = variables + propertys
    return scopeTower.interceptVariableCandidates(this, name, result, location, dispatchReceiver, extensionReceiver)
}

fun ResolutionScope.getContributedFunctionsAndConstructors(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    extensionReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<FunctionDescriptor> {
    val contributedFunctions = getContributedFunctions(name, location)

    val result = ArrayList<FunctionDescriptor>(contributedFunctions)

    getContributedClassifier(name, location)?.let {
        if (DescriptorUtils.isEnum(it) || DescriptorUtils.isEnumEntry(it)) {
            return@let
        }
        result.addAll(getConstructorsOfClassifier(it))
        result.addAll(scopeTower.syntheticScopes.collectSyntheticConstructors(it, location))
    }

    if (contributedFunctions.isNotEmpty()) {
        result.addAll(scopeTower.syntheticScopes.collectSyntheticStaticFunctions(contributedFunctions, location))
    }

    return scopeTower.interceptFunctionCandidates(this, name, result, location, dispatchReceiver, extensionReceiver)
}

private fun getConstructorsOfClassifier(classifier: ClassifierDescriptor?): List<ConstructorDescriptor> {
    val callableConstructors = when (classifier) {
        is EnumEntryDescriptor -> listOf(classifier.unsubstitutedPrimaryConstructor)
        is TypeAliasDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        is ClassDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        else -> emptyList()
    }

    return callableConstructors.filter { it.dispatchReceiverParameter == null }
}

private val ClassDescriptor.canHaveCallableConstructors: Boolean
    get() = !ErrorUtils.isError(this) && !hasClassValueDescriptor

private val TypeAliasDescriptor.canHaveCallableConstructors: Boolean
    get() = classDescriptor != null && !ErrorUtils.isError(classDescriptor) && classDescriptor!!.canHaveCallableConstructors

fun getFakeDescriptorForObject(classifier: ClassifierDescriptor?): FakeCallableDescriptorForObject? =
    when (classifier) {
//        is TypeAliasDescriptor ->
//            classifier.classDescriptor?.let { classDescriptor ->
//                if (classDescriptor.hasClassValueDescriptor)
//                    FakeCallableDescriptorForTypeAliasObject(classifier)
//                else
//                    null
//            }
        is ClassDescriptor ->
            if (classifier.hasClassValueDescriptor)
                FakeCallableDescriptorForObject(classifier)
            else
                null

        else -> null
    }

private fun ResolutionScope.getContributedObjectVariablesIncludeDeprecated(
    name: Name,
    location: LookupLocation
): Collection<DescriptorWithDeprecation<VariableDescriptor>> {
    val (classifier, isOwnerDeprecated) = getContributedClassifierIncludeDeprecated(name, location)
        ?: return emptyList()
    val objectDescriptor = getFakeDescriptorForObject(classifier) ?: return emptyList()
    return listOf(DescriptorWithDeprecation(objectDescriptor, isOwnerDeprecated))
}

private fun ResolutionScope.getContributedObjectVariablesIncludeDeprecateds(
    name: Name,
    location: LookupLocation
): Collection<DescriptorWithDeprecation<VariableDescriptor>> {
    val list = getContributedClassifierIncludeDeprecateds(name, location) ?: return emptyList()


    return list.mapNotNull {
        getFakeDescriptorForObject(it.descriptor)?.let { objectDescriptor ->
            DescriptorWithDeprecation(objectDescriptor, it.isDeprecated)
        }

    }

}

//用于枚举类与枚举项
class EnumClassCallableDescriptor(val type: DeclarationDescriptor) : CallableDescriptor {
    val isEnumEntry get() = DescriptorUtils.isEnumEntry(type)
    private val memberScope = when (type) {
        is ClassDescriptor -> type.unsubstitutedMemberScope
        else -> null
    }
    private val constructors = when (memberScope) {
        is LazyClassMemberScope -> memberScope.getConstructors()
        else -> emptyList()
    }
    var constructor = constructors.firstOrNull()
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitEnumClassCallDescriptor(this, data)
    }

    //    是否具有无参构造
    fun hashUnsubstitutedPrimaryConstructor(): Boolean {

        if (DescriptorUtils.isEnum(type)) return true
        if (DescriptorUtils.isEnumEntry(type)) {
            return (type as EnumEntryDescriptor).hasUnsubstitutedPrimaryConstructor()
        }
        return false
    }


    override val valueParameters: List<ValueParameterDescriptor>
        get() = constructor?.valueParameters?.toList() ?: emptyList()

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

    }


    override val source: SourceElement
        get() = type.toSourceElement

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {

        constructor = constructor?.substitute(substitutor)
        return this
    }

    override val contextReceiverParameters: List<ReceiverParameterDescriptor>
        get() {
            return emptyList()
        }


    override val returnType: CangJieType?
        get() = constructor?.returnType

    override val extensionReceiverParameter: ReceiverParameterDescriptor?
        get() = null


    override val overriddenDescriptors: List<CallableDescriptor>
        get() = emptyList()

    override val dispatchReceiverParameter: ReceiverParameterDescriptor?
        get() = null

    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }


    override val typeParameters: List<TypeParameterDescriptor>
        get() = constructors.firstOrNull()?.typeParameters?.toList() ?: emptyList()

    override fun hasStableParameterNames(): Boolean {
        return false
    }

    override val isStatic: Boolean
        get() = type.isStatic

    override val original: CallableDescriptor
        get() = this
    override val containingDeclaration: DeclarationDescriptor
        get() = type.containingDeclaration ?: type
    override val visibility: DescriptorVisibility
        get() = type.visibility
    override val name: Name
        get() = type.name
}

//    无其他用处，请勿使用，只作用于重载检查
class ClassCallableDescriptor(val type: DeclarationDescriptor) : CallableDescriptor {
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitClassCallDescriptor(this, data)
    }

    override val valueParameters: List<ValueParameterDescriptor>
        get() = emptyList()


    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

    }

    override val source: SourceElement
        get() = type.toSourceElement

    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {
        return this
    }

    override val contextReceiverParameters: Collection<ReceiverParameterDescriptor>
        get() = emptyList()
    override val returnType: CangJieType?
        get() = null

    override val extensionReceiverParameter: ReceiverParameterDescriptor?
        get() = null
    override val overriddenDescriptors: List<CallableDescriptor>
        get() = emptyList()
    override val dispatchReceiverParameter: ReceiverParameterDescriptor?
        get() = null


    override fun hasSynthesizedParameterNames(): Boolean {
        return false
    }


    override val typeParameters: List<TypeParameterDescriptor>
        get() = emptyList()

    override fun hasStableParameterNames(): Boolean {
        return false
    }

    override val isStatic: Boolean
        get() = type.isStatic
    override val original: CallableDescriptor
        get() = this
    override val containingDeclaration: DeclarationDescriptor
        get() = type
    override val visibility: DescriptorVisibility
        get() = type.visibility
    override val name: Name
        get() = type.name
}
