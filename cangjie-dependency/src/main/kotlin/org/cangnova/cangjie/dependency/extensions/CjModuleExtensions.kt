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

package org.cangnova.cangjie.dependency.extensions

import org.cangnova.cangjie.dependency.model.CjDependency
import org.cangnova.cangjie.project.model.CjModule

/**
 * 获取模块的依赖列表
 *
 * 这是一个扩展属性，用于在 cangjie-dependency 模块中访问模块依赖
 * 避免在 cangjie-project 模块中引入对 cangjie-dependency 的循环依赖
 */
val CjModule.dependencies: List<CjDependency>
    get() = emptyList() // 默认返回空列表，具体实现可以在子类中覆盖