package com.linqingying.cangjie.types.checker

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.renderer.DescriptorRenderer
import com.linqingying.cangjie.resolve.calls.inference.wrapWithCapturingSubstitution
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeConstructor
import com.linqingying.cangjie.types.TypeConstructorSubstitution
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.types.typesApproximation.approximateCapturedTypes
import com.linqingying.cangjie.types.util.TypeUtils
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
            var isAnyMarkedNullable = currentSubtype.isMarkedOption

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

                isAnyMarkedNullable = isAnyMarkedNullable || currentType.isMarkedOption

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
