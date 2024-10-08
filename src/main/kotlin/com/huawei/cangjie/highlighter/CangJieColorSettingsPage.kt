package com.huawei.cangjie.highlighter


import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.icon.CangJieIcons
import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.OptionsBundle
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.options.colors.RainbowColorSettingsPage
import com.intellij.openapi.util.NlsSafe

import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class CangJieColorSettingsPage : ColorSettingsPage, RainbowColorSettingsPage {
    override fun getLanguage() = CangJieLanguage
    override fun getIcon() = CangJieIcons.CANGJIE_16
    override fun getHighlighter(): SyntaxHighlighter = CangJieHighlighter()

    override fun getDemoText(): String {
        return """/* Block comment */
<KEYWORD>package</KEYWORD> hello
<KEYWORD>from</KEYWORD> std <KEYWORD>import</KEYWORD> socket.* // line comment
<KEYWORD>import</KEYWORD> socket.* // line comment
/**
 * Doc comment here for `SomeClass`
 * @see <CDOC_LINK>Iterator#next()</CDOC_LINK>
 */
 
<BUILTIN_ANNOTATION>private</BUILTIN_ANNOTATION> class <CLASS>MyClass</CLASS>  {
    func <FUNCTION_DECLARATION>foo</FUNCTION_DECLARATION>(  <PARAMETER>str</PARAMETER> : <TRAIT>String</TRAIT>   ) {
        <PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL>("length\nis ${"$"}{<PARAMETER>str</PARAMETER><SAFE_ACCESS>.</SAFE_ACCESS><INSTANCE_PROPERTY>size</INSTANCE_PROPERTY>} <STRING_ESCAPE><INVALID_STRING_ESCAPE>\e</INVALID_STRING_ESCAPE></STRING_ESCAPE>")
         
 
                   str.<DYNAMIC_FUNCTION_CALL>indexOf</DYNAMIC_FUNCTION_CALL>()
     
 
    }

    <BUILTIN_ANNOTATION>override</BUILTIN_ANNOTATION> func hashCode(): Int {
        return <KEYWORD>super</KEYWORD>.<FUNCTION_CALL>hashCode</FUNCTION_CALL>() * 31
    }
}

 

var <PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION><MUTABLE_VARIABLE>globalCounter</MUTABLE_VARIABLE></PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION> : Int = <NUMBER>5</NUMBER>
 

<KEYWORD>abstract</KEYWORD> class <ABSTRACT_CLASS>Abstract</ABSTRACT_CLASS> {
    prop <INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION>bar</INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION>  : <CLASS>Int</CLASS>  {
        <KEYWORD>get</KEYWORD>() { <NUMBER>5</NUMBER> }
        <KEYWORD>set</KEYWORD>(<PARAMETER>value</PARAMETER>) { <INSTANCE_PROPERTY>field</INSTANCE_PROPERTY> = <PARAMETER><VARIABLE_AS_FUNCTION_CALL>value</VARIABLE_AS_FUNCTION_CALL></PARAMETER> }
    
    
    }
    func <FUNCTION_DECLARATION>test</FUNCTION_DECLARATION>() {
        <INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION>bar</INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION>
    }
}

 

<KEYWORD>enum</KEYWORD>   <ENUM>E</ENUM> { <ENUM_ENTRY>A</ENUM_ENTRY> | <ENUM_ENTRY>B</ENUM_ENTRY> }

<KEYWORD>interface</KEYWORD> <TRAIT>FunctionLike</TRAIT> {
    <KEYWORD>func</KEYWORD> <FUNCTION_DECLARATION>invoke</FUNCTION_DECLARATION>()  
}
<KEYWORD>type</KEYWORD> <TYPE_ALIAS>Predicate</TYPE_ALIAS><<TYPE_PARAMETER>T</TYPE_PARAMETER>> = (<TYPE_PARAMETER>T</TYPE_PARAMETER>) -> <CLASS>Bool</CLASS>

 
<KEYWORD>func</KEYWORD> <FUNCTION_DECLARATION>baz</FUNCTION_DECLARATION>(<PARAMETER>p</PARAMETER>: <TYPE_ALIAS>Predicate</TYPE_ALIAS><<CLASS>Int</CLASS>>) {
  print(1,<NAMED_ARGUMENT>flush :</NAMED_ARGUMENT>  <KEYWORD>true</KEYWORD>)
<KEYWORD>return </KEYWORD> <PARAMETER><VARIABLE_AS_FUNCTION_CALL>p</VARIABLE_AS_FUNCTION_CALL></PARAMETER>(<NUMBER>42</NUMBER>)
} 

 """
    }
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
        val map = HashMap<String, TextAttributesKey>()



        for (field in CangJieHighlightingColors::class.memberProperties) {
            if (field.visibility == KVisibility.PUBLIC) {
                try {
                    map[field.name] = field.getter.call(CangJieHighlightingColors) as TextAttributesKey
                } catch (e: Exception) {
                    assert(false)
                }
            }
//            if (Modifier.isStatic(field.modifiers)) {
//                try {
//                    map[field.name] = field.get(null) as TextAttributesKey
//
//                } catch (e: IllegalAccessException) {
//                    assert(false)
//                }
//
//            }
        }

        map.putAll(DslStyleUtils.descriptionsToStyles)

        return map
    }
//    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
//        val map = HashMap<String, TextAttributesKey>()
//        for (field in CangJieHighlightingColors::class.java.fields) {
//
//            if (Modifier.isStatic(field.modifiers)) {
//                try {
//                    map[field.name] = field.get(null) as TextAttributesKey
//
//                } catch (e: IllegalAccessException) {
//                    assert(false)
//                }
//
//            }
//        }
//
//        map.putAll(DslStyleUtils.descriptionsToStyles)
//
//        return map
//    }

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        infix fun String.to(key: TextAttributesKey) = AttributesDescriptor(this, key)

        return arrayOf(
            CangJieBundle.message("highlighter.descriptor.text.builtin.keyword") to CangJieHighlightingColors.KEYWORD,
            CangJieBundle.message("highlighter.descriptor.text.builtin.keyword.let") to CangJieHighlightingColors.LET_KEYWORD,
            CangJieBundle.message("highlighter.descriptor.text.builtin.keyword.const") to CangJieHighlightingColors.CONST_KEYWORD,
            CangJieBundle.message("highlighter.descriptor.text.builtin.keyword.var") to CangJieHighlightingColors.VAR_KEYWORD,
//            CangJieBundle.message("highlighter.descriptor.text.builtin.annotation") to CangJieHighlightingColors.BUILTIN_ANNOTATION,
            CangJieBundle.message("highlighter.descriptor.text.string.escape") to CangJieHighlightingColors.STRING_ESCAPE,
            CangJieBundle.message("highlighter.descriptor.text.closure.braces") to CangJieHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW,
            CangJieBundle.message("highlighter.descriptor.text.arrow") to CangJieHighlightingColors.ARROW,
            CangJieBundle.message("highlighter.descriptor.text.colon") to CangJieHighlightingColors.COLON,
            CangJieBundle.message("highlighter.descriptor.text.double.colon") to CangJieHighlightingColors.DOUBLE_COLON,
            CangJieBundle.message("highlighter.descriptor.text.safe.access") to CangJieHighlightingColors.SAFE_ACCESS,
            CangJieBundle.message("highlighter.descriptor.text.quest") to CangJieHighlightingColors.QUEST,
            CangJieBundle.message("highlighter.descriptor.text.exclexcl") to CangJieHighlightingColors.EXCLEXCL,
            OptionsBundle.message("options.java.attribute.descriptor.line.comment") to CangJieHighlightingColors.LINE_COMMENT,
            OptionsBundle.message("options.java.attribute.descriptor.block.comment") to CangJieHighlightingColors.BLOCK_COMMENT,
            CangJieBundle.message("highlighter.descriptor.text.cdoc.comment") to CangJieHighlightingColors.DOC_COMMENT,
            CangJieBundle.message("highlighter.descriptor.text.cdoc.tag") to CangJieHighlightingColors.CDOC_TAG,
            CangJieBundle.message("highlighter.descriptor.text.cdoc.value") to CangJieHighlightingColors.CDOC_LINK,
            CangJieBundle.message("highlighter.descriptor.text.abstract.class") to CangJieHighlightingColors.ABSTRACT_CLASS,
//            CangJieBundle.message("highlighter.descriptor.text.annotation") to CangJieHighlightingColors.ANNOTATION,
//            CangJieBundle.message("highlighter.descriptor.text.annotation.attribute.name") to CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES,
            OptionsBundle.message("options.java.attribute.descriptor.type.parameter") to CangJieHighlightingColors.TYPE_PARAMETER,

            CangJieBundle.message("highlighter.descriptor.text.enum") to CangJieHighlightingColors.ENUM,
            CangJieBundle.message("highlighter.descriptor.text.enumEntry") to CangJieHighlightingColors.ENUM_ENTRY,
            CangJieBundle.message("highlighter.descriptor.text.typeAlias") to CangJieHighlightingColors.TYPE_ALIAS,
            CangJieBundle.message("highlighter.descriptor.text.var") to CangJieHighlightingColors.MUTABLE_VARIABLE,
            CangJieBundle.message("highlighter.descriptor.text.local.variable") to CangJieHighlightingColors.LOCAL_VARIABLE,
            CangJieBundle.message("highlighter.descriptor.text.captured.variable") to CangJieHighlightingColors.WRAPPED_INTO_REF,
            CangJieBundle.message("highlighter.descriptor.text.instance.property") to CangJieHighlightingColors.INSTANCE_PROPERTY,
            CangJieBundle.message("highlighter.descriptor.text.instance.property.custom.property.declaration") to CangJieHighlightingColors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
            OptionsBundle.message("options.java.attribute.descriptor.parameter") to CangJieHighlightingColors.PARAMETER,


            CangJieBundle.message("highlighter.descriptor.text.package.property.custom.property.declaration") to CangJieHighlightingColors.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
            CangJieBundle.message("highlighter.descriptor.text.package.property") to CangJieHighlightingColors.PACKAGE_PROPERTY,
            CangJieBundle.message("highlighter.descriptor.text.field") to CangJieHighlightingColors.BACKING_FIELD_VARIABLE,
            CangJieBundle.message("highlighter.descriptor.text.extension.property") to CangJieHighlightingColors.EXTENSION_PROPERTY,
            CangJieBundle.message("highlighter.descriptor.text.synthetic.extension.property") to CangJieHighlightingColors.SYNTHETIC_EXTENSION_PROPERTY,
            CangJieBundle.message("highlighter.descriptor.text.dynamic.property") to CangJieHighlightingColors.DYNAMIC_PROPERTY_CALL,
            CangJieBundle.message("highlighter.descriptor.text.android.extensions.property") to CangJieHighlightingColors.ANDROID_EXTENSIONS_PROPERTY_CALL,
            CangJieBundle.message("highlighter.descriptor.text.func") to CangJieHighlightingColors.FUNCTION_DECLARATION,
            CangJieBundle.message("highlighter.descriptor.text.func.call") to CangJieHighlightingColors.FUNCTION_CALL,
            CangJieBundle.message("highlighter.descriptor.text.dynamic.func.call") to CangJieHighlightingColors.DYNAMIC_FUNCTION_CALL,
            CangJieBundle.message("highlighter.descriptor.text.suspend.func.call") to CangJieHighlightingColors.SUSPEND_FUNCTION_CALL,
            CangJieBundle.message("highlighter.descriptor.text.package.func.call") to CangJieHighlightingColors.PACKAGE_FUNCTION_CALL,
            CangJieBundle.message("highlighter.descriptor.text.extension.func.call") to CangJieHighlightingColors.EXTENSION_FUNCTION_CALL,
            CangJieBundle.message("highlighter.descriptor.text.constructor.call") to CangJieHighlightingColors.CONSTRUCTOR_CALL,
            CangJieBundle.message("highlighter.descriptor.text.variable.as.function.call") to CangJieHighlightingColors.VARIABLE_AS_FUNCTION_CALL,
            CangJieBundle.message("highlighter.descriptor.text.variable.as.function.like.call") to CangJieHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL,
//            CangJieBundle.message("highlighter.descriptor.text.smart.cast") to CangJieHighlightingColors.SMART_CAST_VALUE,
//            CangJieBundle.message("highlighter.descriptor.text.smart.constant") to CangJieHighlightingColors.SMART_CONSTANT,
//            CangJieBundle.message("highlighter.descriptor.text.smart.cast.receiver") to CangJieHighlightingColors.SMART_CAST_RECEIVER,
//            CangJieBundle.message("highlighter.descriptor.text.label") to CangJieHighlightingColors.LABEL,
            CangJieBundle.message("highlighter.descriptor.text.named.argument") to CangJieHighlightingColors.NAMED_ARGUMENT
        ) + DslStyleUtils.descriptionsToStyles.map { (description, key) -> description to key }.toTypedArray()
    }

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String {
        @NlsSafe
        val name = CangJieLanguage.NAME
        return name
    }

    override fun isRainbowType(type: TextAttributesKey): Boolean {
        return type == CangJieHighlightingColors.LOCAL_VARIABLE ||
                type == CangJieHighlightingColors.PARAMETER
    }
}
