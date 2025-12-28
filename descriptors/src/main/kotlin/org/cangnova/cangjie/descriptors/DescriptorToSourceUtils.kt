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

package org.cangnova.cangjie.descriptors

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.cangnova.cangjie.descriptors.synthetic.SyntheticMemberDescriptor
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.resolve.source.getPsi

object DescriptorToSourceUtils {


    @JvmStatic
    fun getSourceFromDescriptor(descriptor: DeclarationDescriptor): PsiElement? {

        return (descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi()
    }

    // NOTE this is also used by CDoc
    // Returns PSI element for descriptor. If there are many relevant elements (e.g. it is fake override
    // with multiple declarations), returns null. It can't find declarations in builtins or decompiled code.
    // In IDE, use DescriptorToSourceUtilsIde instead.
    @JvmStatic
    fun descriptorToDeclaration(descriptor: DeclarationDescriptor): PsiElement? {
        val effectiveReferencedDescriptors = getEffectiveReferencedDescriptors(descriptor)
        return if (effectiveReferencedDescriptors.size == 1) getSourceFromDescriptor(
            effectiveReferencedDescriptors.firstOrNull()
                ?: return null
        ) else null
    }

    private fun collectEffectiveReferencedDescriptors(
        result: MutableList<DeclarationDescriptor>,
        descriptor: DeclarationDescriptor
    ) {
//        if (descriptor is DeclarationDescriptorWithNavigationSubstitute) {
//            collectEffectiveReferencedDescriptors(result, descriptor.substitute)
//            return
//        }

        if (descriptor is CallableMemberDescriptor) {
            val kind = descriptor.kind
            if (kind != DECLARATION && kind != SYNTHESIZED) {
                for (overridden in descriptor.overriddenDescriptors) {
                    collectEffectiveReferencedDescriptors(result, overridden.original)
                }
                return
            }
            if (descriptor is SyntheticMemberDescriptor<*>) {
                collectEffectiveReferencedDescriptors(result, descriptor.baseDescriptorForSynthetic)
                return
            }
        }
        result.add(descriptor)
    }

    @JvmStatic
    fun getEffectiveReferencedDescriptors(descriptor: DeclarationDescriptor): Collection<DeclarationDescriptor> {
        val result = ArrayList<DeclarationDescriptor>()
        collectEffectiveReferencedDescriptors(result, descriptor.original)
        return result
    }
}
