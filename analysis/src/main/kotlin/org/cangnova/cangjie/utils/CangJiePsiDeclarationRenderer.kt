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

package org.cangnova.cangjie.utils

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.*

/**
 * 仓颉 PSI 声明渲染器
 *
 * 将仓颉语言的 PSI 声明节点渲染为可读的字符串表示。
 * 主要用于以下场景：
 * - IDE 的悬浮提示（hover tooltips）
 * - 代码补全的展示文本
 * - 快速文档（quick documentation）
 * - 错误消息中的声明展示
 *
 * 支持的声明类型：
 * - 类型声明（类、接口）
 * - 属性声明（var、let）
 * - 函数声明（包括运算符函数）
 * - 构造器声明
 * - 类型参数
 * - 函数参数
 */
object CangJiePsiDeclarationRenderer {
    /**
     * 将 PSI 声明渲染为字符串
     *
     * 根据不同的声明类型，生成相应的字符串表示。
     *
     * 渲染示例：
     * - 类：`class Foo<T> : Bar`
     * - 接口：`interface Comparable<T>`
     * - 属性：`var name: String`、`let count: Int`
     * - 函数：`fun add(a: Int, b: Int): Int`
     * - 运算符函数：`operator fun +(other: Int): Int`
     * - 构造器：`constructor Foo(name: String)`
     *
     * @param declaration PSI 声明节点
     * @return String? 渲染后的字符串，如果无法渲染则返回 null
     */
    fun render(declaration: CjDeclaration): String? =
        when (declaration) {
            // 渲染类型声明（类、接口等）
            is CjTypeStatement ->
                buildString {
                    // 注释：仓颉暂未支持注解类，相关代码已注释
//                    if (declaration.isAnnotation()) {
//                        append("annotation ")
//                    }
                    if (declaration.isInterface()) {
                        append("interface")
                    }
                    else {
                        append("class")
                    }

                    append(" ")
                    append(declaration.name)

                    // 渲染类型参数列表：<T, U>
                    declaration.typeParameterList?.parameters?.let {
                        append("<")
                        for ((index, cjTypeParameter) in it.withIndex()) {
                            if (index != 0) append(", ")
                            append(cjTypeParameter.name)
                        }
                        append(">")
                    }

                    // 渲染继承的超类或接口
                    val superTypeListEntries = declaration.superTypeListEntries
                    val superClass = superTypeListEntries.filterIsInstance<CjSuperTypeCallEntry>().firstOrNull()
                    if (superClass != null) {
                        superClass.calleeExpression.constructorReferenceExpression?.referencedName?.let {
                            append(" : ")
                            append(it)
                        }
                    }
                    else if (superTypeListEntries.isNotEmpty()) {
                        superTypeListEntries.first().typeReference?.referenceName()?.let {
                            append(" : ")
                            append(it)
                        }
                    }
                }

            // 渲染属性声明
            is CjProperty ->
                buildString {
                    if (declaration.isVar) {
                        append("var")
                    }
                    else {
                        append("val")
                    }
                    append(" ")


                    append(declaration.name)

                    // 渲染属性类型
                    declaration.typeReference?.let {
                        append(": ")
                        append(it.referenceName())
                    }
                }

            // 渲染类型参数
            is CjTypeParameter -> buildString {
                append("<")
                append(declaration.name)
                append(">")
            }

            // 渲染函数参数
            is CjParameter -> buildString {
                appendCjParameter(declaration)
            }

            // 渲染构造器
            is CjConstructor<*> -> buildString {
                append("constructor")
                append(" ")
                append(declaration.name ?: ("`" + SpecialNames.NO_NAME_PROVIDED.asString() + "`"))
                appendValueParameters(declaration)
            }

            // 渲染函数声明
            is CjNamedFunction -> buildString {

                // 如果是运算符函数，添加 operator 关键字
                if (declaration.hasModifier(CjTokens.OPERATOR_KEYWORD)) {
                    append("operator")
                    append(" ")
                }
                append("fun")
                append(" ")

                // 如果有函数名前的类型参数，渲染类型参数列表
                if (declaration.hasTypeParameterListBeforeFunctionName()) {
                    append("<")
                    for ((index, cjTypeParameter) in declaration.typeParameters.withIndex()) {
                        if (index != 0) append(", ")
                        append(cjTypeParameter.name)
                    }
                    append(">")
                    append(" ")
                }



                append(declaration.name)
                appendValueParameters(declaration)

                // 渲染返回类型
                val typeReference = declaration.typeReference
                if (typeReference != null) {
                    append(": ")
                    append(typeReference.referenceName())
                } else if (declaration.hasBlockBody()) {
                    // 如果函数有代码块体但没有显式返回类型，默认为 Unit
                    append(": ")
                    append(StandardNames.FqNames.unitUFqName.shortName())
                }
            }
            else -> null
        }

    /**
     * 提取类型引用的名称字符串
     *
     * 处理以下类型格式：
     * - 用户类型：`Foo`
     * - 可选类型：`Foo?`
     * - 泛型类型：`List<String>`
     * - 嵌套泛型：`Map<String, List<Int>>`
     *
     * @receiver CjTypeReference 类型引用 PSI 节点
     * @return String? 类型名称字符串，如果无法提取则返回 null
     */
    private fun CjTypeReference.referenceName(): String? {
        val type = typeElement as? CjUserType ?: (typeElement as? CjOptionType)?.getInnerType() as? CjUserType ?: return null
        return buildString {
            append(type.referencedName)

            // 如果是可选类型，添加 ? 后缀
            if (typeElement is CjOptionType) {
                append("?")
            }

            // 渲染类型参数列表
            type.typeArgumentList?.arguments?.let {
                append("<")
                for ((index: Int, typeProjection: CjTypeProjection) in it.withIndex()) {
                    if (index != 0) append(", ")
                    when(typeProjection.projectionKind) {
                        // 仓颉语言的类型投影（暂未实现特殊处理）
                        else -> {}
                    }
                    append(typeProjection.typeReference?.referenceName() ?: "??")
                }
                append(">")
            }
        }
    }

    /**
     * 渲染单个函数参数
     *
     * 格式：
     * - 普通参数：`name: Type`
     * - 可变参数：`vararg name: Type`
     * - 带默认值：`name: Type = ...`
     * - 仅类型（withName=false）：`Type`
     *
     * @receiver StringBuilder 字符串构建器
     * @param cjParameter 参数 PSI 节点
     * @param withName 是否包含参数名称，默认为 true
     */
    private fun StringBuilder.appendCjParameter(cjParameter: CjParameter, withName: Boolean = true) {
        if (cjParameter.isVarArg) append("vararg ")
        if (withName) {
            append(cjParameter.name)
            append(": ")
        }
        append(cjParameter.typeReference?.referenceName() ?: "??")
        if (cjParameter.defaultValue != null) {
            append(" = ...")
        }
    }

    /**
     * 渲染函数或构造器的值参数列表
     *
     * 格式：`(Type1, Type2, Type3)`
     * 注意：为了简洁，这里不渲染参数名称（withName=false）
     *
     * @receiver StringBuilder 字符串构建器
     * @param declaration 可调用声明（函数或构造器）
     */
    private fun StringBuilder.appendValueParameters(declaration: CjCallableDeclaration) {
        append("(")
        for ((index, cjParameter: CjParameter) in declaration.valueParameters.withIndex()) {
            if (index != 0) append(", ")
            appendCjParameter(cjParameter, withName = false)
        }
        append(")")
    }


}
