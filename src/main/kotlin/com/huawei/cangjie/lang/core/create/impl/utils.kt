package com.huawei.cangjie.lang.core.create.impl

import com.huawei.cangjie.lang.core.create.Crate

fun Iterable<Crate.Dependency>.flattenTopSortedDeps(): LinkedHashSet<Crate> {
    val flatDeps = linkedSetOf<Crate>()

    for (dep in this) {
        for (flatDep in dep.crate.flatDependencies) {
            flatDeps += flatDep
        }
        flatDeps += dep.crate
    }

    return flatDeps
}
