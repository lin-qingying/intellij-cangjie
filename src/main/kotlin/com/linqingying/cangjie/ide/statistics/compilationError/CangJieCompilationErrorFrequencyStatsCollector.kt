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

package com.linqingying.cangjie.ide.statistics.compilationError

import com.linqingying.cangjie.psi.CjFile
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.StatisticsUploadAssistant
import com.intellij.openapi.application.ApplicationManager

//private val isEnabled: Boolean
//    get() = ApplicationManager.getApplication().run {
//        !isUnitTestMode && !isHeadlessEnvironment && StatisticsUploadAssistant.isCollectAllowedOrForced()
//    }

//object CangJieCompilationErrorFrequencyStatsCollector: CounterUsagesCollector()  {
//    override fun getGroup(): EventLogGroup = group
//
//    private const val CODE_IS_TOTALLY_BROKEN_NUMBER_OF_COMPILATION_ERRORS_IN_FILE_LOWER_BOUND = 21
//
//    private val group = EventLogGroup("cangjie.compilation.error", 2)
//
//    private val compilationErrorIdField =
//        EventFields.StringValidatedByCustomRule("error_id", CangJieCompilationErrorIdValidationRule::class.java)
//
//    private val event = group.registerEvent("error.happened", compilationErrorIdField)
//
//
//    fun recordCompilationErrorsHappened(diagnosticsFactoryNames: Sequence<String>, psiFile: CjFile) {
//        if (!isEnabled) return
//        if (!psiFile.isWritable) return // We are interested only in compilation errors users make themselves
//        val collected =
//            diagnosticsFactoryNames.take(CODE_IS_TOTALLY_BROKEN_NUMBER_OF_COMPILATION_ERRORS_IN_FILE_LOWER_BOUND).toList()
//        if (collected.size >= CODE_IS_TOTALLY_BROKEN_NUMBER_OF_COMPILATION_ERRORS_IN_FILE_LOWER_BOUND) return
//        CangJieCompilationErrorProcessedFilesTimeStampRecorder.getInstance(psiFile.project)
//            .keepOnlyIfHourPassedAndRecordTimestamps(psiFile.virtualFile, collected)
//            .forEach(event::log)
//    }
//}
