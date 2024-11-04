package com.linqingying.cangjie.resolve.scopes.util

import com.linqingying.cangjie.resolve.scopes.HierarchicalScope


val HierarchicalScope.parentsWithSelf: Sequence<HierarchicalScope>
    get() = generateSequence(this) { it.parent }
val HierarchicalScope.parents: Sequence<HierarchicalScope>
    get() = parentsWithSelf.drop(1)
