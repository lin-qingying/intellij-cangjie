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

package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.lexer.CjTokens.*
import java.util.*

val deprecatedParentTargetMap = mapOf<CjKeywordToken, Set<CangJieTarget>>()

val defaultVisibilityTargets: EnumSet<CangJieTarget> = EnumSet.of(
    CangJieTarget.CLASS_ONLY,
    CangJieTarget.STRUCT,
    CangJieTarget.INTERFACE,
    CangJieTarget.ENUM,
    CangJieTarget.MEMBER_FUNCTION,
    CangJieTarget.TOP_LEVEL_FUNCTION,
    CangJieTarget.MEMBER_VARIABLE,
    CangJieTarget.VARIABLE,
    CangJieTarget.FUNCTION,
    CangJieTarget.INTERFACE_MEMBER_FUNCTION,
    CangJieTarget.EXTEND_MEMBER_FUNCTION,
    CangJieTarget.MEMBER_PROPERTY,
    CangJieTarget.TOP_LEVEL_VARIABLE,
    CangJieTarget.CONSTRUCTOR,
    CangJieTarget.TYPEALIAS,
    CangJieTarget.STRUCT_MEMBER_FUNCTION

)

val possibleTargetMap = mapOf(

    STATIC_KEYWORD to EnumSet.of(
        CangJieTarget.MEMBER_FUNCTION,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_VARIABLE,
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
                CangJieTarget.EXTEND_MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
    ),
//    ENUM_KEYWORD to EnumSet.of(CangJieTarget.ENUM_CLASS),
    ABSTRACT_KEYWORD to EnumSet.of(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.LOCAL_CLASS,
//        CangJieTarget.INTERFACE,
//        CangJieTarget.MEMBER_PROPERTY,
//        CangJieTarget.MEMBER_FUNCTION
    ),
    MUT_KEYWORD to EnumSet.of(

        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.STRUCT_MEMBER_FUNCTION
    ),
    OPEN_KEYWORD to EnumSet.of(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.LOCAL_CLASS,
        CangJieTarget.INTERFACE,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_FUNCTION
    ),
//    FINAL_KEYWORD to EnumSet.of(
//        CangJieTarget.CLASS_ONLY,
//        CangJieTarget.LOCAL_CLASS,
//        CangJieTarget.ENUM_CLASS,
//        CangJieTarget.OBJECT,
//        CangJieTarget.MEMBER_PROPERTY,
//        CangJieTarget.MEMBER_FUNCTION
//    ),
    SEALED_KEYWORD to EnumSet.of(CangJieTarget.CLASS_ONLY, CangJieTarget.INTERFACE),
//    INNER_KEYWORD to EnumSet.of(CangJieTarget.CLASS_ONLY),

    OVERRIDE_KEYWORD to EnumSet.of(
        CangJieTarget.MEMBER_PROPERTY, CangJieTarget.MEMBER_FUNCTION, CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.STRUCT_MEMBER_FUNCTION
    ),
    PRIVATE_KEYWORD to defaultVisibilityTargets + CangJieTarget.BACKING_FIELD,
    PUBLIC_KEYWORD to defaultVisibilityTargets + CangJieTarget.MACRO ,
    INTERNAL_KEYWORD to defaultVisibilityTargets + CangJieTarget.BACKING_FIELD,
    PROTECTED_KEYWORD to EnumSet.of(
        CangJieTarget.FUNCTION,
        CangJieTarget.VARIABLE,
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.INTERFACE,
        CangJieTarget.ENUM,
//        CangJieTarget.ANNOTATION_CLASS,
        CangJieTarget.MEMBER_FUNCTION,
//        CangJieTarget.PROPERTY_GETTER,
//        CangJieTarget.PROPERTY_SETTER,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_VARIABLE,
        CangJieTarget.CONSTRUCTOR,
        CangJieTarget.TYPEALIAS
    ),
//    IN_KEYWORD to EnumSet.of(CangJieTarget.TYPE_PARAMETER, CangJieTarget.TYPE_PROJECTION),
//    OUT_KEYWORD to EnumSet.of(CangJieTarget.TYPE_PARAMETER, CangJieTarget.TYPE_PROJECTION),
//    REIFIED_KEYWORD to EnumSet.of(CangJieTarget.TYPE_PARAMETER),
//    VARARG_KEYWORD to EnumSet.of(CangJieTarget.VALUE_PARAMETER, CangJieTarget.PROPERTY_PARAMETER),
//    COMPANION_KEYWORD to EnumSet.of(CangJieTarget.OBJECT),
//    LATEINIT_KEYWORD to EnumSet.of(
//        CangJieTarget.MEMBER_PROPERTY,
//        CangJieTarget.TOP_LEVEL_PROPERTY,
//        CangJieTarget.LOCAL_VARIABLE,
//        CangJieTarget.BACKING_FIELD
//    ),
//    DATA_KEYWORD to EnumSet.of(CangJieTarget.CLASS_ONLY, CangJieTarget.LOCAL_CLASS, CangJieTarget.STANDALONE_OBJECT),
//    INLINE_KEYWORD to EnumSet.of(
//        CangJieTarget.FUNCTION,
//        CangJieTarget.PROPERTY,
//        CangJieTarget.PROPERTY_GETTER,
//        CangJieTarget.PROPERTY_SETTER,
//        CangJieTarget.CLASS_ONLY
//    ),
//    NOINLINE_KEYWORD to EnumSet.of(CangJieTarget.VALUE_PARAMETER),
//    TAILREC_KEYWORD to EnumSet.of(CangJieTarget.FUNCTION),
//    SUSPEND_KEYWORD to EnumSet.of(
//        CangJieTarget.MEMBER_FUNCTION,
//        CangJieTarget.TOP_LEVEL_FUNCTION,
//        CangJieTarget.LOCAL_FUNCTION,
//        CangJieTarget.ANONYMOUS_FUNCTION
//    ),
//    EXTERNAL_KEYWORD to EnumSet.of(
//        CangJieTarget.FUNCTION,
//        CangJieTarget.PROPERTY,
//        CangJieTarget.PROPERTY_GETTER,
//        CangJieTarget.PROPERTY_SETTER,
//        CangJieTarget.CLASS
//    ),
//    ANNOTATION_KEYWORD to EnumSet.of(CangJieTarget.ANNOTATION_CLASS),
//    CROSSINLINE_KEYWORD to EnumSet.of(CangJieTarget.VALUE_PARAMETER),
    CONST_KEYWORD to EnumSet.of(
        CangJieTarget.FUNCTION,
        CangJieTarget.STRUCT_MEMBER_FUNCTION

    ),
    OPERATOR_KEYWORD to EnumSet.of(
        CangJieTarget.MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,

        CangJieTarget.STRUCT_MEMBER_FUNCTION,
    ),
//    INFIX_KEYWORD to EnumSet.of(CangJieTarget.FUNCTION),
//    HEADER_KEYWORD to EnumSet.of(
//        CangJieTarget.TOP_LEVEL_FUNCTION,
//        CangJieTarget.TOP_LEVEL_PROPERTY,
//        CangJieTarget.CLASS_ONLY,
//        CangJieTarget.OBJECT,
//        CangJieTarget.INTERFACE,
//        CangJieTarget.ENUM_CLASS,
//        CangJieTarget.ANNOTATION_CLASS
//    ),
//    IMPL_KEYWORD to EnumSet.of(
//        CangJieTarget.TOP_LEVEL_FUNCTION,
//        CangJieTarget.MEMBER_FUNCTION,
//        CangJieTarget.TOP_LEVEL_PROPERTY,
//        CangJieTarget.MEMBER_PROPERTY,
//        CangJieTarget.CONSTRUCTOR,
//        CangJieTarget.CLASS_ONLY,
//        CangJieTarget.OBJECT,
//        CangJieTarget.INTERFACE,
//        CangJieTarget.ENUM_CLASS,
//        CangJieTarget.ANNOTATION_CLASS,
//        CangJieTarget.TYPEALIAS
//    ),
//    EXPECT_KEYWORD to EnumSet.of(
//        CangJieTarget.TOP_LEVEL_FUNCTION,
//        CangJieTarget.TOP_LEVEL_PROPERTY,
//        CangJieTarget.CLASS_ONLY,
//        CangJieTarget.OBJECT,
//        CangJieTarget.INTERFACE,
//        CangJieTarget.ENUM_CLASS,
//        CangJieTarget.ANNOTATION_CLASS
//    ),
//    ACTUAL_KEYWORD to EnumSet.of(
//        CangJieTarget.TOP_LEVEL_FUNCTION,
//        CangJieTarget.MEMBER_FUNCTION,
//        CangJieTarget.TOP_LEVEL_PROPERTY,
//        CangJieTarget.MEMBER_PROPERTY,
//        CangJieTarget.CONSTRUCTOR,
//        CangJieTarget.CLASS_ONLY,
//        CangJieTarget.OBJECT,
//        CangJieTarget.INTERFACE,
//        CangJieTarget.ENUM_CLASS,
//        CangJieTarget.ANNOTATION_CLASS,
//        CangJieTarget.TYPEALIAS
//    ),
//    FUN_KEYWORD to EnumSet.of(CangJieTarget.INTERFACE),
//    VALUE_KEYWORD to EnumSet.of(CangJieTarget.CLASS_ONLY)
)
val deprecatedTargetMap = mapOf<CjKeywordToken, Set<CangJieTarget>>()

// NOTE: redundant targets must be possible!
val redundantTargetMap = mapOf<CjKeywordToken, Set<CangJieTarget>>(
    OPEN_KEYWORD to EnumSet.of(CangJieTarget.INTERFACE)
)

interface TargetAllowedPredicate {
    fun isAllowed(target: CangJieTarget, languageVersionSettings: LanguageVersionSettings): Boolean
}

fun always(target: CangJieTarget, vararg targets: CangJieTarget) = object : TargetAllowedPredicate {
    private val targetSet = EnumSet.of(target, *targets)

    override fun isAllowed(target: CangJieTarget, languageVersionSettings: LanguageVersionSettings) =
        target in targetSet
}

val possibleParentTargetPredicateMap = mapOf(


    OVERRIDE_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.LOCAL_CLASS,
        CangJieTarget.STRUCT,

        CangJieTarget.INTERFACE,
        CangJieTarget.ENUM,
        CangJieTarget.ENUM_ENTRY
    ),
    PROTECTED_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.LOCAL_CLASS,
        CangJieTarget.ENUM,

        ),
    INTERNAL_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.LOCAL_CLASS,
        CangJieTarget.STRUCT,

        CangJieTarget.ENUM,
        CangJieTarget.ENUM_ENTRY,
        CangJieTarget.FILE
    ),
    PRIVATE_KEYWORD to always(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.LOCAL_CLASS,
        CangJieTarget.STRUCT,

        CangJieTarget.INTERFACE,
        CangJieTarget.ENUM,
        CangJieTarget.ENUM_ENTRY,
        CangJieTarget.FILE
    ),
//    COMPANION_KEYWORD to always(
//        CangJieTarget.CLASS_ONLY,
//        CangJieTarget.INTERFACE,
//        CangJieTarget.ENUM  ,
////        CangJieTarget.ANNOTATION_CLASS
//    ),
//    FINAL_KEYWORD to always(
//        CangJieTarget.CLASS_ONLY,
//        CangJieTarget.LOCAL_CLASS,
//        CangJieTarget.STRUCT,
//
//        CangJieTarget.ENUM ,
//        CangJieTarget.ENUM_ENTRY,
////        CangJieTarget.ANNOTATION_CLASS,
//        CangJieTarget.FILE
//    ),
//    VARARG_KEYWORD to always(CangJieTarget.CONSTRUCTOR, CangJieTarget.FUNCTION, CangJieTarget.CLASS)
)
