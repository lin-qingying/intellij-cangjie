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
package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.storage.StorageManager

 /**
 * 类描述符的基础抽象实现。
 *
 * 该类作为具体类描述符的基类，接收存储管理器和包含声明等通用参数，
 * 并将这些信息传递给上层的 AbstractClassDescriptor 实现公共行为。
 * 子类可以基于此提供更具体的类语义和成员管理。
 *
 * @param storageManager 存储管理器，用于缓存与延迟计算
 * @param containingDeclaration 包含该类的声明描述符（如包或另一个类）
 * @param name 类名
 * @param source 源信息
 */
abstract class ClassDescriptorBase protected constructor(
    storageManager: StorageManager,

    override val containingDeclaration: DeclarationDescriptor,
    name: Name,
    override val source: SourceElement,

    ) : AbstractClassDescriptor(storageManager, name)
{

    }