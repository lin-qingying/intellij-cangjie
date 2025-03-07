package cn.cangnova.cangjie.utils



import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

/**
 * 用于在 IntelliJ IDEA 中运行前台任务的抽象类。
 * 通过扩展此类，可以实现自定义的前台任务逻辑。
 *
 * @param T 任务执行结果的类型。
 * @param project 任务所属的项目，可以为 null。
 * @param title 任务的标题，会显示在 UI 中。
 */
abstract class AbstractForegroundTask<T>(
    private val project: Project?,
    private val title: String
) : Task.Modal(project, title, true) {

    private var result: T? = null // 任务执行结果
    private var error: Throwable? = null // 任务执行过程中捕获的错误

    /**
     * 任务的主要逻辑。
     * @param indicator 进度指示器，用于更新任务的状态。
     */
    override fun run(indicator: ProgressIndicator) {
        indicator.isIndeterminate = true // 设置进度为不确定状态
        try {
            result = execute(indicator) // 执行任务逻辑
        } catch (e: Throwable) {
            error = e // 捕获执行过程中的异常
        }
    }

    /**
     * 任务成功完成时的回调。
     */
    override fun onSuccess() {
        result?.let { handleSuccess(it) }
    }

    /**
     * 任务执行过程中发生异常时的回调。
     * @param error 捕获的异常。
     */
    override fun onThrowable(error: Throwable) {
        this.error = error
        handleError(error)
    }

    /**
     * 任务完成（无论成功、失败还是取消）后的回调。
     */
    override fun onFinished() {
        if (result == null && error == null) {
            handleCancelled()
        }
    }

    /**
     * 执行任务的核心逻辑。
     * 需要子类实现此方法以定义任务的实际操作。
     *
     * @param indicator 进度指示器。
     * @return 返回任务执行的结果。
     */
    protected abstract fun execute(indicator: ProgressIndicator): T

    /**
     * 任务成功完成后处理结果的逻辑。
     * 需要子类实现此方法以定义成功处理逻辑。
     *
     * @param result 任务执行的结果。
     */
    protected abstract fun handleSuccess(result: T)

    /**
     * 处理任务执行过程中发生的异常。
     * 子类可重写此方法以提供自定义的错误处理逻辑。
     *
     * @param error 捕获的异常。
     */
    protected open fun handleError(error: Throwable) {
        error.printStackTrace() // 默认打印错误堆栈
    }

    /**
     * 处理任务被取消的情况。
     * 子类可重写此方法以定义取消任务时的逻辑。
     */
    protected open fun handleCancelled() {
        // 默认情况下不执行任何操作
    }
}
