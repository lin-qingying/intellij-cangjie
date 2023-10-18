package com.huawei.cangjie.lang.core.resolve.ref

import com.huawei.cangjie.lang.core.psi.cangjieStructureModificationTracker
import com.huawei.cangjie.lang.core.psi.ext.findModificationTrackerOwner
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.containers.ConcurrentWeakKeySoftValueHashMap
import com.intellij.util.containers.HashingStrategy
import java.lang.ref.ReferenceQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference


@Service
class CjResolveCache(project: Project) : Disposable {
    private val guard = RecursionManager.createGuard<PsiElement>("CjResolveCache")

    companion object {
        fun getInstance(project: Project): CjResolveCache =
            ServiceManager.getService(project, CjResolveCache::class.java)
    }

    override fun dispose() {

    }
    private val _anyPsiChangeDependentCache: AtomicReference<ConcurrentMap<PsiElement, Any>?> = AtomicReference(null)
    private val anyPsiChangeDependentCache: ConcurrentMap<PsiElement, Any>
        get() = _anyPsiChangeDependentCache.getOrCreateMap()

    private val _cangjieStructureDependentCache: AtomicReference<ConcurrentMap<PsiElement, Any>?> = AtomicReference(null)

    private val cangjieStructureDependentCache: ConcurrentMap<PsiElement, Any>
        get() = _cangjieStructureDependentCache.getOrCreateMap()

    private fun refineDependency(key: PsiElement, dep: ResolveCacheDependency): ResolveCacheDependency =
        when (key.containingFile.virtualFile) {

            null -> ResolveCacheDependency.ANY_PSI_CHANGE

            is VirtualFileWindow -> ResolveCacheDependency.ANY_PSI_CHANGE
            else -> dep
        }

    @Suppress("UNCHECKED_CAST")
    fun <K : PsiElement, V> resolveWithCaching(key: K, dep: ResolveCacheDependency, resolver: (K) -> V): V? {
        ProgressManager.checkCanceled()
        val refinedDep = refineDependency(key, dep)
        val map = getCacheFor(key, refinedDep)
        return map[key] as V? ?: run {
            val stamp = RecursionManager.markStack()
            val result = guard.doPreventingRecursion(key, true) { resolver(key) }
            ensureValidResult(result)

            if (stamp.mayCacheNow()) {
                cache(map, key, result)
            }
            result
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K : PsiElement, V> cache(map: ConcurrentMap<PsiElement, Any>, element: K, result: V?) {
        // optimization: less contention
        val cached = map[element] as V?
        if (cached !== null && cached === result) return
        map[element] = result ?: NULL_RESULT as V
    }

    private fun getCacheFor(element: PsiElement, dep: ResolveCacheDependency): ConcurrentMap<PsiElement, Any> {
        return when (dep) {
            ResolveCacheDependency.LOCAL, ResolveCacheDependency.LOCAL_AND_CANGJIE_STRUCTURE -> {
                val owner = element.findModificationTrackerOwner(strict = false)
                return if (owner != null) {
                    if (dep == ResolveCacheDependency.LOCAL) {
                        CachedValuesManager.getCachedValue(owner, LOCAL_CACHE_KEY) {
                            CachedValueProvider.Result.create(
                                createWeakMap(),
                                owner.modificationTracker
                            )
                        }

                    } else {
                        CachedValuesManager.getCachedValue(owner, LOCAL_CACHE_KEY2) {
                            CachedValueProvider.Result.create(
                                createWeakMap(),
                                owner.project.cangjieStructureModificationTracker,
                                owner.modificationTracker
                            )
                        }
                    }
                } else {
                    cangjieStructureDependentCache
                }

            }

            ResolveCacheDependency.CANGJIE_STRUCTURE -> cangjieStructureDependentCache
            ResolveCacheDependency.ANY_PSI_CHANGE -> anyPsiChangeDependentCache
        }
    }

}

enum class ResolveCacheDependency {

    LOCAL,


    CANGJIE_STRUCTURE,


    LOCAL_AND_CANGJIE_STRUCTURE,


    ANY_PSI_CHANGE,
}
private val NULL_RESULT = Any()
private val NULL_VALUE_REFERENCE = StrongValueReference<Any, Any>(NULL_RESULT)
private val EMPTY_RESOLVE_RESULT = StrongValueReference<Any, Array<ResolveResult>>(ResolveResult.EMPTY_ARRAY)
private val EMPTY_LIST = StrongValueReference<Any, List<Any>>(emptyList())

private val LOCAL_CACHE_KEY: Key<CachedValue<ConcurrentMap<PsiElement, Any>>> = Key.create("LOCAL_CACHE_KEY")
private val LOCAL_CACHE_KEY2: Key<CachedValue<ConcurrentMap<PsiElement, Any>>> = Key.create("LOCAL_CACHE_KEY2")

private fun ensureValidPsi(resolveResult: ResolveResult) {
    val element = resolveResult.element
    if (element != null) {
        PsiUtilCore.ensureValid(element)
    }
}

private fun ensureValidResults(result: Array<*>) =
    result.forEach { ensureValidResult(it) }

private fun ensureValidResults(result: List<*>) =
    result.forEach { ensureValidResult(it) }


private fun ensureValidResult(result: Any?): Unit = when (result) {
    is ResolveResult -> ensureValidPsi(result)
    is Array<*> -> ensureValidResults(result)
    is List<*> -> ensureValidResults(result)
    is PsiElement -> PsiUtilCore.ensureValid(result)
    else -> Unit
}

@Suppress("UnstableApiUsage")
private class StrongValueReference<K, V>(
    private val value: V
) : ConcurrentWeakKeySoftValueHashMap.ValueReference<K, V> {
    override fun getKeyReference(): ConcurrentWeakKeySoftValueHashMap.KeyReference<K, V> {
        // will never GC so this method will never be called so no implementation is necessary
        throw UnsupportedOperationException()
    }

    override fun get(): V = value
}

@Suppress("UNCHECKED_CAST")
private fun <K, V> createStrongReference(value: V): StrongValueReference<K, V> {
    return when {
        value === NULL_RESULT -> NULL_VALUE_REFERENCE as StrongValueReference<K, V>
        value === ResolveResult.EMPTY_ARRAY -> EMPTY_RESOLVE_RESULT as StrongValueReference<K, V>
        value is List<*> && value.size == 0 -> EMPTY_LIST as StrongValueReference<K, V>
        else -> StrongValueReference(value)
    }
}
private fun <K: Any, V: Any> createWeakMap(): ConcurrentMap<K, V> {
    @Suppress("UnstableApiUsage")
    return object : ConcurrentWeakKeySoftValueHashMap<K, V>(
        100,
        0.75f,
        Runtime.getRuntime().availableProcessors(),
        HashingStrategy.canonical()
    ) {
        override fun createValueReference(
            value: V,
            queue: ReferenceQueue<in V>
        ): ValueReference<K, V> {
            val isTrivialValue = value === NULL_RESULT ||
                    value is Array<*> && value.size == 0 ||
                    value is List<*> && value.size == 0
            return if (isTrivialValue) {
                createStrongReference(value)
            } else {
                super.createValueReference(value, queue)
            }
        }

        override fun get(key: K): V? {
            val v = super.get(key)
            return if (v === NULL_RESULT) null else v
        }
    }
}
private fun AtomicReference<ConcurrentMap<PsiElement, Any>?>.getOrCreateMap(): ConcurrentMap<PsiElement, Any> {
    while (true) {
        get()?.let { return it }
        val map = createWeakMap<PsiElement, Any>()
        if (compareAndSet(null, map)) return map
    }
}
