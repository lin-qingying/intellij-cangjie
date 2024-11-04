package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.model.SimpleTypeMarker
//
//sealed class ValueClassRepresentation<Type : SimpleTypeMarker> {
//    abstract val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
//    abstract fun containsPropertyWithName(name: Name): Boolean
//    abstract fun getPropertyTypeByName(name: Name): Type?
//
//    fun <Other : SimpleTypeMarker> mapUnderlyingType(transform: (Type) -> Other): ValueClassRepresentation<Other> = when (this) {
//        is InlineClassRepresentation -> InlineClassRepresentation(underlyingPropertyName, transform(underlyingType))
//        is MultiFieldValueClassRepresentation ->
//            MultiFieldValueClassRepresentation(underlyingPropertyNamesToTypes.map { (name, type) -> name to transform(type) })
//    }
//}
