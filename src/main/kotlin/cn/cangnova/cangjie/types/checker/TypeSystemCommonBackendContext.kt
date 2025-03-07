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

package cn.cangnova.cangjie.types.checker

import cn.cangnova.cangjie.builtins.PrimitiveType
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.FqNameUnsafe
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.types.model.*


interface TypeSystemCommonBackendContext : TypeSystemContext {
//    fun nullableAnyType(): SimpleTypeMarker
//    fun arrayType(componentType: CangJieTypeMarker): SimpleTypeMarker
//    fun CangJieTypeMarker.isArrayOrNullableArray(): Boolean
//
//    fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean

    fun CangJieTypeMarker.hasAnnotation(fqName: FqName): Boolean

    /**
     * @return value of the first argument of the annotation with the given [fqName], if the annotation is present and
     * the argument is of a primitive type or a String, or null otherwise.
     *
     * Note that this method returns null if no arguments are provided, even if the corresponding annotation parameter has a default value.
     *
     * TODO: provide a more granular & elaborate API here to reduce confusion
     */
//    fun CangJieTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any?
//
//    fun TypeConstructorMarker.isInlineClass(): Boolean
//    fun TypeConstructorMarker.isMultiFieldValueClass(): Boolean
//    fun TypeConstructorMarker.getValueClassProperties(): List<Pair<Name, SimpleTypeMarker>>?
//    fun TypeConstructorMarker.isInnerClass(): Boolean
//    fun TypeParameterMarker.getRepresentativeUpperBound(): CangJieTypeMarker
//    fun CangJieTypeMarker.getUnsubstitutedUnderlyingType(): CangJieTypeMarker?
//    fun CangJieTypeMarker.getSubstitutedUnderlyingType(): CangJieTypeMarker?

    fun CangJieTypeMarker.makeNullable(): CangJieTypeMarker =
        asSimpleType()?.withNullability(true) ?: this
//    fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType?
//    fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType?
//
//    fun TypeConstructorMarker.isUnderCangJiePackage(): Boolean
//    fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe?
//
//    fun TypeParameterMarker.getName(): Name
//    fun TypeParameterMarker.isReified(): Boolean
//
//    fun CangJieTypeMarker.isInterfaceOrAnnotationClass(): Boolean
}

interface TypeSystemCommonBackendContextForTypeMapping : TypeSystemCommonBackendContext {
//    fun TypeConstructorMarker.isTypeParameter(): Boolean
//    fun TypeConstructorMarker.asTypeParameter(): TypeParameterMarker
    fun TypeConstructorMarker.defaultType(): CangJieTypeMarker
//    fun TypeConstructorMarker.isScript(): Boolean
//
//    fun SimpleTypeMarker.isSuspendFunction(): Boolean
//    fun SimpleTypeMarker.isKClass(): Boolean

//    fun TypeConstructorMarker.typeWithArguments(arguments: List<CangJieTypeMarker>): SimpleTypeMarker
//    fun TypeConstructorMarker.typeWithArguments(vararg arguments: CangJieTypeMarker): SimpleTypeMarker {
//        return typeWithArguments(arguments.toList())
//    }

//    fun TypeArgumentMarker.adjustedType(): CangJieTypeMarker {
//        if (this.isStarProjection()) return nullableAnyType()
//        return getType()
//    }

//    fun TypeParameterMarker.representativeUpperBound(): CangJieTypeMarker
//
//    fun continuationTypeConstructor(): TypeConstructorMarker
//    fun functionNTypeConstructor(n: Int): TypeConstructorMarker
//
//    fun CangJieTypeMarker.getNameForErrorType(): String?
}
