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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.psiUtil.deleteSemicolon
import org.cangnova.cangjie.psi.psiUtil.parentSubstitute
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference

val Null: Any? = null

/**
 * 基础CangJie语言PSI元素接口
 *
 * 所有CangJie语言的PSI元素都应该实现这个接口，它提供了访问者模式支持
 * 和基本的导航功能。
 */
interface CjElement : NavigatablePsiElement, CjPureElement {

    /**
     * 让所有子元素接受访问者的访问
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的数据
     */
    fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D)

    /**
     * 接受访问者的访问
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的数据
     * @return 访问者返回的结果
     */
    fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R?

    /**
     * 获取引用
     *
     * 不建议在CjElement上直接使用getReference()，因为选择是不可预测的。
     * 应该使用getReferences()获取所有引用，或者使用更具体的方法。
     *
     * @return 此元素的引用，如果有多个引用则返回第一个，如果没有则返回null
     */
    @Deprecated("Don't use getReference() on CjElement for the choice is unpredictable")
    override fun getReference(): PsiReference?
}

/**
 * CjElement接口的基本实现
 *
 * 这个类提供了CjElement接口的标准实现，包括访问者模式支持、
 * 引用处理和父元素解析等功能。大多数CangJie语言的具体PSI元素
 * 都应该继承这个类。
 *
 * @param node AST节点
 */
open class CjElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), CjElement {

    /**
     * 返回元素的字符串表示
     *
     * @return 元素类型的字符串表示
     */
    override fun toString(): String = node.elementType.toString()

    /**
     * 让所有子元素接受访问者的访问
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的数据
     */
    override fun <D> acceptChildren(visitor: CjVisitor<Unit, D>, data: D) {
        CjPsiUtil.visitChildren(this, visitor, data)
    }

    /**
     * 接受PSI元素访问者的访问
     *
     * 如果访问者是CjVisitor，则调用专门的accept方法，
     * 否则调用通用的visitElement方法。
     *
     * @param visitor PSI元素访问者
     */
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is CjVisitor<*, *>) {
            @Suppress("UNCHECKED_CAST")
            accept(visitor as CjVisitor<Any?, Any?>, null as Any?)
        } else {
            visitor.visitElement(this)
        }
    }

    /**
     * 接受CjVisitor的访问
     *
     * @param visitor 要接受的访问者
     * @param data 传递给访问者的数据
     * @return 访问者返回的结果
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitCjElement(this, data)

    /**
     * 获取PSI元素或其父元素
     *
     * @return 当前元素
     */
    override fun getPsiOrParent(): CjElement = this

    /**
     * 获取包含此元素的CjFile
     *
     * @return 包含此元素的CjFile
     * @throws IllegalStateException 如果元素不在CjFile内
     */
    override fun getContainingCjFile(): CjFile {
        val file = containingFile
        if (file !is CjFile) {
            val fileString = if (file != null && file.isValid) " " + file.text else ""
            throw IllegalStateException(
                "CjElement not inside CjFile: " + file + fileString +
                        " for element " + this + " of type " + this.javaClass + " node = " + node,
            )
        }
        return file
    }

    /**
     * 删除此元素
     *
     * 在删除元素之前，尝试删除与之关联的分号
     */
    override fun delete() {
        this.deleteSemicolon()
        super.delete()
    }

    /**
     * 获取此元素的引用
     *
     * 如果元素有多个引用，返回第一个；如果没有引用，返回null
     *
     * @return 此元素的引用
     */
    override fun getReference(): PsiReference? {
        val references = references
        return if (references.size == 1) references[0] else null
    }

    /**
     * 获取此元素的所有引用
     *
     * @return 此元素的所有引用数组
     */
    override fun getReferences(): Array<PsiReference> {
//        if(this is CjBasicType) return emptyArray()

        return CangJieReferenceProvidersService.getReferencesFromProviders(this)
    }

    /**
     * 获取此元素的父元素
     *
     * 首先检查是否有父元素替代品，如果有则返回替代品，
     * 否则返回默认的父元素
     *
     * @return 父元素
     */
    override fun getParent(): PsiElement? {
        val substitute: PsiElement? = this.parentSubstitute
        return substitute ?: super.getParent()
    }

    /**
     * 获取此元素的语言
     *
     * @return CangJie语言实例
     */
    override fun getLanguage(): Language = CangJieLanguage
}

// fun CjElement.findInScope(name: String, ns: Set<Namespace>): PsiElement? {
//    return pickFirstResolveVariant(name) {
//        processNestedScopesUpwards(this, ns, it)
//    }
// }
