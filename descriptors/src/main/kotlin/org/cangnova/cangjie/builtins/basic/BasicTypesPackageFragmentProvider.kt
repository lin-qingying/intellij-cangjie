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

package org.cangnova.cangjie.builtins.basic

import org.cangnova.cangjie.builtins.BinaryOperatorRule
import org.cangnova.cangjie.builtins.BinaryOperatorRuleResultType
import org.cangnova.cangjie.builtins.PrimitiveType
import org.cangnova.cangjie.builtins.StandardNames.BASIC_PACKAGE_FQ_NAME
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.PackageFragmentDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.PrimitiveClassDescriptor
import org.cangnova.cangjie.incremental.components.LookupLocation
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.IntersectionTypeConstructor
import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.utils.Printer

/**
 * BasicTypesPackageFragmentProvider 负责提供基本类型（原始类型）的包片段。
 * 这个类集中管理所有基本类型的创建和访问，将基本类型的逻辑从 CangJieBuiltIns 中分离出来。
 */
class BasicTypesPackageFragmentProvider(
    private val storageManager: StorageManager,
    private val builtInsModule: ModuleDescriptor
) : PackageFragmentProvider {

    private val primitiveClassDescriptors = mutableMapOf<PrimitiveType, PrimitiveClassDescriptor>()

    // 基本包片段，用于承载所有的基本类型
    private val basicTypesPackageFragment: PackageFragmentDescriptor by lazy {
        createBasicTypesPackageFragment()
    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return if (fqName == BASIC_PACKAGE_FQ_NAME) {
            listOf(basicTypesPackageFragment)
        } else {
            emptyList()
        }
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return emptySet()
    }

    private fun createBasicTypesPackageFragment(): PackageFragmentDescriptor {
        return BasicTypesPackageFragmentDescriptor(builtInsModule, this)
    }

    fun createPrimitiveClassDescriptor(type: PrimitiveType): PrimitiveClassDescriptor {
        return primitiveClassDescriptors.getOrPut(type) {
            PrimitiveClassDescriptor(type, storageManager, builtInsModule, builtInsModule.builtIns)
        }
    }

    // 基本类型访问器
    val int64Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.INT64).defaultType
    val int32Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.INT32).defaultType
    val int16Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.INT16).defaultType
    val int8Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.INT8).defaultType
    val intNativeType: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.INTNATIVE).defaultType

    val uint64Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.UINT64).defaultType
    val uint32Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.UINT32).defaultType
    val uint16Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.UINT16).defaultType
    val uint8Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.UINT8).defaultType
    val uintNativeType: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.UINTNATIVE).defaultType

    val float64Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.FLOAT64).defaultType
    val float32Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.FLOAT32).defaultType
    val float16Type: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.FLOAT16).defaultType
    val runeType: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.Rune).defaultType

    val boolType: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.BOOL).defaultType
    val nothingType: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.Nothing).defaultType
    val unitType: SimpleType get() = createPrimitiveClassDescriptor(PrimitiveType.Unit).defaultType





    //    二进制运算规则
    val binaryOperatorRules: MutableMap<CjToken, List<BinaryOperatorRule>> = mutableMapOf()

    //    匹配规则
    fun matchBinaryOperatorRule(token: CjToken, leftType: CangJieType?, rightType: CangJieType?): BinaryOperatorRule {
        if (leftType == null || rightType == null) {
            return BinaryOperatorRule(leftType, rightType, BinaryOperatorRuleResultType.ERROR)
        }

        if (binaryOperatorRules.isEmpty()) {
            fillBinaryOperatorRules()
        }

        val leftType = if (leftType.constructor is IntersectionTypeConstructor) {
            (leftType.constructor as IntersectionTypeConstructor).getAlternativeType()
        } else {
            leftType
        }
        val rightType = if (rightType.constructor is IntegerLiteralTypeConstructor) {
            (rightType.constructor as IntegerLiteralTypeConstructor).getApproximatedType()
        } else {
            rightType
        }

//        查询规则
        val rule = binaryOperatorRules[token]
            ?: return BinaryOperatorRule(leftType, rightType, BinaryOperatorRuleResultType.ERROR)
//根据类型匹配
        for (r in rule) {
            if (r.leftType == leftType && r.rightType == rightType) {
                return r
            }
        }

        return BinaryOperatorRule(leftType, rightType, BinaryOperatorRuleResultType.ERROR)
    }

    //    填充规则
    private fun fillBinaryOperatorRules() {

        binaryOperatorRules[CjTokens.PLUS] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.MINUS] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.MUL] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.DIV] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.MULMUL] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),

            BinaryOperatorRule(float64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.PERC] = listOf(
            BinaryOperatorRule(int64Type, int64Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int32Type, int32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int16Type, int16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(int8Type, int8Type, BinaryOperatorRuleResultType.LEFT),


            BinaryOperatorRule(float16Type, float16Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float32Type, float32Type, BinaryOperatorRuleResultType.LEFT),
            BinaryOperatorRule(float64Type, float64Type, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.GT] = listOf(
            BinaryOperatorRule(boolType, boolType, BinaryOperatorRuleResultType.LEFT),
        )

        binaryOperatorRules[CjTokens.GTEQ] = listOf(
            BinaryOperatorRule(boolType, boolType, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.LT] = listOf(
            BinaryOperatorRule(boolType, boolType, BinaryOperatorRuleResultType.LEFT),
        )
        binaryOperatorRules[CjTokens.LTEQ] = listOf(
            BinaryOperatorRule(boolType, boolType, BinaryOperatorRuleResultType.LEFT),
        )
    }


}

/**
 * 基本类型的包片段描述符
 */
private class BasicTypesPackageFragmentDescriptor(
    module: ModuleDescriptor,
    private val provider: BasicTypesPackageFragmentProvider
) : PackageFragmentDescriptorImpl(module, BASIC_PACKAGE_FQ_NAME) {
    override fun getMemberScope(): MemberScope {
        return _memberScope
    }

    override val containingDeclaration: ModuleDescriptor = module

    private val _memberScope by lazy {
        BasicTypesMemberScope(this, provider)
    }


    override val source: SourceElement = SourceElement.NO_SOURCE
    override val original: DeclarationDescriptorWithSource = this
    override val name: Name = Name.identifier("")
}

/**
 * 基本类型的成员作用域
 */
private class BasicTypesMemberScope(
    private val packageFragment: PackageFragmentDescriptor,
    private val provider: BasicTypesPackageFragmentProvider
) : MemberScope {

    private val allPrimitiveTypes = PrimitiveType.values().toList()


    override fun getContributedClassifier(
        name: Name,
        location: LookupLocation
    ): ClassifierDescriptor? {
        val primitiveType = allPrimitiveTypes.find { it.typeName.asString() == name.asString() }
        return primitiveType?.let { provider.createPrimitiveClassDescriptor(it) }
    }


    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            return allPrimitiveTypes
                .filter { nameFilter(it.typeName) }
                .map { provider.createPrimitiveClassDescriptor(it) }
        }
        return emptyList()
    }

    override fun getContributedVariables(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard VariableDescriptor> {
        return emptyList()
    }

    override fun getContributedPropertys(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard PropertyDescriptor> {
        return emptyList()
    }

    override val functionNames: Set<Name> = emptySet()
    override val variableNames: Set<Name> = emptySet()
    override val classifierNames: Set<Name> = allPrimitiveTypes.map { it.typeName }.toSet()
    override val propertyNames: Set<Name> = emptySet()

    override fun getContributedFunctions(
        name: Name,
        location: LookupLocation
    ): Collection<@JvmWildcard SimpleFunctionDescriptor> {
        return emptyList()
    }

    override fun printScopeStructure(p: Printer) {

    }
}