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

package org.cangnova.cangjie.refactoring
import org.cangnova.cangjie.messages.CangJieRefactoringBundle

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.impl.LocalVariableDescriptor
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.psiUtil.isIdentifier
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.caches.unsafeResolveToDescriptor
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.psi.PsiElement
import java.util.HashMap

fun FqName.hasIdentifiersOnly(): Boolean = pathSegments().all { it.asString().quoteIfNeeded().isIdentifier() }
fun getSuperMethods(declaration: CjDeclaration, ignore: Collection<PsiElement>?): List<PsiElement> {
    if (!declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) return listOf(declaration)
    val (_, overriddenElementsToDescriptor) = getSuperDescriptors(declaration, ignore)
    return if (overriddenElementsToDescriptor.isEmpty()) listOf(declaration) else overriddenElementsToDescriptor.keys.toList()
}

private fun getSuperDescriptors(
    declaration: CjDeclaration,
    ignore: Collection<PsiElement>?
): Pair<CallableDescriptor, Map<PsiElement, CallableDescriptor>> {
    val progressTitle = CangJieRefactoringBundle.message("find.usages.progress.text.declaration.superMethods")
    return ActionUtil.underModalProgress(declaration.project, progressTitle) {
        val declarationDescriptor = declaration.unsafeResolveToDescriptor() as CallableDescriptor

        if (declarationDescriptor is LocalVariableDescriptor) {
            return@underModalProgress (declarationDescriptor to emptyMap<PsiElement, CallableDescriptor>())
        }

        val overriddenElementsToDescriptor = HashMap<PsiElement, CallableDescriptor>()
        for (overriddenDescriptor in DescriptorUtils.getAllOverriddenDescriptors(declarationDescriptor)) {
            val overriddenDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(
                declaration.project,
                overriddenDescriptor
            ) ?: continue
            if (overriddenDeclaration is CjNamedFunction
                || overriddenDeclaration is CjProperty

                || overriddenDeclaration is CjParameter
            ) {
                overriddenElementsToDescriptor[overriddenDeclaration] = overriddenDescriptor
            }
        }

        if (ignore != null) {
            overriddenElementsToDescriptor.keys.removeAll(ignore)
        }

        return@underModalProgress (declarationDescriptor to overriddenElementsToDescriptor)
    }
}
