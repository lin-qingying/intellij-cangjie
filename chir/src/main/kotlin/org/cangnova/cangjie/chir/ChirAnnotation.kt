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

package org.cangnova.cangjie.chir

/**
 * 表示一个可以被注解的 CHIR 元素。
 *
 * 该接口为所有支持注解的 CHIR 元素提供统一的注解访问能力。
 */
interface ChirAnnotationContainer {
    /**
     * 该元素上的注解列表
     */
    val annotations: List<ChirAnnotation>
}

/**
 * 表示一个注解
 */
interface ChirAnnotation : ChirElement {
    /**
     * 注解类型
     */
    val annotationType: ChirAnnotationType

    /**
     * 注解参数
     */
    val arguments: List<ChirAnnotationArgument>

    override fun <R, D> accept(visitor: ChirVisitor<R, D>, data: D): R =
        visitor.visitAnnotation(this, data)
}

/**
 * 注解类型信息
 */
interface ChirAnnotationType {
    /**
     * 注解的完全限定名
     */
    val qualifiedName: String
}

/**
 * 注解参数
 */
interface ChirAnnotationArgument {
    /**
     * 参数名称（可选，对于位置参数可能为 null）
     */
    val name: String?

    /**
     * 参数值
     */
    val value: ChirAnnotationValue
}

/**
 * 注解参数值
 */
sealed interface ChirAnnotationValue {
    /**
     * 常量值（字符串、数字、布尔等）
     */
    data class ConstantValue(val value: Any?) : ChirAnnotationValue

    /**
     * 数组值
     */
    data class ArrayValue(val values: List<ChirAnnotationValue>) : ChirAnnotationValue

    /**
     * 枚举值
     */
    data class EnumValue(val enumType: String, val enumEntry: String) : ChirAnnotationValue

    /**
     * 类引用值
     */
    data class ClassValue(val classType: String) : ChirAnnotationValue

    /**
     * 嵌套注解
     */
    data class AnnotationValue(val annotation: ChirAnnotation) : ChirAnnotationValue
}