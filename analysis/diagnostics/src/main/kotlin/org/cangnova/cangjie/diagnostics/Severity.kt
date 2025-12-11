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
 * 诊断信息严重程度枚举
 *
 * 定义了编译器诊断信息的严重程度级别，用于对代码问题进行分类和优先级排序。
 * 这些级别会影响 IDE 中的视觉提示（如波浪线颜色）以及编译器的处理方式。
 */
enum class Severity {
    /** 信息级别 - 仅供参考的提示信息，不影响编译 */
    INFO,

    /** 错误级别 - 严重问题，会导致编译失败 */
    ERROR,

    /** 警告级别 - 潜在问题，不影响编译但建议修复 */
    WARNING
}