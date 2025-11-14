package org.cangnova.cangjie.protodebugger.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.execution.TargetState
import com.intellij.openapi.diagnostic.Logger

/**
 * 调试器状态管理器
 *
 * 统一管理调试器的所有状态信息，确保状态的一致性和线程安全。
 * 这是调试器状态的唯一真实来源（Single Source of Truth）。
 *
 * 职责：
 * - 维护调试器状态（TargetState）
 * - 维护当前停止的线程信息
 * - 维护调试器能力标志（capabilities）
 * - 维护调试器版本信息
 * - 提供初始化完成信号
 */
class DebuggerStateManager {
    companion object {
        private val LOG = Logger.getInstance(DebuggerStateManager::class.java)
        private const val INITIALIZATION_TIMEOUT_MS = 30000L
    }

    // ==================== 状态字段 ====================

    /**
     * 当前调试器状态
     */
    @Volatile
    private var currentState: TargetState = TargetState.NOT_READY

    /**
     * 当前停止的线程
     */
    @Volatile
    private var stoppedThread: LLThread? = null

    /**
     * 调试器能力标志
     */
    @Volatile
    private var capabilities: Long = 0

    /**
     * 调试器版本
     */
    @Volatile
    private var version: String? = null

    /**
     * 初始化完成信号
     *
     * 当收到 InitializedEvent 广播时，此 Deferred 会被完成。
     * 其他组件可以通过 waitForInitialization() 等待调试器服务器完全就绪。
     */
    private val initialized = CompletableDeferred<Unit>()

    // ==================== 状态查询 ====================

    /**
     * 获取当前调试器状态
     */
    fun getState(): TargetState = currentState

    /**
     * 获取当前停止的线程
     */
    fun getStoppedThread(): LLThread? = stoppedThread

    /**
     * 获取调试器能力标志
     */
    fun getCapabilities(): Long = capabilities

    /**
     * 获取调试器版本
     */
    fun getVersion(): String? = version

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized.isCompleted

    // ==================== 状态更新 ====================

    /**
     * 更新调试器状态
     */
    fun updateState(newState: TargetState) {
        if (currentState != newState) {
            LOG.debug("State transition: $currentState -> $newState")
            currentState = newState
        }
    }

    /**
     * 更新停止的线程
     */
    fun updateStoppedThread(thread: LLThread?) {
        stoppedThread = thread
    }

    /**
     * 更新调试器能力和版本（通常在 InitializedEvent 时调用）
     */
    fun updateCapabilitiesAndVersion(caps: Long, ver: String?) {
        capabilities = caps
        version = ver
        LOG.info("Debugger capabilities updated: $caps, version: $ver")
    }

    /**
     * 标记初始化完成
     *
     * 在收到 InitializedEvent 时调用，通知所有等待初始化的组件。
     */
    fun markInitialized() {
        if (!initialized.isCompleted) {
            initialized.complete(Unit)
            LOG.info("Debugger initialization marked as complete")
        }
    }

    /**
     * 等待调试器初始化完成
     *
     * 此方法会阻塞（挂起）直到收到 InitializedEvent，表示调试器服务器已完全就绪。
     * 在发送任何调试命令之前，应该先调用此方法以确保服务器准备就绪。
     *
     * @throws kotlinx.coroutines.TimeoutCancellationException 如果超时（30秒）
     */
    suspend fun waitForInitialization() {
        try {
            withTimeout(INITIALIZATION_TIMEOUT_MS) {
                initialized.await()
            }
            LOG.debug("Initialization wait completed successfully")
        } catch (e: Exception) {
            LOG.error("Failed to wait for debugger initialization", e)
            throw e
        }
    }

    // ==================== 便利方法 ====================

    /**
     * 检查是否允许发送命令
     */
    fun canSendCommands(): Boolean {
        return isInitialized() && currentState != TargetState.FINISHED
    }

    /**
     * 检查是否在暂停状态
     */
    fun isSuspended(): Boolean {
        return currentState == TargetState.SUSPENDED
    }

    /**
     * 检查是否在运行状态
     */
    fun isRunning(): Boolean {
        return currentState == TargetState.RUNNING
    }

    /**
     * 检查是否已完成
     */
    fun isFinished(): Boolean {
        return currentState == TargetState.FINISHED
    }

    /**
     * 重置状态管理器（用于测试）
     */
    fun reset() {
        currentState = TargetState.NOT_READY
        stoppedThread = null
        capabilities = 0
        version = null
        // 注意：initialized 是 CompletableDeferred，一旦完成就不能重置
    }
}