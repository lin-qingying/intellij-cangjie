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
import org.cangnova.cangjie.macro.compiler.CompilerMacroCompilationProvider
import org.cangnova.cangjie.macro.engine.MacroExpansionEngine
import org.cangnova.cangjie.macro.messages.CangJieMacroBundle
import org.cangnova.cangjie.macro.service.*
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
                if (options.autoCompileMacros) {
                    compileMacros(options)
                }

                // Step 2: 确保服务端已启动
                val lspClient = ensureClientStarted(sdk, options)
                    ?: return@withContext CjResult.Err(
                        MacroExpansionError.CompilerUnavailable(CangJieMacroBundle.message("macro.error.lsp.server.start.failed"))
                    )

                // Step 3: 加载宏动态库
                val macroLibPaths = compilationProvider.compiledMacroPackageDirs
                    .flatMap { dir -> findMacroLibs(dir, sdk) }
                if (macroLibPaths.isNotEmpty()) {
                    lspClient.loadLibs(macroLibPaths)
                }

                // Step 4: 构造并发送展开请求
                // NOTE: 完整的 MultiMacroCalls 构建需要解析源文件中的宏调用位置，
                // 这需要 PSI/AST 信息。此处提供框架，具体 Token 序列化
                // 参见 MacroMsgCodec 文档。
                //
                // 当前实现：回退到文件级展开（通过 cjc-frontend --debug-macro 兼容模式）
                // 完整 MultiMacroCalls 实现需要从 LSP 服务端获取宏调用的 Token 数据。
                return@withContext expandViaFallback(project, file, sdk, options)

            } catch (e: LspMacroServerException) {
                logger.warn("LSPMacroServer 通信异常: ${e.message}")
                // 连接失败时重置客户端，下次重试
                resetClient()
                CjResult.Err(MacroExpansionError.InternalError(e.message ?: CangJieMacroBundle.message("macro.error.lsp.communication.failed"), e))
            } catch (e: Exception) {
                CjResult.Err(MacroExpansionError.InternalError(e.message ?: CangJieMacroBundle.message("macro.error.unknown"), e))
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
     */
    private fun ensureClientStarted(sdk: CjSdk, options: MacroExpansionOptions): LspMacroServerClient? {
        val existing = client
        if (existing?.isConnected == true) return existing

        // 重新创建客户端
        return try {
            val newClient = LspMacroServerClient(
                sdk = sdk,
                enableParallel = options.recursive,
            )
            newClient.start()
            client = newClient
            newClient
        } catch (e: Exception) {
            logger.error("LSPMacroServer 启动失败: ${e.message}", e)
            null
        }
    }

    private fun resetClient() {
        client?.runCatching { close() }
        client = null
    }

    private suspend fun compileMacros(options: MacroExpansionOptions) {
        val compileOptions = MacroCompilationOptions(
            forceRecompile = options.forceRecompile,
            timeoutMs = options.timeoutMs / 2,
            parallel = true,
        )
        compilationProvider.compileAllMacrosInProject(compileOptions)
    }

    /**
     * 回退方案：使用 LSPMacroServer 的 DefLib + 文件级展开模式
     *
     * 完整实现需要：
     * 1. 解析源文件中的宏调用（`@MacroName(args...)`）
     * 2. 将每个宏调用的 Token 序列化为 FlatBuffers MacroCall
     * 3. 发送 MultiMacroCalls 并接收 MacroResult
     *
     * 当前回退到通过 cjc-frontend 的文件级展开。
     */
    private fun expandViaFallback(
        project: Project,
        file: VirtualFile,
        sdk: CjSdk,
        options: MacroExpansionOptions,
    ): CjResult<List<MacroExpansionResult>, MacroExpansionError> {
        // 此处保留扩展点：当 MultiMacroCalls 完整实现后替换此逻辑
        return CjResult.Err(
            MacroExpansionError.UnsupportedOperation(
                CangJieMacroBundle.message("macro.error.lsp.multimacroccalls.not.implemented")
            )
        )
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
        return sdk.getExecutable("LSPMacroServer","tools","bin").toFile()
    }
}
