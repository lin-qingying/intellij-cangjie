package com.linqingying.cangjie.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.editor.colors.TextAttributesKey


object CangJieHighlightInfoTypeSemanticNames {
    // default keys (mostly syntax elements)
    val KEYWORD: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.KEYWORD)
    val BUILTIN_ANNOTATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BUILTIN_ANNOTATION)
    val NUMBER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.NUMBER)
    val STRING: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.STRING)
    val FUNCTION_LITERAL_BRACES_AND_ARROW: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW)
    val COMMA: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.COMMA)
    val SEMICOLON: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SEMICOLON)
    val COLON: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.COLON)
    val DOT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DOT)
    val SAFE_ACCESS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SAFE_ACCESS)
    val EXCLEXCL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.EXCLEXCL)
    val ARROW: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ARROW)
    val BLOCK_COMMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BLOCK_COMMENT)
    val CDOC_LINK: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CDOC_LINK)

    // class kinds
    val CLASS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CLASS)
    val TYPE_PARAMETER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_PARAMETER)
    val ABSTRACT_CLASS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ABSTRACT_CLASS)
    val TRAIT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TRAIT)
    val ANNOTATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ANNOTATION)
    val OBJECT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.OBJECT)
    val ENUM: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ENUM)
    val ENUM_ENTRY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ENUM_ENTRY)
    val TYPE_ALIAS: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.TYPE_ALIAS)

    // variable kinds
    val MUTABLE_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.MUTABLE_VARIABLE)
    val LOCAL_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LOCAL_VARIABLE)
    val PARAMETER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PARAMETER)
    val WRAPPED_INTO_REF: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.WRAPPED_INTO_REF)
    val INSTANCE_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.INSTANCE_PROPERTY)
    val PACKAGE_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PACKAGE_PROPERTY)
    val BACKING_FIELD_VARIABLE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.BACKING_FIELD_VARIABLE)
    val EXTENSION_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.EXTENSION_PROPERTY)
    val SYNTHETIC_EXTENSION_PROPERTY: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SYNTHETIC_EXTENSION_PROPERTY)
    val DYNAMIC_PROPERTY_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DYNAMIC_PROPERTY_CALL)
    val ANDROID_EXTENSIONS_PROPERTY_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ANDROID_EXTENSIONS_PROPERTY_CALL)
    val INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION: HighlightInfoType = createSymbolTypeInfo(
        CangJieHighlightingColors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
    )
    val PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION)

    // functions
    val FUNCTION_LITERAL_DEFAULT_PARAMETER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_LITERAL_DEFAULT_PARAMETER)
    val FUNCTION_DECLARATION: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_DECLARATION)
    val FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.FUNCTION_CALL)
    val PACKAGE_FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.PACKAGE_FUNCTION_CALL)
    val EXTENSION_FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.EXTENSION_FUNCTION_CALL)
    val CONSTRUCTOR_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.CONSTRUCTOR_CALL)
    val DYNAMIC_FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.DYNAMIC_FUNCTION_CALL)
    val SUSPEND_FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SUSPEND_FUNCTION_CALL)
    val VARIABLE_AS_FUNCTION_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.VARIABLE_AS_FUNCTION_CALL)
    val VARIABLE_AS_FUNCTION_LIKE_CALL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL)

    // other
    val SMART_CAST_VALUE: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CAST_VALUE)
    val SMART_CONSTANT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CONSTANT)
    val SMART_CAST_RECEIVER: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.SMART_CAST_RECEIVER)
    val LABEL: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.LABEL)
    val NAMED_ARGUMENT: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.NAMED_ARGUMENT)
    val ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES: HighlightInfoType = createSymbolTypeInfo(CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES)

    private fun createSymbolTypeInfo(attributesKey: TextAttributesKey): HighlightInfoType {
        return HighlightInfoType.HighlightInfoTypeImpl(HighlightInfoType.SYMBOL_TYPE_SEVERITY, attributesKey, false)
    }
}
