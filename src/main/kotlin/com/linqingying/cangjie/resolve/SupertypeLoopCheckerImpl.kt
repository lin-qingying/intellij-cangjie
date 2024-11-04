package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.SupertypeLoopChecker
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeConstructor
import com.linqingying.cangjie.utils.DFS
import com.intellij.util.SmartList


class SupertypeLoopCheckerImpl : SupertypeLoopChecker {
    override fun findLoopsInSupertypesAndDisconnect(
        currentTypeConstructor: TypeConstructor,
        superTypes: Collection<CangJieType>,
        neighbors: (TypeConstructor) -> Iterable<CangJieType>,
        reportLoop: (CangJieType) -> Unit
    ): Collection<CangJieType> {
        val graph = DFS.Neighbors<TypeConstructor> { node -> neighbors(node).map { it.constructor } }

        val superTypesToRemove = SmartList<CangJieType>()

        for (superType in superTypes) {
            if (isReachable(superType.constructor, currentTypeConstructor, graph)) {
                superTypesToRemove.add(superType)
                reportLoop(superType)

                currentTypeConstructor.declarationDescriptor?.let {

                }
            }
        }

        return if (superTypesToRemove.isEmpty()) superTypes else superTypes - superTypesToRemove
    }
}

private fun isReachable(
    from: TypeConstructor, to: TypeConstructor,
    neighbors: DFS.Neighbors<TypeConstructor>
): Boolean {
    var result = false
    DFS.dfs(listOf(from), neighbors, DFS.VisitedWithSet(), object : DFS.AbstractNodeHandler<TypeConstructor, Unit>() {
        override fun beforeChildren(current: TypeConstructor): Boolean {
            if (current == to) {
                result = true
                return false
            }
            return true
        }

        override fun result() = Unit
    })

    return result
}
