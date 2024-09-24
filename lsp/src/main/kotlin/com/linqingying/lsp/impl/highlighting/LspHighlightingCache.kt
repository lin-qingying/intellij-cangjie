package com.linqingying.lsp.impl.highlighting

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.MultiMap

abstract class LspHighlightingCache {
    private val fileToPendingEdits: MultiMap<VirtualFile, PendingEdit> = MultiMap()

    protected fun addPendingEdit(
        file: VirtualFile,
        e: com.intellij.openapi.editor.event.DocumentEvent
    ) {
        fileToPendingEdits.putValue(file, PendingEdit(e.offset, e.oldLength, e.newLength))

    }

    private fun <T : LspTextRangeOwner> applyPendingEdit(
        textRangeOwners: MutableList<T>,
        edit: PendingEdit,
        deleteTouched: Boolean,
        createNewTextRangeOwner: (T, TextRange) -> T
    ) {
        val listIterator = textRangeOwners.listIterator()

        while (listIterator.hasNext()) {
            val owner = listIterator.next()
            val textRange = owner.textRange
            val offsetEnd = edit.offset + edit.oldLength

            when {
                edit.offset >= textRange.endOffset -> continue

                offsetEnd <= textRange.startOffset -> {
                    val newRange = textRange.shiftRight(edit.newLength - edit.oldLength)
                    listIterator.set(createNewTextRangeOwner(owner, newRange))
                }

                deleteTouched -> {
                    listIterator.remove()
                }

                edit.offset >= textRange.startOffset && offsetEnd <= textRange.endOffset -> {
                    val newRange = textRange.grown(edit.newLength - edit.oldLength)

                    listIterator.set(createNewTextRangeOwner(owner, newRange))
                }

                else -> {
                    listIterator.remove()
                }
            }
        }
    }

    @org.jetbrains.annotations.Contract
    protected fun <T : LspTextRangeOwner> applyPendingEdits(
        file: VirtualFile,
        textRangeOwners: List<T>,
        deleteTouched: Boolean,
        createNewTextRangeOwner: (T, TextRange) -> T
    ): List<T> {

        val pendingEdits = fileToPendingEdits[file]

        if (pendingEdits.isEmpty()) {
            return textRangeOwners
        } else {
            val mutableOwners = textRangeOwners.toMutableList()
            for (pendingEdit in pendingEdits) {
                ProgressManager.checkCanceled()

                applyPendingEdit(mutableOwners, pendingEdit, deleteTouched, createNewTextRangeOwner)
            }
            return mutableOwners
        }
    }

    internal open fun clearCache() {
        fileToPendingEdits.clear()
    }

    protected fun removePendingEdits(file: VirtualFile) {
        fileToPendingEdits.remove(file)

    }


    private class PendingEdit(
        val offset: Int,
        val oldLength: Int,
        val newLength: Int
    )
}
