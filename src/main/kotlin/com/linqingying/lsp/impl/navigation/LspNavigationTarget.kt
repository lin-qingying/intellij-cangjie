package com.linqingying.lsp.impl.navigation

import com.linqingying.lsp.api.customization.requests.util.getOffsetInDocument
import com.linqingying.lsp.api.customization.requests.util.getRangeInDocument
import com.intellij.model.Pointer
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.navigation.NavigationRequest
import com.intellij.platform.backend.navigation.NavigationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.Position
import kotlin.math.min


class LspNavigationTarget(val project: Project,val targetFile: VirtualFile,val locationLink: LocationLink) : NavigationTarget {
    override fun createPointer(): Pointer<out NavigationTarget> = Pointer.hardPointer(this)

    override fun computePresentation(): TargetPresentation {
        val builder = TargetPresentation.builder(getShortenedText(targetFile, locationLink))
        val start = locationLink.targetSelectionRange.start
        val presentation = builder.locationText(getFormattedPosition(targetFile, start)).presentation()

        requireNotNull(presentation) { "Presentation cannot be null" }

        return presentation
    }
    private fun getShortenedText(file: VirtualFile, locationLink: LocationLink): String {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return ""
        val rangeInDocument = getRangeInDocument(document, locationLink.targetSelectionRange) ?: return ""

        return when {
            rangeInDocument.length > 0 -> {
                document.getText(rangeInDocument).take(20)
            }
            document.textLength > rangeInDocument.startOffset -> {
                val endOffset = min(document.textLength, rangeInDocument.startOffset + 20)
                document.getText(TextRange(rangeInDocument.startOffset, endOffset)) + "…"
            }
            else -> ""
        }
    }
    override fun navigationRequest(): NavigationRequest? {
        val document = FileDocumentManager.getInstance().getDocument(targetFile) ?: return null
        val start = locationLink.targetSelectionRange.start
        val offsetInDocument = getOffsetInDocument(document, start)

        return offsetInDocument?.let { NavigationRequest.sourceNavigationRequest(project, targetFile, it) }

    }

    private fun getFormattedPosition(file: VirtualFile, position: Position): String {
        return "${file.name}:${position.line + 1}:${position.character + 1}"
    }
}
