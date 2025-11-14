package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CompletableDeferred
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.breakpoint.AddBreakpointResult
import org.cangnova.cangjie.protodebugger.breakpoint.SymbolicBreakpoint
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverConfiguration
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.services.SessionService
import org.cangnova.cangjie.protodebugger.data.LLSymbolicBreakpoint
import org.cangnova.cangjie.protodebugger.data.LLValue
import org.cangnova.cangjie.protodebugger.data.LLWatchpoint
import org.cangnova.cangjie.protodebugger.data.getValueId
import org.cangnova.cangjie.protodebugger.data.makeBreakpoint
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.protocol.ProtobufMessageFactory
import org.cangnova.cangjie.protodebugger.services.BreakpointService
import org.cangnova.cangjie.protodebugger.transport.MessageBus
import proto.Protocol
import proto.ProtocolResponses

/**
 * 断点服务实现
 */
class BreakpointServiceImpl(
    private val facade: DebuggerDriverFacade,
    private val messageBus: MessageBus,
    private val configuration: DebuggerDriverConfiguration,
    private val capabilities: Long
) : BreakpointService {

    /**
     * 等待目标创建完成
     */
    private suspend fun waitForTargetCreation() {
//        if (!facade.sessionService.isTargetCreationComplete()) {
//            val result = facade.sessionService.waitForTargetCreation()
//            if (!result.await()) {
//                throw IllegalStateException("Target creation failed, cannot execute breakpoint operations")
//            }
//        }
    }

    override suspend fun addLineBreakpoint(
        path: String,
        line: Int,
        condition: String?
    ): AddBreakpointResult {
        // 等待目标创建完成
        waitForTargetCreation()

        val convertedPath = configuration.convertToProjectModelPath(path)
        val request = ProtobufMessageFactory.addBreakpoint(
            convertedPath,
            line + 1,
            false,
            condition
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.AddBreakpointResponse::class.java
        )

        return makeBreakpoint(response.breakpoint, response.locationsList)
    }

    override suspend fun addAddressBreakpoint(
        address: Address,
        condition: String?
    ): AddBreakpointResult {
        // 等待目标创建完成
        waitForTargetCreation()

        val request = ProtobufMessageFactory.addBreakpoint(address.unsignedLongValue, condition)

        val response = messageBus.request(
            request,
            ProtocolResponses.AddBreakpointResponse::class.java
        )

        return makeBreakpoint(response.breakpoint, response.locationsList)
    }

    override suspend fun addSymbolicBreakpoint(
        symbolPattern: String,
        module: String?,
        condition: String?
    ): LLSymbolicBreakpoint {
        // 等待目标创建完成
        waitForTargetCreation()

        val breakpoint = SymbolicBreakpoint(
            pattern = symbolPattern,
            module = module,
            condition = condition
        )
        return addSymbolicBreakpoint(breakpoint)
    }

    override suspend fun addSymbolicBreakpoint(
        breakpoint: SymbolicBreakpoint
    ): LLSymbolicBreakpoint {
        // 等待目标创建完成
        waitForTargetCreation()

        val request = ProtobufMessageFactory.addBreakpoint(
            breakpoint.pattern,
            breakpoint.isRegexpPattern,
            breakpoint.module,
            breakpoint.condition,
            breakpoint.threadId
        )

        val response = messageBus.request(
            request,
            ProtocolResponses.AddBreakpointResponse::class.java
        )

        return LLSymbolicBreakpoint(
            id = response.breakpoint.id,
            symbolPattern = breakpoint.pattern,
            condition = breakpoint.condition,
            enabled = true
        )
    }

    override suspend fun addWatchpoint(
        threadId: Long,
        frameIndex: Int,
        value: LLValue,
        expr: String,
        lifetime: LLWatchpoint.Lifetime?,
        accessType: LLWatchpoint.AccessType
    ): LLWatchpoint {
        // 等待目标创建完成
        waitForTargetCreation()

        val expression: String = value.referenceExpression

        val request: Protocol.CompositeRequest = ProtobufMessageFactory.addWatchpoint(
            getValueId(value),

            accessType === LLWatchpoint.AccessType.ANY || accessType === LLWatchpoint.AccessType.READ,
            accessType === LLWatchpoint.AccessType.ANY || accessType === LLWatchpoint.AccessType.WRITE,
            true
        )
        val result = Ref<LLWatchpoint>()
        val error = Ref<DebuggerCommandException>()

        val response = messageBus.request(
            request,
            ProtocolResponses.AddWatchpointResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.errorMessage)
        }

        return LLWatchpoint(response.watchpointId, expression)
    }

    override suspend fun removeBreakpoints(ids: Collection<Int>) {
        // 等待目标创建完成
        waitForTargetCreation()

        for (id in ids) {
            val request = ProtobufMessageFactory.removeBreakpoint(id)
            val response = messageBus.request(
                request,
                ProtocolResponses.RemoveBreakpointResponse::class.java
            )

            if (!response.status.success) {
                throw DebuggerCommandException(
                    DebuggerBundle.message("error.cannot.remove.breakpoint")
                )
            }
        }
    }

    override suspend fun removeWatchpoints(ids: List<Int>) {
        // 等待目标创建完成
        waitForTargetCreation()

        for (id in ids) {
            val request = ProtobufMessageFactory.removeWatchpoint(id)
            val response = messageBus.request(
                request,
                ProtocolResponses.RemoveWatchpointResponse::class.java
            )

            if (!response.status.success) {
                throw DebuggerCommandException(
                    DebuggerBundle.message("error.cannot.remove.watchpoint")
                )
            }
        }
    }

    override fun supportsWatchpoints(): Boolean {
        return (capabilities and CAPABILITY_WATCHPOINTS) != 0L
    }

    override fun supportsWatchpointLifetime(): Boolean {
        return (capabilities and CAPABILITY_WATCHPOINT_LIFETIME) != 0L
    }


    companion object {
        private const val CAPABILITY_WATCHPOINTS = 1L shl 0
        private const val CAPABILITY_WATCHPOINT_LIFETIME = 1L shl 1
    }
}
