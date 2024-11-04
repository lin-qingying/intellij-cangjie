package com.linqingying.cangjie.highlighter

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object CangJieHighlightingColors {
    val KEYWORD: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    //    val KEYWORD = TextAttributesKey.createTextAttributesKey(
//        "CANGJIE_KEYWORD",
////        DefaultLanguageHighlighterColors.KEYWORD
//        ).apply {
//        defaultAttributes.setAttributes(
//            JBColor(0xb40291, 0xb40291),
//            null, null, null, null, Font.BOLD
//        )
//
//    }
//    val KEYWORD1 = TextAttributesKey.createTextAttributesKey(
//        "CANGJIE_KEYWORD1",
//        TextAttributes(
//            JBColor(0xb40291, 0xb40291),
//            null, null, null, Font.BOLD
//        )
//    )
    val BUILTIN_ANNOTATION: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BUILTIN_ANNOTATION", KEYWORD)
    val LET_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_LET", KEYWORD)
    val MUT_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_MUT", KEYWORD)
    val PROP_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_PROP", KEYWORD)

    val VAR_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_VAR", KEYWORD)
    val CONST_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_KEYWORD_CONST", KEYWORD)

    val NUMBER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val STRING: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STRING", DefaultLanguageHighlighterColors.STRING)
    val STRING_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE
    )
    val INVALID_STRING_ESCAPE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INVALID_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE
    )
    val OPERATOR_SIGN: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_OPERATION_SIGN",
        DefaultLanguageHighlighterColors.OPERATION_SIGN
    )
    val PARENTHESIS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PARENTHESIS", DefaultLanguageHighlighterColors.PARENTHESES)
    val BRACES: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val BRACKETS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val FUNCTION_LITERAL_BRACES_AND_ARROW: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_FUNCTION_LITERAL_BRACES_AND_ARROW")
    val COMMA: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_COMMA", DefaultLanguageHighlighterColors.COMMA)
    val SEMICOLON: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val LT_COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_LT_COLON")
    val STATIC: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_STATIC", DefaultLanguageHighlighterColors.KEYWORD)

    val COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_COLON")
    val DOUBLE_COLON: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DOUBLE_COLON")
    val DOT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DOT", DefaultLanguageHighlighterColors.DOT)
    val SAFE_ACCESS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT)
    val QUEST: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_QUEST")
    val EXCLEXCL: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_EXCLEXCL")
    val ARROW: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_ARROW", PARENTHESIS)
    val LINE_COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val BLOCK_COMMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_BLOCK_COMMENT",
        DefaultLanguageHighlighterColors.BLOCK_COMMENT
    )
    val DOC_COMMENT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
    val CDOC_TAG: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("KDOC_TAG_NAME", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
    val CDOC_LINK: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CDOC_LINK", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE)


    val CLASS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME)
    val TYPE_PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)
    val ABSTRACT_CLASS: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ABSTRACT_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME)
    val TRAIT: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_TRAIT", DefaultLanguageHighlighterColors.INTERFACE_NAME)
    val ANNOTATION: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ANNOTATION", DefaultLanguageHighlighterColors.METADATA)
    val OBJECT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_OBJECT", CLASS)
    val ENUM: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ENUM", DefaultLanguageHighlighterColors.CLASS_NAME)
    val ENUM_ENTRY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ENUM_ENTRY", DefaultLanguageHighlighterColors.STATIC_FIELD)
    val TYPE_ALIAS: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_TYPE_ALIAS", CLASS)


    val MUTABLE_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_MUTABLE_VARIABLE")
    val LOCAL_VARIABLE: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_LOCAL_VARIABLE",
        DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    )
    val PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)
    val WRAPPED_INTO_REF: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_WRAPPED_INTO_REF",
        DefaultLanguageHighlighterColors.CLASS_NAME
    )
    val INSTANCE_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_PROPERTY",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD
    )
    val PACKAGE_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_PROPERTY",
        DefaultLanguageHighlighterColors.STATIC_FIELD
    )
    val BACKING_FIELD_VARIABLE: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BACKING_FIELD_VARIABLE")
    val EXTENSION_PROPERTY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_EXTENSION_PROPERTY",
        DefaultLanguageHighlighterColors.STATIC_FIELD
    )
    val SYNTHETIC_EXTENSION_PROPERTY: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SYNTHETIC_EXTENSION_PROPERTY", EXTENSION_PROPERTY)
    val DYNAMIC_PROPERTY_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DYNAMIC_PROPERTY_CALL")
    val ANDROID_EXTENSIONS_PROPERTY_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_ANDROID_EXTENSIONS_PROPERTY_CALL")
    val INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION",
        INSTANCE_PROPERTY
    )
    val PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION",
        PACKAGE_PROPERTY
    )

    // functions
    val FUNCTION_LITERAL_DEFAULT_PARAMETER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CLOSURE_DEFAULT_PARAMETER", PARAMETER)
    val FUNCTION_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_FUNCTION_DECLARATION",
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
    )
    val FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.FUNCTION_CALL
    )
    val PACKAGE_FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_PACKAGE_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.STATIC_METHOD
    )
    val EXTENSION_FUNCTION_CALL: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_EXTENSION_FUNCTION_CALL",
        DefaultLanguageHighlighterColors.STATIC_METHOD
    )
    val CONSTRUCTOR_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_CONSTRUCTOR", DefaultLanguageHighlighterColors.FUNCTION_CALL)
    val DYNAMIC_FUNCTION_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_DYNAMIC_FUNCTION_CALL")
    val SUSPEND_FUNCTION_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SUSPEND_FUNCTION_CALL", FUNCTION_CALL)
    val VARIABLE_AS_FUNCTION_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION")
    val VARIABLE_AS_FUNCTION_LIKE_CALL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION_LIKE")


    val BAD_CHARACTER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
    val SMART_CAST_VALUE: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CAST_VALUE")
    val SMART_CONSTANT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CONSTANT")
    val SMART_CAST_RECEIVER: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_SMART_CAST_RECEIVER")
    val LABEL: TextAttributesKey =
        TextAttributesKey.createTextAttributesKey("CANGJIE_LABEL", DefaultLanguageHighlighterColors.LABEL)
    val DEBUG_INFO: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_DEBUG_INFO")
    val RESOLVED_TO_ERROR: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_RESOLVED_TO_ERROR")
    val NAMED_ARGUMENT: TextAttributesKey = TextAttributesKey.createTextAttributesKey("CANGJIE_NAMED_ARGUMENT")
    val ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
        "CANGJIE_ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES",
        DefaultLanguageHighlighterColors.METADATA
    )
}
