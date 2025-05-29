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

package cn.cangnova.cangjie.highlighter

import cn.cangnova.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import cn.cangnova.cangjie.lang.CangJieLanguage
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.cdoc.parser.CDocKnownTag
import cn.cangnova.cangjie.psi.cdoc.psi.impl.CDocLink
import cn.cangnova.cangjie.psi.cdoc.psi.impl.CDocTag
import com.intellij.codeHighlighting.RainbowHighlighter
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.psi.PsiElement

/**
 * 语法高亮访问器 - 在符号解析之前进行高亮处理
 * Syntax Highlighting Visitor - Handles highlighting before symbol resolution
 *
 * 这个访问器负责在符号解析阶段之前对代码元素进行初步的语法高亮处理。主要功能包括：
 * This visitor is responsible for initial syntax highlighting of code elements before the symbol resolution phase. Main features include:
 *
 * 1. 处理文档链接高亮 (Handles documentation link highlighting)
 * 2. 处理软关键字高亮 (Processes soft keyword highlighting)
 * 3. 处理标签和表达式高亮 (Handles label and expression highlighting)
 * 4. 处理类型参数高亮 (Processes type parameter highlighting)
 * 5. 处理函数声明高亮 (Handles function declaration highlighting)
 *
 * 该访问器作为语法高亮系统的第一道处理环节，主要关注那些不需要符号解析就能确定的语法元素。
 * This visitor serves as the first stage of the syntax highlighting system, focusing on syntactic elements
 * that can be determined without symbol resolution.
 */

class BeforeResolveHighlightingVisitor(holder: HighlightInfoHolder) : AbstractHighlightingVisitor(holder) {
    override fun visitElement(element: PsiElement) {
        val elementType = element.node.elementType
        val attributes = when {
            element is CDocLink && !willApplyRainbowHighlight(element) -> CangJieHighlightInfoTypeSemanticNames.CDOC_LINK

            elementType in CjTokens.SOFT_KEYWORDS -> {
                when (elementType) {
//                    in CjTokens.MODIFIER_KEYWORDS -> CangJieHighlightInfoTypeSemanticNames.BUILTIN_ANNOTATION
//                    in CjTokens.BASICTYPES -> CangJieHighlightInfoTypeSemanticNames.BUILTIN_ANNOTATION
                    else -> CangJieHighlightInfoTypeSemanticNames.KEYWORD
                }
            }

            else -> return
        }

        highlightName(element, attributes)
    }

    private fun willApplyRainbowHighlight(element: CDocLink): Boolean {
        if (!RainbowHighlighter.isRainbowEnabledWithInheritance(
                EditorColorsManager.getInstance().globalScheme,
                CangJieLanguage
            )
        ) {
            return false
        }

        return (element.parent as? CDocTag)?.knownTag == CDocKnownTag.PARAM
    }


    override fun visitExpressionWithLabel(expression: CjExpressionWithLabel) {
        val targetLabel = expression.getTargetLabel()
        if (targetLabel != null) {
            highlightName(targetLabel, CangJieHighlightInfoTypeSemanticNames.LABEL)
        }
    }

    override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry) {
        val calleeExpression = call.calleeExpression
        val typeElement = calleeExpression.typeReference?.typeElement
        if (typeElement is CjUserType) {
            typeElement.referenceExpression?.let {
                highlightName(
                    it,
                    CangJieHighlightInfoTypeSemanticNames.CONSTRUCTOR_CALL
                )
            }
        }
        super.visitSuperTypeCallEntry(call)
    }


    override fun visitTypeParameter(parameter: CjTypeParameter) {
        parameter.nameIdentifier?.let { highlightName(it, CangJieHighlightInfoTypeSemanticNames.TYPE_PARAMETER) }
        super.visitTypeParameter(parameter)
    }

    override fun visitNamedFunction(function: CjNamedFunction) {
        highlightNamedDeclaration(function, CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION)
        super.visitNamedFunction(function)
    }


    override fun visitEnum(cenum: CjEnum) {
        highlightNamedDeclaration(cenum, CangJieHighlightInfoTypeSemanticNames.ENUM)
        super.visitEnum(cenum)
    }

    override fun visitEnumEntry(enumEntry: CjEnumEntry) {
        highlightNamedDeclaration(enumEntry, CangJieHighlightInfoTypeSemanticNames.ENUM_ENTRY)

        super.visitEnumEntry(enumEntry)
    }

    override fun visitVariable(variable: CjVariable) {
        if (variable.isVar) {
            highlightNamedDeclaration(variable, CangJieHighlightInfoTypeSemanticNames.MUTABLE_VARIABLE)
        } else {
            highlightNamedDeclaration(variable, CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE)
        }
        super.visitVariable(variable)
    }

    override fun visitProperty(property: CjProperty) {
        if (property.isVar) {
            highlightNamedDeclaration(property, CangJieHighlightInfoTypeSemanticNames.MUTABLE_PROPERTY)
        } else {
            highlightNamedDeclaration(property, CangJieHighlightInfoTypeSemanticNames.PROPERTY)
        }
        super.visitProperty(property)
    }

    override fun visitTypeReference(typeReference: CjTypeReference) {

        val typeElement = typeReference.typeElement
        if (typeElement is CjUserType) {
            typeElement.referenceExpression?.let {

                highlightName(
                    it,
                    CangJieHighlightInfoTypeSemanticNames.TYPE_REFERENCE
                )

            }
        }
        super.visitTypeReference(typeReference)
    }

    override fun visitTypeAlias(typeAlias: CjTypeAlias) {
        highlightNamedDeclaration(typeAlias, CangJieHighlightInfoTypeSemanticNames.TYPE_ALIAS)

        super.visitTypeAlias(typeAlias)
    }

    override fun visitClass(klass: CjClass) {
        highlightNamedDeclaration(klass, CangJieHighlightInfoTypeSemanticNames.CLASS)
        super.visitClass(klass)
    }

    override fun visitStruct(cstruct: CjStruct) {
        highlightNamedDeclaration(cstruct, CangJieHighlightInfoTypeSemanticNames.STRING)
        super.visitStruct(cstruct)
    }

    override fun visitInterface(cinterface: CjInterface) {
        highlightNamedDeclaration(cinterface, CangJieHighlightInfoTypeSemanticNames.INTERFACE)
        super.visitInterface(cinterface)
    }
}
