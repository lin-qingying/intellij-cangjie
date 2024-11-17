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

package com.linqingying.cangjie.ide.intentions

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.analyzer.analyzeAsReplacement
import com.linqingying.cangjie.descriptors.ParameterDescriptor
import com.linqingying.cangjie.descriptors.impl.AnonymousFunctionDescriptor
import com.linqingying.cangjie.ide.project.tools.projectWizard.core.safeAs
import com.linqingying.cangjie.references.resolveMainReferenceToDescriptors
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.*
import com.linqingying.cangjie.resolve.DescriptorToSourceUtils
import com.linqingying.cangjie.resolve.caches.analyze
import com.linqingying.cangjie.resolve.calls.util.getResolvedCall
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


//TODO 模板
class ReplaceExplicitFunctionLiteralParamWithItIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = CangJieBundle.message("replace.explicit.lambda.parameter.with.it")

    private fun targetFunctionLiteral(element: PsiElement, caretOffset: Int): CjFunctionLiteral? {
        val expression = element.getParentOfType<CjNameReferenceExpression>(true)
        if (expression != null) {
            val target =
                expression.resolveMainReferenceToDescriptors().singleOrNull() as? ParameterDescriptor ?: return null
            val functionDescriptor = target.containingDeclaration as? AnonymousFunctionDescriptor ?: return null
            return DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor) as? CjFunctionLiteral
        }

        val functionLiteral = element.getParentOfType<CjFunctionLiteral>(true) ?: return null
        val arrow = functionLiteral.arrow ?: return null
        if (caretOffset > arrow.endOffset) return null
        return functionLiteral
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val functionLiteral = targetFunctionLiteral(element, editor.caretModel.offset) ?: return false

        val parameter = functionLiteral.valueParameters.singleOrNull() ?: return false
        if (parameter.typeReference != null) return false
        if (parameter.destructuringDeclaration != null) return false

//        if (functionLiteral.anyDescendantOfType<CjFunctionLiteral> { literal ->
//                literal.usesName(element.text) && (!literal.hasParameterSpecification() || literal.usesName(
//                    StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME.identifier))
//            }) return false

        val lambda = functionLiteral.parent as? CjLambdaExpression ?: return false
        val lambdaParent = lambda.parent
        if (lambdaParent is CjMatchEntry || lambdaParent is CjContainerNodeForControlStructureBody) return false
        val call = lambda.getStrictParentOfType<CjCallExpression>()
        if (call != null) {
            val argumentIndex = call.valueArguments.indexOfFirst { it.getArgumentExpression() == lambda }
            val callOrQualified = call.getQualifiedExpressionForSelectorOrThis()
            val newCallOrQualified = callOrQualified.copied()
            val newCall = newCallOrQualified.safeAs<CjQualifiedExpression>()?.callExpression
                ?: newCallOrQualified as? CjCallExpression
                ?: return false
            val newArgument = newCall.valueArguments.getOrNull(argumentIndex) ?: newCall.lambdaArguments.singleOrNull()
            ?: return false
            newArgument.replace(CjPsiFactory(project).createLambdaExpression("", "TODO()"))
            val newContext = newCallOrQualified.analyzeAsReplacement(
                callOrQualified, callOrQualified.analyze(
                    BodyResolveMode.PARTIAL
                )
            )
            if (newCallOrQualified.getResolvedCall(newContext)?.resultingDescriptor == null) return false
        }

        text = CangJieBundle.message("replace.explicit.parameter.0.with.it", parameter.name.toString())
        return true
    }

    private fun CjFunctionLiteral.usesName(name: String): Boolean =
        anyDescendantOfType<CjSimpleNameExpression> { nameExpr ->
            nameExpr.getReferencedName() == name
        }

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
//        val caretOffset = editor.caretModel.offset
//        val functionLiteral = targetFunctionLiteral(element, editor.caretModel.offset) ?: return
//        val cursorInParameterList = functionLiteral.valueParameterList?.textRange?.containsOffset(caretOffset) ?: return
//        ParamRenamingProcessor(editor, functionLiteral, cursorInParameterList).run()

    }
//
//    private class ParamRenamingProcessor(
//        val editor: Editor,
//        val functionLiteral: CjFunctionLiteral,
//        val cursorWasInParameterList: Boolean
//    ) : RenameProcessor(
//        editor.project!!,
//        functionLiteral.valueParameters.single(),
//        StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME.identifier,
//        false,
//        false
//    ) {
//        override fun performRefactoring(usages: Array<out UsageInfo>) {
//            super.performRefactoring(usages)
//
//            functionLiteral.deleteChildRange(functionLiteral.valueParameterList, functionLiteral.arrow ?: return)
//
//            if (cursorWasInParameterList) {
//                editor.caretModel.moveToOffset(functionLiteral.bodyExpression?.textOffset ?: return)
//            }
//
//            val project = functionLiteral.project
//            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
//            CodeStyleManager.getInstance(project).adjustLineIndent(functionLiteral.containingFile, functionLiteral.textRange)
//
//        }
//    }
}
