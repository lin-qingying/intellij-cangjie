package com.huawei.cangjie.storage

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.descriptors.Diagnostics
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice
import com.huawei.cangjie.utils.slicedMap.WritableSlice
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly


class LockBasedLazyResolveStorageManager(private val storageManager: StorageManager) : StorageManager by storageManager,
    LazyResolveStorageManager {
    override fun <K : Any, V : Any> createSoftlyRetainedMemoizedFunction(compute: Function1<K, V>) =
        storageManager.createMemoizedFunction<K, V>(compute, ContainerUtil.createConcurrentSoftValueMap<K, Any>())

    override fun <K : Any, V : Any> createSoftlyRetainedMemoizedFunctionWithNullableValues(compute: Function1<K, V>) =
        storageManager.createMemoizedFunctionWithNullableValues<K, V>(
            compute,
            ContainerUtil.createConcurrentSoftValueMap<K, Any>()
        )


    private class LockProtectedContext(
        private val storageManager: StorageManager,
        private val context: BindingContext
    ) : BindingContext {
        override fun getType(expression: CjExpression): CangJieType? =
            storageManager.compute { context.getType(expression) }

        override fun getDiagnostics(): Diagnostics = storageManager.compute { context.diagnostics }

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K) =
            storageManager.compute { context.get<K, V>(slice, key) }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>) =
            storageManager.compute { context.getKeys<K, V>(slice) }

        override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
            storageManager.compute { context.addOwnDataTo(trace, commitDiagnostics) }
        }

        @TestOnly
        override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>) =
            storageManager.compute { context.getSliceContents<K, V>(slice) }
    }

    private class LockProtectedTrace(private val storageManager: StorageManager, private val trace: BindingTrace) :
        BindingTrace {

        private val context: BindingContext = LockProtectedContext(storageManager, trace.bindingContext)

        override val bindingContext: BindingContext
            get() = context

        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> =
            storageManager.compute { trace.getKeys<K, V>(slice) }

        override fun getType(expression: CjExpression): CangJieType? =
            storageManager.compute { context.getType(expression) }


        override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
            storageManager.compute { trace.record<K, V>(slice, key, value) }
        }

        override fun <K> record(slice: WritableSlice<K, Boolean>, key: K) {
            storageManager.compute { trace.record<K>(slice, key) }
        }

        override fun recordType(expression:CjExpression, type: CangJieType?) {
            storageManager.compute { trace.recordType(expression, type) }
        }

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? =
            storageManager.compute { trace.get<K, V>(slice, key) }


        override fun report(diagnostic: Diagnostic) {
            storageManager.compute { trace.report(diagnostic) }
        }

        override fun wantsDiagnostics() = trace.wantsDiagnostics()
        override fun toString(): String {
            return "Lock-protected trace of LockBasedLazyResolveStorageManager $storageManager"
        }
    }

    override fun createSafeTrace(originalTrace: BindingTrace): BindingTrace =
        LockProtectedTrace(storageManager, originalTrace)

}
