/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve.lazy

import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.asFlexibleType
import org.cangnova.cangjie.types.isFlexible

object ForceResolveUtil {


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

        when (any) {
            is LazyEntity -> {
                val lazyEntity: LazyEntity =
                    any
                lazyEntity.forceResolveAllContents()
            }
            //        else if (any is WithDestructuringDeclaration) {
            //            (any as WithDestructuringDeclaration).destructuringVariables
            //        }
            is CallableDescriptor -> {


                any.contextReceiverParameters
                    .forEach {
                        forceResolveAllContents(it.type)
                    }
                val parameter = any.extensionReceiverParameter
                if (parameter != null) {
                    forceResolveAllContents(parameter.type)
                }
                for (parameterDescriptor in any.valueParameters) {
                    forceResolveAllContents(
                        parameterDescriptor
                    )
                }
                for (typeParameterDescriptor in any.typeParameters) {
                    forceResolveAllContents(typeParameterDescriptor.upperBounds)
                }
                forceResolveAllContents(any.returnType)
                forceResolveAllContents(any.annotations)
            }

            is TypeAliasDescriptor -> {
                val typeAliasDescriptor: TypeAliasDescriptor =
                    any
                forceResolveAllContents(typeAliasDescriptor.underlyingType)
            }
        }
    }

    fun <T : Any> forceResolveAllContents(descriptor: T): T {
        doForceResolveAllContents(descriptor)
        return descriptor
    }

    fun forceResolveAllContents(typeConstructor: TypeConstructor) {
        doForceResolveAllContents(typeConstructor)
    }

    fun forceResolveAllContents(type: CangJieType?): CangJieType? {
        if (type == null || type == NO_EXPECTED_TYPE) return null

        forceResolveAllContents(type.annotations)
        if (type.isFlexible()) {
            forceResolveAllContents(type.asFlexibleType().lowerBound)
            forceResolveAllContents(type.asFlexibleType().upperBound)
        } else {
            forceResolveAllContents(type.constructor)
            for (projection in type.arguments) {

                forceResolveAllContents(projection.type)

            }
        }
        return type
    }


    fun forceResolveAllContents(annotations: Annotations) {
        doForceResolveAllContents(annotations)
        for (annotation in annotations) {
            doForceResolveAllContents(annotation)
        }
    }


}
