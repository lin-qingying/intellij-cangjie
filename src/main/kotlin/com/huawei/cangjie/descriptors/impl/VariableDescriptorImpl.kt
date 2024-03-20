package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitution
import com.huawei.cangjie.types.TypeSubstitutor

class VariableDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    original: VariableDescriptor?,
    annotations: Annotations,
    private val myModality: Modality,
    private val myVisibility: DescriptorVisibility,
    isVar: Boolean,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    source: SourceElement,
    lateInit: Boolean,
    isConst: Boolean,
  private  val myIsExpect: Boolean,
    private val myIsActual: Boolean,
    private val myIsExternal: Boolean,
    isDelegated: Boolean

) : VariableDescriptorWithInitializerImpl(containingDeclaration, annotations, name, null, isVar, source),
    VariableDescriptor {

    val myOriginal: VariableDescriptor = original ?: this


    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): VariableDescriptor {

        return newCopyBuilder()
            .setOwner(newOwner!!)
            .setOriginal(null)
            .setModality(modality!!)
            .setVisibility(visibility!!)
            .setKind(kind!!)
            .setCopyOverrides(copyOverrides)
            .build()!!
    }

    override val original: VariableDescriptor
        get() = if (myOriginal === this) this else myOriginal.original

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
        return visitor.visitVariableDescriptor(this, data)

    }


    override fun getSource(): SourceElement {
        TODO("Not yet implemented")
    }

    override fun getVisibility(): DescriptorVisibility {
        return myVisibility

    }

    override fun getModality(): Modality {
        return myModality

    }

    override fun isExpect(): Boolean {
        return myIsExpect
    }

    override fun isActual(): Boolean {
        return myIsActual
    }

    class CopyConfiguration : CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
        override fun setOwner(owner: DeclarationDescriptor): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setModality(modality: Modality): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setVisibility(visibility: DescriptorVisibility): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setKind(kind: CallableMemberDescriptor.Kind): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setTypeParameters(parameters: MutableList<TypeParameterDescriptor>): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setSubstitution(substitution: TypeSubstitution): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setCopyOverrides(copyOverrides: Boolean): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setName(name: Name): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setOriginal(original: CallableMemberDescriptor?): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setPreserveSourceElement(): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun setReturnType(type: CangJieType): CallableMemberDescriptor.CopyBuilder<VariableDescriptor> {
            TODO("Not yet implemented")
        }

        override fun build(): VariableDescriptor? {
            TODO("Not yet implemented")
        }

    }

    override fun newCopyBuilder(): CopyConfiguration {
        return CopyConfiguration()
    }

    override fun isExternal(): Boolean {
        return myIsExternal
    }

    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor? {
        if (substitutor.isEmpty) {
            return this
        }
        return newCopyBuilder()
            .setSubstitution(substitutor.substitution)
            .setOriginal(original)
            .build()
    }
}