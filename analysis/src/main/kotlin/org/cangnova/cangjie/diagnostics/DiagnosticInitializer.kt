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

import org.cangnova.cangjie.diagnostics.rendering.DiagnosticRenderer
import org.cangnova.cangjie.diagnostics.rendering.DiagnosticRendererRegistry
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import java.lang.reflect.Modifier

/**
 * 诊断工厂初始化器
 *
 * 负责自动初始化所有诊断工厂的名称和渲染器。
 * 通过反射机制扫描所有带有 [DiagnosticHolder] 注解的类，并为每个诊断工厂设置其名称。
 *
 * ## 延迟初始化
 *
 * 为避免在类加载时（静态初始化阶段）依赖服务，本初始化器采用延迟初始化策略：
 * - 不在 `init` 块中进行初始化
 * - 通过 [org.cangnova.cangjie.diagnostics.DiagnosticInitializerStartupActivity]
 *   在项目启动后显式调用 [ensureInitialized]
 * - 避免 IntelliJ 平台的 "Class initialization must not depend on services" 错误
 *
 * @see DiagnosticInitializerStartupActivity
 */
object DiagnosticInitializer {
    private const val WARNING = "_WARNING"
    private const val ERROR = "_ERROR"
    private const val BASE_PACKAGE = "org.cangnova.cangjie.diagnostics"

    @Volatile
    private var initialized = false

    /**
     * 确保诊断系统已初始化
     *
     * 扫描所有带有 [DiagnosticHolder] 注解的类，为每个诊断工厂设置名称和渲染器。
     *
     * 此方法是线程安全的，可以被多次调用（后续调用会立即返回）。
     * 通常由 [DiagnosticInitializerStartupActivity] 在项目启动时调用。
     */
    fun ensureInitialized() {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            // 扫描所有带有 DiagnosticHolder 注解的类
            val reflections = Reflections(
                ConfigurationBuilder()
                    .forPackages(BASE_PACKAGE)
                    .setScanners(Scanners.TypesAnnotated)
            )

            val annotatedClasses = reflections.getTypesAnnotatedWith(DiagnosticHolder::class.java)

            // 初始化每个带注解的类
            for (clazz in annotatedClasses) {
                val annotation = clazz.getAnnotation(DiagnosticHolder::class.java)
                if (annotation != null) {
                    initializeFactoryNames(clazz)
                }
            }

            initialized = true
        }
    }

    /**
     * 初始化工厂名称
     *
     * 扫描指定类的所有静态字段，为每个 [DiagnosticFactory] 设置名称和渲染器。
     *
     * @param aClass 要扫描的类
     */
    private fun initializeFactoryNames(aClass: Class<*>) {
        for (field in aClass.fields) {
            if (Modifier.isStatic(field.modifiers)) {
                try {
                    when (val value = field.get(null)) {
                        is DiagnosticFactory<*> -> {
                            initializeNameAndRenderer(field.name, value)
                        }

                        is DiagnosticFactoryForDeprecation<*, *, *> -> {
                            initializeNameAndRenderer(
                                field.name + ERROR,
                                value.errorFactory
                            )
                            initializeNameAndRenderer(
                                field.name + WARNING,
                                value.warningFactory
                            )
                        }
                    }
                } catch (e: IllegalAccessException) {
                    throw IllegalStateException(e)
                }
            }
        }
    }

    /**
     * 初始化单个工厂的名称和渲染器
     *
     * 从 DiagnosticRendererRegistry 获取渲染器并设置为工厂的默认渲染器
     */
    @Suppress("UNCHECKED_CAST")
    private fun initializeNameAndRenderer(
        name: String,
        factory: DiagnosticFactory<*>
    ) {
        factory.initializeName(name)
        // 从新系统获取渲染器
        factory.defaultRenderer = DiagnosticRendererRegistry.getRenderer(factory) as? DiagnosticRenderer<Any>
    }
}
