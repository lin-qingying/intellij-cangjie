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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.cangnova.cangjie.diagnostics.rendering.DiagnosticRendererRegistry

/**
 * 诊断初始化器启动活动
 *
 * 在项目启动后自动执行，负责初始化诊断系统。
 * 通过反射调用 [DiagnosticInitializer] 来触发所有诊断工厂的注册和渲染器的绑定。
 *
 * ## 为什么需要这个启动活动？
 *
 * 虽然 [DiagnosticInitializer] 是一个 object，会在首次访问时自动初始化，
 * 但是如果没有代码显式访问它，初始化可能会延迟到第一次使用诊断功能时才执行。
 * 这可能导致：
 *
 * 1. **首次使用性能问题**: 第一次触发诊断时会有明显的延迟（因为要进行反射扫描）
 * 2. **并发初始化问题**: 多个线程同时触发初始化可能导致竞态条件
 * 3. **测试不稳定**: 单元测试中的初始化顺序不确定
 *
 * 通过在启动时主动触发初始化，可以：
 * - 在后台线程中完成反射扫描，不影响 IDE 启动速度
 * - 确保在用户开始编辑代码前诊断系统已经就绪
 * - 避免首次使用时的性能抖动
 *
 * ## 工作原理
 *
 * ```
 * IDE 启动
 *   ↓
 * ProjectActivity.execute() 被调用
 *   ↓
 * 访问 DiagnosticInitializer（触发 object 初始化）
 *   ↓
 * DiagnosticInitializer.init 执行
 *   ↓
 * 通过反射扫描所有 @DiagnosticHolder 类
 *   ↓
 * 为所有 DiagnosticFactory 设置名称和渲染器
 *   ↓
 * 诊断系统就绪
 * ```
 *
 * ## 性能考虑
 *
 * - **异步执行**: 在 IDE 启动后的非阻塞阶段执行
 * - **单次初始化**: DiagnosticInitializer 内部有双重检查锁，确保只初始化一次
 * - **跳过测试模式**: 在单元测试模式下跳过初始化，避免影响测试性能
 *
 * @see DiagnosticInitializer
 * @see ProjectActivity
 */
class DiagnosticInitializerStartupActivity : ProjectActivity {
    companion object {
        private val LOG = Logger.getInstance(DiagnosticInitializerStartupActivity::class.java)
    }

    /**
     * 项目启动后执行
     *
     * 触发诊断系统的初始化，包括：
     * 1. [DiagnosticInitializer] - 扫描并注册所有诊断工厂（诊断声明）
     * 2. [DiagnosticRendererRegistry] - 加载所有诊断渲染器（显示信息）
     *
     * **初始化顺序**：
     * - 先初始化 DiagnosticInitializer（诊断声明）
     * - 再初始化 DiagnosticRendererRegistry（显示信息）
     *
     * @param project 当前项目实例（未使用，但由接口要求）
     */
    override suspend fun execute(project: Project) {
        // 在单元测试模式下跳过初始化，避免影响测试性能
        if (ApplicationManager.getApplication().isUnitTestMode) {
            return
        }

        try {
            LOG.info("Initializing CangJie diagnostic system...")

            // 1. 先触发 DiagnosticInitializer 的初始化
            // 扫描所有诊断工厂并设置名称（诊断声明）
            DiagnosticInitializer.ensureInitialized()

            // 2. 再触发 DiagnosticRendererRegistry 的初始化
            // 加载所有渲染器提供者（显示信息）
            // 在启动时初始化，避免在类加载时（静态初始化阶段）依赖服务
            DiagnosticRendererRegistry.ensureInitialized()

            LOG.info("CangJie diagnostic system initialized successfully")
        } catch (e: Exception) {
            // 记录错误但不抛出异常，避免影响插件的其他功能
            LOG.error("Failed to initialize CangJie diagnostic system", e)
        }
    }
}
