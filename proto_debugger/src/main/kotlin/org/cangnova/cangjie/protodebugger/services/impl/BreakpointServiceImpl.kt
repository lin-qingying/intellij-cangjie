package org.cangnova.cangjie.protodebugger.services.impl

import com.intellij.openapi.util.Ref
import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.breakpoint.AddBreakpointResult
import org.cangnova.cangjie.protodebugger.breakpoint.SymbolicBreakpoint
import org.cangnova.cangjie.protodebugger.core.DebuggerDriverFacade
import org.cangnova.cangjie.protodebugger.data.LLDBSymbolicBreakpoint
import org.cangnova.cangjie.protodebugger.data.LLDBVariable
import org.cangnova.cangjie.protodebugger.data.LLDBWatchpoint
import org.cangnova.cangjie.protodebugger.data.getValueId
import org.cangnova.cangjie.protodebugger.data.makeBreakpoint
import org.cangnova.cangjie.protodebugger.exception.DebuggerCommandException
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.protocol.ProtobufFactory
import org.cangnova.cangjie.protodebugger.services.BreakpointService
import org.cangnova.cangjie.protodebugger.transport.MessageBus


/**
 * 断点服务实现
 */
class BreakpointServiceImpl(
    private val facade: DebuggerDriverFacade,
    private val messageBus: MessageBus,
    private val driverFacade: DebuggerDriverFacade,
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


        val request = ProtobufFactory.addLineBreakpoint(
            path = path,
            line = line + 1,

            condition = condition
        )

        val response = messageBus.request(
            request,
            ResponseOuterClass.AddBreakpointResponse::class.java
        )


        return makeBreakpoint(response.lineBreakpoint.breakpoint, response.lineBreakpoint.locationsList)
    }

    override suspend fun addAddressBreakpoint(
        address: Address,
        condition: String?
    ): AddBreakpointResult {
        // 等待目标创建完成
        waitForTargetCreation()

        val request = ProtobufFactory.addAddressBreakpoint(address.asLong , condition)

        val response = messageBus.request(
            request,
             ResponseOuterClass.AddBreakpointResponse::class.java
        )

        return makeBreakpoint(response.addressBreakpoint.breakpoint, response.addressBreakpoint.locationsList)
    }

    override suspend fun addSymbolicBreakpoint(
        symbolPattern: String,
        module: String?,
        condition: String?
    ): LLDBSymbolicBreakpoint {
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
    ): LLDBSymbolicBreakpoint {
        // 等待目标创建完成
        waitForTargetCreation()

        val request = ProtobufFactory.addSymbolBreakpoint(
            breakpoint.pattern,
            breakpoint.isRegexpPattern,
            breakpoint.module,
            breakpoint.condition,
            breakpoint.threadId
        )

        val response = messageBus.request(
            request,
            ResponseOuterClass.AddBreakpointResponse::class.java
        )

        return LLDBSymbolicBreakpoint(
            id = response.symbolBreakpoint.breakpoint.id.id,
            symbolPattern = breakpoint.pattern,
            condition = breakpoint.condition,
            enabled = true
        )
    }

    override suspend fun addWatchpoint(
        threadId: Long,
        frameIndex: Int,
        value: LLDBVariable,
        expr: String,
        lifetime: LLDBWatchpoint.Lifetime?,
        accessType: LLDBWatchpoint.AccessType
    ): LLDBWatchpoint {
        // 等待目标创建完成
        waitForTargetCreation()

        val expression: String = value.referenceExpression

        val request = ProtobufFactory.addWatchpoint(
            getValueId(value),

            accessType === LLDBWatchpoint.AccessType.ANY || accessType === LLDBWatchpoint.AccessType.READ,
            accessType === LLDBWatchpoint.AccessType.ANY || accessType === LLDBWatchpoint.AccessType.WRITE,
            true
        )
        val result = Ref<LLDBWatchpoint>()
        val error = Ref<DebuggerCommandException>()

        val response = messageBus.request(
            request,
            ResponseOuterClass.AddBreakpointResponse::class.java
        )

        if (!response.status.success) {
            throw DebuggerCommandException(response.status.message)
        }

        return LLDBWatchpoint(response.breakPointId.id, expression)
    }

    override suspend fun removeBreakpoints(ids: Collection<Long>) {
        // 等待目标创建完成
        waitForTargetCreation()

        for (id in ids) {
            val request = ProtobufFactory.removeBreakpoint(id)
            val response = messageBus.request(
                request,
                 ResponseOuterClass.RemoveBreakpointResponse::class.java
            )

            if (!response.status.success) {
                throw DebuggerCommandException(
                    DebuggerBundle.message("error.cannot.remove.breakpoint")
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

    override fun close() {

    }


    companion object {
        private const val CAPABILITY_WATCHPOINTS = 1L shl 0
        private const val CAPABILITY_WATCHPOINT_LIFETIME = 1L shl 1
    }
}
