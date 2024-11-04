package com.linqingying.cangjie.types.expressions.typeInfoFactory

import com.linqingying.cangjie.resolve.calls.context.ResolutionContext
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.error.ErrorType
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo

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
fun errorTypeInfo(type: ErrorType, context: ResolutionContext<*>): CangJieTypeInfo =
    createTypeInfo(type, context.dataFlowInfo)

fun errorTypeInfo(type: ErrorType, dataFlowInfo: DataFlowInfo): CangJieTypeInfo = createTypeInfo(type, dataFlowInfo)
