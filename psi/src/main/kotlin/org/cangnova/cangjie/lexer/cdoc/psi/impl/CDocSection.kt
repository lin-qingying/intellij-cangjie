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

package org.cangnova.cangjie.lexer.cdoc.psi.impl

import org.cangnova.cangjie.psi.psiUtil.getChildrenOfType
import com.intellij.lang.ASTNode

/**
 *文档注释中描述单个类、方法或属性的部分由被记录的元素产生。例如，类的文档注释可以有类本身、其主构造函数和每个在主构造函数中定义的属性
 */
class CDocSection(node: ASTNode) : CDocTag(node) {
    /**
     *返回节的名称(引导节的文档标签的名称或对于默认部分为NULL)
     */
    override fun getName(): String? =
        (firstChild as? CDocTag)?.name

    override fun getSubjectName(): String? =
        (firstChild as? CDocTag)?.getSubjectName()

    override fun getContent(): String =
        (firstChild as? CDocTag)?.getContent() ?: super.getContent()

    fun findTagsByName(name: String): List<CDocTag> {
        return getChildrenOfType<CDocTag>().filter { it.name == name }
    }

    fun findTagByName(name: String): CDocTag? = findTagsByName(name).firstOrNull()
}
