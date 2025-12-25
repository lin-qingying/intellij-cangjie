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

package org.cangnova.cangjie.renderer

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.ClassKind.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.TypeProjection

abstract class DescriptorRenderer {
    fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
        val options = (this as DescriptorRendererImpl).options.copy()
        options.changeOptions()
        options.lock()
        return DescriptorRendererImpl(options)
    }

    abstract fun renderMessage(message: String): String

    abstract fun renderType(type: CangJieType): String

    abstract fun renderFlexibleType(lowerRendered: String, upperRendered: String, builtIns: CangJieBuiltIns): String

    abstract fun renderTypeArguments(typeArguments: List<TypeProjection>, other: (StringBuilder) -> Unit = {}): String

    abstract fun renderTypeProjection(typeProjection: TypeProjection): String

    abstract fun renderTypeConstructor(typeConstructor: TypeConstructor): String

    abstract fun renderClassifierName(cclass: ClassifierDescriptor): String

    abstract fun renderAnnotation(annotation: AnnotationDescriptor, target: AnnotationUseSiteTarget? = null): String

    abstract fun render(declarationDescriptor: DeclarationDescriptor): String

    abstract fun renderValueParameters(
        parameters: Collection<ValueParameterDescriptor>,
        synthesizedParameterNames: Boolean
    ): String

    fun renderFunctionParameters(functionDescriptor: FunctionDescriptor): String =
        renderValueParameters(
            functionDescriptor.valueParameters,
            false /*functionDescriptor.hasSynthesizedParameterNames()*/
        )

    abstract fun renderName(name: Name, rootRenderedElement: Boolean): String

    abstract fun renderFqName(fqName: FqNameUnsafe): String

    interface ValueParametersHandler {
        fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder)
        fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder)

        fun appendBeforeValueParameter(
            parameter: ValueParameterDescriptor,
            parameterIndex: Int,
            parameterCount: Int,
            builder: StringBuilder
        )

        fun appendAfterValueParameter(
            parameter: ValueParameterDescriptor,
            parameterIndex: Int,
            parameterCount: Int,
            builder: StringBuilder
        )

        object DEFAULT : ValueParametersHandler {
            override fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder) {
                builder.append("(")
            }

            override fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder) {
                builder.append(")")
            }

            override fun appendBeforeValueParameter(
                parameter: ValueParameterDescriptor,
                parameterIndex: Int,
                parameterCount: Int,
                builder: StringBuilder
            ) {
            }

            override fun appendAfterValueParameter(
                parameter: ValueParameterDescriptor,
                parameterIndex: Int,
                parameterCount: Int,
                builder: StringBuilder
            ) {
                if (parameterIndex != parameterCount - 1) {
                    builder.append(", ")
                }
            }
        }
    }

    companion object {
        fun withOptions(changeOptions: DescriptorRendererOptions.() -> Unit): DescriptorRenderer {
            val options = DescriptorRendererOptionsImpl()
            options.changeOptions()
            options.lock()
            return DescriptorRendererImpl(options)
        }

        @JvmField
        val WITHOUT_MODIFIERS: DescriptorRenderer = withOptions {
            modifiers = emptySet()
        }

        @JvmField
        val COMPACT_WITH_MODIFIERS: DescriptorRenderer = withOptions {
            withDefinedIn = false
        }

        @JvmField
        val COMPACT: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
        }

        @JvmField
        val COMPACT_WITHOUT_SUPERTYPES: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            withoutSuperTypes = true
        }

        @JvmField
        val COMPACT_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            modifiers = emptySet()
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        @JvmField
        val ONLY_NAMES_WITH_SHORT_TYPES: DescriptorRenderer = withOptions {
            withDefinedIn = false
            modifiers = emptySet()
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            withoutTypeParameters = true
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            receiverAfterName = true
            renderCompanionObjectName = true
            withoutSuperTypes = true
            startFromName = true
        }

        @JvmField
        val FQ_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            modifiers = DescriptorRendererModifier.ALL_EXCEPT_ANNOTATIONS
        }

        @JvmField
        val FQ_NAMES_IN_TYPES_WITH_ANNOTATIONS: DescriptorRenderer = withOptions {
            modifiers = DescriptorRendererModifier.ALL
        }

        @JvmField
        val SHORT_NAMES_IN_TYPES: DescriptorRenderer = withOptions {
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.ONLY_NON_SYNTHESIZED
        }

        @JvmField
        val DEBUG_TEXT: DescriptorRenderer = withOptions {
            debugMode = true
            classifierNamePolicy = ClassifierNamePolicy.FULLY_QUALIFIED
            modifiers = DescriptorRendererModifier.ALL
        }

        @JvmField
        val HTML: DescriptorRenderer = withOptions {
            textFormat = RenderingFormat.HTML
            modifiers = DescriptorRendererModifier.ALL
        }

        fun getClassifierKindPrefix(classifier: ClassifierDescriptorWithTypeParameters): String = when (classifier) {
            is TypeAliasDescriptor ->
                "type"

            is InheritableDescriptor ->
                when (classifier.kind) {
                    STRUCT -> "struct"
                    CLASS -> "class"
                    INTERFACE -> "interface"
                    ENUM -> "enum"
                    EXTEND -> "extend"

                    ENUM_ENTRY -> "enum constructor"
                    BASIC -> "basic type"
                    TUPLE -> "tuple"
                    BUILTIN -> "builtIn type"
                }

            else ->
                throw AssertionError("Unexpected classifier: $classifier")
        }
    }
}


/**
 * 参数名称渲染策略枚举
 */
enum class ParameterNameRenderingPolicy {
    ALL, // 渲染所有参数名称
    ONLY_NON_SYNTHESIZED, // 仅渲染非合成的参数名称
    NONE // 不渲染参数名称
}

enum class DescriptorRendererModifier(val includeByDefault: Boolean) {
    VISIBILITY(true),
    MODALITY(true),
    OVERRIDE(true),
    ANNOTATIONS(false),

    //    INNER(true),
    MEMBER_KIND(true),

    STATIC(true),

    //    INLINE(true),
//    EXPECT(true),
    UNSAFE(true),
    CONST(true),

    //    LATEINIT(true),
    FUNC(true),
    VALUE(true)
    ;

    companion object {
        @JvmField
        val ALL_EXCEPT_ANNOTATIONS = entries.filter { it.includeByDefault }.toSet()

        @JvmField
        val ALL = entries.toSet()
    }
}

enum class AnnotationArgumentsRenderingPolicy(
    val includeAnnotationArguments: Boolean = false,
    val includeEmptyAnnotationArguments: Boolean = false
) {
    NO_ARGUMENTS,
    UNLESS_EMPTY(true),
    ALWAYS_PARENTHESIZED(includeAnnotationArguments = true, includeEmptyAnnotationArguments = true)
}

object ExcludedTypeAnnotations {
    val internalAnnotationsForResolve: Set<FqName> = setOf(

    )
}

enum class PropertyAccessorRenderingPolicy {
    PRETTY,
    DEBUG,
    NONE
}
enum class OverrideRenderingPolicy {
    RENDER_OVERRIDE,
    RENDER_OPEN,
    RENDER_OPEN_OVERRIDE
}

enum class RenderingFormat {
    PLAIN {
        override fun escape(string: String) = string
    },
    HTML {
        override fun escape(string: String) = string.replace("<", "&lt;").replace(">", "&gt;")
    };

    abstract fun escape(string: String): String
}