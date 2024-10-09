package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitution
import com.huawei.cangjie.types.TypeSubstitutor


open class VariableDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    outType: CangJieType?,
    isVar: Boolean,
    source: SourceElement,
    visibility: DescriptorVisibility
) : VariableDescriptorWithInitializerImpl(containingDeclaration, Annotations.EMPTY, name, outType, isVar, source),
    CallableMemberDescriptor {
    override val original: VariableDescriptorImpl
        get() = super.original as VariableDescriptorImpl
    private var _visibility: DescriptorVisibility = visibility

    companion object {
        @JvmStatic
        fun create(
            containingDeclaration: DeclarationDescriptor,
            name: Name,
            visibility: DescriptorVisibility,
            isVar: Boolean,
            source: SourceElement
        ): VariableDescriptorImpl {
            return VariableDescriptorImpl(containingDeclaration, name, null, isVar, source, visibility)
        }
    }

    private var overriddenProperties: Collection<VariableDescriptorImpl>? = null

    override fun getOverriddenDescriptors(): Collection<VariableDescriptorImpl> {
        return overriddenProperties ?: emptyList()
    }

    override fun getModality(): Modality {
        return Modality.FINAL
    }

    override fun getKind(): CallableMemberDescriptor.Kind {
        return CallableMemberDescriptor.Kind.DECLARATION
    }

    override fun setOverriddenDescriptors(overriddenDescriptors: MutableCollection<out CallableMemberDescriptor>) {

    }

    override fun <V : Any?> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? {
        return null
    }

    override fun copy(
        newOwner: DeclarationDescriptor?,
        modality: Modality?,
        visibility: DescriptorVisibility?,
        kind: CallableMemberDescriptor.Kind?,
        copyOverrides: Boolean
    ): CallableMemberDescriptor {
        return this
    }

    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out CallableMemberDescriptor> {
        return object : CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
            override fun setOwner(owner: DeclarationDescriptor): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this
            }

            override fun setModality(modality: Modality): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setVisibility(visibility: DescriptorVisibility): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setKind(kind: CallableMemberDescriptor.Kind): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setTypeParameters(parameters: MutableList<TypeParameterDescriptor>): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setSubstitution(substitution: TypeSubstitution): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setCopyOverrides(copyOverrides: Boolean): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setName(name: Name): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setOriginal(original: CallableMemberDescriptor?): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setPreserveSourceElement(): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun setReturnType(type: CangJieType): CallableMemberDescriptor.CopyBuilder<CallableMemberDescriptor> {
                return this

            }

            override fun build(): CallableMemberDescriptor {
                return this@VariableDescriptorImpl
            }

        }
    }


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitVariableDescriptor(this, data)

    }

    override var visibility: DescriptorVisibility = _visibility


    override fun substitute(substitutor: TypeSubstitutor): VariableDescriptor? {
        return null
    }


}
