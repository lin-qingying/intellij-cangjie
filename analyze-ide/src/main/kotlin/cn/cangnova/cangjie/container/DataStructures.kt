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

package cn.cangnova.cangjie.container


fun <T> topologicalSort(items: Iterable<T>, reverseOrder: Boolean = false, dependencies: (T) -> Iterable<T>): List<T> {
    val itemsInProgress = HashSet<T>()
    val completedItems = HashSet<T>()
    val result = ArrayList<T>()

    fun DfsVisit(item: T) {
        if (item in completedItems)
            return

        if (item in itemsInProgress)
            return //throw CycleInTopoSortException()

        itemsInProgress.add(item)

        for (dependency in dependencies(item)) {
            DfsVisit(dependency)
        }

        itemsInProgress.remove(item)
        completedItems.add(item)
        result.add(item)
    }

    for (item in items)
        DfsVisit(item)

    return result.apply { if (!reverseOrder) reverse() }
}

class CycleInTopoSortException : Exception()
