package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.huawei.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import com.huawei.cangjie.types.model.CangJieTypeMarker
import com.huawei.cangjie.types.model.TypeConstructorMarker

class TypeVariableDependencyInformationProvider(
    private val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>,
    private val postponedKtPrimitives: List<PostponedResolvedAtomMarker>,
    private val topLevelType: CangJieTypeMarker?,
    private val typeSystemContext: VariableFixationFinder.Context
)
{
    private val relatedToTopLevelType: MutableSet<TypeConstructorMarker> = hashSetOf()
    private val postponeArgumentsEdges: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()
    private val outerTypeVariables: Set<TypeConstructorMarker>? =
        typeSystemContext.outerTypeVariables
    private val relatedToAllOutputTypes: MutableSet<TypeConstructorMarker> = hashSetOf()

    private val deepTypeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()
    fun isVariableRelatedToTopLevelType(variable: TypeConstructorMarker) =
        relatedToTopLevelType.contains(variable)

    init {
//        computeConstraintEdges()
//        computePostponeArgumentsEdges()
        computeRelatedToAllOutputTypes()
        computeRelatedToTopLevelType()
    }

    private fun computeRelatedToTopLevelType() {
        if (topLevelType == null) return
        topLevelType.forAllMyTypeVariables {
            addAllRelatedNodes(relatedToTopLevelType, it, includePostponedEdges = true)
        }
    }
    fun isVariableRelatedToAnyOutputType(variable: TypeConstructorMarker) = relatedToAllOutputTypes.contains(variable)

    fun isRelatedToOuterTypeVariable(variable: TypeConstructorMarker): Boolean {
        val outerTypeVariables = outerTypeVariables ?: return false
        val myDependent = getDeeplyDependentVariables(variable) ?: return false
        return myDependent.any { it in outerTypeVariables }
    }
    private fun computeRelatedToAllOutputTypes() {
        for (argument in postponedKtPrimitives) {
            if (argument.analyzed) continue
            (argument.outputType ?: continue).forAllMyTypeVariables {
                addAllRelatedNodes(relatedToAllOutputTypes, it, includePostponedEdges = false)
            }
        }
    }
    private fun getConstraintEdges(from: TypeConstructorMarker): Set<TypeConstructorMarker> = deepTypeVariableDependencies[from] ?: emptySet()
    private fun getPostponeEdges(from: TypeConstructorMarker): Set<TypeConstructorMarker> = postponeArgumentsEdges[from] ?: emptySet()

    private fun addAllRelatedNodes(to: MutableSet<TypeConstructorMarker>, node: TypeConstructorMarker, includePostponedEdges: Boolean) {
        if (to.add(node)) {
            for (relatedNode in getConstraintEdges(node)) {
                addAllRelatedNodes(to, relatedNode, includePostponedEdges)
            }
            if (includePostponedEdges) {
                for (relatedNode in getPostponeEdges(node)) {
                    addAllRelatedNodes(to, relatedNode, includePostponedEdges)
                }
            }
        }
    }
    private fun isMyTypeVariable(typeConstructor: TypeConstructorMarker) = notFixedTypeVariables.containsKey(typeConstructor)

    private fun CangJieTypeMarker.forAllMyTypeVariables(action: (TypeConstructorMarker) -> Unit) =
    with(typeSystemContext) {
        contains {
            val typeConstructor = it.typeConstructor()
            if (isMyTypeVariable(typeConstructor)) action(typeConstructor)
            false
        }
    }


    fun getDeeplyDependentVariables(variable: TypeConstructorMarker) = deepTypeVariableDependencies[variable]

}
