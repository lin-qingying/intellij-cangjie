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
 * 带有导航替代的声明描述符接口。
 *
 * 此接口用于表示在代码导航或编辑器跳转时，当前描述符可能有用于导航的替代目标（substitute）。
 * 例如在某些重构或别名场景下，导航应该定位到替代的目标而非当前描述符本身。
 */
interface DeclarationDescriptorWithNavigationSubstitute : DeclarationDescriptor {
    /**
     * 导航时应替代使用的声明描述符。
     */
    val substitute: DeclarationDescriptor
}
