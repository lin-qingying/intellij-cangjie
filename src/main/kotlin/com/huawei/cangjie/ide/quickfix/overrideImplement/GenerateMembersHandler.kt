package com.huawei.cangjie.ide.quickfix.overrideImplement

import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.ide.ShortenReferences
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.caches.descriptor
import com.huawei.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.huawei.cangjie.resolve.descriptorUtil.findCallableMemberBySignature
import com.huawei.cangjie.resolve.source.getPsi
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import kotlin.math.min

abstract class GenerateMembersHandler(final override val toImplement: Boolean) :
    AbstractGenerateMembersHandler<OverrideMemberChooserObject>() {

    override fun collectMembersToGenerate(classOrObject: CjTypeStatement): Collection<OverrideMemberChooserObject> {
        val descriptor = classOrObject.resolveToDescriptorIfAny() ?: return emptySet()
        return collectMembersToGenerate(descriptor, classOrObject.project)
    }

    protected abstract fun collectMembersToGenerate(
        descriptor: ClassDescriptor,
        project: Project
    ): Collection<OverrideMemberChooserObject>

    override fun generateMembers(
        editor: Editor,
        classOrObject: CjTypeStatement,
        selectedElements: Collection<OverrideMemberChooserObject>,
        copyDoc: Boolean
    ) {
        return Companion.generateMembers(editor, classOrObject, selectedElements, copyDoc)
    }

    companion object {
        fun generateMembers(
            editor: Editor?,
            classOrObject: CjTypeStatement,
            selectedElements: Collection<OverrideMemberChooserObject>,
            copyDoc: Boolean
        ) {
            val selectedMemberDescriptors =
                selectedElements.associate { it.generateMember(classOrObject, copyDoc) to it.descriptor }

            val classBody = classOrObject.body
            if (classBody == null) {
                insertMembersAfterAndReformat(editor, classOrObject, selectedMemberDescriptors.keys)
                return
            }
            val offset = editor?.caretModel?.offset ?: classBody.startOffset
            val offsetCursorElement = PsiTreeUtil.findFirstParent(classBody.containingFile.findElementAt(offset)) {
                it.parent == classBody
            }
            if (offsetCursorElement != null && offsetCursorElement != classBody.rBrace && offsetCursorElement != classBody.lBrace) {
                insertMembersAfterAndReformat(editor, classOrObject, selectedMemberDescriptors.keys)
                return
            }
            val classLeftBrace = classBody.lBrace

            val allSuperMemberDescriptors = selectedMemberDescriptors.values
                .mapNotNull { it.containingDeclaration as? ClassDescriptor }
                .associateWith {
                    DescriptorUtils.getAllDescriptors(it.unsubstitutedMemberScope)
                        .filterIsInstance<CallableMemberDescriptor>()
                }

            val implementedElements = mutableMapOf<CallableMemberDescriptor, CjDeclaration>()

            fun ClassDescriptor.findElement(memberDescriptor: CallableMemberDescriptor): CjDeclaration? {
                return implementedElements[memberDescriptor]
                    ?: (findCallableMemberBySignature(memberDescriptor)?.source?.getPsi() as? CjDeclaration)
                        ?.takeIf { it != classOrObject && it !is CjParameter }
                        ?.also { implementedElements[memberDescriptor] = it }
            }

            fun getAnchor(selectedElement: CjDeclaration): PsiElement? {
                val lastElement = classOrObject.declarations.lastOrNull()
                val selectedMemberDescriptor = selectedMemberDescriptors[selectedElement] ?: return lastElement
                val superMemberDescriptors =
                    allSuperMemberDescriptors[selectedMemberDescriptor.containingDeclaration] ?: return lastElement
                val index = superMemberDescriptors.indexOf(selectedMemberDescriptor.original)
                if (index == -1) return lastElement
                val classDescriptor = classOrObject.descriptor as? ClassDescriptor ?: return lastElement

                val upperElement = ((index - 1) downTo 0).firstNotNullOfOrNull {
                    classDescriptor.findElement(superMemberDescriptors[it])
                }
                if (upperElement != null) return upperElement

                val lowerElement = ((index + 1) until superMemberDescriptors.size).firstNotNullOfOrNull {
                    classDescriptor.findElement(superMemberDescriptors[it])
                }
                if (lowerElement != null) return lowerElement.prevSiblingOfSameType() ?: classLeftBrace

                return lastElement
            }

            insertMembersAfterAndReformat(editor, classOrObject, selectedMemberDescriptors.keys) { getAnchor(it) }
        }
    }
}

fun <T : CjDeclaration> insertMembersAfterAndReformat(
    editor: Editor?,
    classOrObject: CjTypeStatement,
    members: Collection<T>,
    anchor: PsiElement? = null,
    getAnchor: (CjDeclaration) -> PsiElement? = { null },
): List<T> {
    val codeStyleManager = CodeStyleManager.getInstance(classOrObject.project)
    return runWriteAction {
        val insertedMembersElementPointers = insertMembersAfter(editor, classOrObject, members, anchor, getAnchor)
        val firstElement = insertedMembersElementPointers.firstOrNull() ?: return@runWriteAction emptyList()

        fun insertedMembersElements() = insertedMembersElementPointers.mapNotNull { it.element }

        ShortenReferences.DEFAULT.process(insertedMembersElements())
        if (editor != null) {
            firstElement.element?.let { moveCaretIntoGeneratedElement(editor, it) }
        }

        insertedMembersElementPointers.onEach { it.element?.let { element -> codeStyleManager.reformat(element) } }
        insertedMembersElements()
    }
}

fun moveCaretIntoGeneratedElement(editor: Editor, element: PsiElement) {
    val project = element.project
    val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element)

    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

    pointer.element?.let { moveCaretIntoGeneratedElementDocumentUnblocked(editor, it) }
}

private fun moveCaretIntoGeneratedElementDocumentUnblocked(editor: Editor, element: PsiElement): Boolean {
    // Inspired by GenerateMembersUtils.positionCaret()

    if (element is CjDeclarationWithBody && element.hasBody()) {
        val expression = element.bodyExpression
        if (expression is CjBlockExpression) {
            val lBrace = expression.lBrace
            val rBrace = expression.rBrace

            if (lBrace != null && rBrace != null) {
                val firstInBlock = lBrace.siblings(forward = true, withItself = false).first { it !is PsiWhiteSpace }
                val lastInBlock = rBrace.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }

                val start = firstInBlock.textRange!!.startOffset
                val end = lastInBlock.textRange!!.endOffset

                editor.moveCaret(min(start, end))

                if (start < end) {
                    editor.selectionModel.setSelection(start, end)
                }

                return true
            }
        }
    }

    if (element is CjDeclarationWithInitializer && element.hasInitializer()) {
        val expression = element.initializer ?: throw AssertionError()

        val initializerRange = expression.textRange

        val offset = initializerRange?.startOffset ?: element.getTextOffset()

        editor.moveCaret(offset)

        if (initializerRange != null) {
            val endOffset = expression.siblings(forward = true, withItself = false).lastOrNull()?.endOffset
                ?: initializerRange.endOffset
            editor.selectionModel.setSelection(initializerRange.startOffset, endOffset)
        }

        return true
    }

    if (element is CjProperty) {
        for (accessor in element.accessors) {
            if (moveCaretIntoGeneratedElementDocumentUnblocked(editor, accessor)) {
                return true
            }
        }
    }

    editor.moveCaret(element.endOffset)
    return false
}

fun <T : CjDeclaration> insertMembersAfter(
    editor: Editor?,
    classOrObject: CjTypeStatement,
    members: Collection<T>,
    anchor: PsiElement? = null,
    getAnchor: (CjDeclaration) -> PsiElement? = { null },
): List<SmartPsiElementPointer<T>> {
    members.ifEmpty { return emptyList() }
    val project = classOrObject.project
    val insertedMembers = SmartList<SmartPsiElementPointer<T>>()
    val (parameters, otherMembers) = members.partition { it is CjParameter }

    parameters.mapNotNullTo(insertedMembers) {
        if (classOrObject !is CjClass) return@mapNotNullTo null

        @Suppress("UNCHECKED_CAST")
        SmartPointerManager.createPointer(
            classOrObject.createPrimaryConstructorParameterListIfAbsent().addParameter(it as CjParameter) as T
        )
    }

    if (otherMembers.isNotEmpty()) {
        val psiFactory = CjPsiFactory(project)
        val tailComments = classOrObject.allChildren.toList()
            .takeLastWhile { it is PsiComment || it is PsiWhiteSpace }
            .map { commentOrSpace ->
                if (commentOrSpace is PsiWhiteSpace) {
                    psiFactory.createWhiteSpace(commentOrSpace.text)
                } else {
                    commentOrSpace.copy().also { commentOrSpace.delete() }
                }
            }
        val body = classOrObject.getOrCreateBody()
        val lBrace = body.lBrace
        if (lBrace != null) {
            tailComments.reversed().map { body.addAfter(it, lBrace) }
        }

        var afterAnchor = anchor ?: findInsertAfterAnchor(editor, body) ?: return emptyList()
        otherMembers.mapTo(insertedMembers) {
            afterAnchor = getAnchor(it) ?: afterAnchor

            if (classOrObject is CjClass && classOrObject.isEnum()) {
                val enumEntries = classOrObject.declarations.filterIsInstance<CjEnumEntry>()
                val bound = (enumEntries.lastOrNull() ?: classOrObject.allChildren.firstOrNull { element ->
                    element.node.elementType == CjTokens.SEMICOLON
                })
                if (it !is CjEnumEntry) {
                    if (bound != null && afterAnchor.startOffset <= bound.startOffset) {
                        afterAnchor = bound
                    }
                } else if (bound == null && body.declarations.isNotEmpty()) {
                    afterAnchor = body.lBrace!!
                } else if (bound != null && afterAnchor.startOffset > bound.startOffset) {
                    afterAnchor = bound.prevSibling!!
                }
            }

//            it.removeModifier(CjTokens.EXTERNAL_KEYWORD)

            @Suppress("UNCHECKED_CAST")
            SmartPointerManager.createPointer((body.addAfter(it, afterAnchor) as T).apply { afterAnchor = this })
        }
    }

    return insertedMembers
}

fun Editor.moveCaret(offset: Int, scrollType: ScrollType = ScrollType.RELATIVE) {
    caretModel.moveToOffset(offset)
    scrollingModel.scrollToCaret(scrollType)
}

private fun findInsertAfterAnchor(editor: Editor?, body: CjClassBody): PsiElement? {
    val lBrace = body.lBrace ?: return null

    val offset = editor?.caretModel?.offset ?: body.startOffset
    val offsetCursorElement = PsiTreeUtil.findFirstParent(body.containingFile.findElementAt(offset)) {
        it.parent == body
    }

    if (offsetCursorElement is PsiWhiteSpace) {
        return removeAfterOffset(offset, offsetCursorElement)
    }

    if (offsetCursorElement != null && offsetCursorElement != body.rBrace) {
        return offsetCursorElement
    }

    val comment = lBrace
        .siblings(withItself = false)
        .takeWhile { it is PsiWhiteSpace || it is PsiComment }
        .lastOrNull { it is PsiComment }

    return comment ?: lBrace
}

private fun removeAfterOffset(offset: Int, whiteSpace: PsiWhiteSpace): PsiElement {
    val spaceNode = whiteSpace.node
    if (spaceNode.textRange.contains(offset)) {
        var beforeWhiteSpaceText = spaceNode.text.substring(0, offset - spaceNode.startOffset)
        if (!StringUtil.containsLineBreak(beforeWhiteSpaceText)) {
            // Prevent insertion on same line
            beforeWhiteSpaceText += "\n"
        }

        val factory = CjPsiFactory(whiteSpace.project)

        val insertAfter = whiteSpace.prevSibling
        whiteSpace.delete()

        val beforeSpace = factory.createWhiteSpace(beforeWhiteSpaceText)
        insertAfter.parent.addAfter(beforeSpace, insertAfter)

        return insertAfter.nextSibling
    }

    return whiteSpace
}

fun CjTypeStatement.getOrCreateBody(): CjClassBody {
    getBody()?.let { return it }

    val newBody = CjPsiFactory(project).createEmptyClassBody()
//    if (this is CjEnumEntry) return addAfter(newBody, initializerList ?: nameIdentifier) as CjClassBody
    return add(newBody) as CjClassBody
}
