package com.linqingying.lsp.api






import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.jetbrains.annotations.ApiStatus

/**
 * Plugins register their implementations of the `LspServerSupportProvider` to add LSP server-based support for some programming language
 * or framework.
 *
 * It's recommended to start LSP servers lazily, only when they are really needed for the current work with the project.
 * Remember that the project might be a huge monorepo, and during an IDE session, a developer may work only with a small part of the
 * project. In this case, it would be great not to start LSP servers that are not needed for the current session.
 * See [fileOpened] documentation.
 *
 * @see [https://microsoft.github.io/language-server-protocol/](https://microsoft.github.io/language-server-protocol/)
 */
@ApiStatus.Experimental
interface LspServerSupportProvider {
    /**
     * [LspServerSupportProvider] implementations may call [LspServerStarter.ensureServerStarted] function in their
     * [LspServerSupportProvider.fileOpened] implementations.
     *
     * Plugins should not store references to [LspServerStarter] instances.
     *
     * See [LspServerSupportProvider.fileOpened] documentation for details.
     */
    interface LspServerStarter {
        /**
         * Looks for an already running [LSP server][LspServer] that has the same roots as the passed [LspServerDescriptor].
         * If such server is found, then this function does nothing. If not, then a new instance of [LspServer] is
         * created and started, the passed [LspServerDescriptor] is used to control the server startup and behavior.
         *
         * For a running [LspServer], the passed [descriptor] object is available as [LspServer.descriptor].
         */
        fun ensureServerStarted(descriptor: com.linqingying.lsp.api.LspServerDescriptor)

    }

    /**
     * This function is a convenient way for the `LspServerSupportProvider` implementation to start an LSP server lazily, only when
     * needed. `fileOpened()` is invoked each time when a file is opened in the editor, unless an already running
     * [LspServer] exists and the opened file is within the server roots.
     *
     * Implementations may check the file type, plugin-specific settings, and call [LspServerStarter.ensureServerStarted] if needed.
     *
     * Typical implementation:
     *
     *    if (file.extension == "foo" && isFooSdkConfigured(project)) {
     *       // class FooLspServerDescriptor extends ProjectWideLspServerDescriptor
     *       serverStarter.ensureServerStarted(FooLspServerDescriptor(project))
     *    }
     *
     * Implementations shouldn't store references to the passed [LspServerStarter].
     *
     * Plugins may want to start an LSP server not on 'file opened in the editor' event but on some other event, for example, on enabling a
     * plugin-specific framework support in Settings. In this case plugins can use [LspServerManager.startServersIfNeeded].
     *
     * @param file a valid local file within the project roots
     */
    @RequiresReadLock
    @RequiresBackgroundThread
    fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerStarter)

    companion object {
        @JvmField
        val EP_NAME = create<LspServerSupportProvider>("com.intellij.lsp.serverSupportProvider")
    }
}
