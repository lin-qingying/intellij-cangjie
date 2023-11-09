package com.huawei.cangjie.lexer

import com.intellij.lexer.FlexAdapter
import com.intellij.psi.tree.IElementType
import java.util.*


class CangJieLexer : FlexAdapter(_JetLexer()) {

//    private val braceStack: Stack<Int> = Stack()
//    private val lBraceCount = 0
//    private val commentDepth = 0
//    private val commentStart = 0


//    override fun advance() {
//        super.advance()
//        if (super.getTokenType() == CjTokens.LBRACE) {
//            braceStack.push(super.getTokenStart())
//        } else if (super.getTokenType() == CjTokens.RBRACE) {
//            braceStack.pop()
//        }
//
//    }

//    override fun getTokenType(): IElementType? {
//        val type = super.getTokenType()
//
//        //处理>>和连续泛型声明a<b<c>>冲突的问题
////        if (type == CjTokens.GT) {
////            val nextType = super.getTokenType()
////            if (nextType == CjTokens.GT) {
////                return CjTokens.GTGT
////            }
////        }
//        return type
//
//    }
}
