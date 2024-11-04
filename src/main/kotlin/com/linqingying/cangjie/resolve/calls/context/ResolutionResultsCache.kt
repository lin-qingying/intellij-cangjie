package com.linqingying.cangjie.resolve.calls.context

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.resolve.DelegatingBindingTrace
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategy


interface ResolutionResultsCache {
    data class CachedData(
        val resolutionResults: OverloadResolutionResultsImpl<*>,
        val deferredComputation: BasicCallResolutionContext,
        val tracing: TracingStrategy,
        val resolutionTrace: DelegatingBindingTrace
    )

    fun record(
        call: Call,
        results: OverloadResolutionResultsImpl<*>,
        deferredComputation: BasicCallResolutionContext,
        tracing: TracingStrategy,
        resolutionTrace: DelegatingBindingTrace
    )

    operator fun get(call: Call): CachedData?
}

class ResolutionResultsCacheImpl : ResolutionResultsCache {
    private val data = HashMap<Call, ResolutionResultsCache.CachedData>()

    override fun record(
        call: Call,
        results: OverloadResolutionResultsImpl<out CallableDescriptor?>,
        deferredComputation: BasicCallResolutionContext,
        tracing: TracingStrategy,
        resolutionTrace: DelegatingBindingTrace
    ) {
        data[call] = ResolutionResultsCache.CachedData(results, deferredComputation, tracing, resolutionTrace)
    }

    override fun get(call: Call): ResolutionResultsCache.CachedData? = data[call]
    fun clear() {
        data.clear()
    }

    fun addData(cache: ResolutionResultsCacheImpl) {
        data.putAll(cache.data)
    }
}


class TemporaryResolutionResultsCache(private val parentCache: ResolutionResultsCache) : ResolutionResultsCache {
    private val innerCache = ResolutionResultsCacheImpl()

    override fun record(
        call: Call,
        results: OverloadResolutionResultsImpl<out CallableDescriptor?>,
        deferredComputation: BasicCallResolutionContext,
        tracing: TracingStrategy,
        resolutionTrace: DelegatingBindingTrace
    ) {

        innerCache.record(call, results, deferredComputation, tracing, resolutionTrace)
    }
//
//    override fun record(call: Call, resolutionTrace: DelegatingBindingTrace) {
//        TODO("Not yet implemented")
//    }

    override fun get(call: Call): ResolutionResultsCache.CachedData? = innerCache[call] ?: parentCache[call]
    fun clear() {
        when (parentCache) {
            is ResolutionResultsCacheImpl -> parentCache.clear()
            is TemporaryResolutionResultsCache -> parentCache.innerCache.clear()
        }
    }

    fun commit() {
        when (parentCache) {
            is ResolutionResultsCacheImpl -> parentCache.addData(innerCache)
            is TemporaryResolutionResultsCache -> parentCache.innerCache.addData(innerCache)
        }
    }
}
