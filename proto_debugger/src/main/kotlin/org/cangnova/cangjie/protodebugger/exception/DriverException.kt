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

package org.cangnova.cangjie.protodebugger.exception

import com.intellij.openapi.util.NlsContexts
import com.intellij.execution.ExecutionException

/**
 * 调试器驱动异常类
 *
 * 该异常类用于表示调试器驱动程序在执行过程中发生的错误。
 * 它继承自IntelliJ平台的ExecutionException，与平台的执行框架集成。
 *
 * 使用场景：
 * - 调试器驱动程序初始化失败
 * - 调试器与目标程序通信失败
 * - 调试器内部错误或状态异常
 * - 驱动程序配置或环境问题
 *
 * @param s 错误消息，描述异常的具体原因，使用@NlsContexts.DialogMessage注解支持本地化
 */
class DriverException(s: @NlsContexts.DialogMessage String?) : ExecutionException(s)
