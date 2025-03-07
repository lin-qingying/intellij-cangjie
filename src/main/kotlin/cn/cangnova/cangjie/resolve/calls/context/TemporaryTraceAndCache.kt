/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve.calls.context

import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.resolve.TemporaryBindingTrace

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
