package com.linqingying.lsp.api


import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly

/**
 * Tracks started LSP servers, allows starting, restarting, and stopping LSP servers.
 *
 * Plugins that want to start LSP servers should implement [LspServerSupportProvider].
 *
 * It's recommended to start LSP servers lazily, only when they are really needed for the current work with the project. Remember that the
 * project might be a huge monorepo, and during an IDE session, a developer may work only with a small part of the project. In this case, it
 * would be great not to start LSP servers that are not needed for the current session. See [LspServerSupportProvider.fileOpened], it allows
 * starting the server lazily, only if a file of interest is opened in the editor.
 *
 * Plugins may want to start an LSP server not on 'file opened in the editor' event but on some other event, for example, on enabling a
 * plugin-specific framework support in Settings. In this case plugins can use [LspServerManager.startServersIfNeeded].
 */
@ApiStatus.Experimental
interface LspServerManager {
    companion object {
        /**
         * Returns an `LspServerManager` instance for the given project. The passed `project` parameter must not be the 'default project'
         * (the fake one used to manage settings for new projects). So, some callers may need to check [Project.isDefault] before calling
         * `LspServerManager.getInstance(project)`.
         */
        @JvmStatic
        fun getInstance(project: Project): LspServerManager = project.service()
    }

    fun getServersForProvider(providerClass: Class<out LspServerSupportProvider>): Collection<com.linqingying.lsp.api.LspServer>

    /**
     * This function is designed for the cases like "a user has enabled some framework support in Settings." It notifies the `providerClass`
     * about the files that are open in the editor by scheduling the [LspServerSupportProvider.fileOpened] function calls. So, if the
     * [fileOpened][LspServerSupportProvider.fileOpened] function is implemented according to its documentation, this function guarantees
     * that all [LSP servers][LspServer] needed for the currently open files will get started.
     *
     * If one or more [LSP servers][LspServer] are already running, and all the files that are open in the editor are within the running
     * server roots, then this function doesn't do anything.
     *
     * This function may be called both from the EDT and from the background thread. If called from EDT, it returns quickly and then calls
     * [LspServerSupportProvider.fileOpened] function from a background thread.
     */
    fun startServersIfNeeded(providerClass: Class<out LspServerSupportProvider>)

    /**
     * This function is designed for the cases like "a user has disabled some framework support in Settings."
     * It stops all running LSP servers associated with the `providerClass`.
     */
    fun stopServers(providerClass: Class<out LspServerSupportProvider>)

    /**
     * Just a shorthand for [stopServers] followed by [startServersIfNeeded]. Typically, a plugin calls this function when, for example, a
     * user has changed some framework-specific settings, and therefore an LSP server needs to be restarted with some other parameters.
     */
    fun stopAndRestartIfNeeded(providerClass: Class<out LspServerSupportProvider>)

    @TestOnly
    
    fun addLspServerManagerListener(listener: LspServerManagerListener, parentDisposable: Disposable)
}

