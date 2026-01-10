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

import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.JpsElementFactory
import org.jetbrains.jps.model.ex.JpsElementTypeBase
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

/**
 * 仓颉源代码根类型
 *
 * 表示仓颉项目中的生产源代码目录。
 */
object CangJieSourceRootType : JpsElementTypeBase<JpsElement>(),
    JpsModuleSourceRootType<JpsElement> {

    override fun createDefaultProperties(): JpsElement =
        JpsElementFactory.getInstance().createDummyElement()

    override fun isForTests(): Boolean = false
}

/**
 * 仓颉测试源代码根类型
 *
 * 表示仓颉项目中的测试源代码目录。
 */
object CangJieTestSourceRootType : JpsElementTypeBase<JpsElement>(),
    JpsModuleSourceRootType<JpsElement> {

    override fun createDefaultProperties(): JpsElement =
        JpsElementFactory.getInstance().createDummyElement()

    override fun isForTests(): Boolean = true
}

/**
 * 仓颉资源根类型
 *
 * 表示仓颉项目中的生产资源目录。
 */
object CangJieResourceRootType : JpsElementTypeBase<JpsElement>(),
    JpsModuleSourceRootType<JpsElement> {

    override fun createDefaultProperties(): JpsElement =
        JpsElementFactory.getInstance().createDummyElement()

    override fun isForTests(): Boolean = false
}

/**
 * 仓颉测试资源根类型
 *
 * 表示仓颉项目中的测试资源目录。
 */
object CangJieTestResourceRootType : JpsElementTypeBase<JpsElement>(),
    JpsModuleSourceRootType<JpsElement> {

    override fun createDefaultProperties(): JpsElement =
        JpsElementFactory.getInstance().createDummyElement()

    override fun isForTests(): Boolean = true
}