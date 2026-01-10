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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.name.Name

/**
 * 仓颉语言中具有名称的 PSI 元素基础接口
 *
 * 该接口用于标识所有具有名称的 PSI 元素,如类、函数、变量等。
 * 通过 [nameAsName] 属性提供统一的名称访问方式。
 *
 * 实现该接口的典型 PSI 元素包括:
 * - 命名声明 (CjNamedDeclaration)
 * - 类声明 (CjClass)
 * - 函数声明 (CjFunction)
 * - 变量声明 (CjProperty)
 */
interface CjNamed {
    /**
     * 获取该元素的名称
     *
     * @return 元素的名称对象,如果元素没有名称则返回 null
     */
    val nameAsName: Name?
}
