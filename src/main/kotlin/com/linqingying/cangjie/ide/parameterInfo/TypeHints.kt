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

package com.linqingying.cangjie.ide.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ConstructorDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.PackageViewDescriptor
import com.linqingying.cangjie.ide.codeinsight.hints.InlayInfoDetails
import com.linqingying.cangjie.ide.codeinsight.hints.InlayInfoOption
import com.linqingying.cangjie.ide.codeinsight.hints.TextInlayInfoDetail
import com.linqingying.cangjie.ide.formatter.cangjieCustomSettings
import com.linqingying.cangjie.ide.intentions.SpecifyTypeExplicitlyIntention
import com.linqingying.cangjie.name.SpecialNames
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.endOffset
import com.linqingying.cangjie.psi.psiUtil.getLineNumber
import com.linqingying.cangjie.psi.psiUtil.isMultiLine
import com.linqingying.cangjie.psi.stubs.elements.getAllBindings
import com.linqingying.cangjie.references.resolveMainReferenceToDescriptors
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.caches.resolveToCall
import com.linqingying.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.sam.SamConstructorDescriptor
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.util.containsError
import com.linqingying.cangjie.types.util.immediateSupertypes
import com.linqingying.cangjie.types.util.isEnum
import com.linqingying.cangjie.types.util.isUnit


fun provideVariableTypeHint(elem: PsiElement, inlayInfoOption: InlayInfoOption): List<InlayInfoDetails> {
    (elem as? CjVariable)?.let { variable ->
        variable.nameIdentifier?.let { ident ->
            provideTypeHint(variable, ident.endOffset, inlayInfoOption)?.let { return listOf(it) }
        }

        variable.pattern.getAllBindings().mapNotNull {
            provideTypeHint(it, it.endOffset, inlayInfoOption)
        }.let {
            if (it.isNotEmpty()) return it
        }
    }
    return when (elem) {
        is CjVariable -> {
            elem.nameIdentifier?.let { ident ->
                provideTypeHint(elem, ident.endOffset, inlayInfoOption)?.let { listOf(it) }
            } ?: elem.pattern.getAllBindings().mapNotNull {
                provideTypeHint(it, it.endOffset, inlayInfoOption)
            }.let {
                if (it.isNotEmpty()) it else emptyList()
            } ?: emptyList()

        }

        is CjBindingPattern -> {
            provideTypeHint(elem, elem.endOffset, inlayInfoOption)?.let { listOf(it) } ?: emptyList()

        }

        else -> emptyList()
    }

}

fun provideTypeHint(element: CjCallableDeclaration, offset: Int, inlayInfoOption: InlayInfoOption): InlayInfoDetails? {
    var type: CangJieType = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(element).unwrap()
    if (type.containsError()) return null
    val declarationDescriptor = type.constructor.declarationDescriptor
    val name = declarationDescriptor?.name
    if (name == SpecialNames.NO_NAME_PROVIDED) {
        if (element is CjVariable && element.isLocal) {
            // for local variables, an anonymous object type is not collapsed to its supertype,
            // so showing the supertype will be misleading
            return null
        }
        type = type.immediateSupertypes().singleOrNull() ?: return null
    } else if (name?.isSpecial == true) {
        return null
    }

    if (element is CjVariable && element.isLocal && type.isUnit() && element.isMultiLine()) {
        val propertyLine = element.getLineNumber()
        val equalsTokenLine = element.equalsToken?.getLineNumber() ?: -1
        val initializerLine = element.initializer?.getLineNumber() ?: -1
        if (propertyLine == equalsTokenLine && propertyLine != initializerLine) {
            val indentBeforeProperty = (element.prevSibling as? PsiWhiteSpace)?.text?.substringAfterLast('\n')
            val indentBeforeInitializer =
                (element.initializer?.prevSibling as? PsiWhiteSpace)?.text?.substringAfterLast('\n')
            if (indentBeforeProperty == indentBeforeInitializer) {
                return null
            }
        }
    }

    return if (isUnclearType(type, element)) {
        val settings = element.containingCjFile.cangjieCustomSettings
        val renderedType = HintsTypeRenderer.getInlayHintsTypeRenderer(element.safeAnalyzeNonSourceRootCode(), element)
            .renderTypeIntoInlayInfo(type)
        val prefix = buildString {
            if (settings.SPACE_BEFORE_TYPE_COLON) {
                append(" ")
            }

            append(":")
            if (settings.SPACE_AFTER_TYPE_COLON) {
                append(" ")
            }
        }

        val inlayInfo = InlayInfo(
            text = "", offset = offset,
            isShowOnlyIfExistedBefore = false, isFilterByBlacklist = true, relatesToPrecedingText = true
        )
        return InlayInfoDetails(inlayInfo, listOf(TextInlayInfoDetail(prefix)) + renderedType, inlayInfoOption)
    } else {
        null
    }
}

private fun isUnclearType(type: CangJieType, element: CjCallableDeclaration): Boolean {
    if (element !is CjProperty) return true

    val initializer = element.initializer ?: return true
    if (initializer is CjConstantExpression || initializer is CjStringTemplateExpression) return false
    if (initializer is CjUnaryExpression && initializer.baseExpression is CjConstantExpression) return false

    if (isConstructorCall(initializer)) {
        return false
    }

    if (initializer is CjDotQualifiedExpression) {
        val selectorExpression = initializer.selectorExpression
        if (type.isEnum()) {
            // Do not show type for enums if initializer has enum entry with explicit enum name: val p = Enum.ENTRY
            val enumEntryDescriptor: DeclarationDescriptor? =
                selectorExpression?.resolveMainReferenceToDescriptors()?.singleOrNull()

            if (enumEntryDescriptor != null && DescriptorUtils.isEnumEntry(enumEntryDescriptor)) {
                return false
            }
        }

        if (initializer.receiverExpression.isClassOrPackageReference() && isConstructorCall(selectorExpression)) {
            return false
        }
    }

    return true
}

private fun isConstructorCall(initializer: CjExpression?): Boolean {
    if (initializer is CjCallExpression) {
        val resolvedCall = initializer.resolveToCall(BodyResolveMode.FULL)
        val resolvedDescriptor = resolvedCall?.candidateDescriptor
        if (resolvedDescriptor is SamConstructorDescriptor) {
            return true
        }
        if (resolvedDescriptor is ConstructorDescriptor &&
            (resolvedDescriptor.constructedClass.declaredTypeParameters.isEmpty() || initializer.typeArgumentList != null)
        ) {
            return true
        }
        return false
    }

    return false
}

private fun CjExpression.isClassOrPackageReference(): Boolean =
    when (this) {
        is CjNameReferenceExpression -> this.resolveMainReferenceToDescriptors().singleOrNull()
            .let { it is ClassDescriptor || it is PackageViewDescriptor }

        is CjDotQualifiedExpression -> this.selectorExpression?.isClassOrPackageReference() ?: false
        else -> false
    }
