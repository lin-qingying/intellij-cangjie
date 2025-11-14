package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.execution.ExecutionException
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.breakpoint.StopPlace
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverConfiguration
import org.cangnova.cangjie.protodebugger.data.LLFrame
import org.cangnova.cangjie.protodebugger.data.LLThread
import org.cangnova.cangjie.protodebugger.data.newLLFrame
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.protocol.ProtobufMessageFactory
import org.cangnova.cangjie.protodebugger.services.SteppingService
import org.cangnova.cangjie.protodebugger.transport.MessageBus
import org.cangnova.cangjie.protodebugger.util.DebuggerSourceFileHash
import proto.Model
import proto.ProtocolResponses

/**
 * 步进控制服务实现
 */
class SteppingServiceImpl(
    private val messageBus: MessageBus,
    private val configuration: DebuggerDriverConfiguration,
    private val capabilities: Long
) : SteppingService {

    override suspend fun resume(): Boolean {
        val request = ProtobufMessageFactory.resume()
        val response = messageBus.request(
            request,
            ProtocolResponses.ContinueResponse::class.java
        )
        return response.status.success
    }

    override suspend fun interrupt(): Boolean {
        val request = ProtobufMessageFactory.suspend()
        val response = messageBus.request(
            request,
            ProtocolResponses.SuspendResponse::class.java
        )
        return response.status.success
    }

    override suspend fun stepInto(
        thread: LLThread,
        forceStepIntoFramesWithNoDebugInfo: Boolean,
        stepByInstruction: Boolean
    ) {
        val request = ProtobufMessageFactory.stepInto(
            thread.id,
            stepByInstruction
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.StepIntoResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.step.into")
            )
        }
    }

    override suspend fun stepOver(
        thread: LLThread,
        stepByInstruction: Boolean
    ) {
        val request = ProtobufMessageFactory.stepOver(thread.id, stepByInstruction)

        val response = messageBus.request(
            request,
            ProtocolResponses.StepOverResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.step.over")
            )
        }
    }

    override suspend fun stepOut(
        thread: LLThread,
        stopInFramesWithNoDebugInfo: Boolean
    ) {
        val request = ProtobufMessageFactory.stepOut(thread.id)

        val response = messageBus.request(
            request,
            ProtocolResponses.StepOutResponse::class.java
        )

        if (!response.status.success) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.step.out")
            )
        }
    }

    override suspend fun runToAddress(address: Address) {
        val request = ProtobufMessageFactory.addBreakpoint(address.unsignedLongValue, null)
        val response = messageBus.request(
            request,
            ProtocolResponses.AddBreakpointResponse::class.java
        )

        // 临时断点，执行后需要移除
        val breakpointId = response.breakpoint.id

        if (!resume()) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.resume.program")
            )
        }
    }

    override suspend fun runToLine(path: String, line: Int) {
        val convertedPath = configuration.convertToProjectModelPath(path)
        val request = ProtobufMessageFactory.addBreakpoint(convertedPath, line + 1, false, null)

        val response = messageBus.request(
            request,
            ProtocolResponses.AddBreakpointResponse::class.java
        )

        // 临时断点，执行后需要移除
        val breakpointId = response.breakpoint.id

        if (!resume()) {
            throw ExecutionException(
                DebuggerBundle.message("error.cannot.resume.program")
            )
        }
    }

    override suspend fun jumpToAddress(
        thread: LLThread,
        address: Address,
        canLeaveFunction: Boolean
    ): StopPlace {
        val request = ProtobufMessageFactory.jumpToAddress(
            thread.id,
            address.unsignedLongValue,
            canLeaveFunction
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.JumpToAddressResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(
                DebuggerBundle.message("error.invalid.response")
            )
        }

        val newFrame = newLLFrame(response.currentFrame)
        return StopPlace(thread, newFrame)
    }

    override suspend fun jumpToLine(
        thread: LLThread,
        path: String,
        line: Int,
        canLeaveFunction: Boolean
    ): StopPlace {
        val convertedPath = configuration.convertToProjectModelPath(path)
        val request = ProtobufMessageFactory.jumpToLine(
            thread.id,
            convertedPath,
            line + 1,
            canLeaveFunction
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.JumpToLineResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(
                DebuggerBundle.message("error.invalid.response")
            )
        }

        val newFrame = newLLFrame(response.currentFrame)
        return StopPlace(thread, newFrame)
    }

    override suspend fun freezeThread(thread: LLThread) {
        val request = ProtobufMessageFactory.freezeThread(thread.id)
        messageBus.send(request)
    }

    override suspend fun unfreezeThread(thread: LLThread) {
        val request = ProtobufMessageFactory.unfreezeThread(thread.id)
        messageBus.send(request)
    }

    override suspend fun freezeOtherThreads(thread: LLThread) {
        // TODO: Implement when protocol supports freezeOtherThreads
        throw UnsupportedOperationException("freezeOtherThreads not yet implemented")
    }

    override suspend fun unfreezeAllThreads(thread: LLThread) {
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
