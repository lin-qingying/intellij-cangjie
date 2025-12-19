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
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.metadata.model.wrapper.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.*
import org.cangnova.cangjie.psi.stubs.impl.*

/**
 * 类 Stub 构建器，用于从 Flatbuffers 元数据构建类相关的 Stub
 *
 * 该构建器负责为 class、interface、struct、enum 等类型声明创建 Stub。
 * Stub 是 IntelliJ 平台用于快速索引和查找的轻量级 PSI 表示。
 *
 * @property parentStub 父 Stub 元素
 * @property outerContext 外部构建上下文
 * @property classDecl 类声明包装器
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
        val classOrObjectStub = createClassOrObjectStubAndModifierListStub() ?: return null

        val typeParameterContext = createTypeParameterListStub(classOrObjectStub)

        createSuperTypeListStub(classOrObjectStub, typeParameterContext)
        val classBodyContext = typeParameterContext.child(
            emptyList(),
            classDecl.name,
            protoContainer = ProtoContainer.Class(
                classDecl,
                outerContext.typeTable,
                outerContext.protoContainer as? ProtoContainer.Class
            )
        )
        createClassBodyStub(classOrObjectStub, classBodyContext)

        return classOrObjectStub
    }

    /**
     * 创建类或对象 Stub 以及修饰符列表 Stub
     *
     * 根据类的类型（class/interface/struct/enum）创建对应的 Stub 实现。
     *
     * @return 创建的 Stub，如果类型不支持则返回 null
     */
    private fun createClassOrObjectStubAndModifierListStub(): StubElement<out PsiElement>? {
        val fqName = outerContext.containerFqName.child(shortName)

        val superTypeRefs = classDecl.superTypes
            .mapNotNull { extractTypeName(it) }
            .map { it.ref() }
            .toTypedArray()

        val classOrObjectStub: StubElement<out PsiElement> = when (classDecl.kind) {
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

        createModifierListStubForDeclaration(
            classOrObjectStub,
            classDecl.visibility,
            classDecl.modality
        )

        return classOrObjectStub
    }

    /**
     * 创建类型参数列表 Stub
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
            val typeParamStub = CangJieTypeParameterStubImpl(typeParamListStub, typeParam.name.ref())

            // 处理类型参数的上界约束
            val uppers = typeParam.uppers
            if (uppers.isNotEmpty()) {
                for (upper in uppers) {
                    TypeClsStubBuilder(typeParamStub, innerContext).createTypeReferenceStub(upper)
                }
            }
        }

        return innerContext
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
            context.protoContainer!!,
            classDecl.functions,
            classDecl.propertys
        )

        // 创建成员变量 Stubs
        for (variable in classDecl.variables) {
            VariableClsStubBuilder(classBodyStub, context, context.protoContainer!!, variable).build()
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

        CangJieEnumEntryStubImpl(
            CjStubElementTypes.ENUM_ENTRY,
            parent,
            fqName.ref(),
            fqName.ref(),
            classId = null,
            name = entryName.ref(),
            isLocal = false
        )
    }

    private fun extractTypeName(typeWrapper: TypeWrapper): org.cangnova.cangjie.name.Name? {
        return TypeClsStubBuilder.extractTypeName(typeWrapper)
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
            hasValOrVar = param.isMemberParam,
            hasDefaultValue = param.declaresDefaultValue
        )

        TypeClsStubBuilder(paramStub, context).createTypeReferenceStub(param.type)
    }
}
