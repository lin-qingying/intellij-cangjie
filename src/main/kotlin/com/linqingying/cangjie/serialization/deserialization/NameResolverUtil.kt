package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.metadata.deserialization.NameResolver
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.Name

fun NameResolver.getClassId(index: Int): ClassId {
    return ClassId.fromString(getQualifiedClassName(index), isLocalClassName(index))
}

fun NameResolver.getName(index: Int): Name =
    Name.guessByFirstCharacter(getString(index))
