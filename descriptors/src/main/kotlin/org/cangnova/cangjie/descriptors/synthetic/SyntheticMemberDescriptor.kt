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

package org.cangnova.cangjie.descriptors.synthetic

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.FunctionDescriptor

/**
 * 合成成员描述符的通用接口，用于表示由其他描述符生成或包装的合成声明。
 *
 * @param T 被合成成员基于的原始声明描述符类型
 */
interface SyntheticMemberDescriptor<out T : DeclarationDescriptor> {
    /**
     * 用于创建或派生该合成成员的基础描述符。
     */
    val baseDescriptorForSynthetic: T
}

/**
 * 表示为函数式接口（SAM）生成的构造器合成描述符。
 */
interface FunctionInterfaceConstructorDescriptor : SyntheticMemberDescriptor<ClassDescriptor>

/**
 * 表示函数式接口适配器的合成描述符，适配器基于某个函数描述符生成。
 *
 * @param T 作为适配目标的函数描述符类型
 */
interface FunctionInterfaceAdapterDescriptor<out T : FunctionDescriptor> : SyntheticMemberDescriptor<T>

/**
 * 表示作为扩展函数形式的函数式接口适配器的合成描述符。
 */
interface FunctionInterfaceAdapterExtensionFunctionDescriptor : SyntheticMemberDescriptor<FunctionDescriptor>
