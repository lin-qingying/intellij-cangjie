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

import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CangJieStdlibProjectDescriptorSmokeTest : CangJieLightCodeInsightFixtureTestCase() {
    @ProjectDescriptorKind(CANGJIE_WITH_STDLIB)
    fun testStdlibDescriptorConfiguresProjectSdkAndStdlibLayout() {
        val sdk = assertNotNull(CjProjectSdkConfig.getInstance(project).getProjectSdk())
        val sdkHome = sdk.homePath

        assertTrue(sdkHome.resolve("modules").resolve("windows_x86_64_llvm").resolve("std.cjo").toFile().isFile)
        assertTrue(sdkHome.resolve("modules").resolve("windows_x86_64_llvm").resolve("std").resolve("std.core.cjo").toFile().isFile)
        assertTrue(sdkHome.resolve("modules").resolve("windows_x86_64_llvm").resolve("std").resolve("std.ast.cjo").toFile().isFile)
        assertTrue(sdkHome.resolve("modules").resolve("windows_x86_64_llvm").resolve("std").resolve("std.objectpool.cjo").toFile().isFile)
    }
}
