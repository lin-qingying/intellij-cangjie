package com.linqingying.cangjie.resolve.calls.context

import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.TemporaryBindingTrace

class TemporaryTraceAndCache(context: ResolutionContext<*>, debugName: String?, expression: CjExpression?) {
    @JvmField
    val trace: TemporaryBindingTrace =
        TemporaryBindingTrace.create(context.trace, debugName, expression)
    @JvmField
    val cache: TemporaryResolutionResultsCache =
        TemporaryResolutionResultsCache(context.resolutionResultsCache)

    /**
     * 提交当前事务到数据库  (这里的数据库并非sql，而是分析缓存数据中心)
     *
     * 本方法确保将当前事务的所有更改持久化到数据库中
     * 通过调用trace和cache的commit方法来完成这一过程
     */
    fun commit() {
        // 提交trace中的事务，确保所有操作被正确记录
        trace.commit()
        // 提交cache中的事务，以更新缓存数据到数据库
        cache.commit()
    }

    fun clear() {
        trace.clear()

        cache.clear()
    }

    companion object {
        @JvmStatic
        fun create(
            context: ResolutionContext<*>,
            debugName: String?,
            expression: CjExpression?
        ): TemporaryTraceAndCache {
            return TemporaryTraceAndCache(context, debugName, expression)
        }
    }
}
