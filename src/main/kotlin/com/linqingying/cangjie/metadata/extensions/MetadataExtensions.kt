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

package com.linqingying.cangjie.metadata.extensions

import com.linqingying.cangjie.metadata.*
import com.linqingying.cangjie.metadata.extensions.*
import com.linqingying.cangjie.metadata.internal.*

import com.linqingying.cangjie.metadata.node.*
import java.util.*

interface MetadataExtensions {
    fun readClassExtensions(cmClass: CmClass, proto: ProtoBuf.Class, c: ReadContext)

    fun readPackageExtensions(cmPackage: CmPackage, proto: ProtoBuf.Package, c: ReadContext)

    fun readModuleFragmentExtensions(
        cmModuleFragment: CmModuleFragment,
        proto: ProtoBuf.PackageFragment,
        c: ReadContext
    )

    fun readFunctionExtensions(cmFunction: CmFunction, proto: ProtoBuf.Function, c: ReadContext)
    fun readVariableExtensions(cmVariable: CmVariable, proto: ProtoBuf.Variable, c: ReadContext)

    fun readPropertyExtensions(cmProperty: CmProperty, proto: ProtoBuf.Property, c: ReadContext)

    fun readConstructorExtensions(cmConstructor: CmConstructor, proto: ProtoBuf.Constructor, c: ReadContext)

    fun readTypeParameterExtensions(
        cmTypeParameter: CmTypeParameter,
        proto: ProtoBuf.TypeParameter,
        c: ReadContext
    )

    fun readTypeExtensions(cmType: CmType, proto: ProtoBuf.Type, c: ReadContext)

    fun readTypeAliasExtensions(cmTypeAlias: CmTypeAlias, proto: ProtoBuf.TypeAlias, c: ReadContext)

    fun readValueParameterExtensions(
        cmValueParameter: CmValueParameter,
        proto: ProtoBuf.ValueParameter,
        c: ReadContext
    )

    fun writeClassExtensions(cmClass: CmClass, proto: ProtoBuf.Class.Builder, c: WriteContext)

    fun writePackageExtensions(cmPackage: CmPackage, proto: ProtoBuf.Package.Builder, c: WriteContext)

    fun writeModuleFragmentExtensions(
        cmModuleFragment: CmModuleFragment, proto: ProtoBuf.PackageFragment.Builder, c: WriteContext
    )

    fun writeFunctionExtensions(cmFunction: CmFunction, proto: ProtoBuf.Function.Builder, c: WriteContext)

    fun writePropertyExtensions(cmProperty: CmProperty, proto: ProtoBuf.Property.Builder, c: WriteContext)
    fun writeVariableExtensions(cmProperty: CmVariable, proto: ProtoBuf.Variable.Builder, c: WriteContext)

    fun writeConstructorExtensions(
        cmConstructor: CmConstructor, proto: ProtoBuf.Constructor.Builder, c: WriteContext
    )

    fun writeTypeParameterExtensions(
        cmTypeParameter: CmTypeParameter, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext
    )

    fun writeTypeExtensions(type: CmType, proto: ProtoBuf.Type.Builder, c: WriteContext)

    fun writeTypeAliasExtensions(typeAlias: CmTypeAlias, proto: ProtoBuf.TypeAlias.Builder, c: WriteContext)

    fun writeValueParameterExtensions(
        valueParameter: CmValueParameter, proto: ProtoBuf.ValueParameter.Builder, c: WriteContext
    )

    fun createClassExtension(): CmClassExtension

    fun createPackageExtension(): CmPackageExtension

    fun createModuleFragmentExtensions(): CmModuleFragmentExtension

    fun createFunctionExtension(): CmFunctionExtension

    fun createPropertyExtension(): CmPropertyExtension

    fun createConstructorExtension(): CmConstructorExtension

    fun createTypeParameterExtension(): CmTypeParameterExtension

    fun createTypeExtension(): CmTypeExtension

    fun createTypeAliasExtension(): CmTypeAliasExtension?

    fun createValueParameterExtension(): CmValueParameterExtension?


    companion object {
        internal val INSTANCES: List<MetadataExtensions> by lazy {
            ServiceLoader.load(MetadataExtensions::class.java, MetadataExtensions::class.java.classLoader).toList()
                .also {
                    if (it.isEmpty()) error(
                        "No MetadataExtensions instances found in the classpath. Please ensure that the META-INF/services/ " +
                                "is not stripped from your application and that the Java virtual machine is not running " +
                                "under a security manager"
                    )
                }
        }
    }
}
