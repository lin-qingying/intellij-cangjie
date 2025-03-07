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

package cn.cangnova.cangjie.ide.codeinsight.hints

import com.intellij.codeInsight.hints.HintInfo
import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import cn.cangnova.cangjie.descriptors.ConstructorDescriptor
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.descriptors.annotations.fqNameOrNull
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.psi.CjCallElement
import cn.cangnova.cangjie.psi.CjCallExpression
import cn.cangnova.cangjie.psi.CjNameReferenceExpression
import cn.cangnova.cangjie.psi.CjValueArgumentList
import cn.cangnova.cangjie.resolve.caches.resolveToCall
import cn.cangnova.cangjie.resolve.descriptorUtil.fqNameSafe

class CangJieInlayParameterHintsProvider : InlayParameterHintsProvider {


    override fun getHintInfo(element: PsiElement): HintInfo? {
        if (!(HintType.PARAMETER_HINT.isApplicable(element))) return null
        val parent: PsiElement = (element as? CjValueArgumentList)?.parent ?: return null
        return (parent as? CjCallElement)?.let { getMethodInfo(it) }
    }

    override fun getDefaultBlackList(): Set<String> {
        return setOf()
    }

    override fun getParameterHints(element: PsiElement): List<InlayInfo> {
        return if (HintType.PARAMETER_HINT.isApplicable(element))
            HintType.PARAMETER_HINT.provideHints(element)
        else emptyList()
    }


    override fun getInlayPresentation(inlayText: String): String = inlayText

    private fun getMethodInfo(elem: CjCallElement): HintInfo.MethodInfo? {
        val resolvedCall = elem.resolveToCall()
        val resolvedCallee = resolvedCall?.candidateDescriptor
        if (resolvedCallee is FunctionDescriptor) {
            val paramNames =
                resolvedCallee.valueParameters.asSequence().map { it.name }.filter { !it.isSpecial }.map(Name::asString).toList()
            val fqName = if (resolvedCallee is ConstructorDescriptor)
                resolvedCallee.containingDeclaration.fqNameSafe.asString()
            else
                (resolvedCallee.fqNameOrNull()?.asString() ?: return null)
            return HintInfo.MethodInfo(fqName, paramNames)
        }
        return null
    }
}

fun PsiElement.isNameReferenceInCall() =
    this is CjNameReferenceExpression && parent is CjCallExpression
