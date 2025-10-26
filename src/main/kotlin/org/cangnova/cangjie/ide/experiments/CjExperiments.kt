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

package org.cangnova.cangjie.ide.experiments

object  CjExperiments {



    const val PROC_MACROS = "org.cangnova.cangjie.macros.proc"
    @EnabledInStable
    const val FN_LIKE_PROC_MACROS = "org.cangnova.cangjie.macros.proc.function-like"
    @EnabledInStable
    const val DERIVE_PROC_MACROS = "org.cangnova.cangjie.macros.proc.derive"
    const val ATTR_PROC_MACROS = "org.cangnova.cangjie.macros.proc.attr"

    @EnabledInStable
    const val CRATES_LOCAL_INDEX = "org.cangnova.cangjie.crates.local.index"

    @EnabledInStable
    const val WSL_TOOLCHAIN = "org.cangnova.cangjie.wsl"

    const val EMULATE_TERMINAL = "org.cangnova.cangjie.emulate.terminal"

    const val INTENTIONS_IN_FN_LIKE_MACROS = "org.cangnova.cangjie.ide.intentions.macros.function-like"

    const val SSR = "org.cangnova.cangjie.ssr"

    const val SOURCE_BASED_COVERAGE = "org.cangnova.cangjie.coverage.source"

    const val MIR_MOVE_ANALYSIS = "org.cangnova.cangjie.mir.move-analysis"
    const val MIR_BORROW_CHECK = "org.cangnova.cangjie.mir.borrow-check"
}

/**
 * Experimental feature should be annotated with `@EnabledInStable` if it is enabled in stable releases,
 * i.e. it is included in `resources-stable/META-INF/experiments.xml` with `percentOfUsers="100"`.
 *
 * Enabled experimental features without `@EnabledInStable` annotation are intended to be collected in
 * [org.cangnova.cangjie.ide.actions.diagnostic.CreateNewGithubIssue]
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnabledInStable
