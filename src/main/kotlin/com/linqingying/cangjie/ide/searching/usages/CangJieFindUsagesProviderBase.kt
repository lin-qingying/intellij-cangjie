package com.linqingying.cangjie.ide.searching.usages

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.configurable.services.CangJieLanguageServerServices
import com.linqingying.cangjie.configurable.services.Feature
import com.linqingying.cangjie.lexer.CangJieLexer
import com.linqingying.cangjie.lexer.CjTokens.*
import com.linqingying.cangjie.psi.*
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet

class CangJieWordsScanner : DefaultWordsScanner(
    CangJieLexer(),
    TokenSet.create(IDENTIFIER, CjNodeTypes.OPERATION_NAME).also { KEYWORDS },
    COMMENTS,
    TokenSet.create(
        INTEGER_LITERAL,
        RUNE_LITERAL,
        FLOAT_LITERAL,
        TRUE_KEYWORD,
        FALSE_KEYWORD
    ).also { STRINGS }
)

open class CangJieFindUsagesProviderBase : FindUsagesProvider {


    override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
        psiElement is CjNamedDeclaration

    override fun getWordsScanner(): WordsScanner? = CangJieWordsScanner()

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return when (element) {
            is CjBindingPattern ->  CangJieBundle.message("find.usages.variable")
            is CjNamedFunction -> CangJieBundle.message("find.usages.function")
            is CjClass -> CangJieBundle.message("find.usages.class")
            is CjParameter -> CangJieBundle.message("find.usages.parameter")
            is CjVariable -> if (element.isLocal)
                CangJieBundle.message("find.usages.variable")
            else
                CangJieBundle.message("find.usages.property")

            is CjDestructuringDeclarationEntry -> CangJieBundle.message("find.usages.variable")
            is CjTypeParameter -> CangJieBundle.message("find.usages.type.parameter")
            is CjSecondaryConstructor -> CangJieBundle.message("find.usages.constructor")

            else -> ""
        }
    }

    val CjDeclaration.containerDescription: String?
        get() {

            (parent as? CjFile)?.parent?.let { return getDescriptiveName(it) }
            return null
        }
//获取要搜索的名称
    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {

            is CjTypeStatement -> {

                run {
                    @Suppress("HardCodedStringLiteral")
                    element.fqName?.asString()
                } ?: element.name ?: CangJieBundle.message("usage.provider.text.unnamed")
            }
            /*is CjProperty ,*/is CjVariable -> {
                val name = element.name ?: ""
                element.containerDescription?.let {
                    CangJieBundle.message(
                        "usage.provider.text.property.of.0",
                        name,
                        it
                    )
                } ?: name
            }

            is CjFunction -> {

                @Suppress("HardCodedStringLiteral")

                return  if(element.isOperator){
                   ( element as CjNamedDeclarationStub<*>).operatorName?.text?.let { "operator  $it(...)" } ?: ""

                }else{
                    element.name?.let { "$it(...)" } ?: ""

                }
            }
//            is CjLabeledExpression -> {
//                @Suppress("HardCodedStringLiteral")
//                element.getLabelName() ?: ""
//            }
            is CjImportAlias -> element.name ?: ""
            is CjLightElement<*, *> -> element.cangjieOrigin?.let { getDescriptiveName(it) } ?: ""
            is CjParameter -> {
//                if (element.isPropertyParameter()) {
//                    val name = element.name ?: ""
//                    element.containerDescription?.let { CangJieBundle.message("usage.provider.text.property.of.0", name, it) } ?: name
//                } else {
                element.name ?: ""
//                }
            }

            is PsiNamedElement -> element.name ?: ""
            else -> ""
        }
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
