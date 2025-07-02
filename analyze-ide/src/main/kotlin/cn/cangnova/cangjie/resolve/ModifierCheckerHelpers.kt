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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.lexer.CjKeywordToken
import cn.cangnova.cangjie.lexer.CjTokens.*
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

    CangJieTarget.MEMBER_PROPERTY,
    CangJieTarget.TOP_LEVEL_VARIABLE,
    CangJieTarget.CONSTRUCTOR,
    CangJieTarget.TYPEALIAS,


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

    ABSTRACT_KEYWORD to EnumSet.of(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.LOCAL_CLASS,

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

    SEALED_KEYWORD to EnumSet.of(CangJieTarget.CLASS_ONLY, CangJieTarget.INTERFACE),

    REDEF_KEYWORD to EnumSet.of(
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.STRUCT_MEMBER_PROPERTY,


        CangJieTarget.ENUM_MEMBER_FUNCTION,
        CangJieTarget.ENUM_MEMBER_PROPERTY,

        CangJieTarget.CLASS_MEMBER_FUNCTION,
        CangJieTarget.CLASS_MEMBER_PROPERTY,


        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_PROPERTY,

        ),
    OVERRIDE_KEYWORD to EnumSet.of(
        CangJieTarget.STRUCT_MEMBER_FUNCTION,
        CangJieTarget.STRUCT_MEMBER_PROPERTY,


        CangJieTarget.ENUM_MEMBER_FUNCTION,
        CangJieTarget.ENUM_MEMBER_PROPERTY,

        CangJieTarget.CLASS_MEMBER_FUNCTION,
        CangJieTarget.CLASS_MEMBER_PROPERTY,


        CangJieTarget.INTERFACE_MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_PROPERTY,

        ),
    PRIVATE_KEYWORD to defaultVisibilityTargets + CangJieTarget.BACKING_FIELD,
    PUBLIC_KEYWORD to defaultVisibilityTargets + CangJieTarget.MACRO,
    INTERNAL_KEYWORD to defaultVisibilityTargets + CangJieTarget.BACKING_FIELD,
    PROTECTED_KEYWORD to EnumSet.of(
        CangJieTarget.FUNCTION,
        CangJieTarget.VARIABLE,
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.INTERFACE,
        CangJieTarget.ENUM,

        CangJieTarget.MEMBER_FUNCTION,

        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_VARIABLE,
        CangJieTarget.CONSTRUCTOR,
        CangJieTarget.TYPEALIAS
    ),

    CONST_KEYWORD to EnumSet.of(
        CangJieTarget.FUNCTION,
        CangJieTarget.STRUCT_MEMBER_FUNCTION

    ),
    OPERATOR_KEYWORD to EnumSet.of(
        CangJieTarget.MEMBER_FUNCTION,
        CangJieTarget.INTERFACE_MEMBER_FUNCTION,

        CangJieTarget.STRUCT_MEMBER_FUNCTION,
    ),

    )
val deprecatedTargetMap = mapOf<CjKeywordToken, Set<CangJieTarget>>()


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
