package com.huawei.cangjie.resolve.deprecation

import com.google.protobuf.EnumValue
import com.huawei.cangjie.config.ApiVersion
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.annotations.AnnotationDescriptor
import com.huawei.cangjie.resolve.argumentValue
import com.huawei.cangjie.resolve.constants.ConstantValue
import com.huawei.cangjie.resolve.constants.StringValue
import com.huawei.cangjie.types.BuiltInAnnotationDescriptor
import com.huawei.cangjie.utils.rethrow

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
