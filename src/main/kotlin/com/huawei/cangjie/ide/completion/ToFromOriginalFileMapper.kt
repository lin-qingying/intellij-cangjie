package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.psi.CjFile
import com.intellij.codeInsight.completion.CompletionParameters


class ToFromOriginalFileMapper private constructor(
    val originalFile: CjFile,
    private val syntheticFile: CjFile,
    private val completionOffset: Int
)
{
    companion object {
        fun create(parameters: CompletionParameters): ToFromOriginalFileMapper {
            val originalFile = parameters.originalFile as CjFile
            val syntheticFile = parameters.position.containingFile as CjFile
            return ToFromOriginalFileMapper(originalFile, syntheticFile, parameters.offset)
        }
    }
}
