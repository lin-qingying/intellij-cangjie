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

/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.linqingying.cangjie.openapiext

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.TaskInfo
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.ex.StatusBarEx
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.util.ui.TimerUtil

/**
 * Like [BackgroundableProcessIndicator], but allows to specify delay to postpone progress bar displaying in UI
 */
class DelayedBackgroundableProcessIndicator(val task: Task.Backgroundable, delay: Int) :
    ProgressWindow(task.isCancellable, true, task.project, null, task.cancelText) {

    private var statusBar: StatusBarEx? = null
    private var didInitializeOnEdt = false
    private var isDisposed = false

    @Volatile
    private var isFinishCalled = false

    init {
        setOwnerTask(task)
        initializeStatusBar()
    }

    init {
        val timer = TimerUtil.createNamedTimer("DelayedBackgroundableProcessIndicator timer", delay) {
            invokeLater(modalityState) {
                if (isRunning && !isFinishCalled && !isDisposed && !myBackgrounded) {
                    background()
                }
            }
        }
        timer.isRepeats = false
        timer.start()
    }

    private fun initializeStatusBar() {
        if (isDisposed || didInitializeOnEdt) return
        didInitializeOnEdt = true
        title = task.title
        if (statusBar == null) {
            val nonDefaultProject = if (task.project == null || task.project.isDisposed || task.project.isDefault) null else task.project
            @Suppress("UnstableApiUsage")
            val frame: IdeFrame? = WindowManagerEx.getInstanceEx().findFrameHelper(nonDefaultProject)
            statusBar = if (frame != null) frame.statusBar as StatusBarEx? else null
        }
    }

    override fun background() {
        if (isDisposed) return
        assert(didInitializeOnEdt) { "Call to background action before showing dialog" }
        task.processSentToBackground()
        doBackground(statusBar)
        super.background()
    }

    private fun doBackground(statusBar: StatusBarEx?) {
        statusBar?.addProgress(this, task)
    }

    override fun prepareShowDialog() {
        // Don't show the modal window
    }

    override fun showDialog() {
        // Don't show the modal window
    }

    override fun finish(task: TaskInfo) {
        isFinishCalled = true
        super.finish(task)
    }

    override fun dispose() {
        super.dispose()
        isDisposed = true
    }
}
