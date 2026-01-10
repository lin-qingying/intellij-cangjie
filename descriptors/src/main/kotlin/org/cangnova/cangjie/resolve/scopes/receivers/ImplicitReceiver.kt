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

import org.cangnova.cangjie.descriptors.DeclarationDescriptor

/**
 * 隐式 "this" 接收器
 *
 * ImplicitReceiver 表示在代码中不显式出现，但在作用域中隐式可用的 "this" 接收器。
 * 例如，在类或扩展的成员函数中，可以直接访问 this 的成员而不需要显式写出 "this."。
 *
 * 典型的使用场景包括：
 * - 类成员函数中的隐式 this（指向类实例）
 * - 扩展（extend）块中的隐式 this（指向被扩展的类型）
 * - Lambda 表达式中的隐式接收器
 *
 * @property declarationDescriptor 声明此接收器的描述符（如类描述符、扩展描述符等）
 *
 * @see ImplicitClassReceiver
 * @see ImplicitExtendReceiver
 * @see ReceiverValue
 */
interface ImplicitReceiver : ReceiverValue {
    /** 声明此接收器的描述符 */
    val declarationDescriptor: DeclarationDescriptor
}
