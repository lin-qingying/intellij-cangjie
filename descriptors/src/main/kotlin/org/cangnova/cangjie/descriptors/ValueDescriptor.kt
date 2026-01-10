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
package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.types.CangJieType


/**
 * 值描述符接口，表示具有类型和包含声明的值
 * 用于描述变量、常量、属性等具有具体值的语言元素
 */
interface ValueDescriptor : CallableDescriptor {

    /**
     * 值的类型，表示这个值在仓颉语言中的数据类型
     */
    val type: CangJieType

    /**
     * 包含声明，表示这个值所属的声明范围（如类、函数、模块等）
     */
    override val containingDeclaration: DeclarationDescriptor
}
