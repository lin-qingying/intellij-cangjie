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

package org.cangnova.cangjie.types.expressions.typeInfoFactory

import org.cangnova.cangjie.resolve.calls.context.ResolutionContext
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.error.ErrorType


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
