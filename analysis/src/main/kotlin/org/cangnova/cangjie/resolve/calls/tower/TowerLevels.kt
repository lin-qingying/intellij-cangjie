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
import org.cangnova.cangjie.resolve.scopes.receivers.QualifierReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils

/**
 * 基于导入作用域的塔式层级
 *
 * 用于处理导入作用域（如 import 语句）的符号解析。
 * 导入作用域包括显式导入和星号导入的符号。
 *
 * @param scopeTower 隐式作用域塔
 * @param importingScope 导入作用域
 */
internal class ImportingScopeBasedTowerLevel(
    scopeTower: ImplicitScopeTower,
    importingScope: ImportingScope
) : ScopeBasedTowerLevel(scopeTower, importingScope)

/**
 * 抽象作用域塔式层级
 *
 * 提供创建候选项描述符的基础功能，是所有具体塔式层级的基类。
 *
 * @property scopeTower 隐式作用域塔，提供解析上下文
 */
internal abstract class AbstractScopeTowerLevel(
    protected val scopeTower: ImplicitScopeTower
) : ScopeTowerLevel {
    /**
     * 查找位置
     *
     * 从作用域塔获取的查找位置，用于记录符号查找操作
     */
    protected val location: LookupLocation get() = scopeTower.location

    /**
     * 创建候选项描述符
     *
     * 将可调用描述符包装为带有绑定分发接收者的候选项，并添加相应的诊断信息。
     *
     * @param descriptor 可调用描述符（函数或属性）
     * @param dispatchReceiver 分发接收者及其智能转换信息
     * @param specialError 特殊错误诊断信息（如弃用警告）
     * @param dispatchReceiverSmartCastType 分发接收者的智能转换类型
     * @return 带有绑定分发接收者的候选项
     */
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
     * @return 包含候选描述符的集合
     */
    override fun getFunctions(
        name: Name
    ): Collection<CandidateWithBoundDispatchReceiver> {
        val result: ArrayList<CandidateWithBoundDispatchReceiver> = ArrayList()

        resolutionScope.getContributedFunctionsAndConstructors(name, location, null, scopeTower)
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

/**
 * 获取贡献的变量并拦截，同时处理枚举构造器
 *
 * 扩展函数，用于从解析作用域中获取变量、属性和枚举的简单构造器，
 * 并通过作用域塔的拦截器进行拦截处理。
 *
 * 此函数特别处理枚举类型的限定符，提取其简单构造器（简单构造器是指不需要参数的构造器，
 * 在仓颉语言中，枚举成员可以作为简单构造器使用）。
 *
 * @param name 要查找的符号名称
 * @param location 查找位置，用于记录查找操作
 * @param qualifier 限定符接收者（如 EnumType.Member）
 * @param dispatchReceiver 分发接收者及其智能转换信息
 * @param scopeTower 隐式作用域塔
 * @return 变量描述符集合，包括普通变量、属性和枚举简单构造器
 */
fun ResolutionScope.getContributedVariablesAndInterceptAndEnumConstructor(
    name: Name,
    location: LookupLocation,
    qualifier: QualifierReceiver,

    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<VariableDescriptor> {
    // 获取作用域中贡献的变量和属性
    val variables = getContributedVariables(name, location)
    val propertys = getContributedPropertys(name, location)

    // 如果限定符是枚举描述符，提取其简单构造器
    val enumConstructor = if (qualifier.descriptor is EnumDescriptor) {
        (qualifier.descriptor as EnumDescriptor).constructors.filter {
            it.isSimpleConstructor && it.name == name
        }
    } else {
        emptyList()
    }

    // 合并所有候选项
    val result = variables + propertys + enumConstructor

    // 通过作用域塔的拦截器进行拦截处理
    return scopeTower.interceptVariableCandidates(this, name, result, location, dispatchReceiver)
}

/**
 * 获取贡献的变量并拦截
 *
 * 扩展函数，用于从解析作用域中获取变量和属性，并通过作用域塔的拦截器进行拦截处理。
 * 这是不处理枚举构造器的简化版本。
 *
 * @param name 要查找的符号名称
 * @param location 查找位置，用于记录查找操作
 * @param dispatchReceiver 分发接收者及其智能转换信息
 * @param scopeTower 隐式作用域塔
 * @return 变量描述符集合，包括普通变量和属性
 */
fun ResolutionScope.getContributedVariablesAndIntercept(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,

    scopeTower: ImplicitScopeTower
): Collection<VariableDescriptor> {
    // 获取作用域中贡献的变量和属性
    val variables = getContributedVariables(name, location)
    val propertys = getContributedPropertys(name, location)
    val result = variables + propertys

    // 通过作用域塔的拦截器进行拦截处理
    return scopeTower.interceptVariableCandidates(this, name, result, location, dispatchReceiver)
}

/**
 * 获取贡献的函数和枚举构造器
 *
 * 扩展函数，用于从解析作用域中获取函数和枚举的函数构造器，
 * 并收集合成的静态函数，最后通过作用域塔的拦截器进行拦截处理。
 *
 * 此函数特别处理枚举类型的限定符，提取其函数构造器（函数构造器是指需要参数的构造器）。
 *
 * @param name 要查找的函数名称
 * @param location 查找位置，用于记录查找操作
 * @param qualifier 限定符接收者（如 EnumType.Constructor）
 * @param dispatchReceiver 分发接收者及其智能转换信息
 * @param scopeTower 隐式作用域塔
 * @return 函数描述符集合，包括普通函数、枚举函数构造器和合成静态函数
 */
fun ResolutionScope.getContributedFunctionsAndEnumConstructors(
    name: Name,
    location: LookupLocation,
    qualifier: QualifierReceiver,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<FunctionDescriptor> {
    // 获取作用域中贡献的函数
    val contributedFunctions = getContributedFunctions(name, location)

    val result = ArrayList<FunctionDescriptor>(contributedFunctions)

    // 如果限定符是枚举描述符，添加其函数构造器
    if (qualifier.descriptor is EnumDescriptor) {
        result.addAll((qualifier.descriptor as EnumDescriptor).constructors.filter {
            it.isFunctionConstructor
        })
    }

    // 如果有贡献的函数，收集合成的静态函数（如扩展函数）
    if (contributedFunctions.isNotEmpty()) {
        result.addAll(scopeTower.syntheticScopes.collectSyntheticStaticFunctions(contributedFunctions, location))
    }

    // 通过作用域塔的拦截器进行拦截处理
    return scopeTower.interceptFunctionCandidates(this, name, result, location, dispatchReceiver)
}

/**
 * 获取贡献的函数和构造器
 *
 * 扩展函数，用于从解析作用域中获取函数和类的构造器，
 * 并收集合成的构造器和静态函数，最后通过作用域塔的拦截器进行拦截处理。
 *
 * 此函数会查找与名称匹配的分类器（类或类型别名），如果找到则获取其构造器。
 * 注意：枚举类型和枚举构造器类型会被排除，因为它们由专门的枚举处理逻辑处理。
 *
 * @param name 要查找的符号名称（可能是函数名或类名）
 * @param location 查找位置，用于记录查找操作
 * @param dispatchReceiver 分发接收者及其智能转换信息
 * @param scopeTower 隐式作用域塔
 * @return 函数描述符集合，包括普通函数、类构造器、合成构造器和合成静态函数
 */
fun ResolutionScope.getContributedFunctionsAndConstructors(
    name: Name,
    location: LookupLocation,
    dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    scopeTower: ImplicitScopeTower
): Collection<FunctionDescriptor> {
    // 获取作用域中贡献的函数
    val contributedFunctions = getContributedFunctions(name, location)

    val result = ArrayList<FunctionDescriptor>(contributedFunctions)

    // 查找与名称匹配的分类器（类或类型别名）
    getContributedClassifier(name, location)?.let {
        // 排除枚举类型和枚举构造器类型，它们由专门的枚举处理逻辑处理
        if (DescriptorUtils.isEnum(it) || DescriptorUtils.isEnumConstructor(it)) {
            return@let
        }
        // 添加分类器的构造器
        result.addAll(getConstructorsOfClassifier(it))
        // 收集合成的构造器（如数据类的 copy 方法等）
        result.addAll(scopeTower.syntheticScopes.collectSyntheticConstructors(it, location))
    }

    // 如果有贡献的函数，收集合成的静态函数（如扩展函数）
    if (contributedFunctions.isNotEmpty()) {
        result.addAll(scopeTower.syntheticScopes.collectSyntheticStaticFunctions(contributedFunctions, location))
    }

    // 通过作用域塔的拦截器进行拦截处理
    return scopeTower.interceptFunctionCandidates(this, name, result, location, dispatchReceiver)
}

/**
 * 获取分类器的构造器
 *
 * 私有函数，用于从分类器（类或类型别名）中提取可调用的构造器。
 * 只返回没有分发接收者参数的构造器（即顶层构造器，不是内部类的构造器）。
 *
 * @param classifier 分类器描述符（可能是类或类型别名）
 * @return 构造器描述符列表
 */
private fun getConstructorsOfClassifier(classifier: ClassifierDescriptor?): List<ConstructorDescriptor> {
    // 根据分类器类型获取其构造器
    val callableConstructors = when (classifier) {
        // 类型别名：如果可以有可调用构造器，则获取其构造器
        is TypeAliasDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        // 类描述符：如果可以有可调用构造器，则获取其构造器
        is ClassDescriptor -> if (classifier.canHaveCallableConstructors) classifier.constructors else emptyList()
        // 其他类型：返回空列表
        else -> emptyList()
    }

    // 只返回没有分发接收者参数的构造器（排除内部类的构造器）
    return callableConstructors.filter { it.dispatchReceiverParameter == null }
}

/**
 * 类描述符是否可以有可调用构造器
 *
 * 扩展属性，判断类是否可以有可调用的构造器。
 * 错误类型和枚举类型不能有可调用的构造器。
 */
private val ClassDescriptor.canHaveCallableConstructors: Boolean
    get() = !ErrorUtils.isError(this) && !isEnum

/**
 * 类型别名描述符是否可以有可调用构造器
 *
 * 扩展属性，判断类型别名是否可以有可调用的构造器。
 * 只有当类型别名指向的类描述符存在且该类可以有可调用构造器时，才返回 true。
 */
private val TypeAliasDescriptor.canHaveCallableConstructors: Boolean
    get() = classDescriptor != null && !ErrorUtils.isError(classDescriptor) && classDescriptor!!.canHaveCallableConstructors

/**
 * 获取贡献的对象变量（包括弃用的）
 *
 * 此函数已废弃。之前用于获取对象的假描述符（getFakeDescriptorForObject），
 * 但枚举构造器现在不再通过这种方式访问。
 *
 * @param name 变量名称
 * @param location 查找位置
 * @return 始终返回空列表
 */
private fun ResolutionScope.getContributedObjectVariablesIncludeDeprecated(
    name: Name,
    location: LookupLocation
): Collection<DescriptorWithDeprecation<VariableDescriptor>> {
    return emptyList()
}

/**
 * 获取贡献的对象变量（包括弃用的，复数形式）
 *
 * 此函数已废弃。之前用于获取对象的假描述符（getFakeDescriptorForObject），
 * 但枚举构造器现在不再通过这种方式访问。
 *
 * @param name 变量名称
 * @param location 查找位置
 * @return 始终返回空列表
 */
private fun ResolutionScope.getContributedObjectVariablesIncludeDeprecateds(
    name: Name,
    location: LookupLocation
): Collection<DescriptorWithDeprecation<VariableDescriptor>> {
    return emptyList()
}
