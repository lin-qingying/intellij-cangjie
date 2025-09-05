/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types.error

import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner


//class MultipleSupertypeTypeInferenceFailure (
//    private val type: CangJieType
//
//) : SimpleType()
//{
//    val intersectedTypes:List<CangJieType> get() {
//        return type.constructor.supertypes.toList()
//    }
//
//    override fun makeOptionalAsSpecified(newNullability: Boolean): SimpleType {
//        return  this
//    }
//
//    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType {
//   return  this
//    }
//
//    @TypeRefinement
//    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType {
//        return  this
//    }
//
//    override val constructor: TypeConstructor
//        get() = type.constructor
//    override val arguments: List<TypeProjection>
//        get() = type.arguments
//    override val attributes: TypeAttributes
//        get() = type.attributes
//    override val isMarkedOption: Boolean
//        get() =type.isMarkedOption
//    override val memberScope: MemberScope
//        get() = type.memberScope
//
//    override fun equals(other: Any?): Boolean {
//        return other == this.type
//    }
//
//    override fun hashCode(): Int {
//        return type.hashCode()
//    }
//}

val CangJieType.isMultipleSupertypeType get() =  this is MultipleSupertypeTypeInferenceFailure
//当具有多个超类型时，推导类型失败使用该类
class MultipleSupertypeTypeInferenceFailure(
    private val type: CangJieType

) : ErrorType(type.constructor,type.memberScope, ErrorTypeKind.MULIT_SMALL_COMMON_SUPERTYPES)
{
    val intersectedTypes:List<CangJieType> get() {
        return type.constructor.supertypes.toList()
    }

    override fun equals(other: Any?): Boolean {
        return other == this.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}
open class ErrorType @JvmOverloads internal constructor(
    override val constructor: TypeConstructor,
    override val memberScope: MemberScope,
    val kind: ErrorTypeKind,
    override val arguments: List<TypeProjection> = emptyList(),
    override val isOption: Boolean = false,
    private vararg val formatParams: String
) : SimpleType() {
    val debugMessage = String.format(kind.debugMessage, *formatParams)


    override val attributes: TypeAttributes
        get() = TypeAttributes.Empty

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType = this

//    fun replaceArguments(newArguments: List<TypeProjection>): ErrorType =
//        ErrorType(constructor, memberScope, kind, newArguments, isMarkedOption, *formatParams)

    override fun makeOptionAsSpecified(isOption: Boolean): SimpleType =
        ErrorType(constructor, memberScope, kind, arguments, isOption, *formatParams)

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner) = this
}
