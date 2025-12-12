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

@file:Suppress("unused")

package org.cangnova.cangjie.diagnostics.infos

import com.google.common.collect.ImmutableSet
import org.cangnova.cangjie.diagnostics.DiagnosticFactory
import org.cangnova.cangjie.diagnostics.infos.errors.*


// ========================================
// 诊断集合
// ========================================

/**
 * 未使用元素诊断集合
 *
 * 包含所有关于未使用元素的诊断（参数、变量等）。
 */
@JvmField
val UNUSED_ELEMENT_DIAGNOSTICS: ImmutableSet<DiagnosticFactory<*>> = ImmutableSet.of(
//    UNUSED_PARAMETER
)

/**
 * 未解析引用诊断集合
 *
 * 包含所有关于未解析引用的诊断。
 */
@JvmField
val UNRESOLVED_REFERENCE_DIAGNOSTICS: ImmutableSet<DiagnosticFactory<*>> = ImmutableSet.of(
    UNRESOLVED_REFERENCE,
    NAMED_PARAMETER_NOT_FOUND,
    UNRESOLVED_REFERENCE_WRONG_RECEIVER
)

/**
 * 必须初始化诊断集合
 *
 * 包含所有关于必须初始化的诊断。
 */
@JvmField
val MUST_BE_INITIALIZED_DIAGNOSTICS: ImmutableSet<DiagnosticFactory<*>> = ImmutableSet.of(
    MUST_BE_INITIALIZED,
    MUST_BE_INITIALIZED_OR_BE_ABSTRACT
)