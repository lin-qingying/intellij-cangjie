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

package org.cangnova.cangjie.debugger.protobuf.memory.config

/**
 * MemoryStore 配置
 *
 * 简化版配置，移除了所有实验性功能。
 *
 * @property pageSize 数据加载的页面大小（用于 Provider）
 */
data class MemoryStoreConfig(

//    虚拟文件是否禁用滚动加载
    val vfsDisableLazyLoad: Boolean = false,
) {
    companion object {
        /**
         * 默认配置
         */
        val DEFAULT = MemoryStoreConfig()


    }

    /**
     * 验证配置的有效性
     *
     * @throws IllegalArgumentException 如果配置无效
     */
    fun validate() {

    }
}