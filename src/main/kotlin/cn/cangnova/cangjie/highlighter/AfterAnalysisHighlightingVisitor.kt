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

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement
import cn.cangnova.cangjie.dag.CangJieDependencyGraph
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.diagnostics.Errors.CYCLIC_IMPORT
import cn.cangnova.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjSimpleNameExpression
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.calls.model.ResolvedCall

class AfterAnalysisVisitor(holder: HighlightInfoHolder, bindingContext: BindingContext) :
    AfterAnalysisHighlightingVisitor(holder, bindingContext) {
    private val dependencyGraph = CangJieDependencyGraph.getInstance(holder.project)
    override fun visitCjFile(file: CjFile) {

        dependencyGraph.updateDependenciesForFile(file)

        val detectCycles = dependencyGraph.findCycleContaining(file.packageFqName)

        if (detectCycles.isNotEmpty()) {
            dependencyGraph.printGraph()
            holder.report(
                CYCLIC_IMPORT.on(file.packageDirective, detectCycles)
            )

        }

    }

}

/**
 * 代码分析后的高亮逻辑
 */
abstract class AfterAnalysisHighlightingVisitor protected constructor(
    holder: HighlightInfoHolder,
    protected var bindingContext: BindingContext
) : AbstractHighlightingVisitor(holder) {
    protected fun attributeKeyForCallFromExtensions(
        expression: CjSimpleNameExpression,
        resolvedCall: ResolvedCall<out CallableDescriptor>
    ): HighlightInfoType? {
        return CangJieHighlightingVisitorExtension.EP_NAME.extensionList.firstNotNullOfOrNull { extension ->
            extension.highlightCall(expression, resolvedCall)
        }
    }

    protected fun attributeKeyForDeclarationFromExtensions(
        element: PsiElement,
        descriptor: DeclarationDescriptor
    ): HighlightInfoType? {
        return CangJieHighlightingVisitorExtension.EP_NAME.extensionList.firstNotNullOfOrNull { extension ->
            extension.highlightDeclaration(element, descriptor)
        }
    }
}
