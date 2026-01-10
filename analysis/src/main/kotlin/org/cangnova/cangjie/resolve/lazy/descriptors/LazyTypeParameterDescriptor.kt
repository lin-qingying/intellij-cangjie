/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.lazy.descriptors

import org.cangnova.cangjie.descriptors.DeclarationDescriptorWithTypeParameters
import org.cangnova.cangjie.descriptors.DescriptorWithResolutionScopes
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.extend.ClassAndExtendDescriptor
import org.cangnova.cangjie.descriptors.impl.AbstractLazyTypeParameterDescriptor
import org.cangnova.cangjie.diagnostics.infos.errors.CYCLIC_GENERIC_UPPER_BOUND
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.lazy.LazyClassContext
import org.cangnova.cangjie.resolve.lazy.LazyEntity
import org.cangnova.cangjie.resolve.source.toSourceElement
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.isError

class LazyTypeParameterDescriptor(
    val c: LazyClassContext,
    containingDeclaration: DeclarationDescriptorWithTypeParameters,
    val typeParameter: CjTypeParameter,
    annotations: Annotations = Annotations.EMPTY,
    index: Int
) : AbstractLazyTypeParameterDescriptor(
    c.storageManager,
    containingDeclaration,
    annotations,
    typeParameter.nameAsSafeName,


    index,
    typeParameter.toSourceElement(),
    c.supertypeLoopChecker
), LazyEntity {


    init {

        this.c.trace.record<CjTypeParameter, TypeParameterDescriptor>(
            BindingContext.TYPE_PARAMETER, typeParameter,
            this
        )
    }

    private fun getUpperBoundsFromWhereClause(): Collection<CjTypeReference> {
        val result: MutableCollection<CjTypeReference> = mutableListOf()

        val typeStatement =
            CjStubbedPsiUtil.getPsiOrStubParent(
                typeParameter,
                CjTypeStatement::class.java, true
            )
        if (typeStatement is CjClass || typeStatement is CjInterface || typeStatement is CjEnum || typeStatement is CjStruct || typeStatement is CjExtend) {
            for (typeConstraint in typeStatement.typeConstraints) {
                val constrainedParameterName = typeConstraint.subjectTypeParameterName
                if (constrainedParameterName != null) {
                    if (name == constrainedParameterName.referencedNameAsName) {
                        c.trace.record(
                            BindingContext.REFERENCE_TARGET, constrainedParameterName,
                            this
                        )


//                        单边界
//                        val boundTypeReference = typeConstraint.boundTypeReference
//                        if (boundTypeReference != null) {
//                            result.add(boundTypeReference)
//                        }
//                        多边界
                        result.addAll(typeConstraint.boundTypeReferences)
                    }
                }
            }
        }

        return result
    }

    private fun getAllUpperBounds(): Collection<CjTypeReference> {

        return typeParameter.extendsBounds
            .plus(getUpperBoundsFromWhereClause())
    }

    private fun resolveBoundType(boundTypeReference: CjTypeReference): CangJieType {
        return c.typeResolver.resolveType(

            containingDeclaration.scopeForClassHeaderResolution, boundTypeReference, c.trace, false
        )
    }


    override val containingDeclaration: DescriptorWithResolutionScopes
        get() = super.containingDeclaration as DescriptorWithResolutionScopes

    override fun reportSupertypeLoopError(type: CangJieType) {
        for (typeReference in getAllUpperBounds()) {
            if (resolveBoundType(typeReference).constructor == type.constructor) {
                c.trace.report(CYCLIC_GENERIC_UPPER_BOUND.on(typeReference))
                return
            }
        }
    }

    override fun resolveUpperBounds(): MutableList<CangJieType> {
        val upperBounds = mutableListOf<CangJieType>()

        for (typeReference in getAllUpperBounds()) {
            val resolvedType = resolveBoundType(typeReference)
            if (!resolvedType.isError) {
                upperBounds.add(resolvedType)
            }
        }

        if (upperBounds.isEmpty()) {
            upperBounds.add(c.moduleDescriptor.builtIns.defaultBound)
        }
        return upperBounds

    }

    override fun forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(
            annotations
        )
        containingDeclaration
        defaultType
        index
        original
        ForceResolveUtil.forceResolveAllContents(typeConstructor)
        ForceResolveUtil.forceResolveAllContents(upperBounds)

    }
}
