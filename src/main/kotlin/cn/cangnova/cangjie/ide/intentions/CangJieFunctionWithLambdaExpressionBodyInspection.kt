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

package cn.cangnova.cangjie.ide.intentions

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import cn.cangnova.cangjie.CangJieBundle
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.ide.codeinsight.inspections.AbstractCangJieInspection
import cn.cangnova.cangjie.ide.quickfix.SpecifyTypeExplicitlyFix
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.*
import cn.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import cn.cangnova.cangjie.types.util.isNothing


class CangJieFunctionWithLambdaExpressionBodyInspection : AbstractCangJieInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : CjVisitorVoid() {
        override fun visitNamedFunction(function: CjNamedFunction) {
            check(function)
        }

//        override fun visitPropertyAccessor(accessor: CjPropertyAccessor) {
//            if (accessor.isSetter) return
//            if (accessor.returnTypeReference != null) return
//            check(accessor)
//        }

        private fun check(element: CjDeclarationWithBody) {
            val callableDeclaration = element.getNonStrictParentOfType<CjCallableDeclaration>() ?: return
            if (callableDeclaration.typeReference != null) return
//            val lambda = element.bodyExpression as? CjLambdaExpression ?: return
//            val functionLiteral = lambda.functionLiteral
//            if (functionLiteral.arrow != null || functionLiteral.valueParameterList != null) return
//            val lambdaBody = functionLiteral.bodyBlockExpression ?: return

//            val used = ReferencesSearch.search(callableDeclaration).any()
            val fixes :List<IntentionWrapper > = listOfNotNull(
                IntentionWrapper(SpecifyTypeExplicitlyFix()),
//                IntentionWrapper(AddArrowIntention()),
//                if (!used &&
//                    lambdaBody.statements.size == 1 &&
//                    lambdaBody.allChildren.none { it is PsiComment }
//                )
//                    RemoveBracesFix()
//                else
//                    null,
//                if (!used) WrapRunFix() else null
            )
            val block = element.bodyBlockExpression ?: return
            holder.registerProblem(
                block,
                CangJieBundle.message("inspection.function.with.lambda.expression.body.display.name"),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                *fixes.toTypedArray()
            )
        }
    }

    private class AddArrowIntention : SpecifyExplicitLambdaSignatureIntention() {
        override fun skipProcessingFurtherElementsAfter(element: PsiElement): Boolean = false
    }

    private class RemoveBracesFix : LocalQuickFix {
        override fun getName() = CangJieBundle.message("remove.braces.fix.text")

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val lambda = descriptor.psiElement as? CjLambdaExpression ?: return
            val singleStatement = lambda.functionLiteral.bodyExpression?.statements?.singleOrNull() ?: return
            val replaced = lambda.replaced(singleStatement)
            replaced.setTypeIfNeed()
        }
    }

    private class WrapRunFix : LocalQuickFix {
        override fun getName() = CangJieBundle.message("wrap.run.fix.text")

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val lambda = descriptor.psiElement as? CjLambdaExpression ?: return
            val body = lambda.functionLiteral.bodyExpression ?: return
            val replaced = lambda.replaced(CjPsiFactory(project).createExpressionByPattern("run { $0 }", body.allChildren))
            replaced.setTypeIfNeed()
        }
    }
}

private fun CjExpression.setTypeIfNeed() {
    val declaration = getStrictParentOfType<CjCallableDeclaration>() ?: return
    val type = (declaration.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType
    if (type?.isNothing() == true) {
        declaration.setType(type)
    }
}
