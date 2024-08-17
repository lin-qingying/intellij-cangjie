package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.TypeAliasDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.intellij.openapi.progress.ProgressManager

object ForceResolveUtil {

@JvmStatic
    fun forceResolveAllContents(scope: MemberScope) {
        forceResolveAllContents(
            DescriptorUtils.getAllDescriptors(
                scope
            )
        )
    }

    fun forceResolveAllContents(descriptors: Iterable<DeclarationDescriptor>) {
        for (descriptor in descriptors) {
            forceResolveAllContents(
                descriptor
            )
        }
    }

    private fun doForceResolveAllContents(any: Any) {
        ProgressManager.checkCanceled()

        if (any is  LazyEntity) {
            val lazyEntity:  LazyEntity =
                any as  LazyEntity
            lazyEntity.forceResolveAllContents()
        }
//        else if (any is WithDestructuringDeclaration) {
//            (any as WithDestructuringDeclaration).destructuringVariables
//        }
        else if (any is  CallableDescriptor) {
            val callableDescriptor:  CallableDescriptor =
                any
//            callableDescriptor.getContextReceiverParameters()
//                .forEach(Consumer< ReceiverParameterDescriptor> { p:  ReceiverParameterDescriptor ->
//                    forceResolveAllContents(
//                        p.getType()
//                    )
//                })
//            val parameter: ReceiverParameterDescriptor =
//                callableDescriptor.getExtensionReceiverParameter()
//            if (parameter != null) {
//                forceResolveAllContents(parameter.getType())
//            }
            for (parameterDescriptor in callableDescriptor.getValueParameters()) {
                forceResolveAllContents<ValueParameterDescriptor>(
                    parameterDescriptor
                )
            }
            for (typeParameterDescriptor in callableDescriptor.getTypeParameters()) {
                forceResolveAllContents(typeParameterDescriptor.getUpperBounds())
            }
//            forceResolveAllContents(callableDescriptor.getReturnType())
            forceResolveAllContents(callableDescriptor.annotations)
        } else if (any is TypeAliasDescriptor) {
            val typeAliasDescriptor:  TypeAliasDescriptor =
                any as  TypeAliasDescriptor
            forceResolveAllContents(typeAliasDescriptor.underlyingType)
        }
    }

    fun <T : Any> forceResolveAllContents(descriptor: T): T {
        doForceResolveAllContents(descriptor)
        return descriptor
    }
@JvmStatic
    fun forceResolveAllContents(annotations: Annotations) {
        doForceResolveAllContents(annotations)
        for (annotation in annotations) {
            doForceResolveAllContents(annotation)
        }
    }


}
