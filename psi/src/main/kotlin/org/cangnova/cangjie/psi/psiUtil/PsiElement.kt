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

package org.cangnova.cangjie.psi.psiUtil

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.parentOfType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*

// ========================================
// 字符串模板相关工具函数
// ========================================

/**
 * 移除字符串模板中的花括号
 *
 * ## 功能说明
 * 将块字符串模板（如 `${name}`）转换为简单名称模板（如 `$name`）
 *
 * ## 适用场景
 * - 仅支持简单的名称引用（如 `${foo}` → `$foo`）
 * - 支持 this 表达式（如 `${this}` → `$this`）
 *
 * @return 替换后的简单名称字符串模板
 */
fun CjBlockStringTemplateEntry.dropCurlyBrackets(): CjSimpleNameStringTemplateEntry {
    val name = when (expression) {
        is CjThisExpression -> CjTokens.THIS_KEYWORD.value
        else -> (expression as CjNameReferenceExpression).referencedNameElement.text
    }

    val newEntry = CjPsiFactory(project).createSimpleNameStringTemplateEntry(name)
    return replaced(newEntry)
}

/**
 * 检查是否可以移除花括号
 *
 * ## 判断条件
 * 1. 表达式必须是简单名称引用或无标签的 this 表达式
 * 2. 后续内容不能导致歧义（通过 canPlaceAfterSimpleNameEntry 检查）
 *
 * ## 示例
 * - `${name}123` → 可以移除花括号变为 `$name123`
 * - `${name}.field` → 不能移除，因为 `.` 会导致歧义
 */
fun CjBlockStringTemplateEntry.canDropCurlyBrackets(): Boolean {
    val expression = this.expression
    return (expression is CjNameReferenceExpression || (expression is CjThisExpression && expression.labelQualifier == null)) &&
        canPlaceAfterSimpleNameEntry(nextSibling)
}

// ========================================
// PSI 模式匹配工具
// ========================================

/**
 * 创建 PSI 元素模式匹配器
 *
 * ## 使用场景
 * - 代码补全中匹配特定类型的 PSI 元素
 * - 引用贡献器中判断光标位置
 *
 * ## 示例
 * ```kotlin
 * psiElement<CjNameReferenceExpression>()
 *     .withParent(psiElement<CjCallExpression>())
 * ```
 */
inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
    return PlatformPatterns.psiElement(I::class.java)
}

/**
 * 检查元素是否在注解参数列表中
 *
 * 用于判断当前代码位置是否在注解的参数列表内，例如：`@MyAnnotation(param = value)`
 */
fun PsiElement.isInsideAnnotationEntryArgumentList(): Boolean =
    parentOfType<CjValueArgumentList>()?.parent is CjAnnotation

// ========================================
// 祖先和后代查找
// ========================================

/**
 * 检查当前元素是否是另一个元素的祖先
 *
 * @param child 可能的子元素
 * @return 如果 child 是当前元素的后代，返回 true
 */
fun PsiElement.isAncestorOf(child: PsiElement): Boolean =
    child.ancestors.contains(this)

/**
 * 获取元素的所有祖先序列
 *
 * ## 特性
 * - 包含当前元素本身
 * - 不包含文件本身（PsiFile 是终点）
 * - 懒加载序列，按需计算
 *
 * ## 使用示例
 * ```kotlin
 * element.ancestors.filterIsInstance<CjFunction>().firstOrNull()
 * ```
 */
val PsiElement.ancestors: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile) null else it.parent
    }

/**
 * 获取两种类型之一的父元素
 *
 * 查找第一个匹配 T 或 V 类型的父元素
 */
inline fun <reified T : PsiElement, reified V : PsiElement> PsiElement.getParentOfTypes2(): PsiElement? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, V::class.java)
}

/**
 * 收集所有指定类型的后代元素
 *
 * @param predicate 过滤条件，默认接受所有元素
 * @return 符合条件的后代元素列表
 */
inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(noinline predicate: (T) -> Boolean = { true }): List<T> {
    return collectDescendantsOfType({ true }, predicate)
}

/**
 * 获取指定类型的祖先（不包含自己）
 *
 * "Strict" 表示查找时不包含当前元素本身
 */
inline fun <reified T : PsiElement> PsiElement.ancestorStrict(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, /* strict */ true)

/**
 * 收集指定类型的后代元素（可控制遍历范围）
 *
 * @param canGoInside 控制是否进入某个元素内部继续查找
 * @param predicate 过滤条件
 * @return 符合条件的后代元素列表
 */
inline fun <reified T : PsiElement> PsiElement.collectDescendantsOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true },
): List<T> = collectDescendantsOfTypeTo(ArrayList(), canGoInside, predicate)

/**
 * 收集后代元素到指定集合
 *
 * ## 性能优化
 * - 可以传入预分配的集合，避免重复创建
 * - 支持控制遍历深度
 *
 * @param to 目标集合
 * @param canGoInside 控制遍历范围
 * @param predicate 过滤条件
 * @return 传入的集合（已填充元素）
 */
inline fun <reified T : PsiElement, C : MutableCollection<T>> PsiElement.collectDescendantsOfTypeTo(
    to: C,
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true },
): C {
    forEachDescendantOfType<T>(canGoInside) {
        if (predicate(it)) {
            to.add(it)
        }
    }
    return to
}

// ========================================
// PSI 元素替换
// ========================================

/**
 * 替换当前 PSI 元素
 *
 * ## 智能处理
 * - 如果替换结果被自动包裹在括号表达式中，自动解包
 * - 如果当前元素和新元素相同，直接返回新元素（避免无效替换）
 *
 * ## 使用场景
 * - 代码重构时替换表达式
 * - 意图操作（Intention Actions）修改代码
 *
 * @param newElement 用于替换的新元素
 * @return 替换后的元素（可能与 newElement 不同，因为 PSI 可能会修改）
 */
inline fun <reified T : PsiElement> PsiElement.replaced(newElement: T): T {
    if (this == newElement) {
        return newElement
    }

    return when (val result = replace(newElement)) {
        is T -> result
        else -> (result as CjParenthesizedExpression).expression as T
    }
}

// ========================================
// 父元素查找
// ========================================

/**
 * 获取指定类型的父元素（可设置停止点）
 *
 * @param strict 是否严格模式（不包含自己）
 * @param stopAt 停止搜索的元素类型列表
 * @return 找到的父元素，如果没有则返回 null
 */
inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean, vararg stopAt: Class<out PsiElement>): T? {
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict, *stopAt)
}

/**
 * 获取指定类型的父元素
 *
 * @param strict 是否严格模式（true = 不包含自己，false = 包含自己）
 */
inline fun <reified T : PsiElement> PsiElement.getParentOfType(strict: Boolean): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, strict)
}

/**
 * 根据字符串类名查找父元素
 *
 * ## 使用场景
 * - 动态查找父元素（类名从配置读取）
 * - 跨模块查找（避免硬编码类依赖）
 *
 * @param psiClassNames 可接受的类名列表（包括父类和接口）
 */
fun PsiElement.parentOfType(vararg psiClassNames: String): PsiElement? {
    fun acceptsClass(javaClass: Class<*>): Boolean {
        if (javaClass.simpleName in psiClassNames) return true
        javaClass.superclass?.let { if (acceptsClass(it)) return true }
        for (superInterface in javaClass.interfaces) {
            if (acceptsClass(superInterface)) return true
        }
        return false
    }
    return generateSequence(this) { it.parent }
        .filter { it !is PsiFile }
        .firstOrNull { acceptsClass(it::class.java) }
}

/**
 * 获取指定类型的父元素（包含自己）
 *
 * @param withSelf 是否包含当前元素本身，默认为 false
 */
inline fun <reified T : PsiElement> PsiElement.parentOfType(withSelf: Boolean = false): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, !withSelf)
}

/**
 * 获取指定类型的父元素（严格模式，不包含自己）
 */
inline fun <reified T : PsiElement> PsiElement.getStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, true)
}

// ========================================
// 标识符和元素类型
// ========================================

/**
 * 获取元素的标识符
 *
 * 仅对简单名称表达式有效，其他元素返回 null
 */
inline val PsiElement.identifier
    get() = when (this) {
        is CjSimpleNameExpression -> this.identifier

        else -> null
    }

/**
 * 提取节点的元素类型
 *
 * ## 注意
 * - 必须保证元素类型不为 null，否则会抛出异常
 * - 使用前确认元素已完全解析
 */
val PsiElement.elementType: IElementType
    get() = elementTypeOrNull!!

/**
 * 安全获取元素类型（可能为 null）
 *
 * ## 重要提示
 * - 小心不要触发 AST 的完整解析（性能敏感）
 * - 使用 PsiUtilCore.getElementType 避免不必要的解析
 */
val PsiElement.elementTypeOrNull: IElementType?
    // XXX: be careful not to switch to AST
    get() = PsiUtilCore.getElementType(this)

// ========================================
// 兄弟元素导航
// ========================================

/**
 * 获取同类型的前一个兄弟元素
 */
inline fun <reified T : PsiElement> T.prevSiblingOfSameType() = PsiTreeUtil.getPrevSiblingOfType(this, T::class.java)

/**
 * 获取同类型的下一个兄弟元素
 */
inline fun <reified T : PsiElement> T.nextSiblingOfSameType() = PsiTreeUtil.getNextSiblingOfType(this, T::class.java)

/**
 * 获取所有兄弟元素序列
 *
 * @param forward 是否向前遍历（true = 下一个兄弟，false = 上一个兄弟）
 * @param withItself 是否包含当前元素本身
 * @return 兄弟元素的懒加载序列
 */
fun PsiElement.siblings(forward: Boolean = true, withItself: Boolean = true): Sequence<PsiElement> {
    return object : Sequence<PsiElement> {
        override fun iterator(): Iterator<PsiElement> {
            var next: PsiElement? = this@siblings
            return object : Iterator<PsiElement> {
                init {
                    if (!withItself) next()
                }

                override fun hasNext(): Boolean = next != null
                override fun next(): PsiElement {
                    val result = next ?: throw NoSuchElementException()
                    next = if (forward) result.nextSibling else result.prevSibling
                    return result
                }
            }
        }
    }
}

/**
 * 获取下一个兄弟元素（忽略空格）
 *
 * @param withItself 是否包含当前元素
 */
fun PsiElement.getNextSiblingIgnoringWhitespace(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace }.firstOrNull()
}

/**
 * 获取下一个兄弟元素（忽略空格和注释）
 *
 * @param withItself 是否包含当前元素
 */
fun PsiElement.getNextSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself).filter { it !is PsiWhiteSpace && it !is PsiComment }.firstOrNull()
}

/**
 * 获取前一个兄弟元素（忽略空格和注释）
 *
 * @param withItself 是否包含当前元素
 */
fun PsiElement.getPrevSiblingIgnoringWhitespaceAndComments(withItself: Boolean = false): PsiElement? {
    return siblings(withItself = withItself, forward = false).filter { it !is PsiWhiteSpace && it !is PsiComment }
        .firstOrNull()
}

// ========================================
// 叶子节点导航
// ========================================

/**
 * 获取下一个叶子节点
 *
 * @param skipEmptyElements 是否跳过空白元素
 */
fun PsiElement.nextLeaf(skipEmptyElements: Boolean = false): PsiElement? = PsiTreeUtil.nextLeaf(this, skipEmptyElements)

/**
 * 获取前一个叶子节点
 *
 * @param skipEmptyElements 是否跳过空白元素
 */
fun PsiElement.prevLeaf(skipEmptyElements: Boolean = false): PsiElement? = PsiTreeUtil.prevLeaf(this, skipEmptyElements)

/**
 * 获取下一个符合条件的叶子节点
 *
 * @param filter 过滤条件
 */
fun PsiElement.nextLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = nextLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.nextLeaf()
    }
    return leaf
}

/**
 * 获取前一个符合条件的叶子节点
 *
 * @param filter 过滤条件
 */
fun PsiElement.prevLeaf(filter: (PsiElement) -> Boolean): PsiElement? {
    var leaf = prevLeaf()
    while (leaf != null && !filter(leaf)) {
        leaf = leaf.prevLeaf()
    }
    return leaf
}

// ========================================
// 表达式相关工具
// ========================================

/**
 * 获取带类型的二元表达式父节点
 *
 * ## 使用场景
 * - 检查表达式是否用于类型检查（如 `is` 或 `as` 表达式）
 * - 分析类型转换上下文
 *
 * ## 处理逻辑
 * 1. 检查父节点是否是函数调用
 * 2. 如果是限定表达式的选择器，进一步检查
 * 3. 跳过所有括号表达式
 * 4. 返回最终的二元类型表达式
 */
fun CjExpression.getBinaryWithTypeParent(): CjBinaryExpressionWithTypeRHS? {
    val callExpression = parent as? CjCallExpression ?: return null
    val possibleQualifiedExpression = callExpression.parent

    val targetExpression = if (possibleQualifiedExpression is CjQualifiedExpression) {
        if (possibleQualifiedExpression.selectorExpression != callExpression) return null
        possibleQualifiedExpression
    } else {
        callExpression
    }

    return targetExpression.topParenthesizedParentOrMe().parent as? CjBinaryExpressionWithTypeRHS
}

/**
 * 获取最外层的括号表达式（或自己）
 *
 * ## 功能
 * - 如果表达式被多层括号包裹，返回最外层的括号表达式
 * - 如果没有括号，返回表达式本身
 *
 * ## 示例
 * - `((a + b))` → 返回外层的 `((a + b))`
 * - `a + b` → 返回 `a + b`
 */
fun CjExpression.topParenthesizedParentOrMe(): CjExpression {
    var result: CjExpression = this
    while (CjPsiUtil.deparenthesizeOnce(result.parent as? CjExpression) == result) {
        result = result.parent as? CjExpression ?: break
    }
    return result
}

/**
 * 解包括号、标签和注解
 *
 * ## 处理的元素类型
 * - 括号表达式：`(expr)` → `expr`
 * - 标签表达式（已注释）：`label@ expr` → `expr`
 * - 注解表达式（已注释）：`@Anno expr` → `expr`
 *
 * @return 解包后的核心表达式
 */
fun PsiElement?.unwrapParenthesesLabelsAndAnnotations(): PsiElement? {
    var unwrapped = this
    while (true) {
        unwrapped = when (unwrapped) {
            is CjParenthesizedExpression -> unwrapped.expression
//            is CjLabeledExpression -> unwrapped.baseExpression
//            is CjAnnotatedExpression -> unwrapped.baseExpression
            else -> return unwrapped
        }
    }
}

// ========================================
// 后代查找
// ========================================

/**
 * 查找第一个符合条件的后代元素
 *
 * @param predicate 过滤条件
 */
inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(noinline predicate: (T) -> Boolean = { true }): T? {
    return findDescendantOfType({ true }, predicate)
}

/**
 * 查找第一个符合条件的后代元素（可控制遍历范围）
 *
 * @param canGoInside 控制是否进入某个元素内部继续查找
 * @param predicate 过滤条件
 */
inline fun <reified T : PsiElement> PsiElement.findDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true },
): T? {
    checkDecompiledText()
    var result: T? = null
    this.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is T && predicate(element)) {
                result = element
                stopWalking()
                return
            }

            if (canGoInside(element)) {
                super.visitElement(element)
            }
        }
    })
    return result
}

/**
 * 检查是否存在符合条件的后代元素
 *
 * @param canGoInside 控制遍历范围
 * @param predicate 过滤条件
 */
inline fun <reified T : PsiElement> PsiElement.anyDescendantOfType(
    crossinline canGoInside: (PsiElement) -> Boolean,
    noinline predicate: (T) -> Boolean = { true },
): Boolean {
    return findDescendantOfType(canGoInside, predicate) != null
}

/**
 * 检查反编译文本访问
 *
 * ## 为什么需要这个检查？
 * - 反编译是昂贵的操作，应该优先使用 Stub 索引
 * - 如果文件已编译且有 Stub，不应该加载反编译文本
 * - 抛出错误提醒开发者使用更高效的方式
 */
fun PsiElement.checkDecompiledText() {
    val file = containingFile
    if (file is CjFile && file.isCompiled && file.stub != null) {
        error("Attempt to load decompiled text, please use stubs instead. Decompile process might be slow and should be avoided")
    }
}

/**
 * 获取指定类型的祖先（包含自己）
 */
inline fun <reified T : PsiElement> PsiElement.ancestorOrSelf(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, /* strict */ false)

// ========================================
// 文本范围和偏移量
// ========================================

/**
 * 获取元素的结束偏移量
 */
val PsiElement.endOffset: Int
    get() = textRange.endOffset

/**
 * 获取元素的起始偏移量
 */
val PsiElement.startOffset: Int
    get() = textRange.startOffset

/**
 * 获取跳过注释后的起始偏移量
 *
 * ## 使用场景
 * - 计算代码的实际起始位置（不包含前置注释）
 * - 生成准确的代码范围
 */
val PsiElement.startOffsetSkippingComments: Int
    get() {
        if (!startsWithComment()) return startOffset // fastpath
        val firstNonCommentChild = generateSequence(firstChild) { it.nextSibling }
            .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
        return firstNonCommentChild?.startOffset ?: startOffset
    }

/**
 * 检查元素是否以注释开始
 */
fun PsiElement.startsWithComment(): Boolean = firstChild is PsiComment

/**
 * 获取不包含注释的文本范围
 */
val PsiElement.textRangeWithoutComments: TextRange
    get() = if (!startsWithComment()) textRange else TextRange(startOffsetSkippingComments, endOffset)

/**
 * 获取所有子元素的范围
 */
val PsiElement.allChildren: PsiChildRange
    get() {
        val first = firstChild
        return if (first != null) PsiChildRange(first, lastChild) else PsiChildRange.EMPTY
    }

// ========================================
// 子元素查找
// ========================================

/**
 * 获取所有指定类型的子元素
 */
inline fun <reified T : PsiElement> PsiElement.getChildrenOfType(): Array<T> {
    return PsiTreeUtil.getChildrenOfType(this, T::class.java) ?: arrayOf()
}

/**
 * 获取第一个指定类型的子元素
 */
inline fun <reified T : PsiElement> PsiElement.getChildOfType(): T? {
    return PsiTreeUtil.getChildOfType(this, T::class.java)
}

// ========================================
// 文件和上下文
// ========================================

/**
 * 将文档转换为 PSI 文件
 */
fun Document.toPsiFile(project: Project): PsiFile? =
    PsiDocumentManager.getInstance(project).getPsiFile(this)

/**
 * 获取包含当前元素的仓颉文件
 *
 * ## 错误处理
 * - 如果不在仓颉文件中，抛出详细的异常信息
 * - 异常信息包含文件内容、元素类型、节点信息，便于调试
 */

fun getContainingCjFile(psi:LazyParseablePsiElement): CjFile{
    val file = psi.containingFile

    if (file is CjFile) return file

    val fileString = if (file != null && file.isValid) file.text else ""
    throw IllegalStateException("CjElement not inside CjFile: $file with text \"$fileString\" for element $psi of type ${psi::class.java} node = ${psi.node}")

}

/**
 * 获取元素的文本及其上下文
 *
 * 用于生成包含上下文信息的元素文本，便于错误报告和调试
 */
fun PsiElement.getElementTextWithContext(): String = getElementTextWithContext(this)

// ========================================
// 工具函数
// ========================================

/**
 * 确保值非空，否则抛出断言错误
 *
 * @param message 错误消息生成器
 */
inline fun <T : Any> T?.sure(message: () -> String): T = this ?: throw AssertionError(message())

/**
 * 搜索范围包含元素检查
 */
operator fun SearchScope.contains(element: PsiElement): Boolean = PsiSearchScopeUtil.isInScope(this, element)

// ========================================
// AST 节点工具函数
// ========================================

/**
 * 获取 AST 节点的所有子节点序列
 */
fun ASTNode.children() = generateSequence(firstChildNode) { node -> node.treeNext }
/**
 * 获取 AST 节点的兄弟节点序列
 *
 * @param forward 是否向前遍历
 */
fun ASTNode.siblings(forward: Boolean = true): Sequence<ASTNode> {
    if (forward) {
        return generateSequence(treeNext) { it.treeNext }
    } else {
        return generateSequence(treePrev) { it.treePrev }
    }
}

/**
 * 获取 AST 节点的所有父节点序列
 */
fun ASTNode.parents() = generateSequence(treeParent) { node -> node.treeParent }

/**
 * 获取 AST 节点的所有叶子节点序列
 *
 * @param forward 是否向前遍历
 */
fun ASTNode.leaves(forward: Boolean = true): Sequence<ASTNode> {
    if (forward) {
        return generateSequence(TreeUtil.nextLeaf(this)) { TreeUtil.nextLeaf(it) }
    } else {
        return generateSequence(TreeUtil.prevLeaf(this)) { TreeUtil.prevLeaf(it) }
    }
}

// ========================================
// 表达式和语句相关
// ========================================

/**
 * 检查表达式是否是空函数体
 */
fun CjExpression.isEmptyBody(): Boolean = this is CjBlockExpression && statements.isEmpty()

/**
 * 获取 return 语句的目标声明
 *
 * 返回包含该 return 语句的函数或 lambda 表达式
 * - 可能是普通函数（CjFunction）
 * - 可能是 lambda 表达式（CjFunctionLiteral）
 */
val CjReturnExpression.returnTarget: CjDeclaration?
    get() {
        return parentOfType<CjFunctionLiteral>() ?: parentOfType<CjFunction>()
    }

/**
 * 获取代码块的 return 目标
 */
val CjBlockExpression.returnTarget get() = parentOfType<CjFunctionLiteral>() ?: parentOfType<CjFunction>()

/**
 * 查找可以被求值的表达式
 *
 * ## 功能说明
 * 从给定的 PSI 元素开始，向上遍历 PSI 树，查找第一个可以被求值的表达式。
 *
 * ## 支持的表达式类型
 * - 简单变量引用（如 `x`）
 * - 引用表达式/属性访问（如 `obj.field`）
 * - 数组访问（如 `arr[0]`）
 * - 二元表达式（如 `a + b`）
 * - 常量表达式（如 `123`, `"hello"`）
 * - 函数调用（如 `func()`）- 仅在 sideEffectsAllowed 为 true 时
 *
 * ## 使用场景
 * - 调试器中的表达式求值
 * - 快速求值（Quick Evaluate）功能
 * - 监视器（Watch）表达式
 *
 * @param sideEffectsAllowed 是否允许有副作用的表达式（如函数调用）
 * @return 可求值的表达式，如果没有则返回 null
 */
fun PsiElement.findEvaluatableExpression(

    sideEffectsAllowed: Boolean
): PsiElement? {
    var current: PsiElement? = this

    while (current != null) {
        // 检查是否是可以求值的表达式类型
        when (current) {
            // 简单变量名 - 总是允许
            is CjSimpleNameExpression -> return current

            // 引用表达式（属性访问）- 总是允许
            is CjReferenceExpression -> {
                // 排除函数调用（函数调用也是引用表达式）
                if (current !is CjCallExpression) {
                    return current
                }
            }

            // 函数调用 - 仅在允许副作用时
            is CjCallExpression -> {
                if (sideEffectsAllowed) {
                    return current
                }
            }

            // 数组访问 - 总是允许
            is CjArrayAccessExpression -> return current

            // 二元表达式（加减乘除等）- 总是允许
            is CjBinaryExpression -> return current

            // 常量表达式（字面量）- 总是允许
            is CjConstantExpression -> return current

            // 如果遇到语句级别的元素，停止向上查找
            is CjBlockExpression,
            is CjDeclaration -> return null
        }

        current = current.parent
    }

    return null
}