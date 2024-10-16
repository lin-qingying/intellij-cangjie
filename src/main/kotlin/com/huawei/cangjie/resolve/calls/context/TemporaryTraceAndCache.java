package com.huawei.cangjie.resolve.calls.context;

import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.resolve.TemporaryBindingTrace;

public class TemporaryTraceAndCache {
    public final TemporaryBindingTrace trace;
    public final TemporaryResolutionResultsCache cache;

    public TemporaryTraceAndCache(ResolutionContext context, String debugName, CjExpression expression) {
        trace = TemporaryBindingTrace.create(context.trace, debugName, expression);
        cache = new TemporaryResolutionResultsCache(context.resolutionResultsCache);
    }

    public static TemporaryTraceAndCache create(ResolutionContext context, String debugName, CjExpression expression) {
        return new TemporaryTraceAndCache(context, debugName, expression);
    }

    /**
     * 提交当前事务到数据库  (这里的数据库并非sql，而是分析缓存数据中心)
     *
     * 本方法确保将当前事务的所有更改持久化到数据库中
     * 通过调用trace和cache的commit方法来完成这一过程
     */
    public void commit() {
        // 提交trace中的事务，确保所有操作被正确记录
        trace.commit();
        // 提交cache中的事务，以更新缓存数据到数据库
        cache.commit();
    }


}
