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

package org.cangnova.cangjie.debugger.protobuf.services.impl

import lldbprotobuf.ResponseOuterClass
import org.cangnova.cangjie.debugger.protobuf.breakpoint.AddBreakpointResult
import org.cangnova.cangjie.debugger.protobuf.core.DebuggerDriverFacade
import org.cangnova.cangjie.debugger.protobuf.data.makeBreakpoint
import org.cangnova.cangjie.debugger.protobuf.exception.DebuggerCommandExceptionException
import org.cangnova.cangjie.debugger.protobuf.memory.Address
import org.cangnova.cangjie.debugger.protobuf.messages.ProtoDebuggerBundle
import org.cangnova.cangjie.debugger.protobuf.protocol.ProtobufFactory
import org.cangnova.cangjie.debugger.protobuf.services.BreakpointService
import org.cangnova.cangjie.debugger.protobuf.transport.MessageBus


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

        val request = ProtobufFactory.addAddressBreakpoint(address.asLong, condition)

        val response = messageBus.request(
            request,
            ResponseOuterClass.AddBreakpointResponse::class.java
        )

        return makeBreakpoint(response.addressBreakpoint.breakpoint, response.addressBreakpoint.locationsList)
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
                throw DebuggerCommandExceptionException(
                    ProtoDebuggerBundle.message("proto.error.cannot.remove.breakpoint")
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
