package com.linqingying.cangjie.ide.statistics.compilationError

import com.linqingying.cangjie.psi.CjFile
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.StatisticsUploadAssistant
import com.intellij.openapi.application.ApplicationManager

private val isEnabled: Boolean
    get() = ApplicationManager.getApplication().run {
        !isUnitTestMode && !isHeadlessEnvironment && StatisticsUploadAssistant.isCollectAllowedOrForced()
    }

object CangJieCompilationErrorFrequencyStatsCollector: CounterUsagesCollector()  {
    override fun getGroup(): EventLogGroup = group

    private const val CODE_IS_TOTALLY_BROKEN_NUMBER_OF_COMPILATION_ERRORS_IN_FILE_LOWER_BOUND = 21

    private val group = EventLogGroup("cangjie.compilation.error", 2)

    private val compilationErrorIdField =
        EventFields.StringValidatedByCustomRule("error_id", CangJieCompilationErrorIdValidationRule::class.java)

    private val event = group.registerEvent("error.happened", compilationErrorIdField)


    fun recordCompilationErrorsHappened(diagnosticsFactoryNames: Sequence<String>, psiFile: CjFile) {
        if (!isEnabled) return
        if (!psiFile.isWritable) return // We are interested only in compilation errors users make themselves
        val collected =
            diagnosticsFactoryNames.take(CODE_IS_TOTALLY_BROKEN_NUMBER_OF_COMPILATION_ERRORS_IN_FILE_LOWER_BOUND).toList()
        if (collected.size >= CODE_IS_TOTALLY_BROKEN_NUMBER_OF_COMPILATION_ERRORS_IN_FILE_LOWER_BOUND) return
        CangJieCompilationErrorProcessedFilesTimeStampRecorder.getInstance(psiFile.project)
            .keepOnlyIfHourPassedAndRecordTimestamps(psiFile.virtualFile, collected)
            .forEach(event::log)
    }
}
