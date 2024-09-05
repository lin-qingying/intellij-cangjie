package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor

open class ValueParameterDescriptorImpl(
    containingDeclaration: CallableDescriptor,
    original: ValueParameterDescriptor?,
    override val index: Int,
    annotations: Annotations,
    name: Name,
    outType: CangJieType,
    private val declaresDefaultValue: Boolean,
//    override val isCrossinline: Boolean,
//    override val isNoinline: Boolean,

    source: SourceElement,

) : AbstractVariableDescriptor(containingDeclaration, annotations, name, outType, source),
    ValueParameterDescriptor {

    companion object {
        @JvmStatic
        fun createWithDestructuringDeclarations(
            containingDeclaration: CallableDescriptor,
            original: ValueParameterDescriptor?,
            index: Int,
            annotations: Annotations,
            name: Name,
            outType: CangJieType,
            declaresDefaultValue: Boolean,
//            isCrossinline: Boolean,
//            isNoinline: Boolean,
//            varargElementType: CangJieType?,
            source: SourceElement,
            destructuringVariables: (() -> List<VariableDescriptor>)?
        ): ValueParameterDescriptorImpl =
            if (destructuringVariables == null)
                ValueParameterDescriptorImpl(
                    containingDeclaration, original, index, annotations, name, outType,
                    declaresDefaultValue, /*isCrossinline, isNoinline, varargElementType,*/ source
                )
            else
                WithDestructuringDeclaration(
                    containingDeclaration, original, index, annotations, name, outType,
                    declaresDefaultValue,/* isCrossinline, isNoinline, varargElementType,*/ source,
                    destructuringVariables
                )
    }

    class WithDestructuringDeclaration internal constructor(
        containingDeclaration: CallableDescriptor,
        original: ValueParameterDescriptor?,
        index: Int,
        annotations: Annotations, name: Name,
        outType: CangJieType,
        declaresDefaultValue: Boolean,
//        isCrossinline: Boolean,
//        isNoinline: Boolean,
//       varargElementType: CangJieType?,
        source: SourceElement,
        destructuringVariables: () -> List<VariableDescriptor>
    ) : ValueParameterDescriptorImpl(
        containingDeclaration, original, index, annotations, name, outType,
        declaresDefaultValue,
//        isCrossinline,
//        isNoinline,
//        varargElementType,
        source
    ) {
        // It's forced to be lazy because its resolution depends on receiver of relevant lambda, that is being created at the same moment
        // as value parameters.
        // Must be forced via ForceResolveUtil.forceResolveAllContents()
        val destructuringVariables by lazy(destructuringVariables)

        override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor {
            return WithDestructuringDeclaration(
                newOwner, null, newIndex, annotations, newName, type, declaresDefaultValue(),
                /*       isCrossinline, isNoinline,  varargElementType,*/ SourceElement.NO_SOURCE
            ) { destructuringVariables }
        }
    }
    override val varargElementType: CangJieType? = null
    private val myOriginal: ValueParameterDescriptor = original ?: this

    override val original
        get() = if (myOriginal === this) this else myOriginal.original

    override val isVar: Boolean
        get() = false

    override fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor {
        return ValueParameterDescriptorImpl(
            newOwner, null, newIndex, annotations, newName, type, declaresDefaultValue(),
            /*isCrossinline, isNoinline, varargElementType, */SourceElement.NO_SOURCE
        )
    }

    override fun getCompileTimeInitializer(): ConstantValue<*>? = null

    override fun cleanCompileTimeInitializerCache() {

    }

    override fun declaresDefaultValue(): Boolean {
        return declaresDefaultValue && (containingDeclaration as CallableMemberDescriptor).kind.isReal
    }

    override fun substitute(substitutor: TypeSubstitutor): ValueParameterDescriptor {
        if (substitutor.isEmpty) return this
        throw UnsupportedOperationException() // TODO
    }


    override fun getOverriddenDescriptors(): Collection<ValueParameterDescriptor> {
        return containingDeclaration.overriddenDescriptors.map {
            it.valueParameters[index]
        }
    }


    override val containingDeclaration
        get() = super.containingDeclaration as CallableDescriptor

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {

        return visitor.visitValueParameterDescriptor(this, data!!)

    }


}
