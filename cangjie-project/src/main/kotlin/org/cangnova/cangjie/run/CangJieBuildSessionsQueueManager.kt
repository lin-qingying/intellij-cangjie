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

package org.cangnova.cangjie.run

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update

/**
 * Manages a queue of build sessions to ensure builds run sequentially.
 * This prevents multiple builds from running simultaneously and interfering with each other.
 */
@Service(Service.Level.PROJECT)
class CangJieBuildSessionsQueueManager(private val project: Project) {

    companion object {
        fun getInstance(project: Project): CangJieBuildSessionsQueueManager = project.service()
    }

    /**
     * Background task queue that executes build tasks sequentially
     */
    val buildSessionsQueue: MergingUpdateQueue = MergingUpdateQueue(
        "CangJie Build Queue",
        300,
        true,
        null,
        project,
        null,
        false
    ).apply {
        isPassThrough = false
    }

    /**
     * Queue a build task for execution
     */
    fun queue(task: Runnable) {
        buildSessionsQueue.queue(object : Update(task) {
            override fun run() {
                task.run()
            }
        })
    }
}