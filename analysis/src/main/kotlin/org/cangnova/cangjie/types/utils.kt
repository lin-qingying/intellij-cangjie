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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.findClassifier
import org.cangnova.cangjie.types.checker.ErrorTypesAreEqualToAnything
import org.cangnova.cangjie.utils.SmartSet
import org.cangnova.cangjie.utils.canBeReferencedViaImport
import kotlin.collections.containsAll
import kotlin.text.replace

/**
 * This is temporary hack for type intersector.
 *
 * It is almost save, because:
 *  - it running only if general algorithm is failed
 *  - returned type is subtype of all [types].
 *
 * But it is hack, because it can give unstable result, but it better than exception.
 */
internal fun hackForTypeIntersector(types: Collection<CangJieType>): CangJieType? {
    if (types.size < 2) return types.firstOrNull()

    return types.firstOrNull { candidate ->
        types.all {
            ErrorTypesAreEqualToAnything.isSubtypeOf(candidate, it)
        }
    }
}

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

private fun TypeProjection.fixTypeProjection(
    scope: LexicalScope?,
    checkTypeParameters: Boolean,
    allowIntersections: Boolean,

    ): TypeProjection? {
    if (!type.isResolvableInScope(scope, checkTypeParameters, allowIntersections)) return null
    if (type.arguments.isEmpty()) return this

    val resolvableArgs = type.arguments.filterTo(SmartSet.create()) { typeProjection ->
        typeProjection.type.isResolvableInScope(scope, checkTypeParameters, allowIntersections)
    }

    if (resolvableArgs.containsAll(type.arguments)) {

        type.asTypeProjection()
    }


    val newArguments = (type.arguments zip type.constructor.parameters).map { (arg, param) ->
        when {
            arg in resolvableArgs -> arg


            else -> return type.asTypeProjection()
        }
    }

    return type.replace(newArguments).asTypeProjection()
}

fun CangJieType.getResolvableApproximations(
    scope: LexicalScope?,
    checkTypeParameters: Boolean,
    allowIntersections: Boolean = false
): Sequence<CangJieType> {
    return (listOf(this) + TypeUtils.getAllSupertypes(this))
        .asSequence()
        .mapNotNull {
            it.asTypeProjection()
                .fixTypeProjection(scope, checkTypeParameters, allowIntersections)
                ?.type
        }
}