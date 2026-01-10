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

package org.cangnova.cangjie.descriptors.data

import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.psi.*


object CjClassInfoUtil {
    fun createClassLikeInfo(typeStatement: CjTypeStatement): CjClassLikeInfo {
        return createTypeStatementInfo(typeStatement)
    }

    /**
     * 创建类型声明信息
     *
     * 注意：枚举条目（CjEnumConstructor）不再是 CjTypeStatement，应使用 createEnumConstructorInfo
     */
    fun createTypeStatementInfo(typeStatement: CjTypeStatement): CjTypeStatementInfo<out CjTypeStatement> {
        if (typeStatement is CjClass) {
            return CjClassInfo(typeStatement, ClassKind.CLASS)
        } else if (typeStatement is CjEnum) {
            return CjClassInfo(typeStatement, ClassKind.ENUM)
        } else if (typeStatement is CjStruct) {
            return CjClassInfo(typeStatement, ClassKind.STRUCT)
        } else if (typeStatement is CjInterface) {
            return CjClassInfo(typeStatement, ClassKind.INTERFACE)
        } else if (typeStatement is CjExtend) {
            return CjClassInfo(typeStatement, ClassKind.EXTEND)
        }

        throw IllegalArgumentException("Unknown declaration type: $typeStatement ${typeStatement.text}")
    }



    /**
     * 创建类或对象信息（支持所有命名声明）
     */
    fun createClassOrObjectInfo(declaration: CjNamedDeclaration): CjClassLikeInfo {
        return when (declaration) {
            is CjClass -> CjClassInfo(declaration, ClassKind.CLASS)
            is CjEnum -> CjClassInfo(declaration, ClassKind.ENUM)
            is CjStruct -> CjClassInfo(declaration, ClassKind.STRUCT)
            is CjInterface -> CjClassInfo(declaration, ClassKind.INTERFACE)
            is CjExtend -> CjClassInfo(declaration, ClassKind.EXTEND)
            else -> throw IllegalArgumentException("Unknown declaration type: $declaration ${declaration.text}")
        }
    }
}
