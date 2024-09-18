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

    public void commit() {

        trace.commit();
        cache.commit();
    }


}
