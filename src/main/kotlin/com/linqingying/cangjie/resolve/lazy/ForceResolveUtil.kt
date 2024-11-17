/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.TypeAliasDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeConstructor
import com.linqingying.cangjie.types.asFlexibleType
import com.linqingying.cangjie.types.isFlexible
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

                    forceResolveAllContents(projection.getType())

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
