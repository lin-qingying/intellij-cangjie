package com.huawei.cangjie.highlighter
//
//import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
//import com.intellij.openapi.editor.HighlighterColors
//import com.intellij.openapi.editor.colors.TextAttributesKey
//import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
//
//
//class CangJieHighlightingColors {
//    companion object {
//        // default keys (mostly syntax elements)
//        val KEYWORD: TextAttributesKey =
//            createTextAttributesKey("CANGJIE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
//        val BUILTIN_ANNOTATION = createTextAttributesKey("CANGJIE_BUILTIN_ANNOTATION", KEYWORD)
//        val LET_KEYWORD = createTextAttributesKey("CANGJIE_KEYWORD_LET", KEYWORD)
//        val VAR_KEYWORD = createTextAttributesKey("CANGJIE_KEYWORD_VAR", KEYWORD)
//        val NUMBER = createTextAttributesKey("CANGJIE_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
//        val STRING = createTextAttributesKey("CANGJIE_STRING", DefaultLanguageHighlighterColors.STRING)
//        val STRING_ESCAPE = createTextAttributesKey(
//            "CANGJIE_STRING_ESCAPE",
//            DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE
//        )
//        val INVALID_STRING_ESCAPE = createTextAttributesKey(
//            "CANGJIE_INVALID_STRING_ESCAPE",
//            DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE
//        )
//        val OPERATOR_SIGN = createTextAttributesKey(
//            "CANGJIE_OPERATION_SIGN",
//            DefaultLanguageHighlighterColors.OPERATION_SIGN
//        )
//        val PARENTHESIS =
//            createTextAttributesKey("CANGJIE_PARENTHESIS", DefaultLanguageHighlighterColors.PARENTHESES)
//        val BRACES = createTextAttributesKey("CANGJIE_BRACES", DefaultLanguageHighlighterColors.BRACES)
//        val BRACKETS =
//            createTextAttributesKey("CANGJIE_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
//        val FUNCTION_LITERAL_BRACES_AND_ARROW =
//            createTextAttributesKey("CANGJIE_FUNCTION_LITERAL_BRACES_AND_ARROW")
//        val COMMA = createTextAttributesKey("CANGJIE_COMMA", DefaultLanguageHighlighterColors.COMMA)
//        val SEMICOLON =
//            createTextAttributesKey("CANGJIE_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
//        val COLON = createTextAttributesKey("CANGJIE_COLON")
//        val DOUBLE_COLON = createTextAttributesKey("CANGJIE_DOUBLE_COLON")
//        val DOT = createTextAttributesKey("CANGJIE_DOT", DefaultLanguageHighlighterColors.DOT)
//        val SAFE_ACCESS =
//            createTextAttributesKey("CANGJIE_SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT)
//        val QUEST = createTextAttributesKey("CANGJIE_QUEST")
//        val EXCLEXCL = createTextAttributesKey("CANGJIE_EXCLEXCL")
//        val ARROW = createTextAttributesKey("CANGJIE_ARROW", PARENTHESIS)
//        val LINE_COMMENT =
//            createTextAttributesKey("CANGJIE_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
//        val BLOCK_COMMENT = createTextAttributesKey(
//            "CANGJIE_BLOCK_COMMENT",
//            DefaultLanguageHighlighterColors.BLOCK_COMMENT
//        )
//        val DOC_COMMENT =
//            createTextAttributesKey("CANGJIE_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT)
//        val CDOC_TAG =
//            createTextAttributesKey("KDOC_TAG_NAME", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG)
//        val CDOC_LINK =
//            createTextAttributesKey("KDOC_LINK", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE)
//
//        // class kinds
//        val CLASS = createTextAttributesKey("CANGJIE_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME)
//        val TYPE_PARAMETER: TextAttributesKey =
//            createTextAttributesKey("CANGJIE_TYPE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)
//        val ABSTRACT_CLASS =
//            createTextAttributesKey("CANGJIE_ABSTRACT_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME)
//        val TRAIT =
//            createTextAttributesKey("CANGJIE_TRAIT", DefaultLanguageHighlighterColors.INTERFACE_NAME)
//        val ANNOTATION: TextAttributesKey =
//            createTextAttributesKey("CANGJIE_ANNOTATION", DefaultLanguageHighlighterColors.METADATA)
//        val OBJECT = createTextAttributesKey("CANGJIE_OBJECT", CLASS)
//        val ENUM: TextAttributesKey =
//            createTextAttributesKey("CANGJIE_ENUM", DefaultLanguageHighlighterColors.CLASS_NAME)
//        val ENUM_ENTRY =
//            createTextAttributesKey("CANGJIE_ENUM_ENTRY", DefaultLanguageHighlighterColors.STATIC_FIELD)
//        val TYPE_ALIAS = createTextAttributesKey("CANGJIE_TYPE_ALIAS", CLASS)
//
//        // variable kinds
//        val MUTABLE_VARIABLE = createTextAttributesKey("CANGJIE_MUTABLE_VARIABLE")
//        val LOCAL_VARIABLE = createTextAttributesKey(
//            "CANGJIE_LOCAL_VARIABLE",
//            DefaultLanguageHighlighterColors.LOCAL_VARIABLE
//        )
//        val PARAMETER =
//            createTextAttributesKey("CANGJIE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER)
//        val WRAPPED_INTO_REF = createTextAttributesKey(
//            "CANGJIE_WRAPPED_INTO_REF",
//            DefaultLanguageHighlighterColors.CLASS_NAME
//        )
//        val INSTANCE_PROPERTY = createTextAttributesKey(
//            "CANGJIE_INSTANCE_PROPERTY",
//            DefaultLanguageHighlighterColors.INSTANCE_FIELD
//        )
//        val PACKAGE_PROPERTY = createTextAttributesKey(
//            "CANGJIE_PACKAGE_PROPERTY",
//            DefaultLanguageHighlighterColors.STATIC_FIELD
//        )
//        val BACKING_FIELD_VARIABLE = createTextAttributesKey("CANGJIE_BACKING_FIELD_VARIABLE")
//        val EXTENSION_PROPERTY = createTextAttributesKey(
//            "CANGJIE_EXTENSION_PROPERTY",
//            DefaultLanguageHighlighterColors.STATIC_FIELD
//        )
//        val SYNTHETIC_EXTENSION_PROPERTY =
//            createTextAttributesKey("CANGJIE_SYNTHETIC_EXTENSION_PROPERTY", EXTENSION_PROPERTY)
//        val DYNAMIC_PROPERTY_CALL = createTextAttributesKey("CANGJIE_DYNAMIC_PROPERTY_CALL")
//        val ANDROID_EXTENSIONS_PROPERTY_CALL =
//            createTextAttributesKey("CANGJIE_ANDROID_EXTENSIONS_PROPERTY_CALL")
//        val INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION = createTextAttributesKey(
//            "CANGJIE_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION",
//            INSTANCE_PROPERTY
//        )
//        val PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION = createTextAttributesKey(
//            "CANGJIE_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION",
//            PACKAGE_PROPERTY
//        )
//
//        // functions
//        val FUNCTION_LITERAL_DEFAULT_PARAMETER =
//            createTextAttributesKey("CANGJIE_CLOSURE_DEFAULT_PARAMETER", PARAMETER)
//        val FUNCTION_DECLARATION = createTextAttributesKey(
//            "CANGJIE_FUNCTION_DECLARATION",
//            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
//        )
//        val FUNCTION_CALL = createTextAttributesKey(
//            "CANGJIE_FUNCTION_CALL",
//            DefaultLanguageHighlighterColors.FUNCTION_CALL
//        )
//        val PACKAGE_FUNCTION_CALL = createTextAttributesKey(
//            "CANGJIE_PACKAGE_FUNCTION_CALL",
//            DefaultLanguageHighlighterColors.STATIC_METHOD
//        )
//        val EXTENSION_FUNCTION_CALL = createTextAttributesKey(
//            "CANGJIE_EXTENSION_FUNCTION_CALL",
//            DefaultLanguageHighlighterColors.STATIC_METHOD
//        )
//        val CONSTRUCTOR_CALL =
//            createTextAttributesKey("CANGJIE_CONSTRUCTOR", DefaultLanguageHighlighterColors.FUNCTION_CALL)
//        val DYNAMIC_FUNCTION_CALL = createTextAttributesKey("CANGJIE_DYNAMIC_FUNCTION_CALL")
//        val SUSPEND_FUNCTION_CALL = createTextAttributesKey("CANGJIE_SUSPEND_FUNCTION_CALL", FUNCTION_CALL)
//        val VARIABLE_AS_FUNCTION_CALL = createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION")
//        val VARIABLE_AS_FUNCTION_LIKE_CALL = createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION_LIKE")
//
//        // other
//        val BAD_CHARACTER =
//            createTextAttributesKey("CANGJIE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER)
//        val SMART_CAST_VALUE = createTextAttributesKey("CANGJIE_SMART_CAST_VALUE")
//        val SMART_CONSTANT = createTextAttributesKey("CANGJIE_SMART_CONSTANT")
//        val SMART_CAST_RECEIVER = createTextAttributesKey("CANGJIE_SMART_CAST_RECEIVER")
//        val LABEL = createTextAttributesKey("CANGJIE_LABEL", DefaultLanguageHighlighterColors.LABEL)
//        val DEBUG_INFO = createTextAttributesKey("CANGJIE_DEBUG_INFO")
//        val RESOLVED_TO_ERROR = createTextAttributesKey("CANGJIE_RESOLVED_TO_ERROR")
//        val NAMED_ARGUMENT = createTextAttributesKey("CANGJIE_NAMED_ARGUMENT")
//        val ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES: TextAttributesKey = createTextAttributesKey(
//            "CANGJIE_ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES",
//            DefaultLanguageHighlighterColors.METADATA
//        )
//    }
//}
//
