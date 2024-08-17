package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor
import java.util.*
//
//open class VariableDescriptorImpl(
//    containingDeclaration: DeclarationDescriptor,
//    original: VariableDescriptor?,
//    annotations: Annotations,
//    modality: Modality,
//    visibility: DescriptorVisibility,
//    isVar: Boolean,
//    name: Name,
//    kind: CallableMemberDescriptor.Kind,
//    source: SourceElement
//) : VariableDescriptorWithInitializerImpl(
//    containingDeclaration,
//    annotations,
//    name,
//    null,
//    isVar,
//    source
//) {
//    private var visibility:  DescriptorVisibility? = null
//
//    fun setVisibility(visibility:  DescriptorVisibility) {
//        this.visibility = visibility
//    }
//
//    companion object {
//        @JvmStatic
//        fun create(
//            containingDeclaration: DeclarationDescriptor,
//            annotations: Annotations,
//            modality: Modality,
//            visibility: DescriptorVisibility,
//            isVar: Boolean,
//            name: Name,
//            kind: CallableMemberDescriptor.Kind,
//            source: SourceElement //            bool lateInit,
//            //            bool isConst,
//            //            bool isExpect,
//            //            bool isActual,
//            //            bool isExternal,
//            //            boolean isDelegated
//        ): VariableDescriptorImpl {
//            return VariableDescriptorImpl(
//                containingDeclaration, null, annotations,
//                modality, visibility, isVar, name, kind, source //                , lateInit, isConst,
//                //                isExpect, isActual, isExternal
//                //                , isDelegated
//            )
//        }
//    }
//
//
//    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
//        return visitor.visitVariableDescriptor(this, data)
//
//    }
//
//    override fun substitute(substitutor: TypeSubstitutor): CallableDescriptor {
////        TODO("Not yet implemented")
//        return this
//    }
//
//    fun setType(
//        outType: CangJieType,
//        typeParameters: List<TypeParameterDescriptor?>,
//        dispatchReceiverParameter: ReceiverParameterDescriptor?,
//        extensionReceiverParameter: ReceiverParameterDescriptor?,
//        contextReceiverParameters: List<ReceiverParameterDescriptor?>
//    ) {
//        setOutType(outType)
//
////        this.typeParameters = ArrayList<TypeParameterDescriptor>(typeParameters)
//
////        this.extensionReceiverParameter = extensionReceiverParameter
////        this.dispatchReceiverParameter = dispatchReceiverParameter
////        this.contextReceiverParameters = contextReceiverParameters
//    }
//
//    override fun getOverriddenDescriptors(): MutableCollection<out CallableDescriptor> {
////        TODO("Not yet implemented")
//        return Collections.emptyList()
//    }
//
//
//}
