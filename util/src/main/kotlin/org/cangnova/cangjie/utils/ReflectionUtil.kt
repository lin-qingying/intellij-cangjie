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
package org.cangnova.cangjie.utils

import com.intellij.openapi.util.Comparing
import com.intellij.util.containers.ContainerUtil
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.function.Predicate

/**
 * ReflectionUtil 提供了一组利用反射机制进行操作的工具方法。
 * 主要功能包括比较对象的公共非最终字段是否相等。
 */
object ReflectionUtil {
    /**
     * 比较两个对象的公共非最终字段是否相等。
     * 此方法通过反射获取对象的所有字段，并跳过标记了 @SkipInEquals 的字段。
     *
     * @param first  第一个对象，用于比较。
     * @param second 第二个对象，用于比较。
     * @return 如果所有比较的字段值都相等，则返回 true；否则返回 false。
     */
    @JvmStatic
    fun comparePublicNonFinalFieldsWithSkip(first: Any, second: Any): Boolean {
        return comparePublicNonFinalFields(
            first,
            second,
            Predicate { field -> field?.getAnnotation<SkipInEquals>(SkipInEquals::class.java) == null })
    }

    /**
     * 比较两个对象的公共非最终字段是否相等。
     * 此方法通过反射获取对象的所有字段，并根据 acceptPredicate 决定是否跳过某些字段。
     *
     * @param first          第一个对象，用于比较。
     * @param second         第二个对象，用于比较。
     * @param acceptPredicate 字段过滤器，决定是否接受某个字段进行比较。
     * @return 如果所有比较的字段值都相等，则返回 true；否则返回 false。
     */
    private fun comparePublicNonFinalFields(
        first: Any,
        second: Any,
        acceptPredicate: Predicate<Field?>?
    ): Boolean {
        val firstFields: MutableSet<Field?> = ContainerUtil.newHashSet<Field?>(*first.javaClass.getFields())

        for (field in second.javaClass.getFields()) {
            if (firstFields.contains(field)) {
                if (isPublic(field) && !isFinal(field) && (acceptPredicate == null || acceptPredicate.test(field))) {
                    try {
                        if (!Comparing.equal<Any?>(field.get(first), field.get(second))) {
                            return false
                        }
                    } catch (e: IllegalAccessException) {
                        throw RuntimeException(e)
                    }
                }
            }
        }

        return true
    }

    /**
     * 检查字段是否为公共字段。
     *
     * @param field 要检查的字段。
     * @return 如果字段是公共的，则返回 true；否则返回 false。
     */
    private fun isPublic(field: Field): Boolean {
        return (field.modifiers and Modifier.PUBLIC) != 0
    }

    /**
     * 检查字段是否为最终字段。
     *
     * @param field 要检查的字段。
     * @return 如果字段是最终的，则返回 true；否则返回 false。
     */
    private fun isFinal(field: Field): Boolean {
        return (field.modifiers and Modifier.FINAL) != 0
    }

    /**
     * SkipInEquals 注解用于标记在比较对象时应跳过的字段。
     * 它在运行时保留，因此可以通过反射访问。
     */
    @Retention(AnnotationRetention.RUNTIME)
    annotation class SkipInEquals
}
