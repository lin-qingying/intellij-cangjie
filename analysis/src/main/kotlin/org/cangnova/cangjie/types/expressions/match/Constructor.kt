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

package org.cangnova.cangjie.types.expressions.match

import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.resolve.caches.type
import org.cangnova.cangjie.resolve.constants.*
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.deccriptorClass
import org.cangnova.cangjie.types.isBoolean
import org.cangnova.cangjie.types.isBuiltinTupleType
import org.cangnova.cangjie.types.isEnum
import org.cangnova.cangjie.types.isUnit
import org.cangnova.cangjie.resolve.constants.ConstantValue as CV
import  org.cangnova.cangjie.types.substitute as Usubstitute

fun List<CjTypeReference>.types(): List<CangJieType> {
    return mapNotNull {
        it.type
    }

}

//this 是 类型参数中的类型 type的上层类型
private fun CangJieType.substitute(type: CangJieType): CangJieType {
    val thisName = this.constructor.declarationDescriptor?.name ?: return this

//查找泛型参数的index
    var index: Int? = null
    type.deccriptorClass?.declaredTypeParameters?.forEachIndexed { index1, typeParameterDescriptor ->
        if (typeParameterDescriptor.name == thisName) {
            index = index1
        }
    }
    if (index != null) {
        return this.Usubstitute(type.arguments[index].type)
    }
    return this
}

sealed class Constructor {

    fun arity(type: CangJieType): Int {

        return when {
            type.isEnum && this is Enum -> {
                entry.typeReferences.size
            }

            type.isBuiltinTupleType && this is Single -> {
                type.arguments.size
            }

            else -> 0
        }

    }

    fun subTypes(type: CangJieType): List<CangJieType> {

        return when (this) {
            is Enum -> {
                entry.typeReferences.types().map {
                    it.substitute(type)
                }
            }

            is Single -> {
                if (type.isBuiltinTupleType) {
                    type.arguments.map { it.type }
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }

    }

    open fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean = false

    /** Enum variants */
    data class Enum(val entry: CjEnumConstructor) : Constructor()
    data class Type(val type: CangJieType) : Constructor()

    /** 不因构造函数而变化的所有模式的构造函数，例如结构模式和固定长度数组 */
    data object Single : Constructor() {
        override fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean = true
    }

    /** Literal values */
    data class ConstantValue(val value: CV<*>) : Constructor() {
        override fun coveredByRange(from: CV<*>, to: CV<*>, included: Boolean): Boolean =
            if (included) {
                value >= from && value <= to
            } else {
                value >= from && value < to
            }
    }

    companion object {

        private fun allConstructorsLazy(ty: CangJieType): Sequence<Constructor> =
            when {
                ty.isBoolean -> sequenceOf(true, false).map { ConstantValue(BoolValue(it)) }
                ty.isUnit -> sequenceOf(true, false).map { ConstantValue(UnitValue) }
                ty.isEnum ->
                    (ty.deccriptorClass?.source?.getPsi() as? CjEnum)?.constructor?.asSequence()?.map { Enum(it) }
                        ?: emptySequence()

                else -> sequenceOf(Single)
            }

        fun allConstructors(ty: CangJieType): List<Constructor> = allConstructorsLazy(ty).toList()

    }
}

private operator fun CV<*>.compareTo(other: CV<*>): Int {
    return when (this) {
        is UnitValue if other is UnitValue -> 0
        is BoolValue if other is BoolValue -> value.compareTo(other.value)
        is Int64Value if other is Int64Value -> value.compareTo(other.value)
        is Int32Value if other is Int32Value -> value.compareTo(other.value)
        is Int16Value if other is Int16Value -> value.compareTo(other.value)
        is Int8Value if other is Int8Value -> value.compareTo(other.value)
        is UInt64Value if other is UInt64Value -> value.compareTo(other.value)
        is UInt32Value if other is UInt32Value -> value.compareTo(other.value)
        is UInt16Value if other is UInt16Value -> value.compareTo(other.value)
        is UInt8Value if other is UInt8Value -> value.compareTo(other.value)
        is Float64Value if other is Float64Value -> value.compareTo(other.value)
        is Float32Value if other is Float32Value -> value.compareTo(other.value)
        is Float16Value if other is Float16Value -> value.compareTo(other.value)
        is StringValue if other is StringValue -> value.compareTo(other.value)
        is RuneValue if other is RuneValue -> value.compareTo(other.value)
        else -> throw CheckMatchException("Comparison of incompatible types: $javaClass and ${other.javaClass}")
    }
}
