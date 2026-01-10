/*
 * Copyright 2026 LinQingYing. and contributors.
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





/**
 * 函数类型种类抽象类
 *
 * 用于描述语言内置或插件自定义的函数类型，如 Function、SuspendFunction、CFunction 等。
 *
 * @property packageFqName 包名
 * @property classNamePrefix 类名前缀，如 "Function""
 * @property annotationOnInvokeClassId 标注在 invoke 函数上的注解类标识，可为空
 */
abstract class FunctionTypeKind internal constructor(
    val packageFqName: FqName,
    val classNamePrefix: String,

    val annotationOnInvokeClassId: ClassId?
){

    /**
     * 指定渲染该函数类型时的前缀。
     * 例如：若 `prefixForTypeRender = @Some`，类型为 `some.CustomFunction2<Int, String, Double>`，
     * 则渲染为 `@Some (Int, String) -> Double`
     */
    open val prefixForTypeRender: String?
        get() = null





    /**
     * 根据参数个数生成对应的类名，如 Function2、Function3。
     */
  open  fun numberedClassName(arity: Int): Name = Name.identifier("$classNamePrefix$arity")

    /**
     * 根据参数个数生成对应的 ClassId。
     */
    fun numberedClassId(arity: Int): ClassId = ClassId(packageFqName, numberedClassName(arity))

    override fun toString(): String {
        return "$packageFqName.${classNamePrefix}N"
    }

    // ------------------------------------------- 内置函数类型种类 -------------------------------------------

    /**
     * 普通函数类型，如 Function0、Function1、Function2。
     */
    object Function : FunctionTypeKind(
        StandardNames.BASIC_PACKAGE_FQ_NAME,
        "Function",

        annotationOnInvokeClassId = null
    ) {

    }
    /**
     * c互操作函数类型，如 CFunc0。
     */
    object CFunction : FunctionTypeKind(
        StandardNames.STD_CORE_PACKAGE_FQ_NAME,
        "CFunction",

        annotationOnInvokeClassId = null
    ) {
        override fun numberedClassName(arity: Int): Name {
            return Name.identifier("CFunc" )
        }
    }
}
val FunctionTypeKind.isBasicFunction : Boolean
    get() = this == FunctionTypeKind.Function
