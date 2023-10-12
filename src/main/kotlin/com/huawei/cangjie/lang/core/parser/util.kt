package com.huawei.cangjie.lang.core.parser

/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

//
//import com.intellij.lang.LanguageParserDefinitions
//import com.intellij.lang.PsiBuilder
//import com.intellij.lang.PsiBuilderFactory
//import com.intellij.lang.parser.GeneratedParserUtilBase
//import com.intellij.openapi.project.Project
//import com.huawei.cangjie.lang.CjLanguage
//import com.huawei.cangjie.lang.core.psi.CjElementTypes
//
//fun Project.createCangJiePsiBuilder(text: CharSequence): PsiBuilder {
//    val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(CjLanguage)
//        ?: error("No parser definition for language $CjLanguage")
//    val lexer = parserDefinition.createLexer(this)
//    return PsiBuilderFactory.getInstance().createBuilder(parserDefinition, lexer, text)
//}
//
///** Creates [PsiBuilder] suitable for Grammar Kit generated methods */
//fun Project.createAdaptedCangJiePsiBuilder(text: CharSequence): PsiBuilder {
//    val b = GeneratedParserUtilBase.adapt_builder_(
//        CjElementTypes.FUNCTION,
//        createCangJiePsiBuilder(text),
//        CangJieParser(),
//        CangJieParser.EXTENDS_SETS_
//    )
//    // Equivalent to `GeneratedParserUtilBase.enter_section_`.
//    // Allows to call `CangJieParser.*` methods without entering the section
//    GeneratedParserUtilBase.ErrorState.get(b).currentFrame = GeneratedParserUtilBase.Frame()
//    return b
//}
//
//inline fun <T> PsiBuilder.probe(action: () -> T): T {
//    val mark = mark()
//    try {
//        return action()
//    } finally {
//        mark.rollbackTo()
//    }
//}
//
//inline fun PsiBuilder.rollbackIfFalse(action: () -> Boolean): Boolean {
//    val mark = mark()
//    return if (action()) {
//        true
//    } else {
//        mark.rollbackTo()
//        false
//    }
//}
//
//fun PsiBuilder.Marker.close(result: Boolean): Boolean {
//    if (result) {
//        drop()
//    } else {
//        rollbackTo()
//    }
//    return result
//}
//
//fun PsiBuilder.clearFrame() {
//    val state = GeneratedParserUtilBase.ErrorState.get(this)
//    val currentFrame = state.currentFrame
//    if (currentFrame != null) {
//        currentFrame.errorReportedAt = -1
//        currentFrame.lastVariantAt = -1
//    }
//}
//
///** Similar to [com.intellij.lang.PsiBuilderUtil.rawTokenText] */
//fun PsiBuilder.rawLookupText(steps: Int): CharSequence {
//    val start = rawTokenTypeStart(steps)
//    val end = rawTokenTypeStart(steps + 1)
//    return if (start == -1 || end == -1) "" else originalText.subSequence(start, end)
//}
