package com.huawei.cangjie.idea.run.cjpm


import com.huawei.cangjie.lang.CangJieFileType
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.json.JsonFileType
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class CjpmRunConfigurationProducer : LazyRunConfigurationProducer<CjpmRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return CjpmRunConfigurationType.instance
    }

    override fun setupConfigurationFromContext(
        configuration: CjpmRunConfiguration, context: ConfigurationContext, sourceElement: Ref<PsiElement>
    ): Boolean {
//如果该项目有运行配置中command为run的运行配置，就使用这个运行配置，否则就创建一个run的运行配置



        val file = context.location?.virtualFile
        val fileName = file?.name
        val fileType = file?.fileType

        val isInSrc = file?.path?.contains("/src/") == true


        if (fileType !is CangJieFileType && (fileType !is JsonFileType && fileName?.startsWith("module") == false)) {
            return false
        } else if (fileType is CangJieFileType && !isInSrc) {
            return false
        }

        // 获取项目
        val project = context.project
        // 获取所有的运行配置
        val runManager = RunManager.getInstance(project)
//        val configurations = runManager.allConfigurationsList.filterIsInstance<CjpmRunConfiguration>()

        // 查找是否存在 command 为 run 的运行配置
//        val runConfiguration = configurations.find { it.command == CjpmCommand.RUN }
//return false
//        return if (runConfiguration != null) {
//             如果存在，则使用这个运行配置
//            configuration.name = runConfiguration.name
//            configuration.command = runConfiguration.command
//            sourceElement.set(context.psiLocation)
//            true
//        } else {
        // 如果不存在，则创建一个新的运行配置
        configuration.name = project.name
        configuration.command = CjpmCommand.RUN
        sourceElement.set(context.psiLocation)
        return true
//            true
//        }
    }

    override fun isConfigurationFromContext(
        configuration: CjpmRunConfiguration, context: ConfigurationContext
    ): Boolean {
//  判断是否是cjpm项目
//        文件扩展名为.cj
//        文件为module.json


        val file = context.location?.virtualFile
        val fileName = file?.name
        val fileType = file?.fileType

//该文件是否处于src目录及其子目录下
        val isInSrc = file?.path?.contains("/src/") == true



        return if (isInSrc) {
            fileType is CangJieFileType && configuration.command == CjpmCommand.RUN
        } else {
            //    判断是否是module.json文件
            fileType is JsonFileType && fileName?.startsWith("module") == true && configuration.command == CjpmCommand.RUN
        }


    }


}
