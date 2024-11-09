package com.linqingying.cangjie.resolve.lazy

import com.linqingying.cangjie.resolve.BindingTraceFilter

/**
 * 枚举类BodyResolveMode定义了不同的代码解析模式
 * 每种模式决定了如何处理绑定追踪信息以及是否进行控制流分析
 *
 * @param bindingTraceFilter 绑定追踪过滤器，决定了哪些绑定信息应该被追踪
 * @param doControlFlowAnalysis 是否执行控制流分析
 * @param resolveAdditionals 是否解析附加信息，默认为true
 */
enum class BodyResolveMode(val bindingTraceFilter: BindingTraceFilter, val doControlFlowAnalysis: Boolean, val resolveAdditionals: Boolean = true) {

    /**
     * 完全解析模式，接受所有的绑定追踪信息，并进行控制流分析
     */
    FULL(BindingTraceFilter.ACCEPT_ALL, doControlFlowAnalysis = true),

    /**
     * 部分解析用于代码补全，不生成诊断信息，但进行控制流分析
     */
    PARTIAL_FOR_COMPLETION(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = true),

    /**
     * 部分解析带诊断信息，接受所有的绑定追踪信息，并进行控制流分析
     */
    PARTIAL_WITH_DIAGNOSTICS(BindingTraceFilter.ACCEPT_ALL, doControlFlowAnalysis = true),

    /**
     * 部分解析带有控制流分析，不生成诊断信息，但进行控制流分析
     */
    PARTIAL_WITH_CFA(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = true),

    /**
     * 部分解析模式，不生成诊断信息，也不进行控制流分析
     */
    PARTIAL(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = false),

    /**
     * 部分解析模式，不生成诊断信息，不进行控制流分析，也不解析附加信息
     */
    PARTIAL_NO_ADDITIONAL(BindingTraceFilter.NO_DIAGNOSTICS, doControlFlowAnalysis = false, resolveAdditionals = false)
    ;

    /**
     * 检查当前解析模式是否不劣于另一个解析模式
     *
     * @param other 另一个BodyResolveMode实例，用于比较
     * @return 如果当前模式不劣于其他模式，则返回true；否则返回false
     */
    fun doesNotLessThan(other: BodyResolveMode): Boolean {
        return this <= other && this.bindingTraceFilter.includesEverythingIn(other.bindingTraceFilter)
    }
}
