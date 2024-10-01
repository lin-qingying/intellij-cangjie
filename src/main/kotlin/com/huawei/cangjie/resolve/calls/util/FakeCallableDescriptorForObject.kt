package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.resolve.descriptorUtil.classValueType
import com.huawei.cangjie.resolve.descriptorUtil.getClassObjectReferenceTarget
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor
import java.util.*

open class FakeCallableDescriptorForObject(
    val classDescriptor: ClassDescriptor,
) : DeclarationDescriptorWithVisibility by classDescriptor.getClassObjectReferenceTarget(), VariableDescriptor {
    //
//    init {
//        assert(classDescriptor.hasClassValueDescriptor) {
//            "FakeCallableDescriptorForObject can be created only for objects, classes with companion object or enum entries: $classDescriptor"
//        }
//
//    }
    open fun getReferencedDescriptor(): ClassifierDescriptorWithTypeParameters =
        classDescriptor.getClassObjectReferenceTarget()

    fun getReferencedObject(): ClassDescriptor = classDescriptor.getClassObjectReferenceTarget()

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> = emptyList()

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null
    override fun hasSynthesizedParameterNames() = false


    override fun getTypeParameters(): List<TypeParameterDescriptor> = Collections.emptyList()

    override fun hasStableParameterNames() = false

    override fun getValueParameters(): List<ValueParameterDescriptor> = Collections.emptyList()

    override fun getReturnType(): CangJieType? = type

//    override fun hasSynthesizedParameterNames() = false
//
//    override fun hasStableParameterNames() = false

    override fun getOverriddenDescriptors(): Set<CallableDescriptor> = Collections.emptySet()

    override fun getType(): CangJieType = classDescriptor.classValueType!!


    override val original: CallableDescriptor
        get() = this


//

//
    override fun getCompileTimeInitializer() = null

    override fun cleanCompileTimeInitializerCache() {}

    override fun getSource(): SourceElement = classDescriptor.source
    override val isConst: Boolean = false
    override val isVar: Boolean = false

//    override fun isConst(): Boolean = false
//
//    override fun isLateInit(): Boolean = false

    override fun equals(other: Any?) =
        other is FakeCallableDescriptorForObject && classDescriptor == other.classDescriptor

    override fun hashCode() = classDescriptor.hashCode()
    override val containingDeclaration: DeclarationDescriptor
        get() = classDescriptor.getClassObjectReferenceTarget().containingDeclaration

    override fun getModality(): Modality {
        return Modality.FINAL
    }


    override fun substitute(substitutor: TypeSubstitutor) = this

//    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}
