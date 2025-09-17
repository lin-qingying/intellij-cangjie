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

package org.cangnova.cangjie.descriptors

/**
 * 可继承声明描述符接口，表示具有继承语义的声明。
 *
 * 该接口专门用于表示那些可以参与继承关系的声明，包括：
 * - 可以有父类/超类的声明
 * - 可以被其他类型继承的声明
 * - 具有继承语义的类型构造体
 *
 * 主要职责：
 * - 定义继承关系和超类型访问
 * - 提供继承相关的类型信息
 * - 支持继承层次结构的类型检查
 * - 为继承相关的IDE功能提供基础
 *
 * 适用于：
 * - 类（Class）- 可以继承其他类和实现接口
 * - 接口（Interface）- 可以继承其他接口
 * - 枚举（Enum）- 可以实现接口
 * - 扩展（Extend）- 为类型添加新的继承关系
 */
interface InheritableDescriptor : DeclarationDescriptorNonRoot {



}