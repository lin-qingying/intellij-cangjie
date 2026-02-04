/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.annotations.FilteredAnnotations
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.findClassifier
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.utils.SmartSet
import org.cangnova.cangjie.utils.canBeReferencedViaImport


fun CangJieType.isResolvableInScope(
    scope: LexicalScope?,
    checkTypeParameters: Boolean,
    allowIntersections: Boolean = false
): Boolean {
    if (constructor is IntersectionTypeConstructor) {
        if (!allowIntersections) {
            return false
        }
        return constructor.supertypes.all {
            it.isResolvableInScope(
                scope,
                checkTypeParameters,
                allowIntersections = true
            )
        }
    }

    if (canBeReferencedViaImport()) return true

    val descriptor = constructor.declarationDescriptor
    if (descriptor == null || descriptor.name.isSpecial) return false
    if (!checkTypeParameters && descriptor is TypeParameterDescriptor) return true

    return scope != null && scope.findClassifier(descriptor.name, NoLookupLocation.FROM_IDE) == descriptor
}

private fun TypeArgument.fixTypeProjection(
    scope: LexicalScope?,
    checkTypeParameters: Boolean,
    allowIntersections: Boolean,

    ): TypeArgument? {
    if (!type.isResolvableInScope(scope, checkTypeParameters, allowIntersections)) return null
    if (type.arguments.isEmpty()) return this

    val resolvableArgs = type.arguments.filterTo(SmartSet.create()) { typeArgument ->
        typeArgument.type.isResolvableInScope(scope, checkTypeParameters, allowIntersections)
    }

    if (resolvableArgs.containsAll(type.arguments)) {
        return this
    }


    val newArguments = (type.arguments zip type.constructor.parameters).map { (arg, param) ->
        when {
            arg in resolvableArgs -> arg
            else -> return null
        }
    }

    return type.replace(newArguments).asTypeArgument()
}

fun CangJieType.isPrimitiveType(): Boolean {
    return CangJieBuiltIns.isPrimitiveType(this)
}

@JvmOverloads
fun CangJieType.replace(
    newArguments: List<TypeArgument> = arguments,
    newAnnotations: Annotations = annotations,
    newArgumentsForUpperBound: List<TypeArgument> = newArguments
): CangJieType {
    if ((newArguments.isEmpty() || newArguments === arguments) && newAnnotations === annotations) return this

    val newAttributes = attributes.replaceAnnotations(
        // Specially handle FilteredAnnotations here due to FilteredAnnotations.isEmpty()
        if (newAnnotations is FilteredAnnotations && newAnnotations.isEmpty()) Annotations.EMPTY else newAnnotations
    )

    return when (val unwrapped = unwrap()) {
        is FlexibleType -> CangJieTypeFactory.flexibleType(
            unwrapped.lowerBound.replace(newArguments, newAttributes),
            unwrapped.upperBound.replace(newArgumentsForUpperBound, newAttributes)
        )

        is SimpleType -> unwrapped.replace(newArguments, newAttributes)
    }
}

@JvmOverloads
fun SimpleType.replace(
    newArguments: List<TypeArgument> = arguments,
    newAttributes: TypeAttributes = attributes
): SimpleType {
    if (newArguments.isEmpty() && newAttributes === attributes) return this

    if (newArguments.isEmpty()) {
        return replaceAttributes(newAttributes)
    }

    if (this is ErrorType) {
        return replaceArguments(newArguments)
    }

    val baseType = CangJieTypeFactory.simpleType(
        newAttributes,
        constructor,
        newArguments
    )
    return if (this.isOption) baseType.makeOption() else baseType
}

fun CangJieType.getResolvableApproximations(
    scope: LexicalScope?,
    checkTypeParameters: Boolean,
    allowIntersections: Boolean = false
): Sequence<CangJieType> {
    return (listOf(this) + TypeUtils.getAllSupertypes(this))
        .asSequence()
        .mapNotNull {
            it.asTypeArgument()
                .fixTypeProjection(scope, checkTypeParameters, allowIntersections)
                ?.type
        }
}