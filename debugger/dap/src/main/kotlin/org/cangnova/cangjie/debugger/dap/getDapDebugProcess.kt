///*
// * Copyright 2025 LinQingYing. and contributors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// * The use of this source code is governed by the Apache License 2.0,
// * which allows users to freely use, modify, and distribute the code,
// * provided they adhere to the terms of the license.
// *
// * The software is provided "as-is", and the authors are not responsible for
// * any damages or issues arising from its use.
// *
// */
//
//package org.cangnova.cangjie.debugger.dap
//
//import com.intellij.execution.runners.ExecutionEnvironment
//import com.intellij.xdebugger.XDebugProcess
//import com.intellij.xdebugger.XDebugSession
//import com.redhat.devtools.lsp4ij.dap.DAPDebugProcess
//import com.redhat.devtools.lsp4ij.dap.configurations.DAPCommandLineState
//import org.cangnova.cangjie.run.CangJieProgramRunConfiguration
//import org.cangnova.cangjie.run.CangJieRunState
//
//fun getDapDebugProcess(
//    state: CangJieRunState<CangJieProgramRunConfiguration>,
//
//    environment: ExecutionEnvironment,
//
//    session: XDebugSession
//): XDebugProcess {
//    return DapDebugProcess1(state, environment, session)
//}
//
//class DapDebugProcess1(
//    state: CangJieRunState<CangJieProgramRunConfiguration>,
//
//    environment: ExecutionEnvironment,
//
//    session: XDebugSession
//) : DAPDebugProcess(
//    createDAPCommandLineState(state, environment),
//    session,
//    state.execute(
//        environment.executor,
//        environment.runner
//    ),
//    true
//) {
//    companion object {
//
//
//        fun createDAPCommandLineState(
//
//            state: CangJieRunState<CangJieProgramRunConfiguration>,
//            environment: ExecutionEnvironment
//        ): DAPCommandLineState {
//
//            return DAPCommandLineState(
//                CangJieDebugAdapterDescriptorFactory.createDebugAdapterDescriptor(
//                    state.configuration.runConfigurationOptions,
//                    environment
//                ),
//                state.configuration.runConfigurationOptions,
//                environment
//            )
//        }
//    }
//
//}