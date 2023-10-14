package com.huawei.cangjie.lang.core.resolve


import com.huawei.cangjie.lang.core.psi.CjLifetime


fun processLifetimeResolveVariants(lifetime: CjLifetime, processor: CjResolveProcessor): Boolean {
//    if (lifetime.isPredefined) return false
//    loop@ for (scope in lifetime.contexts) {
//        val lifetimeParameters = when (scope) {
//            is CjGenericDeclaration -> scope.lifetimeParameters
//            is CjWhereClause -> scope.wherePredList.mapNotNull { it.forLifetimes }.flatMap { it.lifetimeParameterList }
//            is CjForInType -> scope.forLifetimes.lifetimeParameterList
//            is CjPolybound -> scope.forLifetimes?.lifetimeParameterList.orEmpty()
//            else -> continue@loop
//        }
//        if (processor.processAll(lifetimeParameters, LIFETIMES)) return true
//    }

    return false
}
