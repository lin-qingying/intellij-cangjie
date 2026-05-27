/*
 * Copyright 2026 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import org.cangnova.cangjie.CangJiePluginDisposable

/**
 * 对位 Kotlin runtime/stdlib descriptor 家族在仓颉上的真实子集：
 * 带仓颉 stdlib toolchain 的 light project descriptor。
 */
open class CangJieStdlibLightProjectDescriptor(
    private vararg val relativeStdlibPaths: String,
) : CangJieLightProjectDescriptor() {
    override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
        super.configureModule(module, model, contentEntry)

        val sdkHome = CangJiePluginTestCaseBase.createSlimToolchainHome(*relativeStdlibPaths)
        CangJiePluginTestCaseBase.registerProjectToolchain(
            project = module.project,
            parentDisposable = CangJiePluginDisposable.getInstance(module.project),
            sdkHome = sdkHome,
        )
    }

    companion object {
        @JvmField
        val INSTANCE = CangJieStdlibLightProjectDescriptor(
            "std.cjo",
            "std/std.core.cjo",
            "std/std.ast.cjo",
            "std/std.objectpool.cjo",
        )
    }
}
