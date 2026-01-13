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

package org.cangnova.cangjie.types

/**
 * 在公共类型中替换交集类型的替代类型
 *
 * 此函数递归地遍历类型树,当遇到交集类型构造器时,使用其替代类型进行替换。
 * 这在将内部推断类型转换为公共 API 类型时很有用。
 *
 * @param type 要处理的类型
 * @return 替换后的类型
 */
fun substituteAlternativesInPublicType(type: CangJieType): UnwrappedType {
    // 创建一个函数式替换器,专门处理交集类型的替代
    val substitutor = ComposableTypeSubstitutor.create(SubstitutorFunction { constructor ->
        if (constructor is IntersectionTypeConstructor) {
            constructor.getAlternativeType()?.unwrap()
        } else {
            null
        }
    })

    return substitutor.safeSubstitute(type.unwrap())
}
