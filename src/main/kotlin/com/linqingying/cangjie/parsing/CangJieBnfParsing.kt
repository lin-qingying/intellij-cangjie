package com.linqingying.cangjie.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase

class CangJieBnfParsing(builder: SemanticWhitespaceAwarePsiBuilder,private val cangJieParsing: CangJieParsing, isLazy:Boolean): AbstractCangJieParsing(
    builder, isLazy
) {


//    /* ********************************************************** */ // CONSTANT_PATTERN | WILDCARD_PATTERN | BINDING_PATTERN | TUPLE_PATTERN | TYPE_PATTERN | ENUM_PATTERN
//    fun pattern(  l: Int): Boolean {
//
//        var r: Boolean
//        r =  CONSTANT_PATTERN(  l + 1)
//        if (!r) r =  WILDCARD_PATTERN(  l + 1)
//        if (!r) r =  BINDING_PATTERN(  l + 1)
//        if (!r) r =  TUPLE_PATTERN(  l + 1)
//        if (!r) r =  TYPE_PATTERN( l + 1)
//        if (!r) r =  ENUM_PATTERN(  l + 1)
//        return r
//    }

    override fun create(builder: SemanticWhitespaceAwarePsiBuilder?): CangJieParsing =cangJieParsing.create(builder)
}
