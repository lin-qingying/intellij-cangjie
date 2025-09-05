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

package org.cangnova.cangjie.psi
import org.cangnova.cangjie.name.*

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.cangnova.cangjie.psi.psiUtil.identifier
import org.cangnova.cangjie.psi.stubs.CangJieExtendStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjExtend : CjTypeStatement {
    private val _stub: CangJieExtendStub?
        get() = stub as? CangJieExtendStub

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R? {
        return visitor.visitExtend(this, data)
    }

    override val typeName: String
        get() = "extend"

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieExtendStub) : super(stub, CjStubElementTypes.EXTEND)

//    override val fqName: FqName?
//        get() = super.fqName

    override fun getName(): String? {
        return getExtendName()
    }

    private fun getExtendName(): String? {
        return when (val type = receiverTypeReceiver?.typeElement) {
            is CjUserType -> {
                type.referencedName
            }

            is CjOptionType -> {
                return "Option"
            }

            is CjBasicType -> {
                type.name
            }

            else -> null
        }
    }

    override fun getNameIdentifier(): PsiElement? {
        return receiverTypeReceiver?.typeElement?.identifier
    }

    // 被扩展类型
    val receiverTypeReceiver: CjTypeReference?
        get() {
            val stub = stub
            if (stub != null) {

                val childTypeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE)
                return if (childTypeReferences.isNotEmpty()) {
                    childTypeReferences[0]
                } else {
                    null
                }
            }
            return getReceiverTypeRefByTree()
        }
    override val nameAsSafeName: Name
        get() = receiverTypeReceiver?.text?.let {
            if (it.isEmpty()) {
                Name.ERROR_NAME
            } else {
                Name.identifier(it)
            }
        } ?: Name.ERROR_NAME
    override val nameAsName: Name
        get() = name?.let { Name.identifier(it) } ?: Name.ERROR_NAME

    //    扩展id ，需要具有唯一性  ，通过被扩展名，父类，包名，行号
    fun getExtendId(): String {
        val sb = StringBuilder()

        sb.append(name)
        sb.append(getSupernames())

        sb.append(fqName)

        sb.append(textOffset)
        sb.append(textRange)
        sb.append(text)

        return sb.toString()
    }

    private fun getSupernames(): String {
        val list = findChildByType<CjSuperTypeList>(CjNodeTypes.SUPER_TYPE_LIST) ?: return "null"

        val names = list.getChildrenOfType<CjSuperTypeEntry>().map {
            it.children[0].text
        }
        return names.joinToString()
    }

    private fun getReceiverTypeRefByTree(): CjTypeReference? {
        var child = firstChild
        while (child != null) {
            val tt = child.node.elementType
            if (tt === CjTokens.LPAR || tt === CjTokens.COLON) break
            if (child is CjTypeReference) {
                return child
            }
            child = child.nextSibling
        }

        return null
    }
}
