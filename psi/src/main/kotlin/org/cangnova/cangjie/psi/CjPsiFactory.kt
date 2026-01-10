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

package org.cangnova.cangjie.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.jetbrains.annotations.NonNls

/**
 * 标记文件不需要分析
 *
 * 用于标识由 CjPsiFactory 创建的临时文件，这些文件不应被分析。
 */
var CjFile.doNotAnalyze: String? by UserDataProperty(Key.create("DO_NOT_ANALYZE"))

/**
 * 元素上下文
 *
 * 存储 PSI 元素的上下文信息，用于在特定上下文中创建元素。
 */
var CjFile.elementContext: PsiElement? by UserDataProperty(Key.create("ELEMENT_CONTEXT"))

/**
 * 不分析通知消息
 */
private const val DO_NOT_ANALYZE_NOTIFICATION = "This file was created by CjPsiFactory and should not be analyzed\n" +
        "Use createAnalyzableFile to create file that can be analyzed\n"

/**
 * 仓颉 PSI 元素工厂
 *
 * 用于创建各种仓颉语言的 PSI 元素（程序结构接口元素）。
 * 这是一个核心工具类，提供了创建类、函数、表达式、类型等各种 PSI 元素的便捷方法。
 *
 * 主要功能：
 * - 创建类、接口、枚举等声明
 * - 创建函数、属性、参数等成员
 * - 创建表达式、语句、注解等元素
 * - 创建类型引用、导入语句等
 * - 创建注释、标识符等基本元素
 *
 * 使用场景：
 * - 代码重构：创建新的 PSI 元素替换旧元素
 * - 代码生成：生成模板代码
 * - 快速修复：创建修复代码的 PSI 元素
 * - 代码分析：创建临时 PSI 元素进行测试
 *
 * 示例：
 * ```kotlin
 * val factory = CjPsiFactory(project)
 *
 * // 创建简单表达式
 * val expr = factory.createExpression("1 + 2")
 *
 * // 创建函数
 * val func = factory.createFunction("func foo() { }")
 *
 * // 创建类
 * val clazz = factory.createClass("class MyClass { }")
 * ```
 *
 * 重要提示：
 * - 默认情况下，创建的文件会被标记为不分析（doNotAnalyze）
 * - 如需创建可分析的文件，使用 createAnalyzableFile
 * - 大多数方法接受文本参数，会解析文本创建对应的 PSI 元素
 *
 * @property project IntelliJ 项目实例
 * @property markGenerated 是否标记生成的元素（默认 true）
 * @property context 上下文元素，用于创建上下文相关的元素（可选）
 * @property eventSystemEnabled 是否启用事件系统（默认 false）
 */
class CjPsiFactory private constructor(
    private val project: Project,
    private val markGenerated: Boolean,
    private val context: PsiElement?,
    private val eventSystemEnabled: Boolean,
) {
    /**
     * 构造器：创建默认的 PSI 工厂
     *
     * @param project IntelliJ 项目
     * @param markGenerated 是否标记为生成的元素，默认 true
     */
    @JvmOverloads
    constructor(project: Project, markGenerated: Boolean = true) :
            this(project, markGenerated, context = null, eventSystemEnabled = false)

    /**
     * 构造器：创建支持事件系统的 PSI 工厂
     *
     * @param project IntelliJ 项目
     * @param markGenerated 是否标记为生成的元素，默认 true
     * @param eventSystemEnabled 是否启用事件系统
     */
    constructor(project: Project, markGenerated: Boolean = true, eventSystemEnabled: Boolean) :
            this(project, markGenerated, context = null, eventSystemEnabled = eventSystemEnabled)

    /**
     * 构造器：从现有元素创建 PSI 工厂
     *
     * @param element 上下文元素
     * @param markGenerated 是否标记为生成的元素，默认 true
     */
    @JvmOverloads
    constructor(element: CjElement, markGenerated: Boolean = true) : this(
        element.project,
        markGenerated,
        context = null,
        eventSystemEnabled = false,
    )

    /**
     * 创建空的类体
     *
     * 示例：
     * ```kotlin
     * val body = factory.createEmptyClassBody()
     * // 创建 class A{} 的类体部分
     * ```
     *
     * @return 空的抽象类体
     */
    fun createEmptyClassBody(): CjAbstractClassBody {
        return createClass("class A{}").body!!
    }

    /**
     * 创建空白符和箭头
     *
     * 用于函数类型中的箭头符号及其周围的空白符。
     *
     * @return 包含第一个和最后一个元素的 Pair（空白符和箭头）
     */
    fun createWhitespaceAndArrow(): Pair<PsiElement, PsiElement> {
        val functionType = createType("() -> Int").typeElement as CjFunctionType
        return Pair(functionType.findElementAt(2)!!, functionType.findElementAt(3)!!)
    }

    /**
     * 创建类型参数列表
     *
     * 示例：
     * ```kotlin
     * val typeArgs = factory.createTypeArguments("<Int, String>")
     * // 创建类型参数列表 <Int, String>
     * ```
     *
     * @param text 类型参数文本，包括尖括号
     * @return 类型参数列表
     */
    fun createTypeArguments(@NonNls text: String): CjTypeArgumentList {
        val property = createPatternVariable("let x = foo$text()")
        return (property.initializer as CjCallExpression).typeArgumentList!!
    }

    companion object {
        /**
         * 创建上下文相关的 PSI 工厂
         *
         * 创建一个与特定上下文关联的工厂，用于创建上下文相关的 PSI 元素。
         *
         * @param context 上下文元素
         * @param markGenerated 是否标记为生成的元素，默认 true
         * @return 上下文相关的 PSI 工厂实例
         */
        @JvmStatic
        @JvmOverloads
        fun contextual(context: PsiElement, markGenerated: Boolean = true): CjPsiFactory {
            return CjPsiFactory(context.project, markGenerated, context, eventSystemEnabled = false)
        }
    }

    /**
     * 将表达式包装在块表达式中
     *
     * 这是一个特殊的辅助方法，用于 ControlStructureTypingVisitor。
     * 如果表达式已经是块表达式，则直接返回；否则将其包装在块中。
     *
     * TODO: 未来应移除此方法
     *
     * @param expression 要包装的表达式
     * @return 块表达式
     */
    fun wrapInABlockWrapper(expression: CjExpression): CjBlockExpression {
        if (expression is CjBlockExpression) {
            return expression
        }
        val function = createFunction("func f() { ${expression.text} }")
        val block = function.bodyExpression as CjBlockExpression
        return BlockWrapper(block, expression)
    }

    /**
     * 创建属性声明
     *
     * @param name 属性名称
     * @param type 属性类型，可为 null
     * @param isMut 是否为可变属性
     * @param initializer 初始化表达式，可为 null
     * @return 属性声明
     */
    fun createProperty(
        @NonNls name: String,
        @NonNls type: String?,
        isMut: Boolean,
        @NonNls initializer: String?,
    ): CjProperty {
        return createProperty(null, name, type, isMut, initializer)
    }

    /**
     * 创建注解
     *
     * @param text 注解文本
     * @return 注解元素
     */
    fun createAnnotations(text: String): CjAnnotations {
        val function = createFunction(" $text func foo() { }")

        return function.annotations!!
    }

    /**
     * 创建 match 表达式的分支
     *
     * @param entryText 分支文本
     * @return match 分支元素
     */
    fun createMatchEntry(@NonNls entryText: String): CjMatchEntry {
        val function = createFunction("func foo() { match(12) { $entryText } }")
        val matchEntry = PsiTreeUtil.findChildOfType(function, CjMatchEntry::class.java)

        assert(matchEntry != null) { "Couldn't generate match constructor" }
        assert(entryText == matchEntry!!.text) { "Generate when constructor text differs from the given text" }

        return matchEntry
    }

    /**
     * 创建属性声明（不带初始化器）
     *
     * @param name 属性名称
     * @param type 属性类型，可为 null
     * @param isMut 是否为可变属性
     * @return 属性声明
     */
    fun createProperty(@NonNls name: String, @NonNls type: String?, isMut: Boolean): CjProperty {
        return createProperty(name, type, isMut, null)
    }

    /**
     * 创建枚举模式
     *
     * TODO: 待实现
     *
     * @param text 枚举模式文本
     * @return 枚举模式元素
     */
    fun createEnumPattern(text: String): CjEnumPattern {
        TODO()
    }

    /**
     * 创建属性声明（完整版本）
     *
     * @param modifiers 修饰符，可为 null
     * @param name 属性名称
     * @param type 属性类型，可为 null
     * @param isMut 是否为可变属性
     * @param initializer 初始化表达式，可为 null
     * @return 属性声明
     */
    fun createProperty(
        @NonNls modifiers: String?,
        @NonNls name: String,
        @NonNls type: String?,
        @NonNls isMut: Boolean,
        @NonNls initializer: String?,
    ): CjProperty {
        val text = modifiers.let { "$it " } +

                (if (isMut) " mut " else "") + "prop" + name +
                (if (type != null) ":$type" else "") +
                "{" +
                "get(){}" +
                if (isMut) {
                    "set(value){}"
                } else {
                    "" +
                            "}"
                }
        return createProperty(text)
    }

    fun createProperty(@NonNls text: String): CjProperty {
        return createClass(
            "class A { $text }",
        ).properties.first()
    }

    fun createSimpleName(@NonNls name: String): CjSimpleNameExpression {
        return createPatternVariable(name, null, false, name).initializer as CjSimpleNameExpression
    }

    private inline fun <reified E : CjExpression> createExpressionOfType(text: String): E =
        createExpression(text) as? E
            ?: error("Failed to create ${E::class.simpleName} from `$text`")

    fun createSimpleNameStringTemplateEntry(@NonNls name: String): CjSimpleNameStringTemplateEntry {
        val stringTemplateExpression = createExpression($$"\"$$$name\"") as CjStringTemplateExpression
        return stringTemplateExpression.entries[0] as CjSimpleNameStringTemplateEntry
    }

    fun createColon(): PsiElement {
        return createPatternVariable("let x: Int64").findElementAt(5)!!
    }

    fun createPrimaryConstructor(@NonNls text: String = ""): CjPrimaryConstructor {
        return createClass(if (text.isNotEmpty()) "class A { public A$text{} }" else "class A { public A(){} } ").primaryConstructor!!
    }

    private class BlockWrapper(fakeBlockExpression: CjBlockExpression, override val baseExpression: CjExpression) :
        CjBlockExpression(fakeBlockExpression.text), CjPsiUtil.CjExpressionWrapper {

        override val statements: List<CjExpression>
            get() = listOf(baseExpression)

        override fun getParent(): PsiElement = baseExpression.parent

        override fun getPsiOrParent(): CjElement = baseExpression.psiOrParent

        override fun getContainingCjFile() = baseExpression.getContainingCjFile()

        override fun getContainingFile(): PsiFile = baseExpression.containingFile
    }

    fun createCallArguments(@NonNls text: String): CjValueArgumentList {
        val property = createPatternVariable("let x = foo $text")
        return (property.initializer as CjCallExpression).valueArgumentList!!
    }

    fun createDot(): PsiElement {
        return createType("T.(X)").findElementAt(1)!!
    }

    fun createParameterList(@NonNls text: String): CjParameterList {
        return createFunction("func foo$text{}").valueParameterList!!
    }

    fun createFunctionTypeReceiver(typeReference: CjTypeReference): CjFunctionTypeReceiver {
        return (createType("() -> B").typeElement as CjFunctionType).receiver!!.apply {
            this.typeReference.replace(
                typeReference,
            )
        }
    }

    fun createImportDirective(importPath: ImportPath): CjImportDirective {
        if (importPath.fqName.isRoot) {
            throw IllegalArgumentException("import path must not be empty")
        }

        val file = createFile(buildString { appendImport(importPath) })
        return file.importDirectives.first()
    }

    private fun StringBuilder.appendImport(importPath: ImportPath) {
        if (importPath.fqName.isRoot) {
            throw IllegalArgumentException("import path must not be empty")
        }

        append("import ")
        append(importPath.pathStr)

        val alias = importPath.alias
        if (alias != null) {
            append(" as ").append(alias.asString())
        }
    }

    fun createBlockCodeFragment(@NonNls text: String, context: PsiElement?): CjBlockCodeFragment {
        return CjBlockCodeFragment(project, "fragment.cj", text, null, context)
    }

    fun createComment(@NonNls text: String): PsiComment {
        val file = createFile(text)
        val comments = file.children.filterIsInstance<PsiComment>()
        val comment = comments.single()
        assert(comment.text == text)
        return comment
    }

    fun createLambdaExpression(@NonNls parameters: String, @NonNls body: String): CjLambdaExpression =
        (
                if (parameters.isNotEmpty()) {
                    createExpression("{ $parameters -> $body }")
                } else {
                    createExpression("{ $body }")
                }
                ) as CjLambdaExpression

    fun createExpressionCodeFragment(@NonNls text: String, context: PsiElement?): CjExpressionCodeFragment {
        return CjExpressionCodeFragment(project, "fragment.cj", text, null, context)
    }

    fun createSemicolon(): PsiElement {
        return createPatternVariable("let x: Int64;").findElementAt(12)!!
    }

    fun createFunction(@NonNls funDecl: String): CjNamedFunction {
        return createDeclaration(funDecl)
    }

//    fun createIdentifier(text: String): PsiElement =
//        createFromText<CjModDeclItem>("mod ${text.escapeIdentifierIfNeeded()};")?.identifier
//            ?: error("Failed to create identifier: `$text`")

    fun createConstructorKeyword(): PsiElement =
        createClass("class A ").primaryConstructor!!.getInitKeyword()!!

    fun createNameIdentifier(@NonNls name: String) = createNameIdentifierIfPossible(name)!!
    fun createNameIdentifierIfPossible(@NonNls name: String) = createFieldVariable(name, null, false).nameIdentifier
    fun createPatternVariable(@NonNls name: String, @NonNls type: String?, isVar: Boolean): CjPatternVariable {
        return createPatternVariable(name, type, isVar, null)
    }

    fun createFieldVariable(@NonNls name: String, @NonNls type: String?, isVar: Boolean): CjFieldVariable {
        return createFieldVariable(name, type, isVar, null)
    }

    fun createFieldVariable(
        @NonNls name: String,
        @NonNls type: String?,
        isVar: Boolean,
        @NonNls initializer: String?,
    ): CjFieldVariable {
        return createFieldVariable(null, name, type, isVar, initializer)
    }

    fun createPatternVariable(
        @NonNls name: String,
        @NonNls type: String?,
        isVar: Boolean,
        @NonNls initializer: String?,
    ): CjPatternVariable {
        return createPatternVariable(null, name, type, isVar, initializer)
    }

    fun creareDelegatedSuperTypeEntry(@NonNls text: String): CjConstructorDelegationCall {
        return createClass("class A { init() { $text}").secondaryConstructors.first()
            .delegationCall!!
    }

    fun createStruct(@NonNls text: String): CjStruct {
        return createDeclaration(text)
    }

    fun createClass(@NonNls text: String): CjClass {
        return createDeclaration(text)
    }

    fun createTypeStatement(@NonNls text: String): CjTypeStatement {
        return createDeclaration(text)
    }

    fun createFieldVariable(
        @NonNls modifiers: String?,
        @NonNls name: String,
        @NonNls type: String?,
        isVar: Boolean,
        @NonNls initializer: String?,
    ): CjFieldVariable {

        val text = StringBuilder().apply {
            append("class abc")
            append("{")
            modifiers.let { append("$it ") }
            append((if (isVar) " var " else " let ") + name)
            append(if (type != null) ":$type" else "")
            if (initializer == null) {
                append(";")
            } else {
                append(" = $initializer;")
            }
            append("}")
        }
        return createFieldVariable(text.toString())
    }

    fun createPatternVariable(
        @NonNls modifiers: String?,
        @NonNls name: String,
        @NonNls type: String?,
        isVar: Boolean,
        @NonNls initializer: String?,
    ): CjPatternVariable {
        val text = modifiers.let { "$it " } +
                (if (isVar) " var " else " let ") + name +
                (if (type != null) ":$type" else "") + (if (initializer == null) "" else " = $initializer")
        return createPatternVariable(text)
    }

    fun createPackageDirectiveIfNeeded(fqName: FqName): CjPackageDirective? {
        return if (fqName.isRoot) null else createPackageDirective(fqName)
    }

    fun createPackageDirective(fqName: FqName): CjPackageDirective {
        return createFile("package ${fqName.asString()}").packageDirective!!
    }

    private fun doCreateExpression(@NonNls text: String): CjExpression? {
        // 注意：下面的‘\n’很重要--如果没有它，会出现一些奇怪的代码缩进问题
        return createPatternVariable("let x =\n$text").initializer
    }

    fun createFieldVariable(@NonNls text: String): CjFieldVariable {
        return createDeclaration(text)
    }

    fun createPatternVariable(@NonNls text: String): CjPatternVariable {
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
            CangJieFileType.INSTANCE,
            text,
            LocalTimeCounter.currentTime(),
            eventSystemEnabled,
            markGenerated,
        ) as CjFile
    }

    fun createFile(@NonNls fileName: String, @NonNls text: String): CjFile {
        val file = doCreateFile(fileName, text)

        val elementContext = this@CjPsiFactory.context
        if (elementContext != null) {
            file.elementContext = elementContext
        } else {
            file.doNotAnalyze = DO_NOT_ANALYZE_NOTIFICATION
        }

        return file
    }

    fun createFile(@NonNls text: String): CjFile {
        return createFile("dummy.cj", text)
    }

    fun createModifier(modifier: CjKeywordToken): PsiElement {
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

    fun createModifierList(modifier: CjKeywordToken): CjModifierList {
        return createModifierList(modifier.value)
    }

    fun createModifierList(modifier: CjModifierKeywordToken): CjModifierList {
        return createModifierList(modifier.value)
    }

    fun createModifierList(@NonNls text: String): CjModifierList {
        return createClass("$text class x{}").modifierList!!
    }

    fun createComma(): PsiElement {
        return createType("T<X, Y>").findElementAt(3)!!
    }

    fun createTypeIfPossible(@NonNls type: String): CjTypeReference? {
        val typeReference = createPatternVariable("let x : $type").typeReference
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
        return createPatternVariable("let${text}x: Int64").findElementAt(3)!!
    }

    fun createWhiteSpace(): PsiElement {
        return createWhiteSpace(" ")
    }

    fun createNewLine(lineBreaks: Int): PsiElement {
        return createWhiteSpace("\n".repeat(lineBreaks))
    }

    fun createIndent(): PsiElement {
        return createWhiteSpace("    ")
    }

    fun createNewLineAndIndent(length: Int = 4): PsiElement {
        return createWhiteSpace("\n" + " ".repeat(length))
    }

    fun createNewLine(): PsiElement {
        return createWhiteSpace("\n ")
    }

    fun createIdentifier(toString: String): PsiElement? {
        return createClass("class $toString").nameIdentifier
    }


}
