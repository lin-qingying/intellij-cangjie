/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.config

import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer

/**
 * 仓颉 JPS 模型序列化扩展
 *
 * 注册仓颉语言的源根类型，使 IntelliJ 能够识别和处理仓颉项目的源码目录。
 * 通过 JpsModelSerializerExtension 扩展点注册到 IDE 中。
 *
 * ## 注册的源根类型
 * - cangjie-source: 仓颉生产源代码
 * - cangjie-test: 仓颉测试源代码
 * - cangjie-resource: 仓颉生产资源
 * - cangjie-test-resource: 仓颉测试资源
 */
class CangJieJpsModelSerializerExtension : JpsModelSerializerExtension() {

    override fun getModuleSourceRootPropertiesSerializers(): List<JpsModuleSourceRootPropertiesSerializer<*>> {
        return listOf(
            CangJieSourceRootPropertiesSerializer,
            CangJieTestSourceRootPropertiesSerializer,
            CangJieResourceRootPropertiesSerializer,
            CangJieTestResourceRootPropertiesSerializer
        )
    }
}