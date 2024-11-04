package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.psi.Call
import com.linqingying.cangjie.psi.ValueArgument
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo.Companion.EMPTY

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
