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

package com.linqingying.cangjie.ide.quickfix

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.intentions.SpecifyTypeExplicitlyIntention
import com.linqingying.cangjie.psi.CjCallableDeclaration
import com.linqingying.cangjie.psi.CjNamedFunction
import com.linqingying.cangjie.psi.CjProperty
import com.linqingying.cangjie.psi.CjVariable
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.CangJieTypeFactory
import com.linqingying.cangjie.types.asSimpleType
import com.linqingying.cangjie.types.isError

/**
 * 提供一个意图操作，用于在代码中明确指定类型
 * 这个类允许用户在IDE中选择是否将类型转换为可空类型
 *
 * @param convertToNullable 一个布尔值，指示是否将类型转换为可空类型，默认为false
 */
class SpecifyTypeExplicitlyFix(private val convertToNullable: Boolean = false) : PsiElementBaseIntentionAction() {
    /**
     * 返回意图操作的家族名称，用于在IDE的意图操作列表中分类显示
     *
     * @return 意图操作的家族名称
     */
    override fun getFamilyName() = CangJieBundle.message("specify.type.explicitly")

    /**
     * 执行意图操作的主要逻辑
     * 根据给定的元素查找相应的声明，并为其添加明确的类型注解
     *
     * @param project 当前的项目
     * @param editor 代码编辑器
     * @param element 触发意图操作的代码元素
     */
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        // 获取给定元素对应的声明
        val declaration = declarationByElement(element)!!
        // 根据声明获取适当的类型，并根据convertToNullable参数决定是否将其转换为可空类型
        val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(declaration)
            .let { if (convertToNullable) it.convertToNullable() else it }
        // 为声明添加类型注解
        SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, declaration, type)
    }

    /**
     * 检查意图操作是否适用于给定的代码元素
     * 如果元素对应的声明已经有一个类型引用，则返回false，表示该操作不适用
     * 否则，设置意图操作的文本描述，并返回true，表示该操作适用
     *
     * @param project 当前的项目
     * @param editor 代码编辑器
     * @param element 要检查的代码元素
     * @return 如果意图操作适用于给定的元素，则返回true；否则返回false
     */
    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val declaration = declarationByElement(element)
        if (declaration?.typeReference != null) return false
        text = when (declaration) {

            is CjProperty,is CjVariable -> CangJieBundle.message("specify.type.explicitly")

            is CjNamedFunction -> CangJieBundle.message("specify.return.type.explicitly")
            else -> return false
        }
        // 检查是否有错误类型
        return !SpecifyTypeExplicitlyIntention.getTypeForDeclaration(declaration).isError
    }

    /**
     * 辅助函数，用于获取给定元素对应的声明
     * 这个函数通过解析元素的父类型来找到最近的属性或函数声明
     *
     * @param element 要查找的代码元素
     * @return 对应的声明，如果没有找到则返回null
     */
    private fun declarationByElement(element: PsiElement): CjCallableDeclaration? {
        // 使用PsiTreeUtil工具类查找最近的父类型为CjProperty或CjNamedFunction的元素
        return PsiTreeUtil.getParentOfType(element, CjProperty::class.java, CjNamedFunction::class.java)
    }
}

/**
 * 将给定的类型转换为可空类型
 * 这个函数通过CangJieTypeFactory创建一个新的简单类型，其可空性设置为true
 *
 * @return 转换为可空类型的CangJieType实例
 */
fun CangJieType.convertToNullable(): CangJieType = CangJieTypeFactory.simpleType(asSimpleType(), nullable = true)
