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

package cn.cangnova.cangjie.ide.search

import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.FunctionDescriptor
import cn.cangnova.cangjie.psi.*

import cn.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType
import cn.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import cn.cangnova.cangjie.resolve.caches.getResolutionFacade
import cn.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import cn.cangnova.cangjie.resolve.lazy.BodyResolveMode
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.FuzzyType
import cn.cangnova.cangjie.types.toFuzzyType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import cn.cangnova.cangjie.resolve.isExtension
import cn.cangnova.cangjie.types.fuzzyExtensionReceiverType

fun PsiReference.isImportUsage(): Boolean =
    element.getNonStrictParentOfType<CjImportDirectiveItem>() != null

fun PsiElement.getReceiverTypeSearcherInfo(): ReceiverTypeSearcherInfo? {
    val receiverType = runReadAction { extractReceiverType() } ?: return null
    val psiClass = runReadAction { receiverType.toTypeStatement(project) }
    return ReceiverTypeSearcherInfo(psiClass) {
        containsTypeOrDerivedInside(it, receiverType)
    }
}

fun CjFile.forceResolveReferences(elements: List<CjElement>) {
    getResolutionFacade().analyze(elements, BodyResolveMode.PARTIAL)
}

private fun containsTypeOrDerivedInside(declaration: CjDeclaration, typeToSearch: FuzzyType): Boolean {

    fun CangJieType.containsTypeOrDerivedInside(): Boolean {
        return typeToSearch.checkIsSuperTypeOf(this) != null || arguments.any {  it.type.containsTypeOrDerivedInside() }
    }

    val descriptor = declaration.resolveToDescriptorIfAny() as? CallableDescriptor
    val type = descriptor?.returnType
    return type != null && type.containsTypeOrDerivedInside()
}

private fun FuzzyType.toTypeStatement(project: Project): CjTypeStatement? {
    val classDescriptor = type.constructor.declarationDescriptor ?: return null
    return DescriptorToSourceUtilsIde.getAnyDeclaration(project, classDescriptor) as? CjTypeStatement
}
private fun PsiElement.extractReceiverType(): FuzzyType? {
    val descriptor = resolveTargetToDescriptor()?.takeIf { it.isValidOperator() } ?: return null

    return if (descriptor.isExtension) {
        descriptor.fuzzyExtensionReceiverType()!!
    } else {
        val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return null
        classDescriptor.defaultType.toFuzzyType(classDescriptor.typeConstructor.parameters)
    }
}
fun FunctionDescriptor.isValidOperator() = isOperator /*&& OperatorChecks.check(this).isSuccess*/
private fun PsiElement.resolveTargetToDescriptor( ): FunctionDescriptor? {



    return when {
        this is CjDeclaration -> resolveToDescriptorIfAny(BodyResolveMode.FULL)

        else -> null
    } as? FunctionDescriptor
}

