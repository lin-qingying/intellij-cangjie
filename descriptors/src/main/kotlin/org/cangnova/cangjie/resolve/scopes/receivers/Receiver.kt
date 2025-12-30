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
package org.cangnova.cangjie.resolve.scopes.receivers

/**
 * 接收器（Receiver）的顶层标记接口
 *
 * 接收器是仓颉语言中用于表示成员访问、方法调用等操作的目标对象的抽象概念。
 * 在仓颉语言中，接收器可以是显式的（如表达式）或隐式的（如 this）。
 *
 * 这是所有接收器类型的基础接口，具体的接收器类型包括：
 * - [ReceiverValue]: 具有类型信息的接收器值
 * - [ImplicitReceiver]: 隐式接收器（如 this）
 * - [QualifierReceiver]: 限定符接收器（如包名、类名）
 *
 * @see ReceiverValue
 * @see ImplicitReceiver
 * @see QualifierReceiver
 */
interface Receiver 
