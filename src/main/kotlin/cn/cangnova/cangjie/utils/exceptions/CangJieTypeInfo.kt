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

package cn.cangnova.cangjie.utils.exceptions

import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import cn.cangnova.cangjie.types.CangJieType

class CangJieTypeInfo @JvmOverloads constructor(
    val type: CangJieType?,
    val dataFlowInfo: DataFlowInfo,
    val jumpOutPossible: Boolean = false,
    val jumpFlowInfo: DataFlowInfo = dataFlowInfo
){
    fun clearType() = replaceType(null)
    fun replaceJumpOutPossible(jumpOutPossible: Boolean) =
        if (jumpOutPossible == this.jumpOutPossible) this else CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)
    fun replaceJumpFlowInfo(jumpFlowInfo: DataFlowInfo) =
        if (jumpFlowInfo == this.jumpFlowInfo) this else CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceDataFlowInfo(dataFlowInfo: DataFlowInfo) = when (this.dataFlowInfo) {
        // Nothing changed
        dataFlowInfo -> this
        // Jump info is the same as data flow info: change both
        jumpFlowInfo -> CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, dataFlowInfo)
        // Jump info is not the same: change data flow info only
        else -> CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)
    }
    // NB: do not compare type with this.type because this comparison is complex and unstable
    fun replaceType(type: CangJieType?) = CangJieTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

}
