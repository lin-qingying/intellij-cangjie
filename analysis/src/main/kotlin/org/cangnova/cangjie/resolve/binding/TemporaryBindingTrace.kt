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

package org.cangnova.cangjie.resolve.binding

import org.cangnova.cangjie.analysis.AnalyzingUtils

/**
 * TemporaryBindingTrace 类用于创建临时的绑定跟踪对象，它继承自 DelegatingBindingTrace 类，
 * 提供了一些静态方法来创建其实例，并实现了将临时跟踪的信息提交回原始跟踪的方法.
 *
 * @property trace 原始的 BindingTrace 对象
 */
open class TemporaryBindingTrace protected constructor(
    protected val trace: BindingTrace,
    debugName: String,
    filter: BindingTraceFilter
) : DelegatingBindingTrace(
    parentContext = trace.bindingContext,
    name = debugName,
    withParentDiagnostics = true,
    filter = filter,
    allowSliceRewrite = false,
    customSuppressCache = null
) {

    /**
     * 将临时跟踪的信息提交回原始跟踪，并清除临时跟踪.
     */
    fun commit() {
        addOwnDataTo(trace)
        clear()
    }

    /**
     * 将临时跟踪的信息根据指定的过滤器提交回原始跟踪，并选择是否提交诊断信息.
     *
     * @param filter 过滤器，用于筛选要提交的信息
     * @param commitDiagnostics 是否提交诊断信息
     */
    fun commit(filter: TraceEntryFilter, commitDiagnostics: Boolean) {
        addOwnDataTo(trace, filter, commitDiagnostics)
        clear()
    }

    /**
     * 判断是否需要诊断信息.
     *
     * @return 原始跟踪是否需要诊断信息
     */
    override fun wantsDiagnostics(): Boolean = trace.wantsDiagnostics()

    companion object {
        /**
         * 创建一个 TemporaryBindingTrace 实例，使用默认的过滤器.
         *
         * @param trace 原始的 BindingTrace 对象
         * @param debugName 调试名称
         * @return 创建的 TemporaryBindingTrace 实例
         */
        @JvmStatic
        fun create(trace: BindingTrace, debugName: String): TemporaryBindingTrace {
            return create(trace, debugName, BindingTraceFilter.ACCEPT_ALL)
        }

        /**
         * 创建一个 TemporaryBindingTrace 实例，指定过滤器.
         *
         * @param trace 原始的 BindingTrace 对象
         * @param debugName 调试名称
         * @param filter 绑定跟踪过滤器
         * @return 创建的 TemporaryBindingTrace 实例
         */
        @JvmStatic
        fun create(trace: BindingTrace, debugName: String, filter: BindingTraceFilter): TemporaryBindingTrace {
            return TemporaryBindingTrace(trace, debugName, filter)
        }

        /**
         * 创建一个 TemporaryBindingTrace 实例，使用特定的解析主题来生成调试名称.
         *
         * @param trace 原始的 BindingTrace 对象
         * @param debugName 调试名称
         * @param resolutionSubjectForMessage 用于生成调试名称的解析主题
         * @return 创建的 TemporaryBindingTrace 实例
         */
        @JvmStatic
        fun create(trace: BindingTrace, debugName: String, resolutionSubjectForMessage: Any?): TemporaryBindingTrace {
            return create(trace, AnalyzingUtils.formDebugNameForBindingTrace(debugName, resolutionSubjectForMessage))
        }
    }
}
