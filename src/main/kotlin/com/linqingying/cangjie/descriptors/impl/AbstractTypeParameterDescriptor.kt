package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.resolve.scopes.LazyScopeAdapter
import com.linqingying.cangjie.resolve.scopes.TypeIntersectionScope.Companion.create
import com.linqingying.cangjie.storage.NotNullLazyValue
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope
import com.linqingying.cangjie.types.ErrorUtils.createErrorType
import com.linqingying.cangjie.types.TypeAttributes.Companion.Empty
import com.linqingying.cangjie.types.error.ErrorTypeKind


abstract class AbstractTypeParameterDescriptor protected constructor(
    private val storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    private val variance: Variance,

    private val index: Int,
    source: SourceElement,
    supertypeLoopChecker: SupertypeLoopChecker
) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, source),
    TypeParameterDescriptor {

    private val typeConstructor: NotNullLazyValue<TypeConstructor> = storageManager.createLazyValue<TypeConstructor> {
        TypeParameterTypeConstructor(
            storageManager, supertypeLoopChecker
        )
    }
    private val defaultType: NotNullLazyValue<SimpleType>

    init {
        this.defaultType = storageManager.createLazyValue {
            simpleTypeWithNonTrivialMemberScope(
                Empty,
                getTypeConstructor(), emptyList(), false,
                LazyScopeAdapter {
                    create(
                        "Scope for type parameter " + name.asString(),
                        upperBounds
                    )
                }
            )
        }
    }

    override val original: TypeParameterDescriptor
        get() = super.original as TypeParameterDescriptor

    override fun getDefaultType(): SimpleType {
        return defaultType.invoke()
    }

    override fun getVariance(): Variance {
        return variance
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitTypeParameterDescriptor(this, data)
    }


    override fun getUpperBounds(): List<CangJieType> {
        return (getTypeConstructor() as TypeParameterTypeConstructor).supertypes
    }

    override fun getTypeConstructor(): TypeConstructor {
        return typeConstructor.invoke()
    }

    override fun getIndex(): Int {
        return index
    }

    override fun isCapturedFromOuterDeclaration(): Boolean {
        return false
    }

    override fun getStorageManager(): StorageManager {
        return storageManager
    }

    protected abstract fun reportSupertypeLoopError(type: CangJieType)

    protected fun processBoundsWithoutCycles(bounds: List<CangJieType>): List<CangJieType> {
        return bounds
    }

    override fun validate() {
        super<DeclarationDescriptorNonRootImpl>.validate()
    }

    protected abstract fun resolveUpperBounds(): List<CangJieType>

    private inner class TypeParameterTypeConstructor(
        storageManager: StorageManager,
        override val supertypeLoopChecker: SupertypeLoopChecker
    ) :
        AbstractTypeConstructor(storageManager) {
        override fun computeSupertypes(): Collection<CangJieType> {
            return resolveUpperBounds()
        }

        override fun computeExtendSuperTypes(extendId: String?): Collection<CangJieType> {
            return listOf()
        }

        override fun getParameters(): List<TypeParameterDescriptor> {
            return emptyList()
        }

        override fun isFinal(): Boolean {
            return false
        }

        override fun isDenotable(): Boolean {
            return true
        }

        override fun getDeclarationDescriptor(): ClassifierDescriptor {
            return this@AbstractTypeParameterDescriptor
        }

        override fun getBuiltIns(): CangJieBuiltIns {
            return this@AbstractTypeParameterDescriptor.builtIns
        }

        override fun toString(): String {
            return name.toString()
        }

        override fun reportSupertypeLoopError(type: CangJieType) {
            this@AbstractTypeParameterDescriptor.reportSupertypeLoopError(type)
        }

        override fun processSupertypesWithoutCycles(supertypes: List<CangJieType>): List<CangJieType> {
            return processBoundsWithoutCycles(supertypes)
        }

        override fun defaultSupertypeIfEmpty(): CangJieType {
            return createErrorType(ErrorTypeKind.CYCLIC_UPPER_BOUNDS)
        }

        override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
            return classifier is TypeParameterDescriptor /*&&
                    DescriptorEquivalenceForOverrides.INSTANCE.areTypeParametersEquivalent(
                            AbstractTypeParameterDescriptor.this,
                            (TypeParameterDescriptor) classifier,
                            true
                    )*/
        }
    }
}
