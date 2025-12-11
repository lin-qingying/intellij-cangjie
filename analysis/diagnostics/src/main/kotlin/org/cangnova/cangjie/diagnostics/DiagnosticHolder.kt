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

/**
 * 诊断工厂持有者注解
 *
 * 标记包含 [org.cangnova.cangjie.diagnostics.DiagnosticFactory] 实例的 Kotlin 文件。
 * 被此注解标记的文件会在 [org.cangnova.cangjie.diagnostics.DiagnosticInitializer.initializeAll] 调用时
 * 自动被扫描并初始化其中的诊断工厂。
 *
 * 使用示例：
 * ```kotlin
 * @file:DiagnosticHolder
 *
 * package org.cangnova.cangjie.diagnostics.errors
 *
 * @JvmField
 * val MY_ERROR: DiagnosticFactory0<PsiElement> =
 *     DiagnosticFactory0.create(Severity.ERROR)
 * ```
 *
 * @see org.cangnova.cangjie.diagnostics.DiagnosticInitializer
 * @see org.cangnova.cangjie.diagnostics.DiagnosticFactory
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class DiagnosticHolder