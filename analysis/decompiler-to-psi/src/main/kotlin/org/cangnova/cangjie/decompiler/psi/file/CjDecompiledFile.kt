/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.decompiler.psi.file

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.StubTreeLoader
import org.cangnova.cangjie.decompiler.psi.CangJieDecompiledFileViewProvider
import org.cangnova.cangjie.decompiler.psi.text.DecompiledText
import org.cangnova.cangjie.decompiler.psi.text.buildDecompiledText
import org.cangnova.cangjie.decompiler.stub.file.ClsClassFinder
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.deepCopy
import org.cangnova.cangjie.utils.LockedClearableLazyValue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 反编译的仓颉文件。
 *
 * 该类继承自 [CjFile]，用于表示从二进制元数据文件反编译后的仓颉源代码文件。
 * 它从 [CangJieDecompiledFileViewProvider] 获取反编译文本，确保文档和 PSI 使用相同的数据源。
 *
 * @param provider 提供此文件视图的 [CangJieDecompiledFileViewProvider]
 *
 * @see CangJieDecompiledFileViewProvider
 * @see DecompiledText
 */
open class CjDecompiledFile(
    private val provider: CangJieDecompiledFileViewProvider,
) : CjFile(provider, true) {

    /**
     * 获取文件的文本内容。
     *
     * 直接从 provider 获取反编译文本，确保与文档内容一致。
     *
     * @return 反编译后的源代码文本
     */
    override fun getText(): String {
        return provider.decompiledText.get().text
    }

    /**
     * 当内容重新加载时调用。
     *
     * 清除 provider 中缓存的反编译文本，强制下次访问时重新生成。
     */
    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        provider.decompiledText.drop()
    }

    companion object {
        /**
         * 从虚拟文件读取或构建编译后的 stub。
         *
         * 该方法尝试从缓存读取 stub，如果缓存不存在则重新构建。
         * 如果构建失败，返回包含错误信息的无效 stub。
         *
         * @param virtualFile 要读取的虚拟文件
         * @param project 当前项目
         * @return 文件的 stub 树根节点
         */
        fun readOrBuildCompiledStub(virtualFile: VirtualFile, project: Project): CangJieFileStubImpl {
            val stubTree = ClsClassFinder.allowMultifileClassPart {
                val stubLoader = StubTreeLoader.getInstance()

                // The default project is not supported in the stub loader
                if (project.isDefault) {
                    stubLoader.build(/* project = */ null, /* vFile = */ virtualFile, /* psiFile = */ null)
                } else {
                    // Read stub from cache if it is present
                    stubLoader.readOrBuild(/* project = */ project, /* vFile = */ virtualFile, /* psiFile = */ null)
                }
            }

            val fileStub = stubTree?.root as? CangJieFileStubImpl
            return if (fileStub != null) {
                fileStub
            } else {
                val cause = if (stubTree == null) {
                    "stub tree is not found"
                } else {
                    "non-CangJie stub tree (${stubTree::class.simpleName})"
                }

                val text = """
                    // Could not decompile the file: $cause
                """.trimIndent()

                CangJieFileStubImpl.forInvalid(text)
            }
        }
    }
}



private object CompiledStubBuilder : StubBuilder {
    override fun buildStubTree(file: PsiFile): CangJieFileStubImpl {
        requireIsInstance<CjDecompiledFile>(file)
        val stub = CjDecompiledFile.readOrBuildCompiledStub(
            file.viewProvider.virtualFile,
            file.project
        )

        // A copy is required because stubs are stateful and mutable, so they cannot be shared as they are
        val clonedStub = stub.deepCopy()
        clonedStub.psi = file
        return clonedStub
    }

    override fun skipChildProcessingWhenBuildingStubs(parent: ASTNode, node: ASTNode): Boolean = false
}


@OptIn(ExperimentalContracts::class)
public inline fun <reified T> requireIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    require(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}

@OptIn(ExperimentalContracts::class)
public inline fun <reified T> checkIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    check(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}