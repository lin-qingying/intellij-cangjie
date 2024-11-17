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

package com.linqingying.cangjie.psi

import com.linqingying.cangjie.name.FqName
import com.intellij.lang.ASTNode

class CjMultiImportDirective(node: ASTNode) : CjExpressionImpl(node), CjStatementExpression {

    val fqName: FqName?
        get() {
//            val expr = this.findChildByClass(CjNameReferenceExpressionElementType::class.java) ?: this.findChildByClass(
//                DOT_QUALIFIED_EXPRESSION::class.java
//            )
//
//            if (expr != null) {
//                expr as CjExpression
//
//                return CjImportDirective.fqNameFromExpression(expr)
//            }
//            return null

           if(this.children.isEmpty()){
               return null
           }
            return CjImportDirective.fqNameFromExpression(this.children[0] as? CjExpression)
        }
//
//    @IfNotParsed
//    fun getImportedReference(): CjExpression? {
//        val references: Array<CjExpression> =
//            getStubOrPsiChildren<CjExpression>(CjTokenSets.INSIDE_DIRECTIVE_EXPRESSIONS, CjExpression.ARRAY_FACTORY)
//        if (references.size > 0) {
//            return references[0]
//        }
//        return null
//    }
}

class CjMultiImportDirective1(node: ASTNode) : CjExpressionImpl(node), CjStatementExpression
