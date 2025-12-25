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

package org.cangnova.cangjie.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.metadata.model.wrapper.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.*
import org.cangnova.cangjie.psi.stubs.impl.*

abstract class   EnumAndClassClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val classDecl: ClassDeclWrapper,
) {
    private val classId: ClassId = classDecl.classId
    private val shortName = classId.shortClassName
    private val isTopLevel = !classId.isNestedClass


    /**
     * 创建类或对象 Stub 以及修饰符列表 Stub
     *
     * 根据类的类型（class/interface/struct/enum）创建对应的 Stub 实现。
     *
     * @return 创建的 Stub，如果类型不支持则返回 null
     */
    protected fun createClassStubAndModifierListStub(): StubElement<out PsiElement>? {
        val fqName = outerContext.containerFqName.child(shortName)

        val superTypeRefs = classDecl.superTypes
            .mapNotNull { extractTypeName(it) }
            .map { it.ref() }
            .toTypedArray()

        val classStub: StubElement<out PsiElement> = when (classDecl.kind) {
            ClassKind.CLASS -> CangJieClassStubImpl(
                CjClassElementType.stubType,
                parentStub,
                fqName.ref(),
                classId = classId.takeUnless { it.isLocal },
                shortName.ref(),
                superTypeRefs,
                isLocal = false
            )

            ClassKind.INTERFACE -> CangJieInterfaceStubImpl(
                CjInterfaceElementType.stubType,
                parentStub,
                fqName.ref(),
                classId = classId.takeUnless { it.isLocal },
                shortName.ref(),
                superTypeRefs,
                isLocal = false
            )

            ClassKind.STRUCT -> CangJieStructStubImpl(
                CjStructElementType.stubType,
                parentStub,
                fqName.ref(),
                classId = classId.takeUnless { it.isLocal },
                shortName.ref(),
                superTypeRefs,
                isLocal = false
            )

            ClassKind.ENUM -> CangJieEnumStubImpl(
                CjEnumElementType.getStubType(),
                parentStub,
                fqName.ref(),
                classId = classId.takeUnless { it.isLocal },
                shortName.ref(),
                superTypeRefs,
                isLocal = false,
                isNonExhaustive = (classDecl as? EnumWrapper)?.isNonExhaustive ?: false
            )

            else -> return null
        }

        // 先创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(classStub, classDecl.annotations)

        createModifierListStubForDeclaration(
            classStub,
            classDecl.visibility,
            classDecl.modality
        )

        return classStub
    }
    protected fun extractTypeName(typeWrapper: TypeWrapper): org.cangnova.cangjie.name.Name? {
        return TypeClsStubBuilder.extractTypeName(typeWrapper, outerContext)
    }
    /**
     * 创建类型参数列表 Stub
     *
     * 注意：此方法只创建类型参数列表，不创建类型约束列表。
     * 类型约束列表（where 子句）应该在 SUPER_TYPE_LIST 之后创建，
     * 以保持与 PSI 解析顺序一致。
     *
     * @param classOrObjectStub 类或对象 Stub
     * @return 包含类型参数的新上下文
     */
    protected fun createTypeParameterListStub(classOrObjectStub: StubElement<out PsiElement>): ClsStubBuilderContext {
        val typeParameters = classDecl.typeParameters
        if (typeParameters.isEmpty()) {
            return outerContext
        }

        val typeParamListStub = CangJiePlaceHolderStubImpl<CjTypeParameterList>(
            classOrObjectStub,
            CjStubElementTypes.TYPE_PARAMETER_LIST
        )

        val innerContext = outerContext.child(typeParameters)

        for (typeParam in typeParameters) {
            // 只创建类型参数名称，不包含 bounds（bounds 放在 where 子句中）
            CangJieTypeParameterStubImpl(typeParamListStub, typeParam.name.ref())
        }

        // 注意：类型约束列表（where 子句）在 build() 方法中于 SUPER_TYPE_LIST 之后创建

        return innerContext
    }
    /**
     * 创建类型约束列表 Stub（where 子句）
     */
    protected fun createTypeConstraintListStub(
        classOrObjectStub: StubElement<out PsiElement>,
        context: ClsStubBuilderContext
    ) {
        val typeParameters = classDecl.typeParameters
        val constraintsToCreate = mutableListOf<Pair<org.cangnova.cangjie.name.Name, TypeWrapper>>()

        for (typeParam in typeParameters) {
            for (upper in typeParam.uppers) {
                constraintsToCreate.add(Pair(typeParam.name, upper))
            }
        }

        if (constraintsToCreate.isEmpty()) return

        val constraintListStub = CangJiePlaceHolderStubImpl<CjTypeConstraintList>(
            classOrObjectStub,
            CjStubElementTypes.TYPE_CONSTRAINT_LIST
        )

        for ((paramName, upperBound) in constraintsToCreate) {
            val constraintStub = CangJiePlaceHolderStubImpl<CjTypeConstraint>(
                constraintListStub,
                CjStubElementTypes.TYPE_CONSTRAINT
            )
            // 创建类型参数名称引用
            CangJieNameReferenceExpressionStubImpl(constraintStub, paramName.ref(), false)
            // 创建 bound 类型引用
            TypeClsStubBuilder(constraintStub, context).createTypeReferenceStub(upperBound)
        }
    }

    /**
     * 创建父类型列表 Stub
     *
     * @param classOrObjectStub 类或对象 Stub
     * @param context 构建上下文
     */
    protected fun createSuperTypeListStub(
        classOrObjectStub: StubElement<out PsiElement>,
        context: ClsStubBuilderContext
    ) {
        val superTypes = classDecl.superTypes
        if (superTypes.isEmpty()) {
            return
        }

        val superTypeListStub = CangJiePlaceHolderStubImpl<CjSuperTypeList>(
            classOrObjectStub,
            CjStubElementTypes.SUPER_TYPE_LIST
        )

        for (superType in superTypes) {
            val superTypeEntryStub = CangJiePlaceHolderStubImpl<CjSuperTypeEntry>(
                superTypeListStub,
                CjStubElementTypes.SUPER_TYPE_ENTRY
            )
            TypeClsStubBuilder(superTypeEntryStub, context).createTypeReferenceStub(superType)
        }
    }
}

/**
 * 类 Stub 构建器
 *
 * ## 架构作用
 *
 * ClassClsStubBuilder 负责从 Flatbuffers 元数据构建类声明的 PSI Stub。
 * 它支持仓颉语言的类型声明：class、interface、struct。
 *
 * **注意**: 枚举类型由专门的 [EnumClsStubBuilder] 处理。
 *
 * ## 支持的类型声明
 *
 * | 类型 | Stub 类 | 特殊处理 |
 * |------|---------|---------|
 * | class | [CangJieClassStubImpl] | 支持构造函数、属性、方法 |
 * | interface | [CangJieInterfaceStubImpl] | 只有抽象方法 |
 * | struct | [CangJieStructStubImpl] | 值类型，类似 class |
 *
 * ## Stub 构建流程
 *
 * ```
 * ClassClsStubBuilder.build()
 *   ├─ createClassStubAndModifierListStub()
 *   │   ├─ 根据 ClassKind 创建对应的 Stub (Class/Interface/Struct)
 *   │   ├─ 提取父类型引用（用于快速索引）
 *   │   └─ 创建修饰符列表（可见性、模态）
 *   ├─ createTypeParameterListStub()
 *   │   ├─ 创建类型参数列表 <T, U>
 *   │   └─ createTypeConstraintListStub() - where 子句
 *   ├─ createSuperTypeListStub()
 *   │   └─ 创建父类型完整引用（支持跨包类型）
 *   └─ createClassBodyStub()
 *       ├─ createConstructorStubs() - 构造函数
 *       ├─ createDeclarationsStubs() - 函数和属性
 *       └─ 创建成员变量
 * ```
 *
 * ## PSI 结构示例
 *
 * 对于类 `class Box<T: Comparable<T>>(val value: T) : Container<T>`：
 *
 * ```
 * CangJieClassStub ("Box")
 *   ├─ CangJieModifierListStub (public)
 *   ├─ CjTypeParameterList
 *   │   └─ CjTypeParameter ("T")
 *   ├─ CjTypeConstraintList (where T: Comparable<T>)
 *   │   └─ CjTypeConstraint
 *   │       ├─ NameRef ("T")
 *   │       └─ TypeRef (Comparable<T>)
 *   ├─ CjSuperTypeList
 *   │   └─ CjSuperTypeEntry
 *   │       └─ TypeRef (Container<T>)
 *   └─ CjClassBody
 *       ├─ CjConstructor (primary)
 *       │   └─ CjParameterList
 *       │       └─ CjParameter ("value", T)
 *       └─ ... (其他成员)
 * ```
 *
 * ## 快速索引优化
 *
 * 为了支持快速的符号查找，Stub 中存储了一些冗余信息：
 * - **superTypeRefs**: 父类型的名称数组（[StringRef]），用于快速检查继承关系
 * - **classId**: 类的唯一标识符（包含包名和类名），用于快速定位
 * - **fqName**: 完全限定名，用于索引和搜索
 *
 * ## 类型参数和约束
 *
 * 类型参数的 bounds 不直接存储在 [CjTypeParameter] 中，而是：
 * 1. 创建 [CjTypeParameterList] 时只存储参数名
 * 2. 创建 [CjTypeConstraintList] (where 子句) 存储所有约束
 *
 * 这样设计的原因：
 * - 更贴近仓颉语言的语法（where 子句）
 * - 支持多个约束：`where T: A, T: B`
 * - 简化 PSI 结构
 *
 * ## 嵌套类处理
 *
 * 嵌套类通过 [ClassId.isNestedClass] 识别：
 * - 顶层类: `isTopLevel = true`, 直接在文件 Stub 下创建
 * - 嵌套类: `isTopLevel = false`, 在父类的 ClassBody 下创建
 *
 * ## 本地类降级
 *
 * 对于本地类（匿名类、lambda 类）：
 * - `classId.isLocal = true`
 * - Stub 中 `classId` 设置为 `null`
 * - 避免索引冲突和查找错误
 *
 * ## 使用场景
 *
 * 1. **文件级类**: 在 [CangJieMetadataStubBuilder.buildCompatibleFileStub] 中调用
 * 2. **嵌套类**: 在 [createClassBodyStub] 中递归调用
 * 3. **IDE 索引**: 支持类名搜索、继承查找、成员导航
 *
 * @property parentStub 父 Stub 元素（文件 Stub 或外部类 Stub）
 * @property outerContext 外部构建上下文（包含包名、类型表等）
 * @property classDecl 类声明包装器（来自元数据）
 *
 * @see ClassDeclWrapper
 * @see CangJieClassStubImpl
 * @see CangJieInterfaceStubImpl
 * @see CangJieStructStubImpl
 * @see EnumClsStubBuilder
 */
class ClassClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val classDecl: ClassDeclWrapper,
) :EnumAndClassClsStubBuilder(parentStub, outerContext, classDecl){
    private val classId: ClassId = classDecl.classId
    private val shortName = classId.shortClassName
    private val isTopLevel = !classId.isNestedClass

    /**
     * 构建类 Stub
     *
     * @return 创建的类 Stub，如果类型不支持则返回 null
     */
    fun build(): StubElement<out PsiElement>? {
        val classStub = createClassStubAndModifierListStub() ?: return null

        val typeParameterContext = createTypeParameterListStub(classStub)

        // 注意：PSI 解析顺序是 SUPER_TYPE_LIST -> TYPE_CONSTRAINT_LIST
        // stub 构建必须保持相同顺序
        createSuperTypeListStub(classStub, typeParameterContext)
        createTypeConstraintListStub(classStub, typeParameterContext)

        val classBodyContext = typeParameterContext.child(
            emptyList(),
            classDecl.name,
            metadataContainer = MetadataContainer.Class(
                classDecl,
                outerContext.typeTable,
                outerContext.metadataContainer as? MetadataContainer.Class
            )
        )
        createClassBodyStub(classStub, classBodyContext)

        return classStub
    }





    /**
     * 创建类体 Stub
     *
     * 根据类的类型创建对应的类体（ClassBody/InterfaceBody），
     * 并创建所有成员的 Stub（构造函数、函数、属性、变量）。
     *
     * @param classOrObjectStub 类或对象 Stub
     * @param context 构建上下文
     */
    private fun createClassBodyStub(
        classOrObjectStub: StubElement<out PsiElement>,
        context: ClsStubBuilderContext
    ) {
        val classBodyStub: StubElement<out PsiElement> = when (classDecl.kind) {
            ClassKind.INTERFACE -> CangJiePlaceHolderStubImpl<CjInterfaceBody>(
                classOrObjectStub,
                CjStubElementTypes.INTERFACE_BODY
            )

            else -> CangJiePlaceHolderStubImpl<CjClassBody>(
                classOrObjectStub,
                CjStubElementTypes.CLASS_BODY
            )
        }

        // 创建构造函数 Stubs
        createConstructorStubs(classBodyStub, context)

        // 创建成员声明 Stubs（函数和属性）
        createDeclarationsStubs(
            classBodyStub,
            context,
            context.metadataContainer!!,
            classDecl.functions,
            classDecl.propertys
        )

        // 创建成员字段 Stubs
        for (variable in classDecl.variables) {
            FieldClsStubBuilder(classBodyStub, context, context.metadataContainer, variable).build()
        }
    }

    private fun createConstructorStubs(
        classBodyStub: StubElement<out PsiElement>,
        context: ClsStubBuilderContext
    ) {
        for (constructor in classDecl.constructors) {
            ConstructorClsStubBuilder(classBodyStub, context, constructor, classDecl).build()
        }
    }

}

/**
 * 枚举 Stub 构建器
 *
 * ## 架构作用
 *
 * EnumClsStubBuilder 专门负责从 Flatbuffers 元数据构建枚举声明的 PSI Stub。
 * 与类不同，枚举需要特殊处理枚举构造器（enum constructors），而不是普通的函数。
 *
 * ## 枚举特性
 *
 * 在仓颉语言中，枚举条目（enum entries）实际上是枚举构造器：
 * - 每个枚举条目都是一个构造器调用
 * - 枚举条目可以有参数
 * - 枚举构造器使用 [CangJieEnumConstructorStubImpl]
 *
 * ## Stub 构建流程
 *
 * ```
 * EnumClsStubBuilder.build()
 *   ├─ createClassStubAndModifierListStub()
 *   │   ├─ 创建 CangJieEnumStubImpl
 *   │   ├─ 提取父类型引用（用于快速索引）
 *   │   └─ 创建修饰符列表（可见性、模态）
 *   ├─ createTypeParameterListStub()
 *   │   ├─ 创建类型参数列表 <T, U>
 *   │   └─ createTypeConstraintListStub() - where 子句
 *   ├─ createSuperTypeListStub()
 *   │   └─ 创建父类型完整引用（支持跨包类型）
 *   └─ createEnumBodyStub()
 *       ├─ createEnumConstructorStubs() - 枚举构造器（从 constructor 创建）
 *       ├─ createDeclarationsStubs() - 函数和属性（过滤掉枚举构造器）
 *       └─ 创建成员变量
 * ```
 *
 * ## PSI 结构示例
 *
 * 对于枚举 `enum Color { Red, Green(value: Int), Blue }`：
 *
 * ```
 * CangJieEnumStub ("Color")
 *   ├─ CangJieModifierListStub (public)
 *   └─ CjEnumBody
 *       ├─ CangJieEnumConstructorStub ("Red")
 *       ├─ CangJieEnumConstructorStub ("Green")
 *       │   └─ CjParameterList
 *       │       └─ CjParameter ("value", Int)
 *       └─ CangJieEnumConstructorStub ("Blue")
 * ```
 *
 * ## 枚举构造器过滤
 *
 * 由于枚举条目在元数据中可能同时存在于：
 * 1. `EnumWrapper.constructor` - 枚举条目列表
 * 2. `EnumWrapper.functions` - 函数列表（作为构造器函数）
 *
 * 构建器需要：
 * - 从 `constructor` 创建 ENUM_CONSTRUCTOR 类型的 Stub
 * - 从 `functions` 中过滤掉与枚举条目同名的函数，避免重复创建
 *
 * @property parentStub 父 Stub 元素（文件 Stub 或外部类 Stub）
 * @property outerContext 外部构建上下文（包含包名、类型表等）
 * @property enumDecl 枚举声明包装器（来自元数据）
 *
 * @see EnumWrapper
 * @see CangJieEnumStubImpl
 * @see CangJieEnumConstructorStubImpl
 */
class EnumClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val enumDecl: EnumWrapper,
) : EnumAndClassClsStubBuilder(parentStub, outerContext, enumDecl) {
    private val classId: ClassId = enumDecl.classId
    private val shortName = classId.shortClassName
    private val isTopLevel = !classId.isNestedClass

    /**
     * 构建枚举 Stub
     *
     * @return 创建的枚举 Stub
     */
    fun build(): StubElement<out PsiElement> {
        val enumStub = createClassStubAndModifierListStub()!!

        val typeParameterContext = createTypeParameterListStub(enumStub)

        // 注意：PSI 解析顺序是 SUPER_TYPE_LIST -> TYPE_CONSTRAINT_LIST
        // stub 构建必须保持相同顺序
        createSuperTypeListStub(enumStub, typeParameterContext)
        createTypeConstraintListStub(enumStub, typeParameterContext)

        val enumBodyContext = typeParameterContext.child(
            emptyList(),
            enumDecl.name,
            metadataContainer = MetadataContainer.Class(
                enumDecl,
                outerContext.typeTable,
                outerContext.metadataContainer as? MetadataContainer.Class
            )
        )
        createEnumBodyStub(enumStub, enumBodyContext)

        return enumStub
    }

    /**
     * 创建枚举体 Stub
     *
     * 创建枚举体并创建所有成员的 Stub：
     * 1. 枚举构造器（从 constructor 创建）
     * 2. 函数和属性（过滤掉枚举构造器）
     * 3. 成员变量
     *
     * @param enumStub 枚举 Stub
     * @param context 构建上下文
     */
    private fun createEnumBodyStub(
        enumStub: StubElement<out PsiElement>,
        context: ClsStubBuilderContext
    ) {
        val enumBodyStub = CangJiePlaceHolderStubImpl<CjEnumBody>(
            enumStub,
            CjStubElementTypes.ENUM_BODY
        )

        // 1. 创建枚举构造器 Stubs（从枚举条目创建）
        createEnumConstructorStubs(enumBodyStub, context)

        // 2. 创建成员声明 Stubs（函数和属性）
        // 需要过滤掉枚举构造器（它们已经通过 createEnumConstructorStubs 创建）
        val enumEntryNames = enumDecl.entrys.map { it.name }.toSet()
        val functionsToCreate = enumDecl.functions.filterNot { it.name in enumEntryNames }

        createDeclarationsStubs(
            enumBodyStub,
            context,
            context.metadataContainer!!,
            functionsToCreate,
            enumDecl.propertys
        )

        // 3. 创建成员字段 Stubs
        for (variable in enumDecl.variables) {
            FieldClsStubBuilder(enumBodyStub, context, context.metadataContainer, variable).build()
        }
    }

    /**
     * 创建枚举构造器 Stubs
     *
     * 遍历所有枚举条目（EnumConstructorWrapper），为每个条目创建 ENUM_CONSTRUCTOR 类型的 Stub。
     */
    private fun createEnumConstructorStubs(
        enumBodyStub: StubElement<out PsiElement>,
        context: ClsStubBuilderContext
    ) {
        for (entry in enumDecl.entrys) {
            createEnumConstructorStub(enumBodyStub, context, entry)
        }
    }

    /**
     * 创建单个枚举构造器 Stub
     *
     * @param parent 父 Stub 元素（枚举体）
     * @param context 构建上下文
     * @param entry 枚举条目包装器
     */
    private fun createEnumConstructorStub(
        parent: StubElement<out PsiElement>,
        context: ClsStubBuilderContext,
        entry: EnumConstructorWrapper
    ) {
        val entryName = entry.name

        // 提取参数类型数量
        val typeCount = entry.valueParameters.size

        // 获取枚举的 FqName（用于索引）
        val enumFqName = outerContext.containerFqName.child(enumDecl.name)

        // 创建枚举构造器 Stub
        val constructorStub = CangJieEnumConstructorStubImpl(
            CjStubElementTypes.ENUM_CONSTRUCTOR,
            parent,
            entryName.ref(),
            typeCount,
            enumFqName.ref(),
        )

        // 如果有参数类型，创建 TYPE_LIST
        if (typeCount > 0) {
            val typeListStub = CangJiePlaceHolderStubImpl<CjEnumConstructorTypeEntry>(
                constructorStub,
                CjStubElementTypes.TYPE_LIST
            )

            // 为每个参数类型创建 TYPE_REFERENCE
            for (param in entry.valueParameters) {
                TypeClsStubBuilder(typeListStub, context).createTypeReferenceStub(param.type)
            }
        }
    }
}

/**
 * 构造函数 Stub 构建器
 *
 * @property parentStub 父 Stub 元素
 * @property context 构建上下文
 * @property constructor 构造函数包装器
 * @property classDecl 类声明包装器
 */
class ConstructorClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val context: ClsStubBuilderContext,
    private val constructor: ConstructorWrapper,
    private val classDecl: ClassDeclWrapper
) {
    /**
     * 构建构造函数 Stub
     *
     * 根据是否为主构造函数创建不同的 Stub 类型。
     */
    fun build() {
        val name = classDecl.name.ref()

        val constructorStub = if (constructor.isPrimary) {
//            由于主构造函数的参数可能是某个具体的声明，所以标记为从构造函数，渲染时直接渲染声明和从构造函数，i使用isDelegatedCallToThis标记
            CangJieConstructorStubImpl(
                parentStub,
                CjStubElementTypes.SECONDARY_CONSTRUCTOR,
                name,

                hasBody = false,
                isDelegatedCallToThis = false,
                isPrimary = false

            )
        } else {
            CangJieConstructorStubImpl(
                parentStub,
                CjStubElementTypes.SECONDARY_CONSTRUCTOR,
                name,

                hasBody = true,
                isDelegatedCallToThis = false,
                isPrimary = false
            )
        }

        // 先创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(constructorStub, constructor.annotations)

        createModifierListStubForDeclaration(constructorStub, constructor.visibility, null)
        createValueParameterListStub(constructorStub, context, constructor.valueParameters)
    }
}

/**
 * 创建类 Stub 的入口函数
 */
fun createClassStub(
    parentStub: StubElement<out PsiElement>,
    classDecl: ClassDeclWrapper,
    context: ClsStubBuilderContext
): StubElement<out PsiElement>? {
    return if(classDecl.kind == ClassKind.ENUM){
        EnumClsStubBuilder(parentStub, context, classDecl as EnumWrapper).build()
    }else{
        ClassClsStubBuilder(parentStub, context, classDecl).build()
    }
}

/**
 * 创建值参数列表 Stub
 *
 * 为函数或构造函数创建参数列表 Stub，包括参数名称、类型、默认值等信息。
 *
 * @param parent 父 Stub 元素
 * @param context 构建上下文
 * @param valueParameters 值参数包装器列表
 */
fun createValueParameterListStub(
    parent: StubElement<out PsiElement>,
    context: ClsStubBuilderContext,
    valueParameters: List<ValueParameterWrapper>
) {
    val paramListStub = CangJiePlaceHolderStubImpl<CjParameterList>(
        parent,
        CjStubElementTypes.VALUE_PARAMETER_LIST
    )

    for (param in valueParameters) {
        val paramName = computeParameterName(param.name)
        val paramStub = CangJieParameterStubImpl(
            paramListStub,
            null,
            paramName.ref(),
            isMutable = false,
            hasLetOrVar = param.isMemberParam,
            hasDefaultValue = param.declaresDefaultValue,
            isNamed = param.isNamedParam
        )

        // 创建参数的注解 Stub（注解在类型引用之前）
        createAnnotationsStub(paramStub, param.annotations)

        TypeClsStubBuilder(paramStub, context).createTypeReferenceStub(param.type)

        // 如果有默认值，创建 REFERENCE_EXPRESSION stub 占位符
        if (param.declaresDefaultValue) {
            CangJieNameReferenceExpressionStubImpl(
                paramStub,
                StringRef.fromString("COMPILED_CODE")!!
            )
        }
    }
}
