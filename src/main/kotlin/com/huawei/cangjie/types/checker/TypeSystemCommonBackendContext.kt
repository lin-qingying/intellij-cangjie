package com.huawei.cangjie.types.checker

import com.huawei.cangjie.builtins.PrimitiveType
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.name.FqNameUnsafe
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.model.*


interface TypeSystemCommonBackendContext : TypeSystemContext {
    fun nullableAnyType(): SimpleTypeMarker
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

    fun TypeArgumentMarker.adjustedType(): CangJieTypeMarker {
        if (this.isStarProjection()) return nullableAnyType()
        return getType()
    }

//    fun TypeParameterMarker.representativeUpperBound(): CangJieTypeMarker
//
//    fun continuationTypeConstructor(): TypeConstructorMarker
//    fun functionNTypeConstructor(n: Int): TypeConstructorMarker
//
//    fun CangJieTypeMarker.getNameForErrorType(): String?
}
