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

/**
 * 参数描述符接口
 * 表示函数或构造函数的形式参数
 * 
 * 继承自 ValueDescriptor，表示参数是具有类型和值的语言元素
 */
interface ParameterDescriptor : ValueDescriptor {
    /**
     * 原始参数描述符
     * 在类型替换或泛型实例化时，指向原始的未替换参数描述符
     */
    override val original: ParameterDescriptor
}
