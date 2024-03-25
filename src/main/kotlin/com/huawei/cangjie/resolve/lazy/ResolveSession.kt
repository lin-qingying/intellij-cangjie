package com.huawei.cangjie.resolve.lazy

import com.huawei.cangjie.context.GlobalContext
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.storage.LockBasedLazyResolveStorageManager

class ResolveSession(

    globalContext: GlobalContext,

    delegationTrace: BindingTrace,
) : CangJieCodeAnalyzer, LazyClassContext {

    private val trace: BindingTrace

    init {
        val lockBasedLazyResolveStorageManager =
            LockBasedLazyResolveStorageManager(globalContext.storageManager)

        this.trace = lockBasedLazyResolveStorageManager.createSafeTrace(delegationTrace)

    }


    override val bindingContext: BindingContext
        get() = trace.bindingContext
}