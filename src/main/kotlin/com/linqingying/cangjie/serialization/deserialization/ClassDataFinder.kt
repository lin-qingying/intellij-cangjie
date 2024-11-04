package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.name.ClassId

interface ClassDataFinder {
    fun findClassData(classId: ClassId):   ClassData?
}
