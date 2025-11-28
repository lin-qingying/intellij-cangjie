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

package org.cangnova.cangjie.debugger.protobuf.breakpoint

import org.cangnova.cangjie.debugger.protobuf.data.LLDBBreakpoint
import org.cangnova.cangjie.debugger.protobuf.data.LLDBBreakpointLocation

/**
 * 添加断点的结果类
 *
 * 该数据类包含了添加断点操作的完整结果信息，包括创建的断点对象以及该断点对应的所有位置信息。
 * 在调试器中设置断点时，一个符号断点可能会映射到多个实际的位置（如内联函数的多个位置）。
 *
 * @property breakpoint 创建的断点对象，包含断点的所有配置信息
 * @property breakpointLocations 该断点对应的所有位置信息列表，可能包含多个位置
 */
data class AddBreakpointResult(
    /**
     * 创建的断点对象
     */
    val breakpoint: LLDBBreakpoint,

    /**
     * 断点位置列表
     * 一个符号断点可能对应多个实际位置（如内联函数调用处）
     */
    val breakpointLocations: List<LLDBBreakpointLocation>
)
