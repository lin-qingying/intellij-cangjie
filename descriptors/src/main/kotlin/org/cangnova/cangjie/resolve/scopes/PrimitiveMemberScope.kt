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
 */

package org.cangnova.cangjie.resolve.scopes

import org.cangnova.cangjie.builtins.PrimitiveType
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.utils.Printer

/**
 * 基本类型成员作用域
 *
 * 专门为基本类型设计的成员作用域，提供高效的操作符函数查找
 */
class PrimitiveMemberScope(
    private val primitiveType: PrimitiveType,
    private val ownerDescriptor: ClassDescriptor,
    private val storageManager: StorageManager
) : MemberScope {

    /**
     * 操作符函数映射表
     *
     * 按需创建，避免不必要的内存占用
     */
    private val operatorFunctions: Map<Name, List<SimpleFunctionDescriptor>> by lazy {
        createOperatorFunctions()
    }

    /**
     * 所有函数名称集合
     */
    override val functionNames: Set<Name> by lazy {
        operatorFunctions.keys
    }

    /**
     * 获取贡献的函数
     */
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return operatorFunctions[name] ?: emptyList()
    }

    /**
     * 获取贡献的变量（基本类型没有变量）
     */
    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
        return emptyList()
    }

    /**
     * 获取贡献的属性（基本类型没有属性）
     */
    override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return emptyList()
    }

    override fun getContributedMacros(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard MacroDescriptor> {
        TODO("Not yet implemented")
    }

    /**
     * 获取贡献的分类器（基本类型没有嵌套分类器）
     */
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        return null
    }

    /**
     * 获取贡献的描述符
     */
    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val result = mutableListOf<DeclarationDescriptor>()

        // 只返回函数描述符（基本类型只有操作符函数）
        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            operatorFunctions.values.flatten()
                .filter { nameFilter(it.name) }
                .forEach { result.add(it) }
        }

        return result
    }


    /**
     * 获取变量名称集合（基本类型没有变量）
     */
    override val variableNames: Set<Name> = emptySet()

    /**
     * 获取属性名称集合（基本类型没有属性）
     */
    override val propertyNames: Set<Name> = emptySet()

    /**
     * 获取分类器名称集合（基本类型没有嵌套分类器）
     */
    override val classifierNames: Set<Name>? = null

    /**
     * 获取所有函数
     */
    fun getAllFunctions(): List<SimpleFunctionDescriptor> {
        return operatorFunctions.values.flatten()
    }

    /**
     * 打印作用域结构
     */
    override fun printScopeStructure(p: Printer) {
        p.println("PrimitiveMemberScope for ${primitiveType.typeName.asString()}")
        p.pushIndent()

        p.println("Operator functions:")
        p.pushIndent()
        operatorFunctions.forEach { (name, functions) ->
            p.println("${name.asString()}: ${functions.size} overload(s)")
        }
        p.popIndent()

        p.popIndent()
    }

    /**
     * 创建操作符函数映射表
     */
    private fun createOperatorFunctions(): Map<Name, List<SimpleFunctionDescriptor>> {
        val functions = mutableMapOf<Name, MutableList<SimpleFunctionDescriptor>>()

        when (primitiveType) {
            // 有符号整数类型
            PrimitiveType.INT8, PrimitiveType.INT16, PrimitiveType.INT32,
            PrimitiveType.INT64, PrimitiveType.INTNATIVE -> {
                addSignedIntegerOperators(functions)
            }

            // 无符号整数类型
            PrimitiveType.UINT8, PrimitiveType.UINT16, PrimitiveType.UINT32,
            PrimitiveType.UINT64, PrimitiveType.UINTNATIVE -> {
                addUnsignedIntegerOperators(functions)
            }

            // 浮点类型
            PrimitiveType.FLOAT16, PrimitiveType.FLOAT32, PrimitiveType.FLOAT64 -> {
                addFloatOperators(functions)
            }

            // 布尔类型
            PrimitiveType.BOOL -> {
                addBooleanOperators(functions)
            }

            // 字符类型
            PrimitiveType.Rune -> {
                addRuneOperators(functions)
            }

            else -> {
                // 其他类型暂时不添加操作符
            }
        }

        return functions
    }

    /**
     * 添加有符号整数操作符
     */
    private fun addSignedIntegerOperators(functions: MutableMap<Name, MutableList<SimpleFunctionDescriptor>>) {
        // 算术操作符（二元）
        addBinaryOperator(functions, OperatorNameConventions.PLUS)
        addBinaryOperator(functions, OperatorNameConventions.MINUS)
        addBinaryOperator(functions, OperatorNameConventions.TIMES)
        addBinaryOperator(functions, OperatorNameConventions.DIV)
        addBinaryOperator(functions, OperatorNameConventions.REM)

        // 位操作符（二元）
        addBinaryOperator(functions, OperatorNameConventions.AND)
        addBinaryOperator(functions, OperatorNameConventions.OR)
        addBinaryOperator(functions, OperatorNameConventions.XOR)
        addBinaryOperator(functions, OperatorNameConventions.LEFT_SHIFT)
        addBinaryOperator(functions, OperatorNameConventions.RIGHT_SHIFT)

        // 一元操作符
        addUnaryOperator(functions, OperatorNameConventions.UNARY_MINUS)
        addUnaryOperator(functions, OperatorNameConventions.INC)
        addUnaryOperator(functions, OperatorNameConventions.DEC)
        addUnaryOperator(functions, OperatorNameConventions.NOT)
    }

    /**
     * 添加无符号整数操作符
     */
    private fun addUnsignedIntegerOperators(functions: MutableMap<Name, MutableList<SimpleFunctionDescriptor>>) {
        // 算术操作符（二元）- 无符号整数不支持一元减号
        addBinaryOperator(functions, OperatorNameConventions.PLUS)
        addBinaryOperator(functions, OperatorNameConventions.MINUS)
        addBinaryOperator(functions, OperatorNameConventions.TIMES)
        addBinaryOperator(functions, OperatorNameConventions.DIV)
        addBinaryOperator(functions, OperatorNameConventions.REM)

        // 位操作符（二元）
        addBinaryOperator(functions, OperatorNameConventions.AND)
        addBinaryOperator(functions, OperatorNameConventions.OR)
        addBinaryOperator(functions, OperatorNameConventions.XOR)
        addBinaryOperator(functions, OperatorNameConventions.LEFT_SHIFT)
        addBinaryOperator(functions, OperatorNameConventions.RIGHT_SHIFT)

        // 一元操作符（不包括一元减号）
        addUnaryOperator(functions, OperatorNameConventions.INC)
        addUnaryOperator(functions, OperatorNameConventions.DEC)
        addUnaryOperator(functions, OperatorNameConventions.NOT)
    }

    /**
     * 添加浮点操作符
     */
    private fun addFloatOperators(functions: MutableMap<Name, MutableList<SimpleFunctionDescriptor>>) {
        // 算术操作符（二元）
        addBinaryOperator(functions, OperatorNameConventions.PLUS)
        addBinaryOperator(functions, OperatorNameConventions.MINUS)
        addBinaryOperator(functions, OperatorNameConventions.TIMES)
        addBinaryOperator(functions, OperatorNameConventions.DIV)
        addBinaryOperator(functions, OperatorNameConventions.REM)

        // 一元操作符
        addUnaryOperator(functions, OperatorNameConventions.UNARY_MINUS)
    }

    /**
     * 添加布尔操作符
     */
    private fun addBooleanOperators(functions: MutableMap<Name, MutableList<SimpleFunctionDescriptor>>) {
        // 逻辑操作符
        addUnaryOperator(functions, OperatorNameConventions.NOT)
        addBinaryOperator(functions, OperatorNameConventions.ANDAND)
        addBinaryOperator(functions, OperatorNameConventions.OROR)
    }

    /**
     * 添加字符操作符
     */
    private fun addRuneOperators(functions: MutableMap<Name, MutableList<SimpleFunctionDescriptor>>) {
        // 字符类型目前只有比较操作，这里可以根据需要扩展
        // 暂时不添加操作符
    }

    /**
     * 添加二元操作符
     */
    private fun addBinaryOperator(
        functions: MutableMap<Name, MutableList<SimpleFunctionDescriptor>>,
        operatorName: Name
    ) {
        val function = BinaryOperatorDescriptor(operatorName, ownerDescriptor, storageManager)
        functions.getOrPut(operatorName) { mutableListOf() }.add(function)
    }

    /**
     * 添加一元操作符
     */
    private fun addUnaryOperator(
        functions: MutableMap<Name, MutableList<SimpleFunctionDescriptor>>,
        operatorName: Name
    ) {
        val function = UnaryOperatorDescriptor(operatorName, ownerDescriptor, storageManager)
        functions.getOrPut(operatorName) { mutableListOf() }.add(function)
    }

    /**
     * 二元操作符描述符
     */
    private class BinaryOperatorDescriptor(
        operatorName: Name,
        containingDeclaration: DeclarationDescriptor,
        storageManager: StorageManager
    ) : SimpleFunctionDescriptorImpl(
        containingDeclaration,
        null,
        Annotations.EMPTY,
        operatorName,
        CallableMemberDescriptor.Kind.DECLARATION,
        SourceElement.NO_SOURCE
    ) {
        init {
            val ownerType = (containingDeclaration as ClassDescriptor).defaultType

            val rightParameter = ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                this, null, 0, Annotations.EMPTY, Name.identifier("other"),
                false, ownerType, false, SourceElement.NO_SOURCE, null
            )

            val returnType = determineReturnType(operatorName, ownerType)

            initialize(
                null, null, emptyList(), emptyList(),
                listOf(rightParameter),
                returnType,
                Modality.FINAL,
                DescriptorVisibilities.PUBLIC
            )
        }

        override var isOperator: Boolean = true

        /**
         * 确定返回类型
         */
        private fun determineReturnType(operatorName: Name, ownerType: CangJieType): CangJieType {
            return when (operatorName) {
                // 大部分操作符返回相同类型
                OperatorNameConventions.PLUS, OperatorNameConventions.MINUS,
                OperatorNameConventions.TIMES, OperatorNameConventions.DIV,
                OperatorNameConventions.REM, OperatorNameConventions.AND,
                OperatorNameConventions.OR, OperatorNameConventions.XOR,
                OperatorNameConventions.LEFT_SHIFT, OperatorNameConventions.RIGHT_SHIFT -> ownerType

                // 逻辑操作符返回 Boolean（这里简化处理）
                OperatorNameConventions.ANDAND, OperatorNameConventions.OROR -> ownerType

                else -> ownerType
            }
        }
    }

    /**
     * 一元操作符描述符
     */
    private class UnaryOperatorDescriptor(
        operatorName: Name,
        containingDeclaration: DeclarationDescriptor,
        storageManager: StorageManager
    ) : SimpleFunctionDescriptorImpl(
        containingDeclaration,
        null,
        Annotations.EMPTY,
        operatorName,
        CallableMemberDescriptor.Kind.DECLARATION,
        SourceElement.NO_SOURCE
    ) {
        init {
            val ownerType = (containingDeclaration as ClassDescriptor).defaultType
            val returnType = determineReturnType(operatorName, ownerType)

            initialize(
                null, null, emptyList(), emptyList(),
                emptyList(),
                returnType,
                Modality.FINAL,
                DescriptorVisibilities.PUBLIC
            )
        }

        override var isOperator: Boolean = true

        /**
         * 确定返回类型
         */
        private fun determineReturnType(operatorName: Name, ownerType: CangJieType): CangJieType {
            return when (operatorName) {
                // 大部分一元操作符返回相同类型
                OperatorNameConventions.UNARY_MINUS, OperatorNameConventions.INC,
                OperatorNameConventions.DEC, OperatorNameConventions.NOT -> ownerType

                else -> ownerType
            }
        }
    }
}