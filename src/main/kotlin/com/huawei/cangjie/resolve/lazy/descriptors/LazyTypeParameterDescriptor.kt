package com.huawei.cangjie.resolve.lazy.descriptors

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.AbstractLazyTypeParameterDescriptor
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil
import com.huawei.cangjie.resolve.lazy.LazyClassContext
import com.huawei.cangjie.resolve.lazy.LazyEntity
import com.huawei.cangjie.resolve.source.toSourceElement
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.isError

class LazyTypeParameterDescriptor(
    val c: LazyClassContext,
    containingDeclaration: LazyClassDescriptor,
    val typeParameter: CjTypeParameter,
    annotations: Annotations = Annotations.EMPTY,
    index: Int
) : AbstractLazyTypeParameterDescriptor(
    c.storageManager,
    containingDeclaration,
    annotations,
    typeParameter.nameAsSafeName,
    typeParameter.getVariance(),
//    typeParameter.hasModifier(CjTokens.REIFIED_KEYWORD),
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

        val classOrObject =
            CjStubbedPsiUtil.getPsiOrStubParent(
                typeParameter,
                CjTypeStatement::class.java, true
            )
        if (classOrObject is CjClass) {
            for (typeConstraint in classOrObject.getTypeConstraints()) {
                val constrainedParameterName = typeConstraint.subjectTypeParameterName
                if (constrainedParameterName != null) {
                    if (name == constrainedParameterName.getReferencedNameAsName()) {
                        c.trace.record<CjReferenceExpression, DeclarationDescriptor>(
                            BindingContext.REFERENCE_TARGET, constrainedParameterName,
                            this
                        )

                        val boundTypeReference = typeConstraint.boundTypeReference
                        if (boundTypeReference != null) {
                            result.add(boundTypeReference)
                        }
                    }
                }
            }
        }

        return result
    }

    private fun getAllUpperBounds(): Collection<CjTypeReference> {
        return listOfNotNull(typeParameter.extendsBound)
            .plus(getUpperBoundsFromWhereClause())
    }

    private fun resolveBoundType(boundTypeReference: CjTypeReference): CangJieType {
        return c.typeResolver.resolveType(

            containingDeclaration.scopeForClassHeaderResolution, boundTypeReference, c.trace, false
        )
    }


    override val containingDeclaration: LazyClassDescriptor
        get() = super.containingDeclaration as LazyClassDescriptor

    override fun reportSupertypeLoopError(type: CangJieType) {
        for (typeReference in getAllUpperBounds()) {
            if (resolveBoundType(typeReference).constructor == type.constructor) {
                c.trace.report(Errors.CYCLIC_GENERIC_UPPER_BOUND.on(typeReference))
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
        variance
    }
}
