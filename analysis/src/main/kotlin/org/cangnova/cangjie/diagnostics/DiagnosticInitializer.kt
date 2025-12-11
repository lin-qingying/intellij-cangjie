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
 * 初始化会在首次访问此对象时自动执行，无需手动调用。
 */
object DiagnosticInitializer {
    private const val WARNING = "_WARNING"
    private const val ERROR = "_ERROR"
    private const val BASE_PACKAGE = "org.cangnova.cangjie.diagnostics"

    @Volatile
    private var initialized = false

    // 自动初始化
    init {
        initializeAll()
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

    /**
     * 初始化所有诊断包
     *
     * 自动扫描并初始化所有带有 [DiagnosticHolder] 注解的类。
     * 此方法是幂等的，多次调用只会执行一次初始化。
     */
    private fun initializeAll() {
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


}