package com.huawei.cangjie.resolve

import com.huawei.cangjie.lexer.CjKeywordToken
import com.huawei.cangjie.lexer.CjTokens.*
import java.util.*

val defaultVisibilityTargets: EnumSet<CangJieTarget> = EnumSet.of(
    CangJieTarget.CLASS_ONLY,
    CangJieTarget.STRUCT,
    CangJieTarget.INTERFACE,
    CangJieTarget.ENUM,
    CangJieTarget.MEMBER_FUNCTION,
    CangJieTarget.TOP_LEVEL_FUNCTION,
    CangJieTarget.PROPERTY_GETTER,
    CangJieTarget.PROPERTY_SETTER,
    CangJieTarget.MEMBER_PROPERTY,
    CangJieTarget.TOP_LEVEL_VARIABLE,
    CangJieTarget.CONSTRUCTOR,
    CangJieTarget.TYPEALIAS,
)

val possibleTargetMap = mapOf(
//    ENUM_KEYWORD to EnumSet.of(CangJieTarget.ENUM_CLASS),
    ABSTRACT_KEYWORD to EnumSet.of(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.LOCAL_CLASS,
//        CangJieTarget.INTERFACE,
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.MEMBER_FUNCTION
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
    SEALED_KEYWORD to EnumSet.of(CangJieTarget.CLASS_ONLY, CangJieTarget.INTERFACE ),
//    INNER_KEYWORD to EnumSet.of(CangJieTarget.CLASS_ONLY),
    OVERRIDE_KEYWORD to EnumSet.of(CangJieTarget.MEMBER_PROPERTY, CangJieTarget.MEMBER_FUNCTION),
    PRIVATE_KEYWORD to defaultVisibilityTargets + CangJieTarget.BACKING_FIELD,
    PUBLIC_KEYWORD to defaultVisibilityTargets,
    INTERNAL_KEYWORD to defaultVisibilityTargets + CangJieTarget.BACKING_FIELD,
    PROTECTED_KEYWORD to EnumSet.of(
        CangJieTarget.CLASS_ONLY,
        CangJieTarget.STRUCT,
        CangJieTarget.INTERFACE,
        CangJieTarget.ENUM,
//        CangJieTarget.ANNOTATION_CLASS,
        CangJieTarget.MEMBER_FUNCTION,
//        CangJieTarget.PROPERTY_GETTER,
//        CangJieTarget.PROPERTY_SETTER,
        CangJieTarget.MEMBER_PROPERTY,
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
        CangJieTarget.MEMBER_PROPERTY,
        CangJieTarget.TOP_LEVEL_PROPERTY,
        CangJieTarget.TOP_LEVEL_VARIABLE
    ),
    OPERATOR_KEYWORD to EnumSet.of(CangJieTarget.FUNCTION),
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
