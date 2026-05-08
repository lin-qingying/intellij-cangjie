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
 */

package org.cangnova.cangjie.test

import com.intellij.testFramework.LightProjectDescriptor

/**
 * 对位 Kotlin 测试框架中轻量级项目描述符这一层的仓颉实现。
 *
 * Kotlin 插件在 JVM 场景下会继续叠加 Java/JDK 相关描述符；
 * 仓颉 IntelliJ 插件这里不引入 `com.intellij.java`，因此此层只保留
 * IntelliJ Platform 最基础的 light project 能力。
 *
 * toolchain / stdlib / builtins 等仓颉输入由更高层夹具显式注入。
 */
open class CangJieLightProjectDescriptor protected constructor() : LightProjectDescriptor() {
    companion object {
        @JvmField
        val INSTANCE: CangJieLightProjectDescriptor = CangJieLightProjectDescriptor()
    }
}
