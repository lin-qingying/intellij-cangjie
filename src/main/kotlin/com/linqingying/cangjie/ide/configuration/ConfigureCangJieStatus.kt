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

package com.linqingying.cangjie.ide.configuration

import com.intellij.openapi.extensions.ExtensionPointName


enum class ConfigureCangJieStatus {
    /** 使用此配置器正确配置了CangJie。 */
    CONFIGURED,

    /** 配置器不适用于当前项目类型。 */
    NON_APPLICABLE,

    /** 该配置器适用于当前项目类型，可以自动配置CangJie */
    CAN_BE_CONFIGURED,

    /**
     * 配置器适用于当前项目类型且未配置CangJie，
     * 但项目的状态不允许自动配置CangJie。
     */
    BROKEN
}

interface CangJieProjectConfigurator {



    companion object {
        val EP_NAME = ExtensionPointName.create<CangJieProjectConfigurator>("com.linqingying.cangjie.projectConfigurator")
    }
}
