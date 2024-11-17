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

package com.linqingying.cangjie.serialization


import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.GeneratedMessage
import com.google.protobuf.GeneratedMessage.GeneratedExtension
import com.linqingying.cangjie.metadata.ProtoBuf


open class SerializerExtensionProtocol(
    val extensionRegistry: ExtensionRegistryLite,
    val packageFqName: GeneratedExtension<ProtoBuf.Package, Int>,
    val constructorAnnotation: GeneratedExtension<ProtoBuf.Constructor, List<ProtoBuf.Annotation>>,
    val classAnnotation: GeneratedExtension<ProtoBuf.Class, List<ProtoBuf.Annotation>>,
    val functionAnnotation: GeneratedExtension<ProtoBuf.Function, List<ProtoBuf.Annotation>>,
    val functionExtensionReceiverAnnotation: GeneratedExtension<ProtoBuf.Function, List<ProtoBuf.Annotation>>?,
    val propertyAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>,
    val propertyGetterAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>,
    val propertySetterAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>,
    val propertyExtensionReceiverAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>?,
//    val propertyBackingFieldAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>?,
//    val propertyDelegatedFieldAnnotation: GeneratedExtension<ProtoBuf.Property, List<ProtoBuf.Annotation>>?,
    val enumEntryAnnotation: GeneratedExtension<ProtoBuf.EnumEntry, List<ProtoBuf.Annotation>>,
    val compileTimeValue: GeneratedExtension<ProtoBuf.Property, ProtoBuf.Annotation.Argument.Value>,
    val parameterAnnotation: GeneratedExtension<ProtoBuf.ValueParameter, List<ProtoBuf.Annotation>>,
    val typeAnnotation: GeneratedExtension<ProtoBuf.Type, List<ProtoBuf.Annotation>>,
    val typeParameterAnnotation: GeneratedExtension<ProtoBuf.TypeParameter, List<ProtoBuf.Annotation>>
)
