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

package cn.cangnova.cangjie.resolve.calls.model

import cn.cangnova.cangjie.psi.Call
import cn.cangnova.cangjie.psi.ValueArgument
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo.Companion.EMPTY

class DataFlowInfoForArgumentsImpl(initialInfo: DataFlowInfo, call: Call) :
    MutableDataFlowInfoForArguments(initialInfo) {
    private var infoMap: MutableMap<ValueArgument, DataFlowInfo>? = null
    private var nextArgument: MutableMap<ValueArgument, ValueArgument>? = null
    private var resultInfo: DataFlowInfo? = null

    init {
        initNextArgMap(call.valueArguments)
    }

    private fun initNextArgMap(valueArguments: List<ValueArgument>) {
        val iterator = valueArguments.iterator()
        var prev: ValueArgument? = null
        while (iterator.hasNext()) {
            val argument = iterator.next()
            if (prev != null) {
                if (nextArgument == null) {
                    nextArgument = HashMap()
                }
                nextArgument!![prev] = argument
            }
            prev = argument
        }
    }

    override fun getInfo(valueArgument: ValueArgument): DataFlowInfo {
        val infoForArgument = if (infoMap == null) null else infoMap!![valueArgument]
        if (infoForArgument == null) {
            return initialDataFlowInfo
        }
        return initialDataFlowInfo.and(infoForArgument)
    }

    override fun updateInfo(valueArgument: ValueArgument, dataFlowInfo: DataFlowInfo) {
        val next = if (nextArgument == null) null else nextArgument!![valueArgument]
        if (next != null) {
            if (infoMap == null) {
                infoMap = HashMap()
            }
            infoMap!![next] = dataFlowInfo
            return
        }
        //TODO assert resultInfo == null
        resultInfo = dataFlowInfo
    }

    override fun getResultInfo(): DataFlowInfo {
        if (resultInfo == null) return initialDataFlowInfo
        return initialDataFlowInfo.and(resultInfo!!)
    }

    override fun updateResultInfo(dataFlowInfo: DataFlowInfo) {
        if (dataFlowInfo == EMPTY) return

        if (resultInfo == null) resultInfo = initialDataFlowInfo
        resultInfo = resultInfo!!.and(dataFlowInfo)
    }
}
