package com.linqingying.cangjie.ide.cache

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager


fun <T> Module.cacheByClassInvalidatingOnRootModifications(classForKey: Class<*>, provider: () -> T): T {
    return cacheByClass(classForKey, ProjectRootModificationTracker.getInstance(project), provider = provider)
}

fun <T> Module.cacheByClass(classForKey: Class<*>, vararg dependencies: Any, provider: () -> T): T {
    return CachedValuesManager.getManager(project).cache(this, dependencies, classForKey, provider)
}

private fun <T> CachedValuesManager.cache(
    holder: UserDataHolder,
    dependencies: Array<out Any>,
    classForKey: Class<*>,
    provider: () -> T
): T {
    return getCachedValue(
        holder,
        getKeyForClass(classForKey),
        { CachedValueProvider.Result.create(provider(), *dependencies) },
        false
    )
}
