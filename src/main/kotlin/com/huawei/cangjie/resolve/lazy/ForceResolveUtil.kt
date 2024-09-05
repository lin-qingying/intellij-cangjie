package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.TypeAliasDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.asFlexibleType
import com.huawei.cangjie.types.isFlexible
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

        if (any is LazyEntity) {
            val lazyEntity: LazyEntity =
                any
            lazyEntity.forceResolveAllContents()
        }
//        else if (any is WithDestructuringDeclaration) {
//            (any as WithDestructuringDeclaration).destructuringVariables
//        }
        else if (any is CallableDescriptor) {


            any.getContextReceiverParameters()
                .forEach {
                    forceResolveAllContents(it.getType())
                }
            val parameter = any.getExtensionReceiverParameter()
            if (parameter != null) {
                forceResolveAllContents(parameter.getType())
            }
            for (parameterDescriptor in any.getValueParameters()) {
                forceResolveAllContents<ValueParameterDescriptor>(
                    parameterDescriptor
                )
            }
            for (typeParameterDescriptor in any.getTypeParameters()) {
                forceResolveAllContents(typeParameterDescriptor.getUpperBounds())
            }
            forceResolveAllContents(any.getReturnType())
            forceResolveAllContents(any.annotations)
        } else if (any is TypeAliasDescriptor) {
            val typeAliasDescriptor: TypeAliasDescriptor =
                any
            forceResolveAllContents(typeAliasDescriptor.underlyingType)
        }
    }

    fun <T : Any> forceResolveAllContents(descriptor: T): T {
        doForceResolveAllContents(descriptor)
        return descriptor
    }

    fun forceResolveAllContents(typeConstructor: TypeConstructor) {
        doForceResolveAllContents(typeConstructor)
    }
@JvmStatic
    fun forceResolveAllContents(type: CangJieType?): CangJieType? {
        if (type == null) return null

        forceResolveAllContents(type.annotations)
        if (type.isFlexible()) {
            forceResolveAllContents(type.asFlexibleType().lowerBound)
            forceResolveAllContents(type.asFlexibleType().upperBound)
        } else {
            forceResolveAllContents(type.constructor)
            for (projection in type.arguments) {
                if (!projection.isStarProjection()) {
                    forceResolveAllContents(projection.getType())
                }
            }
        }
        return type
    }

    @JvmStatic
    fun forceResolveAllContents(annotations: Annotations) {
        doForceResolveAllContents(annotations)
        for (annotation in annotations) {
            doForceResolveAllContents(annotation)
        }
    }


}
