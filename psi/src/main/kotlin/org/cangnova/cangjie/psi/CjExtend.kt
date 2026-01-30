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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import org.cangnova.cangjie.psi.stubs.CangJieExtendStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

class CjExtend : CjTypeStatement {
    private val _stub: CangJieExtendStub?
        get() = stub as? CangJieExtendStub

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitExtend(this, data)
    }

    override val typeName: String
        get() = "extend"

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieExtendStub) : super(stub, CjStubElementTypes.EXTEND)

//    override val fqName: FqName?
//        get() = super.fqName

    override fun getName(): String? {
        // 优先从 stub 获取名称，避免触发 AST 加载
        _stub?.getName()?.let { return it }
        return getExtendName()
    }

    private fun getExtendName(): String? {
        return when (val type = receiverTypeReceiver?.typeElement) {
            is CjUserType -> {
                type.referencedName
            }

            is CjOptionType -> {
                  "Option"
            }

            is CjBasicType -> {
                type.name
            }

            else -> null
        }
    }

    override fun getNameIdentifier(): PsiElement? {
        return when (val type = receiverTypeReceiver?.typeElement) {
            is CjUserType -> type.referenceExpression?.identifier
            is CjBasicType -> type  // 基本类型本身就是标识符
            is CjOptionType -> type  // Option 类型本身
            else -> null
        }
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

    /**
     * 生成扩展的唯一标识符
     *
     * 遵循编译器的 name mangling 策略，格式：
     * ```
     * packageName:ExtendedType<:Interface1&Interface2&...
     * ```
     *
     * 关键点：
     * 1. 使用包名前缀区分不同包中的同名扩展
     * 2. 使用类型的规范化文本表示（而非简单名）
     * 3. 接口列表按字典序排序，确保一致性
     * 4. 不包含源码位置信息（textOffset、textRange、text），避免格式化导致 ID 变化
     *
     * 对应编译器实现: ASTMangler.cpp::MangleExtendDecl
     *
     * @return 扩展的唯一标识符
     */
    fun getExtendId(): String {
        val parts = mutableListOf<String>()

        // 1. 包名前缀（用于区分不同包中的同名扩展）
        val packageName = fqName?.asString() ?: ""
        parts.add(packageName)
        parts.add(":")

        // 2. 被扩展类型的规范化表示
        val extendedTypeName = receiverTypeReceiver?.text ?: "Unknown"
        parts.add(extendedTypeName)

        // 3. 分隔符（对应编译器的 MANGLE_LT_COLON_PREFIX）
        parts.add("<:")

        // 4. 排序的接口列表（用 & 连接）
        val interfaces = getSortedSupernames()
        parts.add(interfaces)

        // 注意：暂不包含泛型约束，因为当前 PSI 层不易获取完整的约束信息
        // 如需支持，可在后续从 descriptor 层获取

        return parts.joinToString("")
    }

    /**
     * 获取排序后的接口名称列表
     *
     * 对应编译器实现中的接口排序逻辑：
     * ```cpp
     * std::stable_sort(inherits.begin(), inherits.end());
     * ```
     *
     * @return 排序后的接口名称，用 & 连接
     */
    private fun getSortedSupernames(): String {
        val list = findChildByType<CjSuperTypeList>(CjNodeTypes.SUPER_TYPE_LIST)
            ?: return ""

        val names = list.getChildrenOfType<CjSuperTypeEntry>()
            .map { it.children[0].text }
            .sorted()  // 按字典序排序，确保一致性

        return names.joinToString("&")
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
