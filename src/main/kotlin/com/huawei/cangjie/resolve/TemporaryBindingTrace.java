package com.huawei.cangjie.resolve;


import com.huawei.cangjie.descriptors.BindingTrace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * TemporaryBindingTrace 类用于创建临时的绑定跟踪对象，它继承自 DelegatingBindingTrace 类，
 * 提供了一些静态方法来创建其实例，并实现了将临时跟踪的信息提交回原始跟踪的方法.
 */
public class TemporaryBindingTrace extends DelegatingBindingTrace {

    protected final BindingTrace trace;

    /**
     * 构造函数，用于初始化 TemporaryBindingTrace 实例.
     *
     * @param trace     原始的 BindingTrace 对象
     * @param debugName 调试名称
     * @param filter    绑定跟踪过滤器
     */
    protected TemporaryBindingTrace(@NotNull BindingTrace trace, String debugName, BindingTraceFilter filter) {
        super(trace.getBindingContext(), debugName, true, filter, false, null);
        this.trace = trace;
    }

    /**
     * 创建一个 TemporaryBindingTrace 实例，使用默认的过滤器.
     *
     * @param trace     原始的 BindingTrace 对象
     * @param debugName 调试名称
     * @return 创建的 TemporaryBindingTrace 实例
     */
    @NotNull
    public static TemporaryBindingTrace create(@NotNull BindingTrace trace, String debugName) {
        return create(trace, debugName, BindingTraceFilter.Companion.getACCEPT_ALL());
    }

    /**
     * 创建一个 TemporaryBindingTrace 实例，指定过滤器.
     *
     * @param trace     原始的 BindingTrace 对象
     * @param debugName 调试名称
     * @param filter    绑定跟踪过滤器
     * @return 创建的 TemporaryBindingTrace 实例
     */
    @NotNull
    public static TemporaryBindingTrace create(@NotNull BindingTrace trace, String debugName, BindingTraceFilter filter) {
        return new TemporaryBindingTrace(trace, debugName, filter);
    }

    /**
     * 创建一个 TemporaryBindingTrace 实例，使用特定的解析主题来生成调试名称.
     *
     * @param trace                       原始的 BindingTrace 对象
     * @param debugName                   调试名称
     * @param resolutionSubjectForMessage 用于生成调试名称的解析主题
     * @return 创建的 TemporaryBindingTrace 实例
     */
    @NotNull
    public static TemporaryBindingTrace create(@NotNull BindingTrace trace, String debugName, @Nullable Object resolutionSubjectForMessage) {
        return create(trace, AnalyzingUtils.formDebugNameForBindingTrace(debugName, resolutionSubjectForMessage));
    }

    /**
     * 将临时跟踪的信息提交回原始跟踪，并清除临时跟踪.
     */
    public void commit() {
        addOwnDataTo(trace);
        clear();
    }

    /**
     * 将临时跟踪的信息根据指定的过滤器提交回原始跟踪，并选择是否提交诊断信息.
     *
     * @param filter            过滤器，用于筛选要提交的信息
     * @param commitDiagnostics 是否提交诊断信息
     */
    public void commit(@NotNull TraceEntryFilter filter, boolean commitDiagnostics) {
        addOwnDataTo(trace, filter, commitDiagnostics);
        clear();
    }

    /**
     * 判断是否需要诊断信息.
     *
     * @return 原始跟踪是否需要诊断信息
     */
    @Override
    public boolean wantsDiagnostics() {
        return trace.wantsDiagnostics();
    }
}
