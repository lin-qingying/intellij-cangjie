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

package org.cangnova.cangjie.ide.refactoring

import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjTreeVisitorVoid
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import kotlin.math.min


internal const val ellipsis = "${Typography.ellipsis}"

@NlsSafe

fun getExpressionShortText(element: PsiElement): String {
    val text = ((element as? CjElement)?.renderTrimmed() ?: element.text).trimStart()
    val firstNewLinePos = text.indexOf('\n')
    var trimmedText = text.substring(0, if (firstNewLinePos != -1) firstNewLinePos else min(100, text.length))
    if (trimmedText.length != text.length) trimmedText += " $ellipsis"
    return trimmedText
}

fun CjElement.renderTrimmed(): String {
    class Renderer : CjTreeVisitorVoid() {
        val builder = StringBuilder()

        fun render(element: CjElement): String {
            element.accept(this)
            return builder.toString()
        }

        private fun <T : PsiElement> Iterable<T>.join(
            separator: CharSequence = ", ",
            prefix: CharSequence = "",
            postfix: CharSequence = ""
        ) {
            builder.append(prefix)
            for ((count, element) in withIndex()) {
                if (count > 0) builder.append(separator)
                element.accept(this@Renderer)
            }
            builder.append(postfix)
        }

        // Whitespace and comments

        override fun visitWhiteSpace(space: PsiWhiteSpace) {
            val text = space.text
            val newLine = text.indexOf('\n')
            if (newLine != 0) {
                builder.append(' ')
            }
            if (newLine >= 0) {
                builder.append(text.substring(newLine))
            }
        }

        override fun visitComment(comment: PsiComment) {

        }

        // Basic expressions

        override fun visitParenthesizedExpression(expression: CjParenthesizedExpression) {
            builder.append('(')
            expression.expression?.accept(this)
            builder.append(')')
        }

        override fun visitParameterList(list: CjParameterList) {
            list.parameters.join(prefix = "(", postfix = ")")
        }

        override fun visitParameter(parameter: CjParameter) {
            builder.append("${parameter.name}: ")
            parameter.typeReference?.accept(this)
        }


        override fun visitPrefixExpression(expression: CjPrefixExpression) {
            builder.append(expression.operationReference.referencedName)
            expression.baseExpression?.accept(this)
        }

        override fun visitPostfixExpression(expression: CjPostfixExpression) {
            expression.baseExpression?.accept(this)
            builder.append(expression.operationReference.referencedName)
        }

        override fun visitBinaryExpression(expression: CjBinaryExpression) {
            expression.left?.accept(this)
            builder.append(" ${expression.operationReference.referencedName} ")
            expression.right?.accept(this)
        }

        override fun visitBinaryWithTypeRHSExpression(expression: CjBinaryExpressionWithTypeRHS) {
            expression.left.accept(this)
            builder.append(" ${expression.operationReference.referencedName} ")
            expression.right?.accept(this)
        }

        override fun visitIsExpression(expression: CjIsExpression) {
            expression.leftHandSide.accept(this)
            builder.append(" is ")
            expression.typeReference?.accept(this)
        }

        override fun visitArrayAccessExpression(expression: CjArrayAccessExpression) {
            expression.arrayExpression?.accept(this)
            expression.indexExpressions.join(builder, prefix = "[", postfix = "]")
        }

        override fun visitCallExpression(expression: CjCallExpression) {
            expression.calleeExpression?.accept(this)
            expression.typeArgumentList?.accept(this)
            expression.valueArgumentList?.accept(this)
            repeat(expression.lambdaArguments.size) { builder.append("{${  ellipsis}}") }
        }

        override fun visitValueArgumentList(list: CjValueArgumentList) {
            val arguments = list.arguments
            builder.append("(")
            if (arguments.isNotEmpty()) {
                if (arguments.size <= 3 &&
                    arguments.all { it.getArgumentExpression() is CjConstantExpression } &&
                    arguments.sumOf { it.text.length } < 8
                ) {
                    arguments.joinTo(builder) { it.text }
                } else {
                    builder.append( ellipsis)
                }
            }
            builder.append(")")
        }

        override fun visitQualifiedExpression(expression: CjQualifiedExpression) {
            expression.receiverExpression.accept(this)
            builder.append(expression.operationTokenNode.text)
            expression.selectorExpression?.accept(this)
        }

        override fun visitTypeReference(typeReference: CjTypeReference) {
            builder.append(typeReference.text)
        }

        override fun visitThisExpression(expression: CjThisExpression) {
            builder.append("this")

        }

        override fun visitSuperExpression(expression: CjSuperExpression) {
            builder.append("super")


        }

        // Control structures

        override fun visitBreakExpression(expression: CjBreakExpression) {
            builder.append("break")

        }

        override fun visitContinueExpression(expression: CjContinueExpression) {
            builder.append("continue")

        }

        override fun visitThrowExpression(expression: CjThrowExpression) {
            builder.append("throw ")
            expression.thrownExpression?.accept(this)
        }

        override fun visitReturnExpression(expression: CjReturnExpression) {
            builder.append("return")
            expression.getLabelName()?.let { builder.append("@$it") }
            builder.append(' ')
            expression.returnedExpression?.accept(this)
        }

        override fun visitBlockExpression(expression: CjBlockExpression) {
            if (expression.parent is CjFunctionLiteral) {
                super.visitBlockExpression(expression)
            } else {
                builder.append("{${ ellipsis}}")
            }
        }

        override fun visitIfExpression(expression: CjIfExpression) {
            builder.append("if (")
            expression.condition?.accept(this)
            builder.append(")")
            expression.then?.let {
                builder.append(' ')
                it.accept(this)
            }
            expression.`else`?.let {
                builder.append(" else ")
                it.accept(this)
            }
        }



        override fun visitForExpression(expression: CjForExpression) {
            builder.append("for (")
            (expression.loopParameter ?: expression.destructuringDeclaration)?.accept(this)
            builder.append(" in ")
            expression.loopRange?.accept(this)
            builder.append(")")
            expression.body?.let {
                builder.append(' ')
                it.accept(this)
            }
        }

        override fun visitWhileExpression(expression: CjWhileExpression) {
            builder.append("while (")
            expression.condition?.accept(this)
            builder.append(")")
            expression.body?.let {
                builder.append(' ')
                it.accept(this)
            }
        }

        override fun visitDoWhileExpression(expression: CjDoWhileExpression) {
            builder.append("do")
            expression.body?.let {
                builder.append(' ')
                it.accept(this)
            }
            builder.append(" while (")
            expression.condition?.accept(this)
            builder.append(")")
        }

        override fun visitTryExpression(expression: CjTryExpression) {
            builder.append("try {${ ellipsis}}")
        }

        // Declarations

        override fun visitNamedFunction(function: CjNamedFunction) {
            builder.append("fun")
            function.receiverTypeReference?.let {
                builder.append('.')
                it.accept(this)
            }
            function.name?.let { builder.append(" $it") }
            function.valueParameterList?.accept(this)
            function.equalsToken?.let { builder.append(" = ") }
            function.bodyExpression?.accept(this)
        }



        override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor) {
            constructor.valueParameterList?.accept(this)
        }

        override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
            builder.append("constructor")
            constructor.valueParameterList?.accept(this)
            constructor.bodyExpression?.accept(this)
        }

        override fun visitTypeStatement(typeStatement: CjTypeStatement) {
            val keyword =  typeStatement.declarationKeyword
            keyword?.accept(this)

            typeStatement.name?.let { builder.append(" $it") }
            typeStatement.getSuperTypeList()?.accept(this)
            typeStatement.body?.let { builder.append(" {${ ellipsis}}") }

        }


        override fun visitSuperTypeList(list: CjSuperTypeList) {
            list.entries.ifEmpty { return }.join(builder, prefix = " : ")
        }



        override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry) {
            call.typeReference?.accept(this)
            call.valueArgumentList?.accept(this)
        }

        override fun visitSuperTypeEntry(specifier: CjSuperTypeEntry) {
            specifier.typeReference?.accept(this)
        }

        // Default

        override fun visitElement(element: PsiElement) {
            if (element is LeafPsiElement) {
                builder.append(element.text)
            } else {
                super.visitElement(element)
            }
        }
    }

    return Renderer().render(this)
}
