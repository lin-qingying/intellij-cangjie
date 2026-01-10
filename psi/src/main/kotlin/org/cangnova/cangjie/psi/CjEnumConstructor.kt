/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.psi.stubs.CangJieEnumConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

/**
 * 枚举构造器 PSI 元素
 *
 * 根据仓颉语言规范，枚举条目是构造器，用于创建枚举实例。
 * 不是声明，只是枚举的组成部分。
 */
class CjEnumConstructor : CjModifierListOwnerStub<CangJieEnumConstructorStub>,CjModifierListOwner, PsiNameIdentifierOwner, CjAnnotated {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieEnumConstructorStub) : super(stub, CjStubElementTypes.ENUM_CONSTRUCTOR)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitEnumConstructor(this, data)
    }

    override fun getName(): String? {
        val stub = greenStub
        if (stub != null) {
            return stub.name
        }
        return nameIdentifier?.text
    }

    override fun setName(name: String): PsiElement {
        // 枚举构造器名称通常不支持重命名
        return this
    }

    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(CjTokens.IDENTIFIER)
    }

    /**
     * 所属的枚举类型名称
     */
    val enumTypeName: Name
        get() {
            val enum = getStrictParentOfType<CjEnum>()
            return enum?.nameAsSafeName ?: Name.ERROR_NAME
        }

    /**
     * 获取所属枚举类型
     */
    val parentEnum: CjEnum?
        get() = getStrictParentOfType()

    /**
     * 枚举条目的类型入口（参数列表）
     */
    val typeEntry: CjEnumConstructorTypeEntry?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_LIST)

    /**
     * 构造器的参数类型引用列表
     */
    val typeReferences: List<CjTypeReference>
        get() {
            return typeEntry?.getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE) ?: emptyList()
        }

    /**
     * 是否有显式参数（判断是否为有参数构造器）
     */
    fun hasParameters(): Boolean = typeReferences.isNotEmpty()

    /**
     * 获取参数数量
     */
    fun getParameterCount(): Int {
        val stub = greenStub
        return if (stub != null) {
            stub.getTypeCount()
        } else {
            typeReferences.size
        }
    }

    /**
     * 注解列表（枚举构造器可能有注解）
     */
    override val annotations: CjAnnotations?
        get() = getStubOrPsiChild(CjStubElementTypes.ANNOTATIONS)
    override val annotationEntries: List<CjAnnotation>
        get() = annotations?.entries ?: emptyList()

    /**
     * 修饰符列表（枚举构造器可能有修饰符）
     */
    override val modifierList: CjDeclarationModifierList?
        get() = getStubOrPsiChild(CjStubElementTypes.MODIFIER_LIST)

    override fun hasModifier(modifier: CjKeywordToken): Boolean {
        val modifierList = modifierList
        return modifierList != null && modifierList.hasModifier(modifier)
    }

    override fun addModifier(modifier: CjKeywordToken) {
        org.cangnova.cangjie.psi.psiUtil.addModifier(this, modifier)
    }

    override fun removeModifier(modifier: CjKeywordToken) {
        org.cangnova.cangjie.psi.psiUtil.removeModifier(this, modifier)
    }

    override fun toString(): String {
        return "CjEnumConstructor(${name ?: "<anonymous>"})"
    }
}
