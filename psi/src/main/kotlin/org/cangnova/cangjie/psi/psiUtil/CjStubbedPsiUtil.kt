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

package org.cangnova.cangjie.psi.psiUtil

import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjElementImplStub
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

object CjStubbedPsiUtil {
    /**
     * 获取包含给定元素的声明
     *
     * 此函数旨在检索包含指定PsiElement的CjDeclaration对象PsiElement是JetBrains PSI树中的一个元素，
     * 代表代码中的一个结构化部分这个函数特别有用，因为它帮助我们找到PsiElement所属的直接声明，
     * 这在解析代码结构、进行重构或分析代码时非常重要
     *
     * @param element 要查找其包含声明的PsiElement对象
     * @return 包含给定元素的CjDeclaration对象，如果找不到则返回null
     */

    fun getContainingDeclaration(element: PsiElement): CjDeclaration? {
        // 使用自定义方法getPsiOrStubParent来获取父声明，具体逻辑在该方法中实现
        return getPsiOrStubParent(element, CjDeclaration::class.java, true)
    }

    /**
     * 获取包含指定元素的声明。
     * 该方法用于在给定的PsiElement元素中查找其包含的特定类型的声明。
     * 这里的声明是指代码中的命名程序实体，如类、方法或属性等。
     *
     * @param element 要检查的PsiElement元素，它可以是任何 PSI 元素，如标识符、类型引用等。
     * @param declarationClass 要查找的声明的类，用于指定所需声明的类型。
     * @param <T> 泛型参数，表示声明的类型，限定为CjDeclaration的子类型。
     * @return 返回找到的声明对象，如果未找到则返回null。
     */

    fun <T : CjDeclaration> getContainingDeclaration(element: PsiElement, declarationClass: Class<T>): T? {
        return getPsiOrStubParent(element, declarationClass, true)
    }

    /**
     * 获取给定元素的Psi或Stub父元素
     * 该函数旨在帮助查找特定类型的父元素，无论是实际的Psi元素还是Stub元素
     *
     * @param element 要查找父元素的Psi元素
     * @param declarationClass 要查找的父元素的类型，必须是CjElement的子类
     * @param strict 指定是否严格匹配父元素类型如果为false，且给定元素正是所查找的类型，则返回该元素
     * @return 找到的指定类型的父元素，如果没有找到，则返回null
     */

    fun <T : CjElement> getPsiOrStubParent(
        element: PsiElement,
        declarationClass: Class<T>,
        strict: Boolean,
    ): T? {
        // 如果不严格匹配且当前元素即为目标类型，则直接返回当前元素
        if (!strict && declarationClass.isInstance(element)) {
            return element as T
        }
        // 如果元素是Stub元素，尝试从Stub树中查找父元素
        if (element is CjElementImplStub<*>) {
            val stub = element.stub
            if (stub != null) {
                return stub.getParentStubOfType(declarationClass)
            }
        }
        // 如果上述条件都不满足，使用PsiTreeUtil在Psi树中查找父元素
        return PsiTreeUtil.getParentOfType(element, declarationClass, strict)
    }

    /**
     * 获取指定类型的子元素或其对应的stub
     *
     * 此函数旨在处理CjElement类型的元素，尝试获取其子元素或stub子元素
     * 如果找到了匹配类型的子元素，则返回第一个匹配的子元素；如果没有找到，则返回null
     *
     * @param element CjElementImplStub类型的元素，作为查找的父元素
     * @param types TokenSet，指定需要查找的子元素类型集合
     * @param factory ArrayFactory，用于创建指定类型数组的工厂
     * @return T? 返回第一个匹配类型的子元素或null，如果未找到匹配的子元素
     */

    fun <T : CjElement> getStubOrPsiChild(
        element: CjElementImplStub<*>,
        types: TokenSet,
        factory: ArrayFactory<T?>,
    ): T? {
        // 获取所有匹配指定类型的子元素或stub子元素
        val typeElements = element.getStubOrPsiChildren(types, factory)
        // 如果结果集为空，则直接返回null
        if (typeElements.isEmpty()) {
            return null
        }
        // 返回第一个匹配类型的子元素
        return typeElements[0]
    }
}
