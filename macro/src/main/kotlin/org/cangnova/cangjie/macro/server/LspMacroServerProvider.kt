/*
 * Copyright 2026 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.macro.server

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.macro.compiler.CompilerMacroCompilationProvider
import org.cangnova.cangjie.macro.engine.MacroExpansionEngine
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.macro.server.protocol.MacroMsgCodec
import org.cangnova.cangjie.macro.service.*
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.lexer.CjToken
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.result.CjResult
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import org.cangnova.cangjie.toolchain.api.CjSdk
import java.io.File

/**
 * 基于 LSPMacroServer 的宏展开提供者
 *
 * 通过常驻 `LSPMacroServer` 进程展开宏，使用管道 + FlatBuffers 协议通信。
 * 与 [org.cangnova.cangjie.macro.compiler.CompilerMacroExpansionProvider]（文件级）相比：
 * - **优势**：进程复用，无需每次展开都启动新进程；支持宏调用级粒度
 * - **适用**：需要高性能实时宏展开的场景（如 IDE 内联展开预览）
 *
 * ## 工作流程
 *
 * 1. 检查 SDK 中是否存在 `LSPMacroServer` 可执行文件
 * 2. 首次使用时懒启动进程（`start()`）
 * 3. 发送 `DefLib` 消息加载宏动态库
 * 4. 发送 `MultiMacroCalls` 请求展开，接收 `MacroResult` 响应
 * 5. 项目关闭时调用 `dispose()` 停止进程
 *
 * ## 实现 MacroExpansionEngine
 *
 * 本类直接实现 [MacroExpansionEngine] 接口（Provider = 引擎自身），
 * 遵循"由来源者自行实现相关信息"的设计原则。
 *
 * @see LspMacroServerEngine 引擎元信息（委托给此 object）
 */
class LspMacroServerProvider(private val project: Project) :
    MacroExpansionProvider, MacroExpansionEngine by LspMacroServerEngine {

    private val logger = Logger.getInstance(LspMacroServerProvider::class.java)

    private val compilationProvider = CompilerMacroCompilationProvider(project)

    // 懒初始化：首次展开时启动
    @Volatile
    private var client: LspMacroServerClient? = null

    override val name: String = LspMacroServerEngine.displayName

    override val engine: MacroExpansionEngine get() = LspMacroServerEngine

    override fun isAvailable(project: Project): Boolean {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: return false
        if (!sdk.isValid) return false
        val executable = resolveExecutable(sdk)
        return executable.exists()
    }

    override suspend fun expandMacroAtOffset(
        project: Project,
        file: VirtualFile,
        offset: Int,
        options: MacroExpansionOptions,
    ): CjResult<MacroExpansionResult, MacroExpansionError> {
        // LSPMacroServer 当前以文件级展开为基础，从结果中查找最匹配的宏
        val allResults = expandAllMacrosInFile(project, file, options)
        return when (allResults) {
            is CjResult.Ok -> {
                if (allResults.ok.isEmpty()) {
                    return CjResult.Err(MacroExpansionError.MacroNotFound("offset=$offset"))
                }
                val match = allResults.ok.find { r ->
                    r.startOffset != r.endOffset && offset >= r.startOffset && offset < r.endOffset
                }
                CjResult.Ok(match ?: allResults.ok.first())
            }

            is CjResult.Err -> allResults
        }
    }

    override suspend fun expandAllMacrosInFile(
        project: Project,
        file: VirtualFile,
        options: MacroExpansionOptions,
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> {
        val sdk = CjProjectSdkConfig.getInstance(project).getProjectSdk()
            ?: return CjResult.Err(MacroExpansionError.SdkNotConfigured())

        if (!sdk.isValid) {
            return CjResult.Err(MacroExpansionError.CompilerUnavailable(CangJieMacroBundle.message("macro.error.sdk.invalid")))
        }

        return withContext(Dispatchers.IO) {
            try {
                // Step 1: 按需编译宏包
                // Windows 下 LSPMacroServer 加载 DLL 后会持有文件锁，导致链接器无法覆写输出文件。
                // 重新编译前必须先关闭服务端以释放所有已加载的宏动态库文件锁。
                if (options.autoCompileMacros) {
                    resetClient()
                    compileMacros(options, file)
                }

                // Step 2: 确保服务端已启动
                val lspClient = ensureClientStarted(sdk, options)
                    ?: return@withContext CjResult.Err(
                        MacroExpansionError.CompilerUnavailable(CangJieMacroBundle.message("macro.error.lsp.server.start.failed"))
                    )

                // Step 3: 加载宏动态库（必须成功才能进行展开）
                val macroLibPaths = compilationProvider.compiledMacroPackageDirs
                    .flatMap { dir -> findMacroLibs(dir, sdk) }
                if (macroLibPaths.isEmpty()) {
                    return@withContext CjResult.Err(
                        MacroExpansionError.CompilerUnavailable(CangJieMacroBundle.message("macro.error.no.macro.libs"))
                    )
                }
                lspClient.loadLibs(macroLibPaths)

                // Step 4: 使用 MultiMacroCalls 协议展开文件中的所有宏
                val result = expandViaMultiMacroCalls(project, file, sdk, lspClient)

                // Step 5: 重置服务端状态（清空宏声明/调用/诊断），与编译器 SendExitStgTask 行为一致
                lspClient.resetStage()

                return@withContext result

            } catch (e: LspMacroServerException) {
                logger.warn("LSPMacroServer 通信异常: ${e.message}")
                CjResult.Err(
                    MacroExpansionError.InternalError(
                        e.message ?: CangJieMacroBundle.message("macro.error.lsp.communication.failed"), e
                    )
                )
            } catch (e: Exception) {
                CjResult.Err(
                    MacroExpansionError.InternalError(
                        e.message ?: CangJieMacroBundle.message("macro.error.unknown"), e
                    )
                )
            } finally {
                // 无论成功、失败还是提前返回，都关闭服务端进程，
                // 避免残留进程占用 DLL 文件锁或系统资源
                resetClient()
            }
        }
    }

    override suspend fun expandMacrosInRange(
        project: Project,
        file: VirtualFile,
        startOffset: Int,
        endOffset: Int,
        options: MacroExpansionOptions,
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> {
        val allResults = expandAllMacrosInFile(project, file, options)
        return when (allResults) {
            is CjResult.Ok -> CjResult.Ok(
                allResults.ok.filter { r ->
                    r.startOffset >= startOffset && r.endOffset <= endOffset
                }
            )

            is CjResult.Err -> allResults
        }
    }

    /**
     * 释放资源，关闭 LSPMacroServer 进程
     *
     * 应在项目关闭时调用（由 [LspMacroServerService] 负责）。
     */
    fun dispose() {
        resetClient()
    }

    // ─── 私有方法 ──────────────────────────────────────────────────────────────

    /**
     * 确保客户端已启动，懒启动逻辑
     *
     * 若旧客户端已断开连接，先清理旧进程再创建新客户端，避免进程堆积。
     */
    private fun ensureClientStarted(sdk: CjSdk, options: MacroExpansionOptions): LspMacroServerClient? {
        val existing = client
        if (existing?.isConnected == true) return existing

        // 旧客户端可能已断连但进程仍在运行，必须先清理
        if (existing != null) {
            resetClient()
        }

        // 重新创建客户端
        return try {
            val newClient = LspMacroServerClient(
                sdk = sdk,
                // 始终使用并行模式：
                // - 并行模式下 FindMacroDefMethod 失败时发 STATUS_FAIL 响应，Kotlin 端可正常处理
                // - 串行模式下失败时直接退出进程，Kotlin 端会读到 EOF 导致管道关闭
                enableParallel = true,
            )
            newClient.start()
            client = newClient
            newClient
        } catch (e: Exception) {
            logger.error("LSPMacroServer 启动失败: ${e.message}", e)
            null
        } catch (e: LinkageError) {
            logger.error("LSPMacroServer 启动失败（原生库加载错误）: ${e.message}", e)
            null
        }
    }

    /**
     * 关闭当前客户端并确保进程退出
     *
     * 优先尝试优雅关闭（发送 ExitTask + 等待退出），失败时强制终止，
     * 避免残留进程占用端口或文件锁。
     */
    private fun resetClient() {
        val c = client ?: return
        client = null
        try {
            c.close()
        } catch (_: Exception) {
            // 优雅关闭失败，强制终止
            c.runCatching { forceClose() }
        }
    }

    private suspend fun compileMacros(options: MacroExpansionOptions, file: VirtualFile? = null) {
        val compileOptions = MacroCompilationOptions(
            forceRecompile = options.forceRecompile,
            timeoutMs = options.timeoutMs / 2,
            parallel = true,
        )
        compilationProvider.compileAllMacrosInProject(compileOptions, contextFile = file)
    }

    /**
     * 使用 LSPMacroServer 的 MultiMacroCalls 协议展开文件中的所有宏
     *
     * 工作流程：
     * 1. 在 ReadAction 中从 PSI 提取所有宏调用表达式及其 Token 信息
     * 2. 逐条发送 MultiMacroCalls 请求（每条宏调用对应一次请求-响应），收集 MacroResult
     * 3. 将 MacroResult 响应逐一转换为 MacroExpansionResult
     */
    private fun expandViaMultiMacroCalls(
        project: Project,
        file: VirtualFile,
        sdk: CjSdk,
        lspClient: LspMacroServerClient,
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> {
        // Step 1: PSI 提取（ReadAction 中执行）
        val extractedCalls = try {
            ReadAction.compute<List<ExtractedMacroCall>, Exception> {
                extractMacroCallsFromPsi(project, file, sdk)
            }
        } catch (e: Exception) {
            logger.warn("从 PSI 提取宏调用失败: ${e.message}", e)
            return CjResult.Err(
                MacroExpansionError.InternalError(e.message ?: CangJieMacroBundle.message("macro.error.unknown"), e)
            )
        }

        if (extractedCalls.isEmpty()) {
            return CjResult.Ok(emptyList())
        }

        // Step 2: 逐条发送宏调用请求（协议要求每条单独发送，每次接收一条 MacroResult 响应）
        // LspMacroServerException（含管道断开）由外层 expandAllMacrosInFile 统一处理并 resetClient
        val lspResults = lspClient.expandMacros(extractedCalls.map { it.callInfo })

        // Step 3: 转换结果（跳过展开失败的宏调用）
        val results = extractedCalls.zip(lspResults).mapNotNull { (extracted, lspResult) ->
            if (lspResult.isFailed) {
                logger.debug("宏调用展开失败 (${extracted.callInfo.methodName}): ${lspResult.diagnostics}")
                null
            } else {
                val diagnostics = lspResult.diagnostics.map { d ->
                    MacroDiagnostic(
                        level = when (d.severity) {
                            0 -> DiagnosticLevel.INFO
                            1 -> DiagnosticLevel.WARNING
                            else -> DiagnosticLevel.ERROR
                        },
                        message = if (d.hint.isNotEmpty()) "${d.message} (${d.hint})" else d.message,
                    )
                }
                MacroExpansionResult(
                    filePath = file.path,
                    startOffset = extracted.startOffset,
                    endOffset = extracted.endOffset,
                    expandedText = lspResult.toExpandedText(),
                    macroName = extracted.callInfo.idName,
                    diagnostics = diagnostics,
                    source = MacroExpansionSource.Engine(LspMacroServerEngine),
                )
            }
        }

        return CjResult.Ok(results)
    }

    /** PSI 提取结果：宏调用信息 + 在源文件中的字节偏移量（用于 MacroExpansionResult） */
    private data class ExtractedMacroCall(
        val callInfo: MacroMsgCodec.MacroCallInfo,
        val startOffset: Int,
        val endOffset: Int,
    )

    /**
     * 从 PSI 中提取文件内所有宏调用表达式
     *
     * 必须在 ReadAction 中调用。
     */
    private fun extractMacroCallsFromPsi(
        project: Project,
        file: VirtualFile,
        sdk: CjSdk,
    ): List<ExtractedMacroCall> {
        val psiManager = PsiManager.getInstance(project)
        val cjFile = psiManager.findFile(file) as? CjFile ?: return emptyList()
        val document = PsiDocumentManager.getInstance(project).getDocument(cjFile)

        val macroLibs = compilationProvider.compiledMacroPackageDirs
            .flatMap { dir -> findMacroLibs(dir, sdk) }

        return PsiTreeUtil.findChildrenOfType(cjFile, CjMacroExpression::class.java)
            .mapNotNull { macroExpr -> buildExtractedCall(macroExpr, document, macroLibs) }
    }

    /**
     * 从 CjElement token 列表收集 TokenInfo
     *
     * 与 collectLeafTokenInfos(ASTNode) 等价，但直接使用已解析的 tokens 列表，
     * 避免重复遍历 AST 树。
     */
    private fun collectLeafTokenInfosFromTokens(
        tokens: List<PsiElement>,
        document: Document?,
    ): List<MacroMsgCodec.TokenInfo> {
        val result = mutableListOf<MacroMsgCodec.TokenInfo>()
        for (token in tokens) {
            val node = token.node
            val tokenType = node.elementType as? CjToken ?: continue
            val tokenId = tokenType.tokenId
            if (tokenId < 0 || tokenId > 255) continue
            val (beginLine, beginCol) = offsetToLineCol(document, node.startOffset)
            val (endLine, endCol) = offsetToLineCol(document, node.startOffset + node.textLength)
            result.add(
                MacroMsgCodec.TokenInfo(
                    kind = tokenId.toUByte(),
                    value = node.text,
                    begin = MacroMsgCodec.PositionInfo(0, beginLine, beginCol),
                    end = MacroMsgCodec.PositionInfo(0, endLine, endCol),
                )
            )
        }
        return result
    }
    /**
     * 将 CjMacroExpression 转换为 ExtractedMacroCall
     *
     * - methodName 优先从 stub 获取，fallback 到 referenceExpression.text 的末段
     * - packageName 从引用文本中提取点前部分（如 "pkg.MyMacro" → "pkg"）
     * - libPath 通过包名与动态库文件名匹配
     */
    private fun buildExtractedCall(
        macroExpr: CjMacroExpression,
        document: Document?,
        macroLibs: List<String>,
    ): ExtractedMacroCall? {
        val refText = macroExpr.referenceExpression?.text ?: return null
        val dotIdx = refText.lastIndexOf('.')
        val methodName = if (dotIdx >= 0) refText.substring(dotIdx + 1) else refText
        val packageName = if (dotIdx > 0) refText.substring(0, dotIdx) else ""

        if (methodName.isBlank()) return null

        val libPath = resolveLibPath(packageName, macroLibs)
// 方式二：通过 tokens 列表直接构建
        val argTokens = macroExpr.input?.tokens?.let { tokens ->
            collectLeafTokenInfosFromTokens(tokens, document)
        } ?: emptyList()
//        val argTokens = macroExpr.input?.node?.let { node ->
//            collectLeafTokenInfos(node, document)
//        } ?: emptyList()

        val hasAttrs = macroExpr.attr != null
        val attrTokens = macroExpr.attr?.node?.let { node ->
            collectLeafTokenInfos(node, document)
        } ?: emptyList()

        val range = macroExpr.textRange
        val startOffset = range.startOffset
        val endOffset = range.endOffset
        val (beginLine, beginCol) = offsetToLineCol(document, startOffset)
        val (endLine, endCol) = offsetToLineCol(document, endOffset)

        // 计算宏导出函数名（与 C++ Utils::GetMacroFuncName 对齐）
        // DLL 导出符号格式：macroCall_c_<name>_<pkg>（普通宏）或 macroCall_a_<name>_<pkg>（属性宏）
        val fullPackageName = if (packageName.isNotEmpty()) {
            packageName
        } else {
            extractPackageNameFromLibPath(libPath)
        }
        val mangledMethodName = computeMacroFuncName(methodName, fullPackageName, hasAttrs)

        val callInfo = MacroMsgCodec.MacroCallInfo(
            idName = methodName,
            idPos = MacroMsgCodec.PositionInfo(0, beginLine, beginCol),
            hasAttrs = hasAttrs,
            args = argTokens,
            attrs = attrTokens,
            methodName = mangledMethodName,
            packageName = packageName,
            libPath = libPath,
            begin = MacroMsgCodec.PositionInfo(0, beginLine, beginCol),
            end = MacroMsgCodec.PositionInfo(0, endLine, endCol),
        )

        return ExtractedMacroCall(callInfo, startOffset, endOffset)
    }

    /**
     * 递归收集 ASTNode 下所有叶子 Token 的位置和类型信息
     *
     * 只收集 [CjToken.tokenId] >= 0 的有效编译器 Token（跳过复合节点和无效 tokenId 的叶子）。
     */
    private fun collectLeafTokenInfos(root: ASTNode, document: Document?): List<MacroMsgCodec.TokenInfo> {
        val result = mutableListOf<MacroMsgCodec.TokenInfo>()
        collectLeafTokenInfosImpl(root, document, result)
        return result
    }

    private fun collectLeafTokenInfosImpl(
        node: ASTNode,
        document: Document?,
        result: MutableList<MacroMsgCodec.TokenInfo>,
    ) {
        if (node.firstChildNode == null) {
            val tokenType = node.elementType as? CjToken ?: return
            val tokenId = tokenType.tokenId
            if (tokenId < 0 || tokenId > 255) return
            val (beginLine, beginCol) = offsetToLineCol(document, node.startOffset)
            val (endLine, endCol) = offsetToLineCol(document, node.startOffset + node.textLength)
            result.add(
                MacroMsgCodec.TokenInfo(
                    kind = tokenId.toUByte(),
                    value = node.text,
                    begin = MacroMsgCodec.PositionInfo(0, beginLine, beginCol),
                    end = MacroMsgCodec.PositionInfo(0, endLine, endCol),
                )
            )
        } else {
            var child = node.firstChildNode
            while (child != null) {
                collectLeafTokenInfosImpl(child, document, result)
                child = child.treeNext
            }
        }
    }

    /**
     * 根据包名匹配宏动态库路径
     *
     * 命名约定：`lib-macro_<pkg_with_underscores>.<ext>`
     * 匹配规则：将包名中 '.' 替换为 '_' 后与库文件名做子串匹配。
     * 若仅有一个库，直接使用；若匹配失败，使用第一个可用库。
     */
    private fun resolveLibPath(packageName: String, macroLibs: List<String>): String {
        if (macroLibs.isEmpty()) return ""
        if (macroLibs.size == 1) return macroLibs.first()
        if (packageName.isNotEmpty()) {
            val pkgSegment = packageName.replace('.', '_')
            val match = macroLibs.firstOrNull { libPath ->
                File(libPath).nameWithoutExtension.contains(pkgSegment, ignoreCase = true)
            }
            if (match != null) return match
        }
        return macroLibs.first()
    }

    /**
     * 计算宏导出函数名
     *
     * 遵循编译器 `Utils::GetMacroFuncName` 的命名约定：
     * - 普通宏: `macroCall_c_<name>_<fullPackageName>`
     * - 属性宏: `macroCall_a_<name>_<fullPackageName>`
     * 其中 fullPackageName 中的 '.' 替换为 '_'
     */
    private fun computeMacroFuncName(macroName: String, fullPackageName: String, isAttr: Boolean): String {
        val prefix = if (isAttr) "macroCall_a_" else "macroCall_c_"
        return "${prefix}${macroName}_${fullPackageName}".replace('.', '_')
    }

    /**
     * 从宏动态库路径中提取包名
     *
     * 仅移除 `lib-macro_` 前缀和最后的平台扩展名（`.dll`/`.so`/`.dylib`），保留完整包名。
     * 例如：`lib-macro_untitled89.a.dll` → `untitled89.a`
     *
     * TODO: 从库路径反推包名并不可靠（如包名含 '.' 与扩展名中的 '.' 可能混淆），
     *       后续应通过 PSI 引用解析或 import 语句获取宏定义的完整包名。
     */
    private fun extractPackageNameFromLibPath(libPath: String): String {
        if (libPath.isEmpty()) return ""
        val fileName = File(libPath).name
        val prefix = "lib-macro_"
        if (!fileName.startsWith(prefix)) return ""
        val afterPrefix = fileName.substring(prefix.length)
        val lastDotIdx = afterPrefix.lastIndexOf('.')
        return if (lastDotIdx >= 0) afterPrefix.substring(0, lastDotIdx) else afterPrefix
    }

    /** 将文档偏移量转为 1-based 行列号 */
    private fun offsetToLineCol(document: Document?, offset: Int): Pair<Int, Int> {
        document ?: return Pair(0, 0)
        val safeOffset = offset.coerceIn(0, document.textLength)
        val line = document.getLineNumber(safeOffset)
        val col = safeOffset - document.getLineStartOffset(line)
        return Pair(line + 1, col + 1)
    }

    private fun findMacroLibs(dir: String, sdk: CjSdk): List<String> {
        val directory = File(dir)
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        val ext = sdk.macroLibExtension
        return directory.listFiles()
            ?.filter { it.name.startsWith("lib-macro_") && it.name.endsWith(".$ext") }
            ?.map { it.absolutePath }
            ?: emptyList()
    }

    private fun resolveExecutable(sdk: CjSdk): File {
        return sdk.getExecutable("LSPMacroServer", "tools", "bin").toFile()
    }
}
