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
 * 成员描述符接口，表示类、对象、接口或枚举中的成员（字段、方法、属性等）
 * 
 * 继承自 DeclarationDescriptorNonRoot 和 DeclarationDescriptorWithVisibility，
 * 提供成员的可见性和修饰符信息
 */
interface MemberDescriptor : DeclarationDescriptorNonRoot, DeclarationDescriptorWithVisibility {
    /**
     * 成员的修饰符（如 abstract、final、open 等）
     * 用于描述成员的访问控制和继承特性
     */
    val  modality: Modality


    /**
     * 成员的可见性（如 public、protected、private、internal 等）
     * 控制成员在代码中的访问范围
     */
    override val visibility: DescriptorVisibility



    /**
     * 判断成员是否为不安全操作
     * 默认返回 false，需要在具体实现中根据业务逻辑重写
     * 
     * @return 如果成员包含可能引发运行时错误的不安全操作，返回 true
     */
    val isUnsafe: Boolean
        get() = false // 默认实现，由具体子类根据需要重写

}
