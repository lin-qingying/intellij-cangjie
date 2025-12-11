/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.descriptors.annotations

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.annotationClass
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.fqNameUnsafe
import org.cangnova.cangjie.types.model.AnnotationMarker


/**
 * 扩展方法：返回安全的完全限定名或 null（当 fqNameUnsafe 不安全时）。
 */
fun DeclarationDescriptor.fqNameOrNull(): FqName? = fqNameUnsafe.takeIf { it.isSafe }?.toSafe()

/**
 * 注解描述符接口，表示一个注解实例的元信息。
 *
 * 该接口提供注解的类型、完全限定名（如可获取）、所有的值参数以及源信息。
 */
interface AnnotationDescriptor : AnnotationMarker {
    /** 注解的类型 */
    val type: CangJieType

    /**
     * 注解的完全限定名（如果可解析且非错误类型），否则为 null
     */
    val fqName: FqName?
        get() = annotationClass?.takeUnless(ErrorUtils::isError)?.fqNameOrNull()

    /** 注解的所有值参数映射，键为参数名，值为常量值 */
    val allValueArguments: Map<Name, ConstantValue<*>>

    /** 注解的源信息，用于导航和错误报告 */
    val source: SourceElement


}
