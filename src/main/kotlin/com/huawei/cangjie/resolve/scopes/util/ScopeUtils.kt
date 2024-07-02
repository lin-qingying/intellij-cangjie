package com.huawei.cangjie.resolve.scopes.util

import com.huawei.cangjie.resolve.scopes.HierarchicalScope


val HierarchicalScope.parentsWithSelf: Sequence<HierarchicalScope>
    get() = generateSequence(this) { it.parent }
