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

package com.linqingying.cangjie.serialization.deserialization

import com.google.protobuf.ExtensionRegistryLite
import com.linqingying.cangjie.metadata.builtins.BuiltInsProtoBuf
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.serialization.SerializerExtensionProtocol

object BuiltInSerializerProtocol   : SerializerExtensionProtocol(
ExtensionRegistryLite.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions),
BuiltInsProtoBuf.packageFqName,
BuiltInsProtoBuf.constructorAnnotation,
BuiltInsProtoBuf.classAnnotation,
BuiltInsProtoBuf.functionAnnotation,
functionExtensionReceiverAnnotation = null,
BuiltInsProtoBuf.propertyAnnotation,
BuiltInsProtoBuf.propertyGetterAnnotation,
BuiltInsProtoBuf.propertySetterAnnotation,
propertyExtensionReceiverAnnotation = null,

BuiltInsProtoBuf.enumEntryAnnotation,
BuiltInsProtoBuf.compileTimeValue,
BuiltInsProtoBuf.parameterAnnotation,
BuiltInsProtoBuf.typeAnnotation,
BuiltInsProtoBuf.typeParameterAnnotation
) {
    const val BUILTINS_FILE_EXTENSION = "cangjie_builtins"
    const val DOT_DEFAULT_EXTENSION = ".$BUILTINS_FILE_EXTENSION"

    fun getBuiltInsFilePath(fqName: FqName): String =
        fqName.asString().replace('.', '/') + "/" + getBuiltInsFileName(
            fqName
        )

    fun getBuiltInsFileName(fqName: FqName): String =
        shortName(fqName) + DOT_DEFAULT_EXTENSION

    private fun shortName(fqName: FqName): String =
        if (fqName.isRoot) "default-package" else fqName.shortName().asString()
}
