package com.huawei.cangjie.highlighter;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;

import java.awt.*;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public class CangJieHighlightingColors {



    // default keys (mostly syntax elements) B40291
    public final static TextAttributesKey KEYWORD  =
            createTextAttributesKey("CANGJIE_KEYWORD", new TextAttributes(new JBColor(0xb40291, 0xb40291), null, null, null, Font.BOLD));
    public final static TextAttributesKey BUILTIN_ANNOTATION = createTextAttributesKey("CANGJIE_BUILTIN_ANNOTATION", KEYWORD);
    public final static TextAttributesKey LET_KEYWORD = createTextAttributesKey("CANGJIE_KEYWORD_LET", KEYWORD);
    public final static TextAttributesKey VAR_KEYWORD = createTextAttributesKey("CANGJIE_KEYWORD_VAR", KEYWORD);
  public  final  static  TextAttributesKey IMPORT_KEYWORD = createTextAttributesKey("CANGJIE_KEYWORD_IMPORT", KEYWORD);
    public final static TextAttributesKey NUMBER = createTextAttributesKey("CANGJIE_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public final static TextAttributesKey STRING = createTextAttributesKey("CANGJIE_STRING", DefaultLanguageHighlighterColors.STRING);
    public final static TextAttributesKey STRING_ESCAPE = createTextAttributesKey(
            "CANGJIE_STRING_ESCAPE",
            DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE
    );
    public final static TextAttributesKey INVALID_STRING_ESCAPE = createTextAttributesKey(
            "CANGJIE_INVALID_STRING_ESCAPE",
            DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE
    );
    public final static TextAttributesKey OPERATOR_SIGN = createTextAttributesKey(
            "CANGJIE_OPERATION_SIGN",
            DefaultLanguageHighlighterColors.OPERATION_SIGN
    );
    public final static TextAttributesKey PARENTHESIS =
            createTextAttributesKey("CANGJIE_PARENTHESIS", DefaultLanguageHighlighterColors.PARENTHESES);
    public final static TextAttributesKey BRACES = createTextAttributesKey("CANGJIE_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public final static TextAttributesKey BRACKETS =
            createTextAttributesKey("CANGJIE_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public final static TextAttributesKey FUNCTION_LITERAL_BRACES_AND_ARROW =
            createTextAttributesKey("CANGJIE_FUNCTION_LITERAL_BRACES_AND_ARROW");
    public final static TextAttributesKey COMMA = createTextAttributesKey("CANGJIE_COMMA", DefaultLanguageHighlighterColors.COMMA);
    public final static TextAttributesKey SEMICOLON =
            createTextAttributesKey("CANGJIE_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
    public final static TextAttributesKey COLON = createTextAttributesKey("CANGJIE_COLON");
    public final static TextAttributesKey DOUBLE_COLON = createTextAttributesKey("CANGJIE_DOUBLE_COLON");
    public final static TextAttributesKey DOT = createTextAttributesKey("CANGJIE_DOT", DefaultLanguageHighlighterColors.DOT);
    public final static TextAttributesKey SAFE_ACCESS =
            createTextAttributesKey("CANGJIE_SAFE_ACCESS", DefaultLanguageHighlighterColors.DOT);
    public final static TextAttributesKey QUEST = createTextAttributesKey("CANGJIE_QUEST");
    public final static TextAttributesKey EXCLEXCL = createTextAttributesKey("CANGJIE_EXCLEXCL");
    public final static TextAttributesKey ARROW = createTextAttributesKey("CANGJIE_ARROW", PARENTHESIS);
    public final static TextAttributesKey LINE_COMMENT =
            createTextAttributesKey("CANGJIE_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public final static TextAttributesKey BLOCK_COMMENT = createTextAttributesKey(
            "CANGJIE_BLOCK_COMMENT",
            DefaultLanguageHighlighterColors.BLOCK_COMMENT
    );
    public final static TextAttributesKey DOC_COMMENT =
            createTextAttributesKey("CANGJIE_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);
    public final static TextAttributesKey CDOC_TAG =
            createTextAttributesKey("KDOC_TAG_NAME", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG);
    public final static TextAttributesKey CDOC_LINK =
            createTextAttributesKey("KDOC_LINK", DefaultLanguageHighlighterColors.DOC_COMMENT_TAG_VALUE);

    // class kinds
    public final static TextAttributesKey CLASS = createTextAttributesKey("CANGJIE_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME);
    public final static TextAttributesKey TYPE_PARAMETER  =
    createTextAttributesKey("CANGJIE_TYPE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER);
    public final static TextAttributesKey ABSTRACT_CLASS =
            createTextAttributesKey("CANGJIE_ABSTRACT_CLASS", DefaultLanguageHighlighterColors.CLASS_NAME);
    public final static TextAttributesKey TRAIT =
            createTextAttributesKey("CANGJIE_TRAIT", DefaultLanguageHighlighterColors.INTERFACE_NAME);
    public final static TextAttributesKey ANNOTATION  =
    createTextAttributesKey("CANGJIE_ANNOTATION", DefaultLanguageHighlighterColors.METADATA);
    public final static TextAttributesKey OBJECT = createTextAttributesKey("CANGJIE_OBJECT", CLASS);
    public final static TextAttributesKey ENUM  =
    createTextAttributesKey("CANGJIE_ENUM", DefaultLanguageHighlighterColors.CLASS_NAME);
    public final static TextAttributesKey ENUM_ENTRY =
            createTextAttributesKey("CANGJIE_ENUM_ENTRY", DefaultLanguageHighlighterColors.STATIC_FIELD);
    public final static TextAttributesKey TYPE_ALIAS = createTextAttributesKey("CANGJIE_TYPE_ALIAS", CLASS);

    // variable kinds
    public final static TextAttributesKey MUTABLE_VARIABLE = createTextAttributesKey("CANGJIE_MUTABLE_VARIABLE");
    public final static TextAttributesKey LOCAL_VARIABLE = createTextAttributesKey(
            "CANGJIE_LOCAL_VARIABLE",
            DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    );
    public final static TextAttributesKey PARAMETER =
            createTextAttributesKey("CANGJIE_PARAMETER", DefaultLanguageHighlighterColors.PARAMETER);
    public final static TextAttributesKey WRAPPED_INTO_REF = createTextAttributesKey(
            "CANGJIE_WRAPPED_INTO_REF",
            DefaultLanguageHighlighterColors.CLASS_NAME
    );
    public final static TextAttributesKey INSTANCE_PROPERTY = createTextAttributesKey(
            "CANGJIE_INSTANCE_PROPERTY",
            DefaultLanguageHighlighterColors.INSTANCE_FIELD
    );
    public final static TextAttributesKey PACKAGE_PROPERTY = createTextAttributesKey(
            "CANGJIE_PACKAGE_PROPERTY",
            DefaultLanguageHighlighterColors.STATIC_FIELD
    );
    public final static TextAttributesKey BACKING_FIELD_VARIABLE = createTextAttributesKey("CANGJIE_BACKING_FIELD_VARIABLE");
    public final static TextAttributesKey EXTENSION_PROPERTY = createTextAttributesKey(
            "CANGJIE_EXTENSION_PROPERTY",
            DefaultLanguageHighlighterColors.STATIC_FIELD
    );
    public final static TextAttributesKey SYNTHETIC_EXTENSION_PROPERTY =
            createTextAttributesKey("CANGJIE_SYNTHETIC_EXTENSION_PROPERTY", EXTENSION_PROPERTY);
    public final static TextAttributesKey DYNAMIC_PROPERTY_CALL = createTextAttributesKey("CANGJIE_DYNAMIC_PROPERTY_CALL");
    public final static TextAttributesKey ANDROID_EXTENSIONS_PROPERTY_CALL =
            createTextAttributesKey("CANGJIE_ANDROID_EXTENSIONS_PROPERTY_CALL");
    public final static TextAttributesKey INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION = createTextAttributesKey(
            "CANGJIE_INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION",
            INSTANCE_PROPERTY
    );
    public final static TextAttributesKey PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION = createTextAttributesKey(
            "CANGJIE_PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION",
            PACKAGE_PROPERTY
    );

    // functions
    public final static TextAttributesKey FUNCTION_LITERAL_DEFAULT_PARAMETER =
            createTextAttributesKey("CANGJIE_CLOSURE_DEFAULT_PARAMETER", PARAMETER);
    public final static TextAttributesKey FUNCTION_DECLARATION = createTextAttributesKey(
            "CANGJIE_FUNCTION_DECLARATION",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
    );
    public final static TextAttributesKey FUNCTION_CALL = createTextAttributesKey(
            "CANGJIE_FUNCTION_CALL",
            DefaultLanguageHighlighterColors.FUNCTION_CALL
    );
    public final static TextAttributesKey PACKAGE_FUNCTION_CALL = createTextAttributesKey(
            "CANGJIE_PACKAGE_FUNCTION_CALL",
            DefaultLanguageHighlighterColors.STATIC_METHOD
    );
    public final static TextAttributesKey EXTENSION_FUNCTION_CALL = createTextAttributesKey(
            "CANGJIE_EXTENSION_FUNCTION_CALL",
            DefaultLanguageHighlighterColors.STATIC_METHOD
    );
    public final static TextAttributesKey CONSTRUCTOR_CALL =
            createTextAttributesKey("CANGJIE_CONSTRUCTOR", DefaultLanguageHighlighterColors.FUNCTION_CALL);
    public final static TextAttributesKey DYNAMIC_FUNCTION_CALL = createTextAttributesKey("CANGJIE_DYNAMIC_FUNCTION_CALL");
    public final static TextAttributesKey SUSPEND_FUNCTION_CALL = createTextAttributesKey("CANGJIE_SUSPEND_FUNCTION_CALL", FUNCTION_CALL);
    public final static TextAttributesKey VARIABLE_AS_FUNCTION_CALL = createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION");
    public final static TextAttributesKey VARIABLE_AS_FUNCTION_LIKE_CALL = createTextAttributesKey("CANGJIE_VARIABLE_AS_FUNCTION_LIKE");


    // other
    public final static TextAttributesKey BAD_CHARACTER =
            createTextAttributesKey("CANGJIE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);
    public final static TextAttributesKey SMART_CAST_VALUE = createTextAttributesKey("CANGJIE_SMART_CAST_VALUE");
    public final static TextAttributesKey SMART_CONSTANT = createTextAttributesKey("CANGJIE_SMART_CONSTANT");
    public final static TextAttributesKey SMART_CAST_RECEIVER = createTextAttributesKey("CANGJIE_SMART_CAST_RECEIVER");
    public final static TextAttributesKey LABEL = createTextAttributesKey("CANGJIE_LABEL", DefaultLanguageHighlighterColors.LABEL);
    public final static TextAttributesKey DEBUG_INFO = createTextAttributesKey("CANGJIE_DEBUG_INFO");
    public final static TextAttributesKey RESOLVED_TO_ERROR = createTextAttributesKey("CANGJIE_RESOLVED_TO_ERROR");
    public final static TextAttributesKey NAMED_ARGUMENT = createTextAttributesKey("CANGJIE_NAMED_ARGUMENT");
    public final static TextAttributesKey ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES  = createTextAttributesKey(
            "CANGJIE_ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES",
            DefaultLanguageHighlighterColors.METADATA
            );

}
