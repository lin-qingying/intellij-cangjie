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

package org.cangnova.cangjie.utils

import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.NonNls
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * 创建一个与 IntelliJ Registry 绑定的布尔标志属性代理
 *
 * 该函数返回一个属性代理，允许通过 Kotlin 属性语法读写 IntelliJ Registry 中的布尔值。
 *
 * 使用场景：
 * - 需要在代码中访问 IntelliJ Registry 配置
 * - 需要动态切换功能开关
 * - 需要通过属性语法简化 Registry 访问
 *
 * 示例：
 * ```kotlin
 * object MyFeatureFlags {
 *     var enableNewParser by registryFlag("cangjie.feature.newParser")
 *     var enableOptimization by registryFlag("cangjie.optimization.enabled")
 * }
 *
 * // 读取
 * if (MyFeatureFlags.enableNewParser) {
 *     // 使用新解析器
 * }
 *
 * // 写入
 * MyFeatureFlags.enableOptimization = true
 * ```
 *
 * @param key Registry 键名（不能包含国际化字符串）
 * @return 布尔类型的属性代理
 */
fun registryFlag(@NonNls key: String): ReadWriteProperty<Any?, Boolean> {
    return object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = Registry.`is`(key)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = Registry.get(key).setValue(value)
    }
}