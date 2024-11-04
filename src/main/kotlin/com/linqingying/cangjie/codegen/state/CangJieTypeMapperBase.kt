/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.linqingying.cangjie.codegen.state


import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.types.checker.TypeSystemCommonBackendContext
import com.linqingying.cangjie.types.model.CangJieTypeMarker
import org.jetbrains.org.objectweb.asm.Type

abstract class CangJieTypeMapperBase {
    abstract val typeSystem: TypeSystemCommonBackendContext

    abstract fun mapClass(classifier: ClassifierDescriptor): Type

//    abstract fun mapTypeCommon(type: CangJieTypeMarker, mode: TypeMappingMode): Type

    fun mapDefaultImpls(descriptor: ClassDescriptor): Type =
        Type.getObjectType(mapClass(descriptor).internalName  )
}
