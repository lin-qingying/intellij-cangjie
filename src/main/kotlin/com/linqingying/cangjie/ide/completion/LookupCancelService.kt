package com.linqingying.cangjie.ide.completion

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.ThreadingAssertions

@Service(Service.Level.PROJECT)
class LookupCancelService {
    private var lastReminiscence: Reminiscence? = null
    internal class Reminiscence(editor: Editor, offset: Int) {
        var editor: Editor? = editor
        private var marker: RangeMarker? = editor.document.createRangeMarker(offset, offset)

        // forget about auto-popup cancellation when the caret is moved to the start or before it
        private var editorListener: CaretListener? = object : CaretListener {
            override fun caretPositionChanged(e: CaretEvent) {
                if (marker != null && (!marker!!.isValid || editor.logicalPositionToOffset(e.newPosition) <= offset)) {
                    dispose()
                }
            }
        }

        init {
            ThreadingAssertions.assertEventDispatchThread()
            editor.caretModel.addCaretListener(editorListener!!)
        }

        fun matches(editor: Editor, offset: Int): Boolean {
            return editor == this.editor && marker?.startOffset == offset
        }

        fun dispose() {
            ThreadingAssertions.assertEventDispatchThread()
            if (marker != null) {
                editor!!.caretModel.removeCaretListener(editorListener!!)
                marker = null
                editor = null
                editorListener = null
            }
        }
    }

    fun wasAutoPopupRecentlyCancelled(editor: Editor, offset: Int): Boolean {
        return lastReminiscence?.matches(editor, offset) ?: false
    }
    companion object {
        fun getInstance(project: Project): LookupCancelService = project.service()

        fun getServiceIfCreated(project: Project): LookupCancelService? = project.getServiceIfCreated(LookupCancelService::class.java)

        val AUTO_POPUP_AT = Key<Int>("LookupCancelService.AUTO_POPUP_AT")
    }

}
