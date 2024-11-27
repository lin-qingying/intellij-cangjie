/*
 * Copyright 2024 LinQingYing. and contributors.
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
                "null cannot be cast to non-null type com.linqingying.lsp.impl.usages.LspSearchTarget"
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
