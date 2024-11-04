package com.linqingying.cangjie.container


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
