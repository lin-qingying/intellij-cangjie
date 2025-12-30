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

package org.cangnova.cangjie.quickDoc

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.resolve.DescriptorUtils

/**
 * 不需要生成超链接的"无聊"内置类型集合。
 *
 * 这些基本类型在文档中以纯文本显示，不提供点击导航功能。
 */
private val boringBuiltinClasses = setOf(
    StandardNames.FqNames.unitUFqName,
    StandardNames.FqNames.int8UFqName,
    StandardNames.FqNames.int16UFqName,
    StandardNames.FqNames.int32UFqName,
    StandardNames.FqNames.int64UFqName,
    StandardNames.FqNames.runeUFqName,
    StandardNames.FqNames.boolUFqName,
    StandardNames.FqNames.float16UFqName,
    StandardNames.FqNames.float32UFqName,
    StandardNames.FqNames.float64UFqName,
    )

/**
 * 判断分类器是否为"无聊"的内置类型。
 *
 * @receiver ClassifierDescriptor 分类器描述符
 * @return 如果是基本内置类型则返回 `true`
 */
fun ClassifierDescriptor.isBoringBuiltinClass(): Boolean = DescriptorUtils.getFqName(this) in boringBuiltinClasses

