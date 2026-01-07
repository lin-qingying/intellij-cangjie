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

package org.cangnova.cangjie.resolve.calls.tower

import com.intellij.util.SmartList
import com.intellij.util.containers.addIfNotNull
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.qualified.isEnum
import org.cangnova.cangjie.resolve.scopes.*
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils

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
        if (DescriptorUtils.isEnum(it) || DescriptorUtils.isEnumConstructor(it)) {
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

        is TypeAliasDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        is ClassDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        else -> emptyList()
    }

    return callableConstructors.filter { it.dispatchReceiverParameter == null }
}

private val ClassDescriptor.canHaveCallableConstructors: Boolean
    get() = !ErrorUtils.isError(this) && !isEnum

private val TypeAliasDescriptor.canHaveCallableConstructors: Boolean
    get() = classDescriptor != null && !ErrorUtils.isError(classDescriptor) && classDescriptor!!.canHaveCallableConstructors

private fun ResolutionScope.getContributedObjectVariablesIncludeDeprecated(
    name: Name,
    location: LookupLocation
): Collection<DescriptorWithDeprecation<VariableDescriptor>> {
    // 删除了 getFakeDescriptorForObject - 枚举构造器不再通过这种方式访问
    return emptyList()
}

private fun ResolutionScope.getContributedObjectVariablesIncludeDeprecateds(
    name: Name,
    location: LookupLocation
): Collection<DescriptorWithDeprecation<VariableDescriptor>> {
    // 删除了 getFakeDescriptorForObject - 枚举构造器不再通过这种方式访问
    return emptyList()
}
