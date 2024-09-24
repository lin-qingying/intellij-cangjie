package com.linqingying.lsp.impl.usages

import com.intellij.find.usages.api.SearchTarget
import com.intellij.find.usages.api.UsageHandler
import com.intellij.model.Pointer
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.platform.backend.presentation.TargetPresentation.Companion.builder
import com.intellij.util.IconUtil.getIcon
import com.linqingying.lsp.api.LspBundle
import com.linqingying.lsp.api.LspServer
import org.eclipse.lsp4j.Position
import org.jetbrains.annotations.NonNls
import kotlin.jvm.internal.Intrinsics

internal class LspSearchTarget(
    val lspServers: Collection<LspServer>,
    val file: VirtualFile,
    val position: Position
) : SearchTarget {

    private val fileNameAndPosition: @NlsSafe String =
        file.name + ":" + (position.line + 1) + ":" + (position.character + 1)

    private val lspServerPresentableName: @NonNls String = "LSP"


    override val usageHandler: UsageHandler
        get() {


            return UsageHandler.createEmptyUsageHandler(
                LspBundle.message(
                    "0.find.references.1",
                    *arrayOf(lspServerPresentableName, fileNameAndPosition)
                )
            )
        }

    override fun createPointer(): Pointer<out SearchTarget> {
        return Pointer.hardPointer(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        } else if (!Intrinsics.areEqual(this.javaClass, other?.javaClass)) {
            return false
        } else {
            Intrinsics.checkNotNull(
                other,
                "null cannot be cast to non-null type com.intellij.platform.lsp.impl.usages.LspSearchTarget"
            )
            other as LspSearchTarget
            return if (!Intrinsics.areEqual(this.file, other.file)) {
                false
            } else {
                Intrinsics.areEqual(this.position, other.position)
            }
        }
    }

    override fun hashCode(): Int {
        var var1 = file.hashCode()
        var1 = 31 * var1 + position.hashCode()
        return var1
    }


    override fun presentation(): TargetPresentation {
        val targetPresentation = builder(fileNameAndPosition)
            .icon(getIcon(file, 1, lspServers.first().project))
            .presentation()

        return targetPresentation
    }
}
