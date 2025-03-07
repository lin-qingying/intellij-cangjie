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

package cn.cangnova.cangjie.ide.run.modulejson


import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import cn.cangnova.cangjie.ide.run.cjpm.CjpmCommandConfigurationType
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys


class CjpmUpdateAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {

        val project = e.project ?: return

        // 获取 RunManager
        val runManager = RunManager.getInstance(project)

// 查找 command 为 UPDATE 的运行配置
        var runConfiguration = runManager.allSettings.find {
            it.configuration is CjpmCommandConfiguration && (it.configuration as CjpmCommandConfiguration).command == "update"
        }

        // 如果没有找到，就创建一个新的运行配置
        if (runConfiguration == null) {
            runConfiguration = runManager.createConfiguration("update", CjpmCommandConfigurationType.instance.factory)
            (runConfiguration.configuration as CjpmCommandConfiguration).command = "update"
            runManager.addConfiguration(runConfiguration)
        }

        // 设置新的运行配置为默认运行配置
//        runManager.selectedConfiguration = runConfiguration

        // 执行运行配置
        val executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
        ProgramRunnerUtil.executeConfiguration(runConfiguration, executor)


    }


    override fun update(e: AnActionEvent) {

        val file = e.getData(PlatformDataKeys.VIRTUAL_FILE)


        val fileName = file?.name

        val fileType = file?.fileType

        val isCjpmModuleJson = fileName?.startsWith("module") == true && fileType?.name == "JSON"


        e.presentation.isEnabledAndVisible = isCjpmModuleJson


    }
}
