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

package org.cangnova.cangjie.toolchain.api

import org.cangnova.cangjie.toolchain.CangJieVersion
import java.nio.file.Path

/**
 * CangJie工具基础接口
 */
interface CjTool {
    /**
     * 工具所属的工具链
     */
    val toolchain: CjToolchain

    /**
     * 工具的可执行文件路径
     */
    val executable: Path

    /**
     * 获取工具版本
     *
     * @return 工具版本信息
     */
    fun getVersion(): CangJieVersion?
}
