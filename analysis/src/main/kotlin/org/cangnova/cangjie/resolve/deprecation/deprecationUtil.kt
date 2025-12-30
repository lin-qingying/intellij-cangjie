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

package org.cangnova.cangjie.resolve.deprecation

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.config.ApiVersion
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptorWithVisibility
import org.cangnova.cangjie.descriptors.DescriptorVisibilityUtils
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.OverridingUtil
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.argumentValue
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.constants.StringValue
import org.cangnova.cangjie.resolve.findOriginalTopMostOverriddenDescriptors
import org.cangnova.cangjie.resolve.languageVersionSettings
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.getImplicitReceiversHierarchy
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import org.cangnova.cangjie.utils.isExtension

fun <D : CallableMemberDescriptor> D.getDirectlyOverriddenDeclarations(): Collection<D> {
    val result = java.util.LinkedHashSet<D>()
    for (overriddenDescriptor in overriddenDescriptors) {
        @Suppress("UNCHECKED_CAST")
        when (overriddenDescriptor.kind) {
            CallableMemberDescriptor.Kind.DECLARATION -> result.add(overriddenDescriptor as D)
            CallableMemberDescriptor.Kind.FAKE_OVERRIDE, CallableMemberDescriptor.Kind.DELEGATION -> result.addAll((overriddenDescriptor as D).getDirectlyOverriddenDeclarations())
            CallableMemberDescriptor.Kind.SYNTHESIZED -> {
                //do nothing
            }

            else -> throw AssertionError("Unexpected callable kind ${overriddenDescriptor.kind}: $overriddenDescriptor")
        }
    }
    return OverridingUtil.filterOutOverridden(result)
}

fun <T : DeclarationDescriptor> T.unwrapIfFakeOverride(): T {
    return if (this is CallableMemberDescriptor) DescriptorUtils.unwrapFakeOverride(this) else this
}
fun compareDescriptors(
    project: Project,
    currentDescriptor: DeclarationDescriptor?,
    originalDescriptor: DeclarationDescriptor?
): Boolean {
    if (currentDescriptor == originalDescriptor) return true
    if (currentDescriptor == null || originalDescriptor == null) return false

    if (currentDescriptor.name != originalDescriptor.name) return false


    if (compareDescriptorsText(project, currentDescriptor, originalDescriptor)) return true

    if (originalDescriptor is CallableDescriptor && currentDescriptor is CallableDescriptor) {
        val overriddenOriginalDescriptor = originalDescriptor.findOriginalTopMostOverriddenDescriptors()
        val overriddenCurrentDescriptor = currentDescriptor.findOriginalTopMostOverriddenDescriptors()

        if (overriddenOriginalDescriptor.size != overriddenCurrentDescriptor.size) return false
        return overriddenCurrentDescriptor.zip(overriddenOriginalDescriptor).all {
            compareDescriptorsText(project, it.first, it.second)
        }
    }

    return false
}
private fun compareDescriptorsText(project: Project, d1: DeclarationDescriptor, d2: DeclarationDescriptor): Boolean {
    if (d1 == d2) return true
    if (d1.name != d2.name) return false

    val renderedD1 = IdeDescriptorRenderers.SOURCE_CODE.render(d1)
    val renderedD2 = IdeDescriptorRenderers.SOURCE_CODE.render(d2)
    if (renderedD1 == renderedD2) return true

    val declarations1 = DescriptorToSourceUtilsIde.getAllDeclarations(project, d1)
    val declarations2 = DescriptorToSourceUtilsIde.getAllDeclarations(project, d2)
    return declarations1 == declarations2 && declarations1.isNotEmpty()
}
private fun DeclarationDescriptorWithVisibility.isVisible(
    from: DeclarationDescriptor,
    receiverExpression: CjExpression?,
    bindingContext: BindingContext? = null,
    resolutionScope: LexicalScope? = null,
    languageVersionSettings: LanguageVersionSettings
): Boolean {
    if (DescriptorVisibilityUtils.isVisibleWithAnyReceiver(this, from, languageVersionSettings)) return true

    if (bindingContext == null || resolutionScope == null) return false

    // for extension, it makes no sense to check explicit receiver because we need dispatch receiver which is implicit in this case
    if (receiverExpression != null && !isExtension) {
        val receiverType = bindingContext.getType(receiverExpression) ?: return false
        val explicitReceiver = ExpressionReceiver.create(receiverExpression, receiverType, bindingContext)
        return DescriptorVisibilityUtils.isVisible(explicitReceiver, this, from, languageVersionSettings)
    } else {
        return resolutionScope.getImplicitReceiversHierarchy().any {
            DescriptorVisibilityUtils.isVisible(it.value, this, from, languageVersionSettings)
        }
    }
}

fun DeclarationDescriptorWithVisibility.isVisible(
    context: PsiElement,
    receiverExpression: CjExpression?,
    bindingContext: BindingContext,
    resolutionFacade: ResolutionFacade
): Boolean {
    val resolutionScope = context.getResolutionScope(bindingContext, resolutionFacade)
    val from = resolutionScope.ownerDescriptor
    return isVisible(
        from,
        receiverExpression,
        bindingContext,
        resolutionScope,
        resolutionFacade.languageVersionSettings
    )
}

@DefaultImplementation(DeprecationSettings.Default::class)
interface DeprecationSettings {
    fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor): Boolean

    object Default : DeprecationSettings {
        override fun propagatedToOverrides(deprecationAnnotation: AnnotationDescriptor) = true
    }
}

fun DescriptorBasedDeprecationInfo.deprecatedByAnnotationReplaceWithExpression(): String? =
    (this as? DeprecatedByAnnotation)?.replaceWithValue

// CangJie does not have DeprecatedSinceCangJie annotation
// Deprecation is handled differently in CangJie




fun ClassDescriptor.findCallableMemberBySignature(
    signature: CallableMemberDescriptor,
    allowOverridabilityConflicts: Boolean = false
): CallableMemberDescriptor? {
    val descriptorKind =
        if (signature is FunctionDescriptor) DescriptorKindFilter.FUNCTIONS else DescriptorKindFilter.VARIABLES
    return defaultType.memberScope
        .getContributedDescriptors(descriptorKind)
        .filterIsInstance<CallableMemberDescriptor>()
        .firstOrNull {
            if (it.containingDeclaration != this) return@firstOrNull false
            val overridability =
                OverridingUtil.DEFAULT.isOverridableBy(it as CallableDescriptor, signature, null).result
            overridability == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE || (allowOverridabilityConflicts && overridability == OverridingUtil.OverrideCompatibilityInfo.Result.CONFLICT)
        }
}