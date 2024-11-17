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

package com.linqingying.cangjie.references

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjReferenceExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.utils.slicedMap.ReadOnlySlice
import com.linqingying.cangjie.utils.slicedMap.Slices
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.impl.source.resolve.ResolveCache


interface CjReference : PsiPolyVariantReference {
    val resolver: ResolveCache.PolyVariantResolver<CjReference>
    override fun getElement(): CjElement

    fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> = getTargetDescriptors(bindingContext)
    fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>

    val resolvesByNames: Collection<Name>

}


abstract class CjSimpleReference<T : CjReferenceExpression>(expression: T) : AbstractCjReference<T>(expression)

//interface CjReference : PsiPolyVariantReference {
//
//    override fun getElement(): CjElement
//
//    override fun resolve(): CjElement?
//
//    fun multiResolve(): List<CjElement>
//}
//
//
//
val BINDING_RESOLVE_TO_DESCRIPTORS: ReadOnlySlice<CjReference, Collection<DeclarationDescriptor>> =
    Slices.createSimpleSlice()

fun CjReference.resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> =
    resolveToDescriptors(bindingContext)
//fun CjReference.resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> {
//    return when (this) {
//        is CjReference -> resolveToDescriptors(bindingContext)
////        is CjDefaultAnnotationArgumentReference -> {
////            when (val declaration = resolve()) {
////                is CjDeclaration -> {
////                    val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
////                    // passed bindingContext may not contain information about declarations from other modules
////                        ?: declaration.resolveToDescriptorIfAny(BodyResolveMode.PARTIAL)
////
////                    listOfNotNull(descriptor)
////                }
////
////                is PsiMember -> listOfNotNull(declaration.getJavaOrCangJieMemberDescriptor())
////                else -> emptyList()
////            }
////        }
//        else -> {
//            bindingContext[BINDING_RESOLVE_TO_DESCRIPTORS, this]?.let { return it }
//
//            error("Reference $this should be CjFe10Reference but was ${this::class}")
//        }
//    }
//}
