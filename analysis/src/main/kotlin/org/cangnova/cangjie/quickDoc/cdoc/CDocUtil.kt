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

package org.cangnova.cangjie.quickDoc.cdoc

import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.*
import org.cangnova.cangjie.utils.toLowerCaseAsciiOnly
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag

/**
 * 通过 PSI 查找 CDoc 文档注释。
 *
 * 该函数会按照以下优先级查找文档：
 * 1. 查找元素自己的 CDoc 注释（[lookupOwnedCDoc]）
 * 2. 如果没有自己的注释，则在容器中查找相关的 CDoc 标签（[lookupCDocInContainer]）
 *
 * ## 使用场景
 *
 * - 获取类、函数、属性等声明的文档
 * - 获取构造函数的 `@constructor` 标签文档
 * - 获取参数的 `@param` 标签文档
 * - 获取属性的 `@property` 标签文档
 *
 * @receiver CjElement 要查找文档的仓颉元素
 * @return CDocContent 文档内容，包含主要内容和所有相关章节；如果没有找到文档则返回 `null`
 *
 * @see CDocContent
 * @see lookupOwnedCDoc
 * @see lookupCDocInContainer
 */
fun CjElement.findCDocByPsi(): CDocContent? {
    return this.lookupOwnedCDoc()
        ?: this.lookupCDocInContainer()
}

/**
 * 查找元素自己拥有的 CDoc 文档注释。
 *
 * ## 特殊处理
 *
 * ### 主构造函数
 * 主构造函数的文档位于其所属类的 CDoc 中的 `@constructor` 标签下。
 * 如果找到 `@constructor` 标签，会同时返回相关的 `@param` 章节。
 *
 * ### 示例
 * ```kotlin
 * /**
 *  * 用户类
 *  * @constructor 创建用户实例
 *  * @param name 用户名
 *  * @param age 年龄
 *  */
 * class User(val name: String, val age: Int)
 * ```
 *
 * @receiver CjElement 要查找文档的仓颉元素
 * @return CDocContent 文档内容；如果该元素没有自己的文档则返回 `null`
 */
private fun CjElement.lookupOwnedCDoc(): CDocContent? {
    // CDoc for primary constructor is located inside of its class CDoc
    val psiDeclaration = when (this) {
        is CjPrimaryConstructor -> containingTypeStatement
        else -> this
    }

    if (psiDeclaration is CjDeclaration) {
        val cdoc = psiDeclaration.docComment
        if (cdoc != null) {
            if (this is CjConstructor<*>) {
                // ConstructorDescriptor resolves to the same JetDeclaration
                val constructorSection = cdoc.findSectionByTag(CDocKnownTag.CONSTRUCTOR)
                if (constructorSection != null) {
                    // if annotated with @constructor tag and the caret is on constructor definition,
                    // then show @constructor description as the main content, and additional sections
                    // that contain @param tags (if any), as the most relatable ones
                    // practical example: val foo = Fo<caret>o("argument") -- show @constructor and @param content
                    val paramSections = cdoc.findSectionsContainingTag(CDocKnownTag.PARAM)
                    return CDocContent(constructorSection, paramSections)
                }
            }
            return CDocContent(cdoc.getDefaultSection(), cdoc.getAllSections())
        }
    }
    return null
}

/**
 * 查找包含指定标签的所有章节。
 *
 * 与 [CDoc.findSectionByTag] 不同，该方法会深度搜索嵌套的标签，
 * 而不仅仅是查找顶层标签。
 *
 * @receiver CDoc 文档注释
 * @param tag 要查找的标签类型
 * @return 包含指定标签的所有章节列表
 */
private fun CDoc.findSectionsContainingTag(tag: CDocKnownTag): List<CDocSection> {
    return getChildrenOfType<CDocSection>()
        .filter { it.findTagByName(tag.name.toLowerCaseAsciiOnly()) != null }
}

/**
 * 在容器中查找相关的 CDoc 标签。
 *
 * 当元素本身没有文档注释时，会在包含它的容器（类或函数）中查找相关标签：
 *
 * ## 查找规则
 *
 * ### 属性参数
 * ```kotlin
 * /**
 *  * @property name 用户名
 *  * @param age 年龄
 *  */
 * class User(val name: String, age: Int)
 * ```
 * 对于 `name`，会查找 `@property` 标签；如果没找到，则查找 `@param` 标签。
 *
 * ### 普通参数
 * ```kotlin
 * /**
 *  * @param x 横坐标
 *  * @param y 纵坐标
 *  */
 * fun move(x: Int, y: Int)
 * ```
 * 会查找对应的 `@param` 标签。
 *
 * ### 类型参数
 * ```kotlin
 * /**
 *  * @param T 元素类型
 *  */
 * class List<T>
 * ```
 * 会查找对应的 `@param` 标签。
 *
 * ### 独立声明的属性
 * ```kotlin
 * /**
 *  * @property count 计数器
 *  */
 * class Counter {
 *     var count: Int = 0
 * }
 * ```
 * 会查找对应的 `@property` 标签。
 *
 * @receiver CjElement 要查找文档的仓颉元素
 * @return CDocContent 找到的文档内容；如果没有找到相关标签则返回 `null`
 */
private fun CjElement.lookupCDocInContainer(): CDocContent? {
    val subjectName = name
    val containingDeclaration =
        PsiTreeUtil.findFirstParent(this, true) {
            it is CjDeclarationWithBody && it !is CjPrimaryConstructor
                    || it is CjTypeStatement
        }

    val containerCDoc = containingDeclaration?.getChildOfType<CDoc>()
    if (containerCDoc == null || subjectName == null) return null
    val propertySection = containerCDoc.findSectionByTag(CDocKnownTag.PROPERTY, subjectName)
    val paramTag = containerCDoc.findDescendantOfType<CDocTag> { it.knownTag == CDocKnownTag.PARAM && it.getSubjectName() == subjectName }

    val primaryContent = when {
        // class Foo(val <caret>s: String)
        this is CjParameter && this.isPropertyParameter() -> propertySection ?: paramTag
        // fun some(<caret>f: String) || class Some<<caret>T: Base> || Foo(<caret>s = "argument")
        this is CjParameter || this is CjTypeParameter -> paramTag
        // if this property is declared separately (outside primary constructor), but it's for some reason
        // annotated as @property in class's description, instead of having its own CDoc
        this is CjProperty && containingDeclaration is CjTypeStatement -> propertySection
        else -> null
    }
    return primaryContent?.let {
        // makes little sense to include any other sections, since we found
        // documentation for a very specific element, like a property/param
        CDocContent(it, sections = emptyList())
    }
}
