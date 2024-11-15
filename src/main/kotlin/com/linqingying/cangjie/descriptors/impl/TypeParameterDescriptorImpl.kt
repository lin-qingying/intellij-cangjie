package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.descriptors.SupertypeLoopChecker
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.DescriptorUtils.getFqName
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.types.isError
import kotlin.jvm.functions.Function1

class TypeParameterDescriptorImpl private constructor(
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,

    variance: Variance,
    name: Name,
    index: Int,
    source: SourceElement,
    private val reportCycleError:((CangJieType?)->Unit?)?,
    supertypeLoopsChecker: SupertypeLoopChecker,
    storageManager: StorageManager
) : AbstractTypeParameterDescriptor(
    storageManager,
    containingDeclaration,
    annotations,
    name,
    variance,  /*reified,*/
    index,
    source,
    supertypeLoopsChecker
) {
    var isInitialized: Boolean = false
        private set
    private val upperBounds: MutableList<CangJieType> = ArrayList(1)

    fun addUpperBound(bound: CangJieType) {
        checkUninitialized()
        doAddUpperBound(bound)
    }

    private fun doAddUpperBound(bound: CangJieType) {
        if (bound.isError) return
        upperBounds.add(bound) // TODO : Duplicates?
    }

    fun addDefaultUpperBound() {
        checkUninitialized()

        if (upperBounds.isEmpty()) {
            doAddUpperBound(containingDeclaration.builtIns.defaultBound)
        }
    }


    private fun nameForAssertions(): String {
        return name.toString() + " declared in " + getFqName(containingDeclaration)
    }

    private fun checkUninitialized() {
        check(!isInitialized) { "Type parameter descriptor is already initialized: " + nameForAssertions() }
    }

    fun setInitialized() {
        checkUninitialized()
        isInitialized = true
    }


    override fun reportSupertypeLoopError(type: CangJieType) {
        if (reportCycleError == null) return
        reportCycleError.invoke(type)
    }

    private fun checkInitialized() {
        check(isInitialized) { "Type parameter descriptor is not initialized: " + nameForAssertions() }
    }

    override fun resolveUpperBounds(): List<CangJieType> {
        checkInitialized()
        return upperBounds
    }


    companion object {
        @JvmStatic
        fun createWithDefaultBound(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,

            variance: Variance,
            name: Name,
            index: Int,
            storageManager: StorageManager
        ): TypeParameterDescriptor {
            val typeParameterDescriptor = createForFurtherModification(
                containingDeclaration,
                annotations,
                variance,
                name,
                index,
                SourceElement.NO_SOURCE,
                storageManager
            )
            typeParameterDescriptor.addUpperBound(containingDeclaration.builtIns.defaultBound)
            typeParameterDescriptor.setInitialized()
            return typeParameterDescriptor
        }

        @JvmStatic
        fun createForFurtherModification(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,

            variance: Variance,
            name: Name,
            index: Int,
            source: SourceElement,
            storageManager: StorageManager
        ): TypeParameterDescriptorImpl {
            return createForFurtherModification(
                containingDeclaration, annotations,  variance, name, index, source,
                null, SupertypeLoopChecker.EMPTY, storageManager
            )
        }
        @JvmStatic
        fun createForFurtherModification(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,

            variance: Variance,
            name: Name,
            index: Int,
            source: SourceElement,
            reportCycleError: ((CangJieType?)->Unit?)?,
            supertypeLoopsResolver: SupertypeLoopChecker,
            storageManager: StorageManager
        ): TypeParameterDescriptorImpl {
            return TypeParameterDescriptorImpl(
                containingDeclaration, annotations, variance, name,
                index, source, reportCycleError, supertypeLoopsResolver, storageManager
            )
        }
    }
}
