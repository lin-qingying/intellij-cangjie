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

package org.cangnova.cangjie.diagnostics

import com.intellij.openapi.diagnostic.logger
import org.cangnova.cangjie.diagnostics.generated.initializeAllDiagnostics
import org.cangnova.cangjie.diagnostics.generated.getDiagnosticFactoryCount

/**
 * 诊断工厂初始化器（KSP 版本）
 *
 * 使用编译期代码生成替代运行时反射，提供更好的性能和可靠性。
 *
 * ## 工作原理
 *
 * 1. **编译期**：KSP 处理器扫描所有带 `@DiagnosticHolder` 注解的文件
 * 2. **代码生成**：自动生成 `initializeAllDiagnostics()` 函数
 * 3. **运行期**：直接调用生成的初始化函数
 *
 * ## 优势
 *
 * - ✅ 无运行时反射开销
 * - ✅ 类型安全，编译期错误检测
 * - ✅ 不受类加载顺序影响
 * - ✅ 生成代码可查看和调试
 * - ✅ 完整的 IDE 支持
 *
 * @see org.cangnova.cangjie.diagnostics.generated.initializeAllDiagnostics
 * @see DiagnosticInitializerStartupActivity
 */
object DiagnosticInitializer {
    private val LOG = logger<DiagnosticInitializer>()

    @Volatile
    private var initialized = false

    /**
     * 确保诊断系统已初始化
     *
     * 调用 KSP 生成的初始化函数，为所有 DiagnosticFactory 设置名称。
     *
     * 此方法是线程安全的，可以被多次调用（后续调用会立即返回）。
     * 通常由 [DiagnosticInitializerStartupActivity] 在项目启动时调用。
     */
    fun ensureInitialized() {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            try {
                val startTime = System.currentTimeMillis()

                // 调用 KSP 生成的初始化函数
                // 该函数在编译期由 DiagnosticProcessor 自动生成
                initializeAllDiagnostics()

                val duration = System.currentTimeMillis() - startTime
                val count = getDiagnosticFactoryCount()

                LOG.info("✓ Initialized $count diagnostic factories in ${duration}ms (KSP-generated)")
                initialized = true
            } catch (e: NoSuchMethodError) {
                LOG.error(
                    "KSP 生成的初始化函数未找到。请运行 './gradlew :analysis:kspKotlin' 生成代码",
                    e
                )
                throw IllegalStateException(
                    "诊断工厂初始化代码未生成。请运行 Gradle 构建以生成 KSP 代码。",
                    e
                )
            } catch (e: Exception) {
                LOG.error("Failed to initialize diagnostic factories", e)
                throw e
            }
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized

    /**
     * 重置初始化状态（仅用于测试）
     */
    @Deprecated("仅用于测试，生产代码请勿使用")
    fun resetForTesting() {
        synchronized(this) {
            initialized = false
        }
    }
}
