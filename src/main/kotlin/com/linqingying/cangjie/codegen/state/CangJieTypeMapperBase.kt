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
