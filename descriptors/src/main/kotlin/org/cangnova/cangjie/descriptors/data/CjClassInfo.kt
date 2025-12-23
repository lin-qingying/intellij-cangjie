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

package org.cangnova.cangjie.descriptors.data

import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*

/**
 * 枚举构造器信息（全量重构版本）
 *
 * 枚举条目是构造器，不是类型声明，因此不继承 CjTypeStatementInfo。
 * 直接实现 CjClassLikeInfo 接口。
 */
class CjEnumConstructorInfo(
    val entry: CjEnumConstructor,
) : CjClassLikeInfo {
    override val classKind: ClassKind = ClassKind.ENUM_ENTRY

    val typeReferences: List<CjTypeReference> = entry.typeReferences

    override val containingPackageFqName: FqName
        get() = entry.parentEnum?.containingCjFile?.packageFqName ?: FqName.ROOT

    override val modifierList: CjModifierList?
        get() = entry.modifierList

    override val scopeAnchor: CjElement
        get() = entry

    // 枚举构造器不是类型声明，返回父枚举类型
    override val correspondingClass: CjTypeStatement?
        get() = entry.parentEnum

    override val typeParameterList: CjTypeParameterList?
        get() = null  // 构造器本身没有类型参数

    override val primaryConstructorParameters: List<CjParameter>
        get() = emptyList()  // 枚举构造器使用类型引用而非参数

    override val danglingAnnotations: List<CjAnnotation>
        get() = emptyList()

    override val declarations: List<CjDeclaration>
        get() = emptyList()
}

open class CjClassInfo<T : CjTypeStatement>(
    element: T,
    override val classKind: ClassKind = ClassKind.CLASS
) : CjTypeStatementInfo<T>(element) {
    override val typeParameterList: CjTypeParameterList?
        get() = element.typeParameterList

}
