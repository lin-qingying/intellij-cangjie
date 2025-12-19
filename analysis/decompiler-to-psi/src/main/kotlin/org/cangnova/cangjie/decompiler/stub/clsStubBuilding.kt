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
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.DescriptorVisibilities
import org.cangnova.cangjie.descriptors.DescriptorVisibility
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.metadata.model.wrapper.FunctionWrapper
import org.cangnova.cangjie.metadata.model.wrapper.PropertyWrapper
import org.cangnova.cangjie.metadata.model.wrapper.VariableWrapper
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.CangJieUserTypeStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

fun computeParameterName(name: Name): Name {
    return when {
        name == SpecialNames.IMPLICIT_SET_PARAMETER -> StandardNames.DEFAULT_VALUE_PARAMETER
        SpecialNames.isAnonymousParameterName(name) -> Name.identifier("_")
        else -> name
    }
}

/**
 * 创建不兼容 ABI 版本的文件 Stub
 */
fun createIncompatibleAbiVersionFileStub(): CangJieFileStubImpl = createFileStub(FqName.ROOT)

/**
 * 创建文件 Stub
 */
fun createFileStub(packageFqName: FqName): CangJieFileStubImpl {
    val fileStub = CangJieFileStubImpl.forFile(packageFqName)
    setupFileStub(fileStub, packageFqName)
    return fileStub
}

/**
 * 设置文件 Stub 的包声明和导入列表
 */
private fun setupFileStub(fileStub: CangJieFileStubImpl, packageFqName: FqName) {
    // 创建正确的 CangJiePackageDirectiveStub 实例，而不是使用占位符 Stub
    val packageDirectiveStub = CangJiePackageDirectiveStubImpl(fileStub)
    createStubForPackageName(packageDirectiveStub, packageFqName)
    CangJiePlaceHolderStubImpl<CjImportList>(fileStub, CjStubElementTypes.IMPORT_LIST)
}

/**
 * 递归创建包名 Stub
 */
fun createStubForPackageName(packageDirectiveStub: StubElement<out PsiElement>, packageFqName: FqName) {
    val segments = packageFqName.pathSegments()
    val iterator = segments.listIterator(segments.size)

    fun recCreateStubForPackageName(current: StubElement<out PsiElement>) {
        when (iterator.previousIndex()) {
            -1 -> return
            0 -> {
                CangJieNameReferenceExpressionStubImpl(current, iterator.previous().ref())
                return
            }
            else -> {
                val lastSegment = iterator.previous()
                val receiver = CangJiePlaceHolderStubImpl<CjDotQualifiedExpression>(
                    current,
                    CjStubElementTypes.DOT_QUALIFIED_EXPRESSION
                )
                recCreateStubForPackageName(receiver)
                CangJieNameReferenceExpressionStubImpl(receiver, lastSegment.ref())
            }
        }
    }

    recCreateStubForPackageName(packageDirectiveStub)
}

/**
 * Name 转 StringRef 扩展函数
 */
fun Name.ref(): StringRef = StringRef.fromString(this.asString())!!

/**
 * FqName 转 StringRef 扩展函数
 */
fun FqName.ref(): StringRef = StringRef.fromString(this.asString())!!

/**
 * 创建修饰符列表 Stub
 */
fun createModifierListStub(
    parent: StubElement<out PsiElement>,
    modifiers: Collection<CjModifierKeywordToken>
): CangJieModifierListStubImpl? {
    if (modifiers.isEmpty()) {
        return null
    }
    return CangJieModifierListStubImpl(
        parent,
        ModifierMaskUtils.computeMask { it in modifiers },
        CjStubElementTypes.MODIFIER_LIST
    )
}

/**
 * 创建空修饰符列表 Stub
 */
fun createEmptyModifierListStub(parent: CangJieStubBaseImpl<*>): CangJieModifierListStubImpl {
    return CangJieModifierListStubImpl(
        parent,
        ModifierMaskUtils.computeMask { false },
        CjStubElementTypes.MODIFIER_LIST
    )
}

/**
 * 根据可见性和模态创建修饰符列表 Stub
 */
fun createModifierListStubForDeclaration(
    parent: StubElement<out PsiElement>,
    visibility: DescriptorVisibility,
    modality: Modality?,
    additionalModifiers: List<CjModifierKeywordToken> = emptyList()
): CangJieModifierListStubImpl {
    val modifiers = mutableListOf<CjModifierKeywordToken>()

    // 添加可见性修饰符
    when (visibility) {
        DescriptorVisibilities.PUBLIC -> modifiers.add(CjTokens.PUBLIC_KEYWORD)
        DescriptorVisibilities.PRIVATE -> modifiers.add(CjTokens.PRIVATE_KEYWORD)
        DescriptorVisibilities.PROTECTED -> modifiers.add(CjTokens.PROTECTED_KEYWORD)
        DescriptorVisibilities.INTERNAL -> modifiers.add(CjTokens.INTERNAL_KEYWORD)
    }

    // 添加模态修饰符
    when (modality) {
        Modality.ABSTRACT -> modifiers.add(CjTokens.ABSTRACT_KEYWORD)
        Modality.OPEN -> modifiers.add(CjTokens.OPEN_KEYWORD)
        Modality.SEALED -> modifiers.add(CjTokens.SEALED_KEYWORD)
        else -> {}
    }

    modifiers.addAll(additionalModifiers)

    return createModifierListStub(parent, modifiers)
        ?: CangJieModifierListStubImpl(
            parent,
            ModifierMaskUtils.computeMask { false },
            CjStubElementTypes.MODIFIER_LIST
        )
}

/**
 * 创建类型名称 Stub
 */
fun createStubForTypeName(
    typeClassId: ClassId,
    parent: StubElement<out PsiElement>,
    bindTypeArguments: (CangJieUserTypeStub, Int) -> Unit = { _, _ -> }
): CangJieUserTypeStub {
    val substituteWithAny = typeClassId.isLocal

    val fqName = if (substituteWithAny) StandardNames.FqNames.anyUFqName
    else typeClassId.asSingleFqName().toUnsafe()

    val segments = fqName.pathSegments().asReversed()
    assert(segments.isNotEmpty())
    val classesNestedLevel = segments.size - if (substituteWithAny) 1 else typeClassId.packageFqName.pathSegments().size

    fun recCreateStubForType(current: StubElement<out PsiElement>, level: Int): CangJieUserTypeStub {
        val lastSegment = segments[level]
        val userTypeStub = CangJieUserTypeStubImpl(current)
        if (level + 1 < segments.size) {
            recCreateStubForType(userTypeStub, level + 1)
        }
        CangJieNameReferenceExpressionStubImpl(userTypeStub, lastSegment.ref(), level < classesNestedLevel)
        if (!substituteWithAny) {
            bindTypeArguments(userTypeStub, level)
        }
        return userTypeStub
    }

    return recCreateStubForType(parent, level = 0)
}

/**
 * 创建声明 Stubs（函数和属性）
 */
fun createDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer,
    functionWrappers: List<FunctionWrapper>,
    propertyWrappers: List<PropertyWrapper>,
) {
    for (propertyWrapper in propertyWrappers) {
        PropertyClsStubBuilder(parentStub, outerContext, protoContainer, propertyWrapper).build()
    }
    for (functionWrapper in functionWrappers) {
        FunctionClsStubBuilder(parentStub, outerContext, protoContainer, functionWrapper).build()
    }
}

/**
 * 创建包声明 Stubs
 */
fun createPackageDeclarationsStubs(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    protoContainer: ProtoContainer.Package,
    functionWrappers: List<FunctionWrapper>,
    variableWrappers: List<VariableWrapper>
) {
    for (functionWrapper in functionWrappers) {
        FunctionClsStubBuilder(parentStub, outerContext, protoContainer, functionWrapper).build()
    }
    for (variableWrapper in variableWrappers) {
        VariableClsStubBuilder(parentStub, outerContext, protoContainer, variableWrapper).build()
    }
}