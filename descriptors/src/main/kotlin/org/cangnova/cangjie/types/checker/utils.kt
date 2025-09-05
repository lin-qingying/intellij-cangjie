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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.resolve.call.inference.wrapWithCapturingSubstitution
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeConstructorSubstitution
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.Variance
import org.cangnova.cangjie.types.approximateCapturedTypes
import java.util.ArrayDeque

interface NewTypeVariableConstructor : TypeConstructor {
    val originalTypeParameter: TypeParameterDescriptor?
}
private class SubtypePathNode(val type: CangJieType, val previous: SubtypePathNode?)
private fun CangJieType.approximate() = approximateCapturedTypes(this).upper

fun findCorrespondingSupertype(
    subtype: CangJieType, supertype: CangJieType,
    typeCheckingProcedureCallbacks: TypeCheckingProcedureCallbacks = TypeCheckerProcedureCallbacksImpl()
): CangJieType? {
    val queue = ArrayDeque<SubtypePathNode>()
    queue.add(SubtypePathNode(subtype, null))

    val supertypeConstructor = supertype.constructor

    while (!queue.isEmpty()) {
        val lastPathNode = queue.poll()
        val currentSubtype = lastPathNode.type
        val constructor = currentSubtype.constructor

        if (typeCheckingProcedureCallbacks.assertEqualTypeConstructors(constructor, supertypeConstructor)) {
            var substituted = currentSubtype
            var isAnyMarkedNullable = currentSubtype.isOption

            var currentPathNode = lastPathNode.previous

            while (currentPathNode != null) {
                val currentType = currentPathNode.type
                substituted = if (currentType.arguments.any { it.projectionKind != Variance.INVARIANT }) {
                    TypeConstructorSubstitution.create(currentType)
                        .wrapWithCapturingSubstitution().buildSubstitutor()
                        .safeSubstitute(substituted, Variance.INVARIANT)
                        .approximate()
                }
                else {
                    TypeConstructorSubstitution.create(currentType)
                        .buildSubstitutor()
                        .safeSubstitute(substituted, Variance.INVARIANT)
                }

                isAnyMarkedNullable = isAnyMarkedNullable || currentType.isOption

                currentPathNode = currentPathNode.previous
            }

            val substitutedConstructor = substituted.constructor
            if (!typeCheckingProcedureCallbacks.assertEqualTypeConstructors(substitutedConstructor, supertypeConstructor)) {
                throw AssertionError("Type constructors should be equals!\n" +
                        "substitutedSuperType: ${substitutedConstructor.debugInfo()}, \n\n" +
                        "supertype: ${supertypeConstructor.debugInfo()} \n" +
                        typeCheckingProcedureCallbacks.assertEqualTypeConstructors(substitutedConstructor, supertypeConstructor))
            }

            return TypeUtils.makeOptionalAsSpecified(substituted, isAnyMarkedNullable)
        }

        for (immediateSupertype in constructor.supertypes) {
            queue.add(SubtypePathNode(immediateSupertype, lastPathNode))
        }
    }

    return null
}
private fun TypeConstructor.debugInfo() = buildString {
    operator fun String.unaryPlus() = appendLine(this)

    + "type: ${this@debugInfo}"
    + "hashCode: ${this@debugInfo.hashCode()}"
    + "javaClass: ${this@debugInfo::class.java.canonicalName}"
    var declarationDescriptor: DeclarationDescriptor? = declarationDescriptor
    while (declarationDescriptor != null) {

        + "fqName: ${DescriptorRenderer.FQ_NAMES_IN_TYPES.render(declarationDescriptor)}"
        + "javaClass: ${declarationDescriptor::class.java.canonicalName}"

        declarationDescriptor = declarationDescriptor.containingDeclaration
    }
}
