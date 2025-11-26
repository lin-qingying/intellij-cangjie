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

package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.execution.ExecutionException
import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.breakpoint.DebugPausePoint
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.protocol.ProtobufFactory
import org.cangnova.cangjie.protodebugger.services.SteppingService
import org.cangnova.cangjie.protodebugger.transport.MessageBus


/**
 * 步进控制服务实现
 */
class SteppingServiceImpl(
    private val messageBus: MessageBus,
    private val driverFacade: DebuggerDriverFacade,
    private val capabilities: Long
) : SteppingService {

    override suspend fun resume(): Boolean {

        val request = ProtobufFactory.resume()
        val response = messageBus.request(
            request,
             ResponseOuterClass.ContinueResponse::class.java
        )
        return response.status.success
    }

    override suspend fun suspend(): Boolean {
        TODO()
        val request = ProtobufFactory.suspend()
        val response = messageBus.request(
            request,
             ResponseOuterClass.SuspendResponse::class.java
        )
        return response.status.success
    }

    override suspend fun stepInto(
        thread: LLDBThread,
        forceStepIntoFramesWithNoDebugInfo: Boolean,
        stepByInstruction: Boolean
    ) {

        val request = ProtobufFactory.step(
            thread.id,
            stepByInstruction
        )

        val response = messageBus.request(
            request,
             ResponseOuterClass.StepIntoResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.step.into")
            )
        }
    }

    override suspend fun stepOver(
        thread: LLDBThread,
        stepByInstruction: Boolean
    ) {

        val request = ProtobufFactory.stepOver(thread.id, stepByInstruction)

        val response = messageBus.request(
            request,
            ResponseOuterClass.StepOverResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.step.over")
            )
        }
    }

    override suspend fun stepOut(
        thread: LLDBThread,
        stopInFramesWithNoDebugInfo: Boolean
    ) {

        val request = ProtobufFactory.stepOut(thread.id)

        val response = messageBus.request(
            request,
             ResponseOuterClass.StepOutResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.step.out")
            )
        }
    }

    override suspend fun runToAddress(address: Address) {
        val thread = driverFacade.stateManager.getStoppedThread()
            ?: throw ExecutionException(
                DebuggerBundle.message("error.no.stopped.thread")
            )

        val request = ProtobufFactory.runToAddress(thread.id, address.value)

        val response = messageBus.request(
            request,
            ResponseOuterClass.RunToCursorResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.run.to.address")
            )
        }
    }


    override suspend fun runToLine(path: String, line: Int) {
        val thread = driverFacade.stateManager.getStoppedThread()
            ?: throw ExecutionException(
                DebuggerBundle.message("error.no.stopped.thread")
            )

        val request = ProtobufFactory.runToLine(thread.id, path, line)

        val response = messageBus.request(
            request,
            ResponseOuterClass.RunToCursorResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.run.to.line")
            )
        }
    }

    override suspend fun jumpToAddress(
        thread: LLDBThread,
        address: Address,
        canLeaveFunction: Boolean
    ): DebugPausePoint {
        TODO()
//        val request = ProtobufFactory.jumpToAddress(
//            thread.id,
//            address.unsignedLongValue,
//            canLeaveFunction
//        )
//
//        val response = messageBus.request(
//            request,
//            ProtocolResponses.JumpToAddressResponse::class.java
//        )
//
//        if (!response.status.success) {
//            throw DebuggerCommandExceptionException(
//                DebuggerBundle.message("error.invalid.response")
//            )
//        }
//
//        val newFrame = createLLDBFrame(response.currentFrame)
//        return DebugPausePoint(thread, newFrame)
    }

    override suspend fun jumpToLine(
        thread: LLDBThread,
        path: String,
        line: Int,
        canLeaveFunction: Boolean
    ): DebugPausePoint {
        TODO()
//        val convertedPath = configuration.convertToProjectModelPath(path)
//        val request = ProtobufFactory.jumpToLine(
//            thread.id,
//            convertedPath,
//            line + 1,
//            canLeaveFunction
//        )
//
//        val response = messageBus.request(
//            request,
//            ProtocolResponses.JumpToLineResponse::class.java
//        )
//
//        if (!response.status.success) {
//            throw DebuggerCommandExceptionException(
//                DebuggerBundle.message("error.invalid.response")
//            )
//        }
//
//        val newFrame = createLLDBFrame(response.currentFrame)
//        return DebugPausePoint(thread, newFrame)
    }

    override suspend fun freezeThread(thread: LLDBThread) {
        val request = ProtobufFactory.freezeThread(thread.id)
        messageBus.send(request)
    }

    override suspend fun unfreezeThread(thread: LLDBThread) {
        val request = ProtobufFactory.unfreezeThread(thread.id)
        messageBus.send(request)
    }

    override suspend fun freezeOtherThreads(thread: LLDBThread) {
        // TODO: Implement when protocol supports freezeOtherThreads
        throw UnsupportedOperationException("freezeOtherThreads not yet implemented")
    }

    override suspend fun unfreezeAllThreads(thread: LLDBThread) {
        // TODO: Implement when protocol supports unfreezeAllThreads
        throw UnsupportedOperationException("unfreezeAllThreads not yet implemented")
    }

    override fun supportsJumpToLine(): Boolean {
        return false // 根据实际能力返回
    }

    override fun supportsFreezeOtherThreads(): Boolean {
        return (capabilities and CAPABILITY_FREEZE_OTHER_THREADS) != 0L
    }

    override fun supportsFreezeSingleThread(): Boolean {
        return (capabilities and CAPABILITY_FREEZE_SINGLE_THREAD) != 0L
    }


    companion object {
        private const val CAPABILITY_FREEZE_OTHER_THREADS = 1L shl 2
        private const val CAPABILITY_FREEZE_SINGLE_THREAD = 1L shl 3
    }
}
