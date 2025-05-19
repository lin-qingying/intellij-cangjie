package cn.cangnova.cangjie.ide.refactoring.chooseContainer

import cn.cangnova.cangjie.lang.CangJieLanguage
import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.psi.psiUtil.allChildren
import cn.cangnova.cangjie.utils.collapseSpaces
import com.intellij.codeInsight.navigation.PsiTargetNavigator
import com.intellij.codeInsight.navigation.TargetPresentationProvider
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.codeInsight.unwrap.RangeSplitter
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.impl.file.PsiPackageBase
import com.intellij.psi.impl.light.LightElement
import java.util.*
import javax.swing.Icon

/**
 * Shows a container chooser popup if有多个可选容器，否则直接回调onSelect。
 * @param containers 可选的容器列表
 * @param editor 当前编辑器
 * @param title 弹窗标题
 * @param highlightSelection 是否高亮选中项
 * @param selection 默认选中项
 * @param toPsi 容器到PsiElement的映射
 * @param onSelect 用户选择后的回调
 */
fun <T> selectContainerIfNeeded(
    containers: List<T>,
    editor: Editor,
    @NlsContexts.PopupTitle title: String,
    highlightSelection: Boolean,
    selection: T? = null,
    toPsi: (T) -> PsiElement,
    onSelect: (T) -> Unit
): Unit =
    selectContainerIfNeededImpl(containers, editor, title, highlightSelection, selection, toPsi, onSelect)

/**
 * Shows a container chooser popup forPsiElement类型的容器。
 * @param containers 可选的PsiElement容器列表
 * @param editor 当前编辑器
 * @param title 弹窗标题
 * @param highlightSelection 是否高亮选中项
 * @param selection 默认选中项
 * @param onSelect 用户选择后的回调
 */
fun <T : PsiElement> selectContainerIfNeeded(
    containers: List<T>,
    editor: Editor,
    @NlsContexts.PopupTitle title: String,
    highlightSelection: Boolean,
    selection: T? = null,
    onSelect: (T) -> Unit
): Unit =
    selectContainerIfNeededImpl(containers, editor, title, highlightSelection, selection, null, onSelect)

private fun <T> selectContainerIfNeededImpl(
    containers: List<T>,
    editor: Editor,
    @NlsContexts.PopupTitle title: String,
    highlightSelection: Boolean,
    selection: T? = null,
    toPsi: ((T) -> PsiElement)?,
    onSelect: (T) -> Unit
) {
    when {
        containers.isEmpty() -> return
        containers.size == 1 -> onSelect(containers.first())
        toPsi != null -> showContainerChooser(containers, editor, title, highlightSelection, toPsi, onSelect)
        else -> {
            @Suppress("UNCHECKED_CAST")
            showContainerChooser(
                containers as List<PsiElement>,
                editor,
                title,
                highlightSelection,
                selection as PsiElement?,
                onSelect = onSelect as (PsiElement) -> Unit
            )
        }
    }
}

/**
 * 弹出容器选择器，支持自定义PsiElement映射。
 * @param containers 容器列表
 * @param editor 编辑器
 * @param title 弹窗标题
 * @param highlightSelection 是否高亮
 * @param toPsi 容器到PsiElement的映射
 * @param onSelect 选择回调
 */
private fun <T> showContainerChooser(
    containers: List<T>,
    editor: Editor,
    @NlsContexts.PopupTitle title: String,
    highlightSelection: Boolean,
    toPsi: (T) -> PsiElement,
    onSelect: (T) -> Unit
) {
    val psiElements = containers.map(toPsi)
    showPsiContainerChooser(
        elements = psiElements,
        editor = editor,
        title = title,
        highlightSelection = highlightSelection,
        psi2Container = { containers[psiElements.indexOf(it)] },
        onSelect = onSelect
    )
}

/**
 * 弹出PsiElement容器选择器。
 * @param elements PsiElement列表
 * @param editor 编辑器
 * @param title 弹窗标题
 * @param highlightSelection 是否高亮
 * @param selection 默认选中项
 * @param onSelect 选择回调
 */
private fun <T : PsiElement> showContainerChooser(
    elements: List<T>,
    editor: Editor,
    @NlsContexts.PopupTitle title: String,
    highlightSelection: Boolean,
    selection: T? = null,
    onSelect: (T) -> Unit
): Unit = showPsiContainerChooser(
    elements = elements,
    editor = editor,
    title = title,
    highlightSelection = highlightSelection,
    selection = selection,
    psi2Container = { it },
    onSelect = onSelect,
)

/**
 * 实际弹出PsiElement容器选择器的实现。
 * @param elements PsiElement列表
 * @param editor 编辑器
 * @param title 弹窗标题
 * @param highlightSelection 是否高亮
 * @param selection 默认选中项
 * @param psi2Container PsiElement到容器的映射
 * @param onSelect 选择回调
 */
private fun <T, E : PsiElement> showPsiContainerChooser(
    elements: List<E>,
    editor: Editor,
    @NlsContexts.PopupTitle title: String,
    highlightSelection: Boolean,
    selection: E? = null,
    psi2Container: (E) -> T,
    onSelect: (T) -> Unit,
) {
    invokeLater {
        val popup = createPsiElementPopup(
            editor,
            elements,
            containerPopupPresentationProvider(),
            title,
            highlightSelection,
            selection,
        ) { psiElement ->
            @Suppress("UNCHECKED_CAST")
            onSelect(psi2Container(psiElement as E))
            true
        }
        popup.showInBestPositionFor(editor)
    }
}

/**
 * 创建PsiElement选择弹窗。
 * @param editor 编辑器
 * @param elements PsiElement列表
 * @param presentationProvider 展示提供者
 * @param title 弹窗标题
 * @param highlightSelection 是否高亮
 * @param selection 默认选中项
 * @param processor 选择回调
 */
private fun <T : PsiElement> createPsiElementPopup(
    editor: Editor,
    elements: List<T>,
    presentationProvider: TargetPresentationProvider<T>,
    @NlsContexts.PopupTitle title: String?,
    highlightSelection: Boolean,
    selection: T? = null,
    processor: (T) -> Boolean
): JBPopup {
    val project = elements.firstOrNull()?.project
        ?: throw IllegalArgumentException("Can't create popup because no elements are provided")
    val highlighter = if (highlightSelection) SelectionAwareScopeHighlighter(editor) else null
    return PsiTargetNavigator(elements)
        .presentationProvider(presentationProvider)
        .selection(selection)
        .builderConsumer { builder ->
            builder
                .setItemSelectedCallback { presentation ->
                    highlighter?.dropHighlight()
                    val psiElement =
                        (presentation?.item as? SmartPsiElementPointer<*>)?.element ?: return@setItemSelectedCallback
                    highlighter?.highlight(psiElement)
                }
                .setItemChosenCallback {
                    @Suppress("UNCHECKED_CAST")
                    val element = (it.item as? SmartPsiElementPointer<T>)?.element ?: return@setItemChosenCallback
                    processor(element)
                }
                .addListener(object : JBPopupListener {
                    override fun onClosed(event: LightweightWindowEvent) {
                        highlighter?.dropHighlight()
                    }
                })
        }
        .createPopup(project, title)
}

/**
 * 提供容器弹窗的展示逻辑。
 */
fun containerPopupPresentationProvider(): TargetPresentationProvider<PsiElement> =
    object : PsiTargetPresentationRenderer<PsiElement>() {

        @NlsSafe
        private fun PsiElement.renderText(): String = when (this) {
            is SeparateFileWrapper -> CangJieBundle.message("refactoring.extract.to.separate.file.text")
            is PsiPackageBase -> qualifiedName
            is PsiFile -> name
            is CjTypeStatement -> {
                val list = mutableListOf<String>()
                modifierList?.let {
                    for (child in it.allChildren) {
                        if (child is CjAnnotationEntry || child is CjAnnotation || child is PsiWhiteSpace) continue
                        list.add(child.text)
                    }
                }
                declarationKeyword?.text?.let(list::add)
                name?.let(list::add)
                StringUtil.shortenTextWithEllipsis(list.joinToString(separator = " "), 53, 0)
            }

            is CjNamedFunction -> {
                val list = mutableListOf<String>()
                for (child in allChildren) {
                    if (child is PsiComment) continue
                    if (child is CjBlockExpression) break
                    list.add(child.text)
                }
                StringUtil.shortenTextWithEllipsis(list.joinToString(separator = "").trim(), 53, 0)
            }

            else -> {
                val text = text ?: "<invalid text>"
                StringUtil.shortenTextWithEllipsis(text.collapseSpaces(), 53, 0)
            }
        }

        private fun PsiElement.getRepresentativeElement(): PsiElement = when (this) {
            is CjBlockExpression -> (parent as? CjDeclarationWithBody) ?: this
            is CjAbstractClassBody -> parent as CjTypeStatement
            else -> this
        }

        override fun getElementText(element: PsiElement): String {
            val representativeElement = element.getRepresentativeElement()
            return representativeElement.renderText()
        }

        override fun getContainerText(element: PsiElement): String? = null

        override fun getIcon(element: PsiElement): Icon? = super.getIcon(element.getRepresentativeElement())
    }

/**
 * 高亮器层级常量。
 */
val HIGHLIGHTER_LEVEL = HighlighterLayer.SELECTION + 1

/**
 * 支持高亮选中范围的高亮器。
 * @property editor 编辑器
 */
private class SelectionAwareScopeHighlighter(val editor: Editor) {
    private val highlighters = ArrayList<RangeHighlighter>()

    private fun addHighlighter(r: TextRange, attr: TextAttributes) {
        highlighters.add(
            editor.markupModel.addRangeHighlighter(
                r.startOffset,
                r.endOffset,
                HIGHLIGHTER_LEVEL,
                attr,
                HighlighterTargetArea.EXACT_RANGE
            )
        )
    }

    fun highlight(wholeAffected: PsiElement) {
        dropHighlight()

        val affectedRange = wholeAffected.textRange ?: return

        val attributes =
            EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)!!
        val selectedRange = with(editor.selectionModel) { TextRange(selectionStart, selectionEnd) }
        val textLength = editor.document.textLength
        for (r in RangeSplitter.split(affectedRange, Collections.singletonList(selectedRange))) {
            if (r.endOffset <= textLength) addHighlighter(r, attributes)
        }
    }

    fun dropHighlight() {
        highlighters.forEach { it.dispose() }
        highlighters.clear()
    }
}

/**
 * 用于表示"提取到单独文件"的包装类。
 */
class SeparateFileWrapper(manager: PsiManager) : LightElement(manager, CangJieLanguage) {
    override fun toString(): String = ""
}