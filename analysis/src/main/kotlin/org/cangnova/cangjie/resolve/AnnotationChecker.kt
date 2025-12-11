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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.CangJieTarget.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.types.expressions.ExpressionTypingUtils

class AnnotationChecker {


    fun check(annotated: CjAnnotated, trace: BindingTrace, descriptor: DeclarationDescriptor? = null) {

    }

    fun checkExpression(expression: CjExpression, trace: BindingTrace) {

    }

    companion object {

        fun getActualTargetList(
            annotated: CjElement, descriptor: DeclarationDescriptor?, context: BindingContext
        ): TargetList {
            return when (annotated) {
                is CjTypeStatement -> {
                    (descriptor as? ClassDescriptor)?.let {
                        TargetList(
                            CangJieTarget.classActualTargets(
                                it.kind,

                                isLocalClass = DescriptorUtils.isLocal(it)
                            )
                        )
                    } ?: TargetLists.T_CLASSIFIER
                }

                is CjDestructuringDeclarationEntry -> TargetLists.T_LOCAL_VARIABLE
                is CjProperty -> {
                    when {

                        annotated.parent is CjStruct || annotated.parent?.parent is CjStruct -> TargetLists.T_STRUCT_MEMBER_PROPERTY
                        annotated.parent is CjInterface || annotated.parent?.parent is CjInterface -> TargetLists.T_INTERFACE_MEMBER_PROPERTY
                        annotated.parent is CjExtend || annotated.parent?.parent is CjExtend -> TargetLists.T_EXTEND_MEMBER_PROPERTY
                        annotated.parent is CjClass || annotated.parent?.parent is CjClass -> TargetLists.T_CLASS_MEMBER_PROPERTY
                        annotated.parent is CjEnum || annotated.parent?.parent is CjEnum -> TargetLists.T_ENUM_MEMBER_PROPERTY

                        annotated.parent is CjTypeStatement || annotated.parent is CjAbstractClassBody -> TargetLists.T_MEMBER_PROPERTY
                        else -> TargetLists.T_MEMBER_PROPERTY
                    }

                }

                is CjVariable -> {
                    when {
                        annotated.isLocal -> TargetLists.T_LOCAL_VARIABLE
                        annotated.isMember -> TargetLists.T_MEMBER_VARIABLE
                        else -> TargetLists.T_TOP_LEVEL_VARIABLE
                    }
                }

                is CjParameter -> {
                    val destructuringDeclaration = annotated.destructuringDeclaration
                    when {
                        destructuringDeclaration != null -> TargetLists.T_DESTRUCTURING_DECLARATION
                        annotated.hasLetOrVar() -> TargetLists.T_VALUE_PARAMETER_WITH_LET
                        else -> TargetLists.T_VALUE_PARAMETER_WITHOUT_LET
                    }
                }

                is CjConstructor<*> -> TargetLists.T_CONSTRUCTOR
                is CjMacroDeclaration -> TargetLists.T_MACRO
                is CjFunction -> {
                    when {
                        ExpressionTypingUtils.isFunctionExpression(descriptor) -> TargetLists.T_FUNCTION_EXPRESSION
                        annotated.name == null -> TargetLists.T_FUNCTION_EXPRESSION
                        annotated.isLocal -> TargetLists.T_LOCAL_FUNCTION
                        annotated.parent is CjStruct || annotated.parent.parent is CjStruct -> TargetLists.T_STRUCT_MEMBER_FUNCTION
                        annotated.parent is CjInterface || annotated.parent.parent is CjInterface -> TargetLists.T_INTERFACE_MEMBER_FUNCTION
                        annotated.parent is CjExtend || annotated.parent.parent is CjExtend -> TargetLists.T_EXTEND_MEMBER_FUNCTION
                        annotated.parent is CjClass || annotated.parent.parent is CjClass -> TargetLists.T_CLASS_MEMBER_FUNCTION
                        annotated.parent is CjEnum || annotated.parent.parent is CjEnum -> TargetLists.T_ENUM_MEMBER_FUNCTION

                        annotated.parent is CjTypeStatement || annotated.parent is CjAbstractClassBody -> TargetLists.T_MEMBER_FUNCTION
                        else -> TargetLists.T_TOP_LEVEL_FUNCTION
                    }
                }

                is CjTypeAlias -> TargetLists.T_TYPEALIAS
//                is CjPropertyAccessor -> if (annotated.isGetter) TargetLists.T_PROPERTY_GETTER else TargetLists.T_PROPERTY_SETTER
                is CjTypeReference -> TargetLists.T_TYPE_REFERENCE
                is CjFile -> TargetLists.T_FILE
                is CjTypeParameter -> TargetLists.T_TYPE_PARAMETER
                is CjTypeProjection -> TargetLists.T_TYPE_PROJECTION

                is CjAnonymousInitializer -> TargetLists.T_INITIALIZER
                is CjDestructuringDeclaration -> TargetLists.T_DESTRUCTURING_DECLARATION
                is CjLambdaExpression -> TargetLists.T_FUNCTION_LITERAL

                is CjExpression -> TargetLists.T_EXPRESSION
                else -> TargetLists.EMPTY
            }
        }

        fun getDeclarationSiteActualTargetList(
            annotated: CjElement, descriptor: ClassDescriptor?, context: BindingContext
        ): List<CangJieTarget> {
            return getActualTargetList(annotated, descriptor, context).defaultTargets
        }
    }
}
private typealias TargetList = AnnotationTargetList
private typealias TargetLists = AnnotationTargetLists

class AnnotationTargetList(
    val defaultTargets: List<CangJieTarget>,
    val canBeSubstituted: List<CangJieTarget> = emptyList(),
    val onlyWithUseSiteTarget: List<CangJieTarget> = emptyList()
)

enum class CangJieTarget(val description: String, val isDefault: Boolean = true) {
    LOCAL_CLASS("local class", false), CLASS("class"), EXTEND("extend"), CLASS_ONLY("class", false), STRUCT(
        "struct",
        false
    ),
    ENUM("enum ", false), INTERFACE("interface", false), ENUM_ENTRY(
        "enum entry",
        false
    ),
    PROPERTY("property"),                      // includes *_PROPERTY (with and without backing field), PROPERTY_PARAMETER, ENUM_ENTRY
    VARIABLE("variable"),                      // includes *_PROPERTY (with and without backing field), PROPERTY_PARAMETER, ENUM_ENTRY
    TYPEALIAS("typealias", false), DESTRUCTURING_DECLARATION(
        "destructuring declaration",
        false
    ),
    EXPRESSION("expression", false),           // includes FUNCTION_LITERAL, OBJECT_LITERAL
    FIELD("field"),

    LOCAL_VARIABLE("local variable"),// includes MEMBER_PROPERTY_WITH_FIELD, TOP_LEVEL_PROPERTY_WITH_FIELD, PROPERTY_PARAMETER, ENUM_ENTRY
    INITIALIZER("initializer", false), VALUE_PARAMETER("value parameter"), MEMBER_VARIABLE(
        "member variable",
        false
    ),
    MEMBER_PROPERTY("member property", false), CLASS_MEMBER_PROPERTY(
        "class member property",
        false
    ),
    STRUCT_MEMBER_PROPERTY("struct member property", false), EXTEND_MEMBER_PROPERTY(
        "extend member property",
        false
    ),
    INTERFACE_MEMBER_PROPERTY("interface member property", false),

    ENUM_MEMBER_PROPERTY("enum member property", false),


    MACRO("macro"),

    // includes PROPERTY_PARAMETER, with and without field/delegate
    CONSTRUCTOR("constructor"), PROPERTY_GETTER("getter"), PROPERTY_SETTER("setter"), LAMBDA_EXPRESSION(
        "lambda expression",
        false
    ),
    TOP_LEVEL_FUNCTION("top level function", false), TOP_LEVEL_VARIABLE(
        "top level variable",
        false
    ), // with and without field/delegate
    BACKING_FIELD("backing field"), TOP_LEVEL_PROPERTY("top level property", false), FILE(
        "file",
        false
    ),
    TYPE_PROJECTION(
        "type projection",
        false
    ),
    FUNCTION("function"),                      // includes *_FUNCTION and FUNCTION_LITERAL
    ANONYMOUS_FUNCTION("anonymous function", false), LOCAL_FUNCTION(
        "local function",
        false
    ),
    TYPE_PARAMETER("type parameter", false), MEMBER_FUNCTION(
        "member function",
        false
    ),
    STRUCT_MEMBER_FUNCTION("struct member function", false), CLASS_MEMBER_FUNCTION(
        "class member function",
        false
    ),
    INTERFACE_MEMBER_FUNCTION("interface member function", false),
    EXTEND_MEMBER_FUNCTION(
        "extend member function",
        false
    ),
    ENUM_MEMBER_FUNCTION("enum member function", false),

    TYPE("type usage", false),

    ;

    companion object {
        val LOCAL_CLASS_LIST = listOf(LOCAL_CLASS, CLASS)
        val CLASS_LIST = listOf(CLASS_ONLY, CLASS)
        val STRUCT_LIST = listOf(STRUCT, CLASS)
        val INTERFACE_LIST = listOf(INTERFACE, CLASS)
        val ENUM_LIST = listOf(ENUM, CLASS)
        val ENUM_ENTRY_LIST = listOf(ENUM_ENTRY, PROPERTY, VARIABLE, FIELD)
        val EXTEND_LIST = listOf(EXTEND)
        fun classActualTargets(
            kind: ClassKind,

            isLocalClass: Boolean
        ): List<CangJieTarget> = when (kind) {
            ClassKind.EXTEND ->
                // inner local classes should be CLASS_ONLY, not LOCAL_CLASS

                EXTEND_LIST


//        ClassKind.ANNOTATION_CLASS -> ANNOTATION_CLASS_LIST
            ClassKind.CLASS ->
                // inner local classes should be CLASS_ONLY, not LOCAL_CLASS
                if (isLocalClass) {
                    LOCAL_CLASS_LIST
                } else {
                    CLASS_LIST
                }

            ClassKind.STRUCT ->

                STRUCT_LIST

            ClassKind.INTERFACE -> INTERFACE_LIST
            ClassKind.ENUM -> if (isLocalClass) {
                LOCAL_CLASS_LIST
            } else {
                ENUM_LIST
            }

            ClassKind.ENUM_ENTRY -> ENUM_ENTRY_LIST
            else -> TODO()
        }
    }

}

object AnnotationTargetLists {
    val T_CLASSIFIER = targetList(CLASS)
    val T_DESTRUCTURING_DECLARATION = targetList(DESTRUCTURING_DECLARATION)
    val T_INITIALIZER = targetList(INITIALIZER)
    val T_VALUE_PARAMETER_WITHOUT_LET = targetList(VALUE_PARAMETER)
    val T_LOCAL_VARIABLE = targetList(LOCAL_VARIABLE)
    val T_MEMBER_VARIABLE = targetList(MEMBER_VARIABLE, VARIABLE)
    val T_MEMBER_PROPERTY = targetList(MEMBER_PROPERTY, PROPERTY)
    val T_STRUCT_MEMBER_PROPERTY = targetList(
        STRUCT_MEMBER_PROPERTY,


        *T_MEMBER_PROPERTY.defaultTargets.toTypedArray()
    )
    val T_CLASS_MEMBER_PROPERTY = targetList(
        CLASS_MEMBER_PROPERTY,


        *T_MEMBER_PROPERTY.defaultTargets.toTypedArray()
    )
    val T_ENUM_MEMBER_PROPERTY = targetList(
        ENUM_MEMBER_PROPERTY,


        *T_MEMBER_PROPERTY.defaultTargets.toTypedArray()
    )
    val T_EXTEND_MEMBER_PROPERTY = targetList(
        EXTEND_MEMBER_PROPERTY,


        *T_MEMBER_PROPERTY.defaultTargets.toTypedArray()
    )
    val T_INTERFACE_MEMBER_PROPERTY = targetList(
        INTERFACE_MEMBER_PROPERTY,


        *T_MEMBER_PROPERTY.defaultTargets.toTypedArray()
    )

    val T_TOP_LEVEL_PROPERTY = targetList(TOP_LEVEL_PROPERTY, PROPERTY)
    val T_TOP_LEVEL_VARIABLE = targetList(TOP_LEVEL_VARIABLE, VARIABLE)

    val T_TYPEALIAS = targetList(TYPEALIAS)
    val T_VALUE_PARAMETER_WITH_LET = targetList(VALUE_PARAMETER, VARIABLE, MEMBER_PROPERTY) {
        extraTargets(FIELD)
//        onlyWithUseSiteTarget(PROPERTY_GETTER, PROPERTY_SETTER)
    }
    val T_CONSTRUCTOR = targetList(CONSTRUCTOR)
    val T_MACRO = targetList(MACRO)

    val T_EXPRESSION = targetList(EXPRESSION)
    val T_FUNCTION_LITERAL = targetList(LAMBDA_EXPRESSION, FUNCTION, EXPRESSION)
    val T_TYPE_PARAMETER = targetList(TYPE_PARAMETER)
    val T_TYPE_PROJECTION = targetList(TYPE_PROJECTION)
    val T_FILE = targetList(FILE)
    val T_FUNCTION_EXPRESSION = targetList(ANONYMOUS_FUNCTION, FUNCTION, EXPRESSION)
    val T_LOCAL_FUNCTION = targetList(LOCAL_FUNCTION, FUNCTION) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }
    val T_MEMBER_FUNCTION = targetList(MEMBER_FUNCTION, FUNCTION) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }
    val T_TOP_LEVEL_FUNCTION = targetList(TOP_LEVEL_FUNCTION, FUNCTION) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }
    val T_STRUCT_MEMBER_FUNCTION = targetList(
        STRUCT_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray()
    ) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }
    val T_CLASS_MEMBER_FUNCTION = targetList(
        CLASS_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray()
    ) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }
    val T_ENUM_MEMBER_FUNCTION = targetList(
        ENUM_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray()
    ) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }

    val T_TYPE_REFERENCE = targetList(TYPE) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }
    val T_INTERFACE_MEMBER_FUNCTION = targetList(
        INTERFACE_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray()
    ) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }
    val T_EXTEND_MEMBER_FUNCTION = targetList(
        EXTEND_MEMBER_FUNCTION, *T_MEMBER_FUNCTION.defaultTargets.toTypedArray()
    ) {
        onlyWithUseSiteTarget(VALUE_PARAMETER)
    }

    private fun targetList(
        vararg target: CangJieTarget, otherTargets: TargetListBuilder.() -> Unit = {}
    ): AnnotationTargetList {
        val builder = TargetListBuilder(*target)
        builder.otherTargets()
        return builder.build()
    }

    val EMPTY = targetList()

    private class TargetListBuilder(vararg val defaultTargets: CangJieTarget) {
        private var canBeSubstituted: List<CangJieTarget> = listOf()
        private var onlyWithUseSiteTarget: List<CangJieTarget> = listOf()

        fun extraTargets(vararg targets: CangJieTarget) {
            canBeSubstituted = targets.toList()
        }

        fun onlyWithUseSiteTarget(vararg targets: CangJieTarget) {
            onlyWithUseSiteTarget = targets.toList()
        }

        fun build() = AnnotationTargetList(defaultTargets.toList(), canBeSubstituted, onlyWithUseSiteTarget)
    }
}
