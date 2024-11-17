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

package com.linqingying.cangjie.ide.experiments

object CjExperiments {
    @EnabledInStable
    const val BUILD_TOOL_WINDOW = "com.linqingying.cangjie.cjpm.build.tool.window"

    @EnabledInStable
    const val EVALUATE_BUILD_SCRIPTS = "com.linqingying.cangjie.cjpm.evaluate.build.scripts"

    const val CJPM_FEATURES_SETTINGS_GUTTER = "com.linqingying.cangjie.cjpm.features.settings.gutter"

    const val PROC_MACROS = "com.linqingying.cangjie.macros.proc"
    @EnabledInStable
    const val FN_LIKE_PROC_MACROS = "com.linqingying.cangjie.macros.proc.function-like"
    @EnabledInStable
    const val DERIVE_PROC_MACROS = "com.linqingying.cangjie.macros.proc.derive"
    const val ATTR_PROC_MACROS = "com.linqingying.cangjie.macros.proc.attr"

    @EnabledInStable
    const val FETCH_ACTUAL_STDLIB_METADATA = "com.linqingying.cangjie.cjpm.fetch.actual.stdlib.metadata"

    @EnabledInStable
    const val CRATES_LOCAL_INDEX = "com.linqingying.cangjie.crates.local.index"

    @EnabledInStable
    const val WSL_TOOLCHAIN = "com.linqingying.cangjie.wsl"

    const val EMULATE_TERMINAL = "com.linqingying.cangjie.cjpm.emulate.terminal"

    const val INTENTIONS_IN_FN_LIKE_MACROS = "com.linqingying.cangjie.ide.intentions.macros.function-like"

    const val SSR = "com.linqingying.cangjie.ssr"

    const val SOURCE_BASED_COVERAGE = "com.linqingying.cangjie.coverage.source"

    const val MIR_MOVE_ANALYSIS = "com.linqingying.cangjie.mir.move-analysis"
    const val MIR_BORROW_CHECK = "com.linqingying.cangjie.mir.borrow-check"
}

/**
 * Experimental feature should be annotated with `@EnabledInStable` if it is enabled in stable releases,
 * i.e. it is included in `resources-stable/META-INF/experiments.xml` with `percentOfUsers="100"`.
 *
 * Enabled experimental features without `@EnabledInStable` annotation are intended to be collected in
 * [com.linqingying.cangjie.ide.actions.diagnostic.CreateNewGithubIssue]
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnabledInStable
