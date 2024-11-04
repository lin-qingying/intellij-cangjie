package com.linqingying.cangjie.resolve.calls.inference.components

import com.linqingying.cangjie.resolve.calls.inference.model.VariableWithConstraints
import com.linqingying.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import com.linqingying.cangjie.types.model.CangJieTypeMarker
import com.linqingying.cangjie.types.model.TypeConstructorMarker
import com.linqingying.cangjie.utils.SmartSet

class TypeVariableDependencyInformationProvider(
    private val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>,
    private val postponedCjPrimitives: List<PostponedResolvedAtomMarker>,
    private val topLevelType: CangJieTypeMarker?,
    private val typeSystemContext: VariableFixationFinder.Context
)
{
    private val relatedToTopLevelType: MutableSet<TypeConstructorMarker> = hashSetOf()
    private val postponeArgumentsEdges: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()
    private val outerTypeVariables: Set<TypeConstructorMarker>? =
        typeSystemContext.outerTypeVariables
    private val relatedToAllOutputTypes: MutableSet<TypeConstructorMarker> = hashSetOf()
    /*
     * Not oriented edges
     * TypeVariable(A) has UPPER(TypeVariable(B)) => A and B are related shallowly
     */
    private val shallowTypeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()

    private val deepTypeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> = hashMapOf()
    fun isVariableRelatedToTopLevelType(variable: TypeConstructorMarker) =
        relatedToTopLevelType.contains(variable)

    init {
        computeConstraintEdges()
        computePostponeArgumentsEdges()
        computeRelatedToAllOutputTypes()
        computeRelatedToTopLevelType()
    }

    fun areVariablesDependentShallowly(a: TypeConstructorMarker, b: TypeConstructorMarker): Boolean {
        if (a == b) return true

        val shallowDependencies = shallowTypeVariableDependencies[a] ?: return false

        return shallowDependencies.any { it == b } ||
                shallowTypeVariableDependencies.values.any { dependencies -> a in dependencies && b in dependencies }
    }
    private fun computePostponeArgumentsEdges() {
        fun addPostponeArgumentsEdges(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            postponeArgumentsEdges.getOrPut(from) { hashSetOf() }.add(to)
        }

        for (argument in postponedCjPrimitives) {
            if (argument.analyzed) continue

            val typeVariablesInOutputType = SmartSet.create<TypeConstructorMarker>()
            (argument.outputType ?: continue).forAllMyTypeVariables { typeVariablesInOutputType.add(it) }
            if (typeVariablesInOutputType.isEmpty()) continue

            for (inputType in argument.inputTypes) {
                inputType.forAllMyTypeVariables { from ->
                    for (to in typeVariablesInOutputType) {
                        addPostponeArgumentsEdges(from, to)
                    }
                }
            }
        }
    }

    fun getShallowlyDependentVariables(variable: TypeConstructorMarker) = shallowTypeVariableDependencies[variable]
    private fun computeConstraintEdges() {
        fun addConstraintEdgeForDeepDependency(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            deepTypeVariableDependencies.getOrPut(from) { linkedSetOf() }.add(to)
            deepTypeVariableDependencies.getOrPut(to) { linkedSetOf() }.add(from)
        }

        fun addConstraintEdgeForShallowDependency(from: TypeConstructorMarker, to: TypeConstructorMarker) {
            shallowTypeVariableDependencies.getOrPut(from) { linkedSetOf() }.add(to)
            shallowTypeVariableDependencies.getOrPut(to) { linkedSetOf() }.add(from)
        }

        for (variableWithConstraints in notFixedTypeVariables.values) {
            val from = variableWithConstraints.typeVariable.freshTypeConstructor(typeSystemContext)

            for (constraint in variableWithConstraints.constraints) {
                val constraintTypeConstructor = constraint.type.typeConstructor(typeSystemContext)

                constraint.type.forAllMyTypeVariables {
                    if (isMyTypeVariable(it)) {
                        addConstraintEdgeForDeepDependency(from, it)
                    }
                }
                if (isMyTypeVariable(constraintTypeConstructor)) {
                    addConstraintEdgeForShallowDependency(from, constraintTypeConstructor)
                }
            }
        }
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
        for (argument in postponedCjPrimitives) {
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
