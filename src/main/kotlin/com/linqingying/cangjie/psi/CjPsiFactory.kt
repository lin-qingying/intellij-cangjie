package com.linqingying.cangjie.psi

import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.utils.checkWithAttachment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.util.LocalTimeCounter
import org.jetbrains.annotations.NonNls



var CjFile.doNotAnalyze: String? by UserDataProperty(Key.create("DO_NOT_ANALYZE"))
var CjFile.analysisContext: PsiElement? by UserDataProperty(Key.create("ANALYSIS_CONTEXT"))


private const val DO_NOT_ANALYZE_NOTIFICATION = "This file was created by CjPsiFactory and should not be analyzed\n" +
        "Use createAnalyzableFile to create file that can be analyzed\n"
class CjPsiFactory private constructor(
    private val project: Project,
    private val markGenerated: Boolean,
    private val context: PsiElement?,
    private val eventSystemEnabled: Boolean,
) {

    companion object {
        @JvmStatic
        @JvmOverloads
        fun contextual(context: PsiElement, markGenerated: Boolean = true): CjPsiFactory {
            return CjPsiFactory(context.project, markGenerated, context, eventSystemEnabled = false)
        }
    }
    fun createColon(): PsiElement {
        return createVariable("let x: Int64").findElementAt(5)!!
    }

    fun createExpressionCodeFragment(@NonNls text: String, context: PsiElement?): CjExpressionCodeFragment {
        return CjExpressionCodeFragment(project, "fragment.cj", text, null, context)
    }
    fun createSemicolon(): PsiElement {
        return createVariable("let x: Int64;").findElementAt(12)!!
    }


//    fun createIdentifier(text: String): PsiElement =
//        createFromText<CjModDeclItem>("mod ${text.escapeIdentifierIfNeeded()};")?.identifier
//            ?: error("Failed to create identifier: `$text`")

    fun createConstructorKeyword(): PsiElement =
        createClass("class A ").primaryConstructor!!.getInitKeyword()!!

    fun createNameIdentifier(@NonNls name: String) = createNameIdentifierIfPossible(name)!!
    fun createNameIdentifierIfPossible(@NonNls name: String) = createVariable(name, null, false).nameIdentifier
    fun createVariable(@NonNls name: String, @NonNls type: String?, isVar: Boolean): CjVariable {
        return createVariable(name, type, isVar, null)
    }
    fun createVariable(@NonNls name: String, @NonNls type: String?, isVar: Boolean, @NonNls initializer: String?): CjVariable {
        return createVariable(null, name, type, isVar, initializer)
    }
    fun creareDelegatedSuperTypeEntry(@NonNls text: String): CjConstructorDelegationCall {
        val colonOrEmpty = if (text.isEmpty()) "" else ": "
        return createClass("class A { constructor()$colonOrEmpty$text {}").secondaryConstructors.first().getDelegationCall()
    }
    fun createClass(@NonNls text: String): CjClass {
        return createDeclaration(text)
    }
    fun createVariable(
        @NonNls modifiers: String?,
        @NonNls name: String,
        @NonNls type: String?,
        isVar: Boolean,
        @NonNls initializer: String?
    ): CjVariable {
        val text = modifiers.let { "$it " } +
                (if (isVar) " var " else " let ") + name +
                (if (type != null) ":$type" else "") + (if (initializer == null) "" else " = $initializer")
        return createVariable(text)
    }

    fun createPackageDirectiveIfNeeded(fqName: FqName): CjPackageDirective? {
        return if (fqName.isRoot) null else createPackageDirective(fqName)
    }
    fun createPackageDirective(fqName: FqName): CjPackageDirective {
        return createFile("package ${fqName.asString()}").packageDirective!!
    }
    @JvmOverloads
    constructor(project: Project, markGenerated: Boolean = true) :
            this(project, markGenerated, context = null, eventSystemEnabled = false)

    constructor(project: Project, markGenerated: Boolean = true, eventSystemEnabled: Boolean) :
            this(project, markGenerated, context = null, eventSystemEnabled = eventSystemEnabled)

    private fun doCreateExpression(@NonNls text: String):CjExpression? {
        //注意：下面的‘\n’很重要--如果没有它，会出现一些奇怪的代码缩进问题
        return createVariable("let x =\n$text").initializer
    }
    fun createVariable(@NonNls text: String): CjVariable {
        return createDeclaration(text)
    }

    fun <TDeclaration : CjDeclaration> createDeclaration(@NonNls text: String): TDeclaration {
        val file = createFile(text)
        val declarations = file.declarations

        checkWithAttachment(declarations.size == 1, { "unexpected ${declarations.size} declarations" }) {
            it.withAttachment("text.cj", text)
            for (d in declarations.withIndex()) {
                it.withPsiAttachment("declaration${d.index}.cj", d.value)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return declarations.first() as TDeclaration
    }
    private fun doCreateFile(@NonNls fileName: String, @NonNls text: String): CjFile {
        return PsiFileFactory.getInstance(project).createFileFromText(
            fileName,
            CangJieFileType,
            text,
            LocalTimeCounter.currentTime(),
            eventSystemEnabled,
            markGenerated
        ) as CjFile
    }

    fun createFile(@NonNls fileName: String, @NonNls text: String): CjFile {
        val file = doCreateFile(fileName, text)

        val analysisContext = this@CjPsiFactory.context
        if (analysisContext != null) {
            file.analysisContext = analysisContext
        } else {
            file.doNotAnalyze = DO_NOT_ANALYZE_NOTIFICATION
        }

        return file
    }
    fun createFile(@NonNls text: String): CjFile {
        return createFile("dummy.cj", text)
    }
    fun createModifier(modifier: CjModifierKeywordToken): PsiElement {
        return createModifierList(modifier.value).getModifier(modifier)!!
    }
    fun createExpressionIfPossible(@NonNls text: String): CjExpression? {
        val expression = try {
            doCreateExpression(text) ?: return null
        } catch (ignored: Throwable) {
            return null
        }
        return if (expression.text == text) expression else null
    }


    fun createModifierList(modifier: CjModifierKeywordToken): CjModifierList {
        return createModifierList(modifier.value)
    }
    fun createModifierList(@NonNls text: String): CjModifierList {
        return createClass("$text interface x").modifierList!!
    }



    fun createComma(): PsiElement {
        return createType("T<X, Y>").findElementAt(3)!!
    }
    fun createTypeIfPossible(@NonNls type: String): CjTypeReference? {
        val typeReference = createVariable("val x : $type").typeReference
        return if (typeReference?.text == type) typeReference else null
    }
    fun createType(@NonNls type: String): CjTypeReference {
        val typeReference = createTypeIfPossible(type)
        if (typeReference == null || typeReference.text != type) {
            throw IllegalArgumentException("Incorrect type: $type")
        }
        return typeReference
    }

    fun createExpression(@NonNls text: String): CjExpression {
        val expression = doCreateExpression(text) ?: error("Failed to create expression from text: '$text'")
        assert(expression.text == text) {
            "Failed to create expression from text: '$text', resulting expression's text was: '${expression.text}'"
        }
        return expression
    }

    fun createWhiteSpace(@NonNls text: String): PsiElement {
        return createVariable("let${text}x: Int64").findElementAt(3)!!
    }
    fun createWhiteSpace(): PsiElement {
        return createWhiteSpace(" ")
    }

    fun createNewLine(): PsiElement {
        return createWhiteSpace("\n ")
    }
}
