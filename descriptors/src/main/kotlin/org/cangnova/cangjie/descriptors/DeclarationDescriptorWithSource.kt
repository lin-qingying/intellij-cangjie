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
 * 带有源码信息的声明描述符接口
 * 
 * 继承自 DeclarationDescriptor，提供源码位置信息和原始描述符引用
 * 用于支持代码导航、错误报告、重构等IDE功能
 */
interface DeclarationDescriptorWithSource : DeclarationDescriptor {

    /**
     * 获取此声明的源码元素信息
     * 
     * 包含源码文件、行号、列号等位置信息
     * 用于错误报告、代码导航和源代码映射
     * 
     * @return 描述源码位置的 SourceElement 对象
     */
    val source: SourceElement

    /**
     * 获取此描述符的原始（未经过加工或修改的）版本
     * 
     * 在处理泛型、类型参数替换或其他变换时，保持对原始声明的引用
     * 用于类型检查和符号解析的正确性
     * 
     * @return 原始的 DeclarationDescriptorWithSource 实例
     */
    override val original: DeclarationDescriptorWithSource
}
