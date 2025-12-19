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

package org.cangnova.cangjie.decompiler.navigation

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.decompiler.psi.file.CjDecompiledFile
import org.cangnova.cangjie.decompiler.psi.text.getQualifiedName
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.fqNameSafe
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.isAny

/**
 * 通过 Descriptor 在反编译文件中查找对应的 PSI 声明
 *
 * 该对象提供了从 Descriptor（编译器级别的描述符）到 PSI 元素（IDE 级别的语法树节点）的映射。
 * 它通过分析反编译文件的结构，找到与给定 Descriptor 匹配的 PSI 声明。
 *
 * ## 核心功能
 *
 * - **Descriptor 到 PSI 的映射**: 根据 Descriptor 的类型和属性，在反编译文件中定位对应的 PSI 元素
 * - **类型匹配**: 比较 Descriptor 和 PSI 声明的类型签名是否一致
 * - **参数匹配**: 验证函数/构造器的参数列表是否匹配
 * - **接收者匹配**: 检查扩展函数的接收者类型
 *
 * ## 使用场景
 *
 * - **导航到库定义**: 从引用跳转到反编译的库代码
 * - **显示库 API 详情**: 在代码补全、悬浮文档中展示库函数签名
 * - **查找用法**: 在库代码中查找符号的使用位置
 */
internal object ByDescriptorIndexer {
    private val LOG = Logger.getInstance(this::class.java)

    /**
     * 在反编译文件中查找与 Descriptor 对应的声明
     *
     * 该方法是核心查找逻辑，它会：
     * 1. 处理特殊情况（类型别名构造器、值参数、主构造器等）
     * 2. 处理合成成员（fake override、伪造的调用描述符等）
     * 3. 在反编译文件中定位成员声明
     * 4. 对可调用成员进行签名匹配验证
     *
     * @param descriptor 要查找的 Descriptor
     * @param file 反编译文件
     * @return 对应的 PSI 声明元素，如果未找到则返回 null
     */
    fun getDeclarationForDescriptor(descriptor: DeclarationDescriptor, file: CjDecompiledFile): CjDeclaration? {
        val original = descriptor.original

        // 1. 处理类型别名构造器：直接返回类型别名本身
        if (original is TypeAliasConstructorDescriptor) {
            return getDeclarationForDescriptor(original.typeAliasDescriptor, file)
        }

        // 2. 处理值参数：在其所属的可调用声明中查找对应的参数
        if (original is ValueParameterDescriptor) {
            val callable = original.containingDeclaration
            val callableDeclaration = getDeclarationForDescriptor(callable, file) as? CjCallableDeclaration ?: return null
            if (original.index >= callableDeclaration.valueParameters.size) {
                LOG.error(
                    "Parameter count mismatch for ${original}[${original.index}] vs " +
                            callableDeclaration.valueParameterList?.text
                )
                return null
            }
            return callableDeclaration.valueParameters[original.index]
        }

        // 3. 处理主构造器：返回类本身或其主构造器
        if (original is ConstructorDescriptor && original.isPrimary) {
            val classOrObject = getDeclarationForDescriptor(original.containingDeclaration, file) as? CjTypeStatement
            return classOrObject?.primaryConstructor ?: classOrObject
        }

        // 4. 处理不应该写入反编译文本的合成成员
        // 这些成员不会出现在反编译文件中，返回其包含的类
        if (original is CallableMemberDescriptor && original.mustNotBeWrittenToDecompiledText() &&
            original.containingDeclaration is ClassDescriptor
        ) {
            return getDeclarationForDescriptor(original.containingDeclaration, file)
        }

        // 5. 处理成员声明
        if (original is MemberDescriptor) {
            val declarationContainer: CjDeclarationContainer? = when {
                // 顶层声明：在文件中查找
                DescriptorUtils.isTopLevelDeclaration(original) -> file

                // 类成员：在类中查找
                original.containingDeclaration is ClassDescriptor ->
                    getDeclarationForDescriptor(original.containingDeclaration as ClassDescriptor, file) as? CjTypeStatement

                else -> error("Unexpected $original with container: ${original.containingDeclaration}")
            }

            if (declarationContainer != null) {
                val descriptorName = original.name.asString()
                val declarations = when {
                    // 构造器：使用类的所有构造器
                    original is FunctionDescriptor && declarationContainer is CjClass -> declarationContainer.constructors

                    // 其他成员：按名称过滤
                    else -> declarationContainer.declarations.filter { it.name == descriptorName }
                }

                // 在候选声明中查找匹配的
                return declarations.firstOrNull { declaration ->
                    if (original is CallableDescriptor) {
                        // 可调用成员：需要进行签名匹配
                        declaration is CjCallableDeclaration && isSameCallable(declaration, original)
                    } else {
                        // 非可调用成员（如类、对象）：不需要签名匹配
                        declaration !is CjCallableDeclaration
                    }
                }
            }
        }

        error("Should not be reachable: $descriptor")
    }

    /**
     * 判断 PSI 可调用声明和 Descriptor 是否表示同一个函数/属性
     *
     * 通过比较以下几个方面来判断：
     * - 接收者类型（扩展函数/属性）
     * - 返回类型
     * - 类型参数
     * - 值参数列表
     *
     * @param declaration PSI 可调用声明
     * @param original Descriptor
     * @return 是否匹配
     */
    fun isSameCallable(
        declaration: CjCallableDeclaration,
        original: CallableDescriptor
    ): Boolean {
        // 1. 检查接收者类型是否匹配（扩展函数/属性）
        if (!receiverTypesMatch(declaration.receiverTypeReference, original.extensionReceiverParameter)) return false

        // 2. 检查返回类型是否匹配
        if (!returnTypesMatch(declaration, original)) return false

        // 3. 检查类型参数是否匹配
        if (!typeParametersMatch(declaration, original)) return false

        // 4. 检查值参数列表是否匹配
        if (!parametersMatch(declaration, original)) return false

        return true
    }

    /**
     * 检查返回类型是否匹配
     *
     * 构造器不需要检查返回类型。
     * 对于没有显式返回类型的声明（IDE 导航场景），跳过检查。
     */
    private fun returnTypesMatch(declaration: CjCallableDeclaration, descriptor: CallableDescriptor): Boolean {
        // 构造器没有返回类型
        if (declaration is CjConstructor<*>) return true

        // typeReference 可能为 null（在 IDE 源码导航中，对于没有显式返回类型的函数）
        // 这种情况下不比较返回类型
        val typeReference = declaration.typeReference ?: return true

        return areTypesTheSame(descriptor.returnType!!, typeReference)
    }

    /**
     * 检查类型参数是否匹配
     *
     * 比较：
     * - 类型参数数量
     * - 每个类型参数的名称
     * - 每个类型参数的上界约束
     */
    private fun typeParametersMatch(declaration: CjCallableDeclaration, descriptor: CallableDescriptor): Boolean {
        if (declaration.typeParameters.size != descriptor.typeParameters.size) return false

        // 按名称分组约束
        val boundsByName = declaration.typeConstraints.groupBy { it.subjectTypeParameterName?.referencedName }

        descriptor.typeParameters.zip(declaration.typeParameters) { descriptorTypeParam, psiTypeParameter ->
            // 检查名称
            if (descriptorTypeParam.name.toString() != psiTypeParameter.name) return false

            // 收集所有边界
            val psiBounds = mutableListOf<CjTypeReference>()
            psiTypeParameter.extendsBound?.let { psiBounds.add(it) }
            boundsByName[psiTypeParameter.name]?.forEach {
                it.boundTypeReference?.let { bound -> psiBounds.add(bound) }
            }

            // 过滤掉 Any? 类型的上界（默认上界）
            val expectedBounds = descriptorTypeParam.upperBounds.filter { !it.isAny() }
            if (psiBounds.size != expectedBounds.size) return false

            // 逐一比较边界
            expectedBounds.zip(psiBounds) { expectedBound, candidateBound ->
                if (!areTypesTheSame(expectedBound, candidateBound)) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * 检查参数列表是否匹配
     *
     * 比较：
     * - 参数数量
     * - 每个参数是否为可变参数
     * - 每个参数的类型
     */
    private fun parametersMatch(
        declaration: CjCallableDeclaration,
        original: CallableDescriptor
    ): Boolean {
        if (declaration.valueParameters.size != original.valueParameters.size) {
            return false
        }

        declaration.valueParameters.zip(original.valueParameters).forEach { (cjParam, paramDesc) ->
            val isVarargs = cjParam.isVarArg
            if (isVarargs != (paramDesc.varargElementType != null)) {
                return false
            }

            // 可变参数：比较元素类型；普通参数：比较参数类型
            val typeToCompare = if (isVarargs) paramDesc.varargElementType!! else paramDesc.type
            if (!areTypesTheSame(typeToCompare, cjParam.typeReference!!)) {
                return false
            }
        }
        return true
    }

    /**
     * 检查接收者类型是否匹配
     *
     * 扩展函数/属性有接收者，普通成员没有接收者。
     */
    private fun receiverTypesMatch(
        cjTypeReference: CjTypeReference?,
        receiverParameter: ReceiverParameterDescriptor?,
    ): Boolean {
        if (cjTypeReference != null) {
            if (receiverParameter == null) return false
            val receiverType = receiverParameter.type
            if (!areTypesTheSame(receiverType, cjTypeReference)) {
                return false
            }
        } else if (receiverParameter != null) return false
        return true
    }

    /**
     * 判断类型是否相同
     *
     * 通过比较完全限定名来判断类型是否相同。
     * 支持类型别名的展开。
     *
     * @param cangJieType 来自 Descriptor 的类型
     * @param cjTypeReference 来自 PSI 的类型引用
     * @return 类型是否相同
     */
    private fun areTypesTheSame(
        cangJieType: CangJieType,
        cjTypeReference: CjTypeReference
    ): Boolean {
        val qualifiedName = getQualifiedName(cjTypeReference) ?: return false
        val declarationDescriptor = cangJieType.constructor.declarationDescriptor ?: return false

        // 类型参数：直接比较名称
        if (declarationDescriptor is TypeParameterDescriptor) {
            return declarationDescriptor.name.asString() == qualifiedName
        }

        // 普通类型：比较完全限定名
        return declarationDescriptor.fqNameSafe.asString() == qualifiedName
    }

    /**
     * 从类型引用中获取完全限定名
     *
     * @param typeReference PSI 类型引用
     * @return 完全限定名，如果无法获取则返回 null
     */
    private fun getQualifiedName(typeReference: CjTypeReference): String? {
        // 对于用户类型（如 std.collection.ArrayList），返回其完全限定名
        val userType = typeReference.typeElement as? CjUserType ?: return null
        return getQualifiedName(userType)
    }
}

/**
 * 判断可调用成员描述符是否不应该写入反编译文本
 *
 * 某些合成成员不应该出现在反编译文件中：
 * - Fake override（伪覆盖）：这些是编译器自动生成的覆盖方法
 * - 某些合成方法：如枚举类的 values()、valueOf() 等
 *
 * @see org.jetbrains.kotlin.analysis.decompiler.stub.mustNotBeWrittenToStubs
 */
private fun CallableMemberDescriptor.mustNotBeWrittenToDecompiledText(): Boolean {
    return when (kind) {
        CallableMemberDescriptor.Kind.DECLARATION, CallableMemberDescriptor.Kind.DELEGATION -> false
        CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> true
        CallableMemberDescriptor.Kind.SYNTHESIZED -> syntheticMemberMustNotBeWrittenToDecompiledText()
    }
}

/**
 * 判断合成成员是否不应该写入反编译文本
 *
 * 目前主要处理枚举类的合成方法。
 */
private fun CallableMemberDescriptor.syntheticMemberMustNotBeWrittenToDecompiledText(): Boolean {
    val containingClass = containingDeclaration as? ClassDescriptor ?: return false

    return when {

        else -> false
    }
}
