package com.linqingying.lsp.impl.navigation

import com.intellij.model.Pointer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.navigation.NavigationRequest
import com.intellij.platform.backend.navigation.NavigationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.linqingying.lsp.util.getOffsetInDocument
import com.linqingying.lsp.util.getRangeInDocument
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.Position
import kotlin.math.min

  class LspNavigationTarget(
    val project: Project,
    val targetFile: VirtualFile,
    val locationLink: LocationLink
) : NavigationTarget {

    override fun computePresentation(): TargetPresentation {
        val targetPresentationBuilder =
            TargetPresentation.builder(this.getShortenedText(this.targetFile, this.locationLink))
        val startPosition = this.locationLink.targetSelectionRange.start
        requireNotNull(startPosition) { "Start position cannot be null" }

        val targetPresentation = targetPresentationBuilder
            .locationText(this.formatFilePosition(this.targetFile, startPosition))
            .presentation()

        return targetPresentation
    }

    override fun createPointer(): Pointer<LspNavigationTarget> {
        return Pointer.hardPointer(this)
    }

    private fun formatFilePosition(
        virtualFile: VirtualFile,
        position: Position
    ): @NlsSafe String {
        return virtualFile.name + ":" + (position.line + 1) + ":" + (position.character + 1)


    }

    private fun getShortenedText(
        virtualFile: VirtualFile,
        locationLink: LocationLink
    ): @NlsSafe String {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return ""

        val range = locationLink.targetSelectionRange ?: return ""
        val textRange = getRangeInDocument(document, range) ?: return ""

        return if (textRange.length > 0) {
            StringUtil.shortenTextWithEllipsis(document.getText(textRange), 20, 0)
        } else if (document.textLength > textRange.startOffset) {
            val endOffset = min(document.textLength, textRange.startOffset + 20)
            val text = document.getText(TextRange(textRange.startOffset, endOffset))
            "$text…"
        } else {
            ""
        }
    }

    override fun navigationRequest(): NavigationRequest? {
        val document = FileDocumentManager.getInstance().getDocument(this.targetFile) ?: return null

        val startPosition = this.locationLink.targetSelectionRange.start
        requireNotNull(startPosition) { "Start position cannot be null" }

        val offset = getOffsetInDocument(document, startPosition)
        return offset?.let { NavigationRequest.sourceNavigationRequest(this.project, this.targetFile, it) }

    }
}

