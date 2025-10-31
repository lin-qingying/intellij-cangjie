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

package org.cangnova.cangjie.project

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project


/**
 * Abstract interface for accessing project settings.
 * This interface can be accessed by all modules that depend on the common module.
 */

@Service(Service.Level.PROJECT)

class CangJieSettingsData {


    /**
     * Whether automatic updates are enabled for this project.
     */
    var autoUpdateEnabled: Boolean = false

    /**
     * Whether to use offline mode for package management.
     */
    var useOffline: Boolean = false

    /**
     * Whether to compile all targets.
     */
    var compileAllTargets: Boolean = false

    /**
     * Update settings from CangJieProjectSettingsService
     */
    fun updateFromProjectSettings(
        autoUpdateEnabled: Boolean,
        useOffline: Boolean,
        compileAllTargets: Boolean
    ) {
        this.autoUpdateEnabled = autoUpdateEnabled
        this.useOffline = useOffline
        this.compileAllTargets = compileAllTargets
    }
}

/**
 * Extension property to access project settings from any Project instance.
 * Throws an exception if the settings service is not available.
 */
val Project.cangjieSettingsData: CangJieSettingsData
    get() = service<CangJieSettingsData>()