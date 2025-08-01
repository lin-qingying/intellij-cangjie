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

package cn.cangnova.cangjie.resolve.deprecation

import cn.cangnova.cangjie.config.ApiVersion
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import cn.cangnova.cangjie.resolve.argumentValue
import cn.cangnova.cangjie.resolve.constants.StringValue
import cn.cangnova.cangjie.types.BuiltInAnnotationDescriptor

internal sealed class DeprecatedByAnnotation(
    val annotation: AnnotationDescriptor,
    override val target: DeclarationDescriptor,
    final override val propagatesToOverrides: Boolean,
    final override val forcePropagationToOverrides: Boolean = false,
) : DescriptorBasedDeprecationInfo() {

    init {
        require(!forcePropagationToOverrides || propagatesToOverrides) {
            "if something is `forcePropagationToOverrides`, it's expected that `propagatesToOverrides` == true, too"
        }
    }

    override val message: String?
        get() = (annotation.argumentValue("message") as? StringValue)?.value

    internal val replaceWithValue: String?
        get() {
            return null
//            val replaceWithAnnotation = (annotation.argumentValue(Deprecated::replaceWith.name) as? AnnotationValue)?.value
//            return (replaceWithAnnotation?.argumentValue(ReplaceWith::expression.name) as? StringValue)?.value
        }

    class StandardDeprecated(
        annotation: AnnotationDescriptor,
        target: DeclarationDescriptor,
        propagatesToOverrides: Boolean,
        forcePropagationToOverrides: Boolean = false,
    ) : DeprecatedByAnnotation(annotation, target, propagatesToOverrides, forcePropagationToOverrides) {
        override val deprecationLevel: DeprecationLevelValue
            get() = DeprecationLevelValue.WARNING
//            get() = when ((annotation.argumentValue("level") as? EnumValue)?.enumEntryName?.asString()) {
//                "WARNING" -> DeprecationLevelValue.WARNING
//                "ERROR" -> ERROR
//                "HIDDEN" -> HIDDEN
//                else -> WARNING
//            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StandardDeprecated) return false

            if (annotation != other.annotation) return false
            if (target != other.target) return false
            if (propagatesToOverrides != other.propagatesToOverrides) return false

            return true
        }

        override fun hashCode(): Int {
            var hash = annotation.hashCode()
            hash = hash * 31 + target.hashCode()
            hash = hash * 31 + propagatesToOverrides.hashCode()
            return hash
        }
    }

    class DeprecatedSince(
        annotation: AnnotationDescriptor,
        target: DeclarationDescriptor,
        propagatesToOverrides: Boolean,
        override val deprecationLevel: DeprecationLevelValue
    ) : DeprecatedByAnnotation(annotation, target, propagatesToOverrides) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DeprecatedSince) return false

            if (annotation != other.annotation) return false
            if (target != other.target) return false
            if (propagatesToOverrides != other.propagatesToOverrides) return false
            if (deprecationLevel != other.deprecationLevel) return false

            return true
        }

        override fun hashCode(): Int {
            var hash = annotation.hashCode()
            hash = hash * 31 + target.hashCode()
            hash = hash * 31 + propagatesToOverrides.hashCode()
            hash = hash * 31 + deprecationLevel.hashCode()
            return hash
        }
    }

    companion object {
        fun create(
            deprecatedAnnotation: AnnotationDescriptor,
            deprecatedSinceKotlinAnnotation: AnnotationDescriptor?,
            target: DeclarationDescriptor,
            propagatesToOverrides: Boolean,
            apiVersion: ApiVersion
        ): DeprecatedByAnnotation? {
            if (deprecatedSinceKotlinAnnotation != null) {
                val level = computeLevelForDeprecatedSinceCangJie(deprecatedSinceKotlinAnnotation, apiVersion) ?: return null
                return DeprecatedSince(deprecatedAnnotation, target, propagatesToOverrides, level)
            }
            val forcePropagationToOverrides =
                (deprecatedAnnotation as? BuiltInAnnotationDescriptor)?.forcePropagationDeprecationToOverrides == true
            return StandardDeprecated(
                deprecatedAnnotation,
                target,
                propagatesToOverrides || forcePropagationToOverrides,
                forcePropagationToOverrides = forcePropagationToOverrides
            )
        }
    }
}
