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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.SupertypeLoopChecker
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.utils.DFS
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
