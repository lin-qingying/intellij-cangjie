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

package org.cangnova.cangjie.highlighter

import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.intellij.openapi.options.colors.RainbowColorSettingsPage
import com.intellij.openapi.util.NlsSafe
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

class CangJieColorSettingsPage : ColorSettingsPage, RainbowColorSettingsPage {
    override fun getLanguage() = CangJieLanguage
    override fun getIcon() = CangJieIcons.CANGJIE
    override fun getHighlighter(): SyntaxHighlighter = CangJieHighlighter()

    override fun getDemoText(): String {
        return """/* 块注释 Block comment */
<KEYWORD>package</KEYWORD> demo

// 行注释 Line comment
<KEYWORD>import</KEYWORD> std.{io.*, math.*}

// 宏声明
<KEYWORD>macro</KEYWORD> <MACRO_DECLARATION>name</MACRO_DECLARATION>(<PARAMETER>input</PARAMETER>: <TYPE_REFERENCE>Tokens</TYPE_REFERENCE>): <TYPE_REFERENCE>Tokens</TYPE_REFERENCE> {
    <KEYWORD>return</KEYWORD> <PARAMETER>input</PARAMETER>
}

// 宏调用
<MACRO_CALL>@derive</MACRO_CALL>[<STRING>"a"</STRING>]

// 接口声明
<KEYWORD>interface</KEYWORD> <INTERFACE>Printable</INTERFACE> {
    <KEYWORD>func</KEYWORD> <FUNCTION_DECLARATION>print</FUNCTION_DECLARATION>(): <TYPE_REFERENCE>Unit</TYPE_REFERENCE>
}
// 枚举定义
    <KEYWORD>enum</KEYWORD> <ENUM>Color</ENUM> <BRACES>{</BRACES>
        <ENUM_CONSTRUCTOR>RED</ENUM_CONSTRUCTOR><COMMA>,</COMMA>
        <ENUM_CONSTRUCTOR>GREEN</ENUM_CONSTRUCTOR><COMMA>,</COMMA>
        <ENUM_CONSTRUCTOR>BLUE</ENUM_CONSTRUCTOR>
    <BRACES>}</BRACES>
// 结构体声明
<KEYWORD>struct</KEYWORD> <STRUCT>Point</STRUCT> {
 
}

/**
 * CDoc 文档注释
 * @see <CDOC_LINK>Iterator#next()</CDOC_LINK>
 */
<KEYWORD>public</KEYWORD> <KEYWORD>class</KEYWORD> <CLASS>DemoClass</CLASS> <OPERATOR_SIGN><:</OPERATOR_SIGN> <TYPE_REFERENCE>Runnable</TYPE_REFERENCE> {
    // 属性定义
    <KEYWORD>const</KEYWORD> PI: <TYPE_REFERENCE>Float</TYPE_REFERENCE> = <NUMBER>3.14159</NUMBER>
    <KEYWORD>let</KEYWORD> name: <TYPE_REFERENCE>String</TYPE_REFERENCE> = <STRING>"Hello"</STRING>
    <KEYWORD>var</KEYWORD> <MUTABLE_VARIABLE>count</MUTABLE_VARIABLE>: <TYPE_REFERENCE>Int</TYPE_REFERENCE> = <NUMBER>0</NUMBER>
    
    // 可变属性定义
    <KEYWORD>mut</KEYWORD> <KEYWORD>prop</KEYWORD> <MUTABLE_PROPERTY>items</MUTABLE_PROPERTY>: <TYPE_REFERENCE>String</TYPE_REFERENCE> {
        <KEYWORD>get</KEYWORD>() {
            <KEYWORD>return</KEYWORD> <STRING>""</STRING>
        }
        <KEYWORD>set</KEYWORD>(<PARAMETER>value</PARAMETER>) {
            // setter implementation
        }
    }
     // 构造函数
    <FUNCTION_DECLARATION>init</FUNCTION_DECLARATION>(<PARAMETER>name</PARAMETER>: <TYPE_REFERENCE>String</TYPE_REFERENCE>) {
        <KEYWORD>this</KEYWORD><DOT>.</DOT><INSTANCE_PROPERTY>name</INSTANCE_PROPERTY> = <PARAMETER>name</PARAMETER>
    }
    
    // 可变方法定义
    <KEYWORD>mut</KEYWORD> <KEYWORD>func</KEYWORD> <FUNCTION_DECLARATION>calculate</FUNCTION_DECLARATION>(<PARAMETER>x</PARAMETER>: <TYPE_REFERENCE>Int</TYPE_REFERENCE>, <PARAMETER>y</PARAMETER>: <TYPE_REFERENCE>Int</TYPE_REFERENCE>): <TYPE_REFERENCE>Int</TYPE_REFERENCE> {
        <KEYWORD>let</KEYWORD> <LOCAL_VARIABLE>result</LOCAL_VARIABLE> = <PACKAGE_FUNCTION_CALL>max</PACKAGE_FUNCTION_CALL>(<PARAMETER>x</PARAMETER>, <PARAMETER>y</PARAMETER>) <OPERATOR_SIGN>*</OPERATOR_SIGN> <NUMBER>2</NUMBER>
        <KEYWORD>return</KEYWORD> <LOCAL_VARIABLE>result</LOCAL_VARIABLE>
    }
    // Lambda 表达式
    <KEYWORD>let</KEYWORD> <LOCAL_VARIABLE>operation</LOCAL_VARIABLE> = { <PARAMETER>a1</PARAMETER>: <TYPE_REFERENCE>Int</TYPE_REFERENCE>, <PARAMETER>a2</PARAMETER>: <TYPE_REFERENCE>String</TYPE_REFERENCE> =>
        <PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL>(<PARAMETER>a1</PARAMETER>)
        <PACKAGE_FUNCTION_CALL>println</PACKAGE_FUNCTION_CALL>(<PARAMETER>a2</PARAMETER>)
    }
     // 类型别名
    <KEYWORD>type</KEYWORD> <TYPE_ALIAS>String1</TYPE_ALIAS> = <TYPE_REFERENCE>String</TYPE_REFERENCE>
    
    // 错误字符和解析错误示例
    <BAD_CHARACTER>@</BAD_CHARACTER> <RESOLVED_TO_ERROR>unknownFunction</RESOLVED_TO_ERROR>()
}"""
    }

// 重写getAdditionalHighlightingTagToDescriptorMap方法，返回一个Map，其中包含CangJieHighlightingColors类中所有公共属性的名称和对应的TextAttributesKey
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> {
        // 创建一个HashMap，用于存储属性名称和对应的TextAttributesKey
        val map = HashMap<String, TextAttributesKey>()

        // 遍历CangJieHighlightingColors类的所有成员属性
        for (field in CangJieHighlightingColors::class.memberProperties) {

            if (field.visibility == KVisibility.PUBLIC) {
                try {
                    // 将属性名称和对应的TextAttributesKey存入map中
                    map[field.name] = field.getter.call(CangJieHighlightingColors) as TextAttributesKey
                } catch (e: Exception) {
                    // 如果出现异常，断言失败
                    assert(false)
                }
            }
//            // 如果属性是静态的
//            if (Modifier.isStatic(field.modifiers)) {
//                try {
//                    // 将属性名称和对应的TextAttributesKey存入map中
//                    map[field.name] = field.get(null) as TextAttributesKey
//
//                } catch (e: IllegalAccessException) {
//                    // 如果出现异常，断言失败
//                    assert(false)
//                }
//
//            }
        }

        return map
    }


    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        infix fun String.to(key: TextAttributesKey) = AttributesDescriptor(this, key)

        return arrayOf(
            CangJieHighlightingBundle.message("highlighter.descriptor.text.builtin.keyword") to CangJieHighlightingColors.KEYWORD,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.builtin.keyword.let") to CangJieHighlightingColors.LET_KEYWORD,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.builtin.keyword.quote") to CangJieHighlightingColors.QUOTE_KEYWORD,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.builtin.keyword.const") to CangJieHighlightingColors.CONST_KEYWORD,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.builtin.keyword.var") to CangJieHighlightingColors.VAR_KEYWORD,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.builtin.keyword.mut") to CangJieHighlightingColors.MUT_KEYWORD,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.builtin.keyword.prop") to CangJieHighlightingColors.PROP_KEYWORD,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.builtin.keyword.static") to CangJieHighlightingColors.STATIC_KEYWORD,

            // 字面量相关的高亮设置
            CangJieHighlightingBundle.message("highlighter.descriptor.text.literal.number") to CangJieHighlightingColors.NUMBER,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.literal.string") to CangJieHighlightingColors.STRING,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.literal.string.escape") to CangJieHighlightingColors.STRING_ESCAPE,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.literal.string.invalid.escape") to CangJieHighlightingColors.INVALID_STRING_ESCAPE,

//            CangJieBundle.message("highlighter.descriptor.text.builtin.annotation") to CangJieHighlightingColors.BUILTIN_ANNOTATION,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.string.escape") to CangJieHighlightingColors.STRING_ESCAPE,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.closure.braces") to CangJieHighlightingColors.FUNCTION_LITERAL_BRACES_AND_ARROW,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.arrow") to CangJieHighlightingColors.ARROW,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.colon") to CangJieHighlightingColors.COLON,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.double.colon") to CangJieHighlightingColors.DOUBLE_COLON,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.safe.access") to CangJieHighlightingColors.SAFE_ACCESS,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.quest") to CangJieHighlightingColors.QUEST,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.exclexcl") to CangJieHighlightingColors.EXCLEXCL,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.line.comment") to CangJieHighlightingColors.LINE_COMMENT,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.operator.sign") to CangJieHighlightingColors.OPERATOR_SIGN,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.block.comment") to CangJieHighlightingColors.BLOCK_COMMENT,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.cdoc.comment") to CangJieHighlightingColors.DOC_COMMENT,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.cdoc.tag") to CangJieHighlightingColors.CDOC_TAG,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.cdoc.value") to CangJieHighlightingColors.CDOC_LINK,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.abstract.class") to CangJieHighlightingColors.ABSTRACT_CLASS,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.annotation") to CangJieHighlightingColors.ANNOTATION,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.annotation.attribute.name") to CangJieHighlightingColors.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.type.reference") to CangJieHighlightingColors.TYPE_REFERENCE,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.type.parameter") to CangJieHighlightingColors.TYPE_PARAMETER,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.typeDefined") to CangJieHighlightingColors.TYPE_DEFINED,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.typeAlias") to CangJieHighlightingColors.TYPE_ALIAS,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.enum") to CangJieHighlightingColors.ENUM,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.enumEntry") to CangJieHighlightingColors.ENUM_CONSTRUCTOR,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.var") to CangJieHighlightingColors.MUTABLE_VARIABLE,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.local.variable") to CangJieHighlightingColors.LOCAL_VARIABLE,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.captured.variable") to CangJieHighlightingColors.WRAPPED_INTO_REF,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.instance.property") to CangJieHighlightingColors.INSTANCE_PROPERTY,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.instance.property.custom.property.declaration") to CangJieHighlightingColors.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.parameter") to CangJieHighlightingColors.PARAMETER,

//            CangJieHighlightingBundle.message("highlighter.descriptor.text.package.property.custom.property.declaration") to CangJieHighlightingColors.PACKAGE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.package.variable") to CangJieHighlightingColors.PACKAGE_VARIABLE,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.field") to CangJieHighlightingColors.BACKING_FIELD_VARIABLE,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.extension.property") to CangJieHighlightingColors.EXTENSION_PROPERTY,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.synthetic.extension.property") to CangJieHighlightingColors.SYNTHETIC_EXTENSION_PROPERTY,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.dynamic.property") to CangJieHighlightingColors.DYNAMIC_PROPERTY_CALL,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.func") to CangJieHighlightingColors.FUNCTION_DECLARATION,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.func.call") to CangJieHighlightingColors.FUNCTION_CALL,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.dynamic.func.call") to CangJieHighlightingColors.DYNAMIC_FUNCTION_CALL,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.suspend.func.call") to CangJieHighlightingColors.SUSPEND_FUNCTION_CALL,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.package.func.call") to CangJieHighlightingColors.PACKAGE_FUNCTION_CALL,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.extension.func.call") to CangJieHighlightingColors.EXTENSION_FUNCTION_CALL,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.constructor.call") to CangJieHighlightingColors.CONSTRUCTOR_CALL,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.variable.as.function.call") to CangJieHighlightingColors.VARIABLE_AS_FUNCTION_CALL,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.variable.as.function.like.call") to CangJieHighlightingColors.VARIABLE_AS_FUNCTION_LIKE_CALL,
//            CangJieBundle.message("highlighter.descriptor.text.smart.cast") to CangJieHighlightingColors.SMART_CAST_VALUE,
//            CangJieBundle.message("highlighter.descriptor.text.smart.constant") to CangJieHighlightingColors.SMART_CONSTANT,
//            CangJieBundle.message("highlighter.descriptor.text.smart.cast.receiver") to CangJieHighlightingColors.SMART_CAST_RECEIVER,
//            CangJieBundle.message("highlighter.descriptor.text.label") to CangJieHighlightingColors.LABEL,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.func.named.argument") to CangJieHighlightingColors.NAMED_ARGUMENT,

            // 括号和标点符号
            CangJieHighlightingBundle.message("highlighter.descriptor.text.parenthesis") to CangJieHighlightingColors.PARENTHESIS,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.braces") to CangJieHighlightingColors.BRACES,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.brackets") to CangJieHighlightingColors.BRACKETS,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.comma") to CangJieHighlightingColors.COMMA,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.semicolon") to CangJieHighlightingColors.SEMICOLON,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.dot") to CangJieHighlightingColors.DOT,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.lt.colon") to CangJieHighlightingColors.LT_COLON,

            // 类和接口
            CangJieHighlightingBundle.message("highlighter.descriptor.text.class") to CangJieHighlightingColors.CLASS,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.interface") to CangJieHighlightingColors.INTERFACE,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.struct") to CangJieHighlightingColors.STRUCT,

            // 变量和属性
            CangJieHighlightingBundle.message("highlighter.descriptor.text.mutable.property") to CangJieHighlightingColors.MUTABLE_PROPERTY,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.property") to CangJieHighlightingColors.PROPERTY,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.instance.variable") to CangJieHighlightingColors.INSTANCE_VARIABLE,

            // 智能转换
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.smart.cast.value") to CangJieHighlightingColors.SMART_CAST_VALUE,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.smart.constant") to CangJieHighlightingColors.SMART_CONSTANT,
//            CangJieHighlightingBundle.message("highlighter.descriptor.text.smart.cast.receiver") to CangJieHighlightingColors.SMART_CAST_RECEIVER,

            // 宏
            CangJieHighlightingBundle.message("highlighter.descriptor.text.macro.declaration") to CangJieHighlightingColors.MACRO_DECLARATION,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.macro.call") to CangJieHighlightingColors.MACRO_CALL,

            // 其他
            CangJieHighlightingBundle.message("highlighter.descriptor.text.debug.info") to CangJieHighlightingColors.DEBUG_INFO,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.bad.character") to CangJieHighlightingColors.BAD_CHARACTER,
            CangJieHighlightingBundle.message("highlighter.descriptor.text.resolved.to.error") to CangJieHighlightingColors.RESOLVED_TO_ERROR,
        )
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
