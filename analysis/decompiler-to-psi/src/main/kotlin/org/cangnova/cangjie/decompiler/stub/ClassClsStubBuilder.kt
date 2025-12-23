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

/**
 * 类 Stub 构建器
 *
 * ## 架构作用
 *
 * ClassClsStubBuilder 负责从 Flatbuffers 元数据构建类声明的 PSI Stub。
 * 它支持仓颉语言的所有类型声明：class、interface、struct、enum。
 *
 * ## 支持的类型声明
 *
 * | 类型 | Stub 类 | 特殊处理 |
 * |------|---------|---------|
 * | class | [CangJieClassStubImpl] | 支持构造函数、属性、方法 |
 * | interface | [CangJieInterfaceStubImpl] | 只有抽象方法 |
 * | struct | [CangJieStructStubImpl] | 值类型，类似 class |
 * | enum | [CangJieEnumStubImpl] | 需要创建枚举项 Stub |
 *
 * ## Stub 构建流程
 *
 * ```
 * ClassClsStubBuilder.build()
 *   ├─ createClassStubAndModifierListStub()
 *   │   ├─ 根据 ClassKind 创建对应的 Stub (Class/Interface/Struct/Enum)
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
 *       ├─ 创建成员变量
 *       └─ (enum only) createEnumEntryStubs() - 枚举项
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
 * ## 枚举特殊处理
 *
 * 枚举类型需要额外处理：
 * 1. 创建 [CjEnumBody] 而不是 [CjClassBody]
 * 2. 调用 [createEnumEntryStubs] 创建所有枚举项
 * 3. 枚举项使用 [CangJieEnumConstructorStubImpl]
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
 * @see CangJieEnumStubImpl
 */
class ClassClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val classDecl: ClassDeclWrapper,
) {
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
     * 创建类或对象 Stub 以及修饰符列表 Stub
     *
     * 根据类的类型（class/interface/struct/enum）创建对应的 Stub 实现。
     *
     * @return 创建的 Stub，如果类型不支持则返回 null
     */
    private fun createClassStubAndModifierListStub(): StubElement<out PsiElement>? {
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
                isLocal = false
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
    private fun createTypeParameterListStub(classOrObjectStub: StubElement<out PsiElement>): ClsStubBuilderContext {
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
    private fun createTypeConstraintListStub(
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
    private fun createSuperTypeListStub(
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

    /**
     * 创建类体 Stub
     *
     * 根据类的类型创建对应的类体（ClassBody/InterfaceBody/EnumBody），
     * 并创建所有成员的 Stub（构造函数、函数、属性、变量、枚举项）。
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
            ClassKind.ENUM -> CangJiePlaceHolderStubImpl<CjEnumBody>(
                classOrObjectStub,
                CjStubElementTypes.ENUM_BODY
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

        // 创建成员变量 Stubs
        for (variable in classDecl.variables) {
            VariableClsStubBuilder(classBodyStub, context, context.metadataContainer, variable).build()
        }

        // 如果是枚举，创建枚举项 Stubs
        if (classDecl is EnumWrapper) {
            createEnumEntryStubs(classBodyStub, context, classDecl)
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

    private fun createEnumEntryStubs(
        classBodyStub: StubElement<out PsiElement>,
        context: ClsStubBuilderContext,
        enumWrapper: EnumWrapper
    ) {
        for (entry in enumWrapper.entrys) {
            createEnumEntryStub(classBodyStub, context, entry)
        }
    }

    private fun createEnumEntryStub(
        parent: StubElement<out PsiElement>,
        context: ClsStubBuilderContext,
        entry: EnumEntryWrapper
    ) {
        val entryName = entry.name
        val fqName = context.containerFqName.child(entryName)
        val parentEnumFqName = context.containerFqName

        // 提取参数信息（如果元数据中有）
        val parameterCount = 0  // TODO: 从 entry 中提取实际参数数量
        val parameterTypeNames = emptyList<String>()  // TODO: 从 entry 中提取实际参数类型

        CangJieEnumConstructorStubImpl(
            CjStubElementTypes.ENUM_CONSTRUCTOR,
            parent,
            fqName.ref(),              // 枚举条目 FQN
            parentEnumFqName.ref(),    // 父枚举 FQN
            classId = null,
            name = entryName.ref(),
            parameterCount,            // 参数数量
            parameterTypeNames,        // 参数类型名称
            isLocal = false
        )
    }

    private fun extractTypeName(typeWrapper: TypeWrapper): org.cangnova.cangjie.name.Name? {
        return TypeClsStubBuilder.extractTypeName(typeWrapper, outerContext)
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
            CangJieConstructorStubImpl(
                parentStub,
                CjStubElementTypes.PRIMARY_CONSTRUCTOR,
                name,
                hasBody = false,
                isDelegatedCallToThis = false
            )
        } else {
            CangJieConstructorStubImpl(
                parentStub,
                CjStubElementTypes.SECONDARY_CONSTRUCTOR,
                name,
                hasBody = true,
                isDelegatedCallToThis = false
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
    return ClassClsStubBuilder(parentStub, context, classDecl).build()
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
