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

package org.cangnova.cangjie.types.functions

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

abstract class FunctionTypeKind internal constructor(
    val packageFqName: FqName,
    val classNamePrefix: String,
    val isReflectType: Boolean,
    val annotationOnInvokeClassId: ClassId?
){
    /*
       * This constructor is needed to enforce not nullable [annotationOnInvokeClassId] for
       *   functional kinds provided by compiler plugins
       */
    constructor(
        packageFqName: FqName,
        classNamePrefix: String,
        annotationOnInvokeClassId: ClassId,
        isReflectType: Boolean
    ) : this(packageFqName, classNamePrefix, isReflectType, annotationOnInvokeClassId)
    /*
       * Specifies how corresponding type will be rendered
       * E.g. if `prefixForTypeRender = @Some` and type is `some.CustomFunction2<Int, String, Double>`,
       *   then type will be rendered as `@Some (Int, String) -> Double`
       */
    open val prefixForTypeRender: String?
        get() = null

    /**
     * Specifies the first language version for which to serialize custom function types
     * into cangjie metadata. Until that version, custom function types will be serialized
     * with the legacy scheme: as a FunctionN/KFunctionN with the annotation used for the
     * custom function type.
     *
     * If no version is specified, custom function types are serialized to cangjie metadata.
     *
     * Serialization using the legacy format allows libraries compiled with K2 with a
     * K2 plugin that uses custom function types to be used by clients using a K1 compiler
     * with a K1 compiler plugin that understands the custom function types.
     */
    open val serializeAsFunctionWithAnnotationUntil: String?
        get() = null

    /**
     * @return corresponding non-reflect kind for reflect kind
     * @return [this] if [isReflectType] is false
     *
     * Should be overridden for reflect kinds
     */
    open fun nonReflectKind(): FunctionTypeKind {
        return if (isReflectType) error("Should be overridden explicitly") else this
    }

    /**
     * @return corresponding reflect kind for non-reflect kind
     * @return [this] if [isReflectType] is true
     *
     * Should be overridden for non reflect kinds
     */
    open fun reflectKind(): FunctionTypeKind {
        return if (isReflectType) this else error("Should be overridden explicitly")
    }

    fun numberedClassName(arity: Int): Name = Name.identifier("$classNamePrefix$arity")

    fun numberedClassId(arity: Int): ClassId = ClassId(packageFqName, numberedClassName(arity))

    override fun toString(): String {
        return "$packageFqName.${classNamePrefix}N"
    }

    // ------------------------------------------- Builtin functional kinds -------------------------------------------

    object Function : FunctionTypeKind(
        StandardNames.BASIC_PACKAGE_FQ_NAME,
        "Function",
        isReflectType = false,
        annotationOnInvokeClassId = null
    ) {
//        override fun reflectKind(): FunctionTypeKind = CFunction
    }
//
//    object SuspendFunction : FunctionTypeKind(
//        StandardNames.COROUTINES_PACKAGE_FQ_NAME,
//        "SuspendFunction",
//        isReflectType = false,
//        annotationOnInvokeClassId = null
//    ) {
//        override val prefixForTypeRender: String
//            get() = "suspend"
//
//        override fun reflectKind(): FunctionTypeKind = CSuspendFunction
//    }
//
//    object CFunction : FunctionTypeKind(
//        StandardNames.CANGJIE_REFLECT_FQ_NAME,
//        "CFunction",
//        isReflectType = true,
//        annotationOnInvokeClassId = null
//    ) {
//        override fun nonReflectKind(): FunctionTypeKind = Function
//    }
//
//    object CSuspendFunction : FunctionTypeKind(
//        StandardNames.CANGJIE_REFLECT_FQ_NAME,
//        "CSuspendFunction",
//        isReflectType = true,
//        annotationOnInvokeClassId = null
//    ) {
//        override fun nonReflectKind(): FunctionTypeKind = SuspendFunction
//    }
}
val FunctionTypeKind.isBasicFunction : Boolean
    get() = this.nonReflectKind() == FunctionTypeKind.Function
