package com.huawei.cangjie.types.expressions.typeInfoFactory

import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo

fun noTypeInfo(context: ResolutionContext<*>): CangJieTypeInfo = noTypeInfo(context.dataFlowInfo)
/*
 * Functions in this file are intended to create type info instances in different circumstances
 */

fun createTypeInfo(type: CangJieType?, dataFlowInfo: DataFlowInfo): CangJieTypeInfo =
    CangJieTypeInfo(type, dataFlowInfo)

fun createTypeInfo(
    type: CangJieType?,
    dataFlowInfo: DataFlowInfo,
    jumpPossible: Boolean,
    jumpFlowInfo: DataFlowInfo
): CangJieTypeInfo =
    CangJieTypeInfo(type, dataFlowInfo, jumpPossible, jumpFlowInfo)

fun createTypeInfo(type: CangJieType?): CangJieTypeInfo = createTypeInfo(type, DataFlowInfo.EMPTY)

fun createTypeInfo(type: CangJieType?, context: ResolutionContext<*>): CangJieTypeInfo =
    createTypeInfo(type, context.dataFlowInfo)

fun noTypeInfo(dataFlowInfo: DataFlowInfo): CangJieTypeInfo = createTypeInfo(null, dataFlowInfo)