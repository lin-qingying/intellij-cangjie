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

package org.cangnova.cangjie.resolve.calls.context

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.psi.Call
import org.cangnova.cangjie.resolve.binding.DelegatingBindingTrace
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import org.cangnova.cangjie.resolve.calls.tasks.TracingStrategy


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
        results: OverloadResolutionResultsImpl<out CallableDescriptor>,
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
        results: OverloadResolutionResultsImpl<out CallableDescriptor>,
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
