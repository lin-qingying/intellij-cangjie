package com.huawei.cangjie.idea.highlighter.suspender

import com.huawei.cangjie.idea.core.CangJiePluginDisposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.intellij.util.Alarm
import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.TimeUnit

/**
 * 控制编辑器的语法高亮暂停
 */
@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class CangJieHighlightingSuspender(private val project: Project) {

    private val timeoutSeconds = Registry.intValue("cangjie.highlighting.suspended.timeout", 10)

    private val lastThrownExceptionTimestampPerFile = mutableMapOf<VirtualFile, Long>()
    private val suspendTimeoutMs = TimeUnit.SECONDS.toMillis(timeoutSeconds.toLong())
    private val updateQueue = Alarm(Alarm.ThreadToUse.SWING_THREAD, CangJiePluginDisposable.getInstance(project))

    private fun cleanup() {
        val timestamp = System.currentTimeMillis()

        val filesToUpdate = mutableListOf<VirtualFile>()
        val filesToUpdateLater = mutableListOf<VirtualFile>()
        synchronized(lastThrownExceptionTimestampPerFile) {
            if (lastThrownExceptionTimestampPerFile.isEmpty()) return

            val it = lastThrownExceptionTimestampPerFile.entries.iterator()
            while (it.hasNext()) {
                val next = it.next()
                if (timestamp - next.value > suspendTimeoutMs) {
                    filesToUpdate += next.key
                    it.remove()
                }
            }
            filesToUpdateLater.addAll(lastThrownExceptionTimestampPerFile.keys)
        }

        updateQueue.cancelAllRequests()
        filesToUpdate.forEach(::updateNotifications)
        filesToUpdateLater.forEach(::scheduleUpdate)
    }

    private fun scheduleUpdate(file: VirtualFile) {
        updateQueue.apply { addRequest(Runnable { updateNotifications(file) }, suspendTimeoutMs + 1) }
    }

    private fun updateNotifications(file: VirtualFile) {
        EditorNotifications.getInstance(project).updateNotifications(file)
    }

    /**
     * @return true, when file is suspended for the 1st time (within a timeout window)
     */
    fun suspend(file: VirtualFile): Boolean {
        cleanup()

        if (suspendTimeoutMs <= 0) return false

        val timestamp = System.currentTimeMillis()
        // daemon is restarted when exception is thrown
        // if there is a recurred error (e.g. within a resolve) it could lead to infinite highlighting loop
        // so, do not rethrow exception too often to disable HL for a while
        val lastThrownExceptionTimestamp = synchronized(lastThrownExceptionTimestampPerFile) {
            val lastThrownExceptionTimestamp = lastThrownExceptionTimestampPerFile[file] ?: run {
                lastThrownExceptionTimestampPerFile[file] = timestamp
                0L
            }
            lastThrownExceptionTimestamp
        }

        scheduleUpdate(file)
        updateNotifications(file)

        return timestamp - lastThrownExceptionTimestamp > suspendTimeoutMs
    }


    companion object {
        fun getInstance(project: Project): CangJieHighlightingSuspender = project.service()
    }
}