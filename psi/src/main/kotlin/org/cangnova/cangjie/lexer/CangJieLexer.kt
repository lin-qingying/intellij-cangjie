/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.lexer

import com.intellij.lexer.FlexAdapter

class CangJieLexer : FlexAdapter(_CangJieLexer()) {

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
// //        if (type == CjTokens.GT) {
// //            val nextType = super.getTokenType()
// //            if (nextType == CjTokens.GT) {
// //                return CjTokens.GTGT
// //            }
// //        }
//        return type
//
//    }
}
