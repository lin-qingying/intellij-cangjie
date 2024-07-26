/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package com.huawei.cangjie.debugger.lang

import com.huawei.cangjie.debugger.settings.CjDebuggerSettings
import com.jetbrains.cidr.execution.debugger.CidrFrameTypeDecorator
import com.jetbrains.cidr.execution.debugger.CidrStackFrame
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriverConfiguration
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import com.jetbrains.cidr.execution.debugger.evaluation.CidrPhysicalValue
import com.jetbrains.cidr.execution.debugger.evaluation.renderers.ValueRendererUtils
import com.jetbrains.cidr.toolchains.OSType
import org.jetbrains.annotations.VisibleForTesting


//class CjFrameTypeDecorator(private val frame: CidrStackFrame) : CidrFrameTypeDecorator {
//    override fun getValueDisplayType(value: CidrPhysicalValue, renderForUiLabel: Boolean): String {
//        val shouldDecorate = CjDebuggerSettings.getInstance().decorateMsvcTypeNames
//        val driverConfiguration = frame.process.runParameters.debuggerDriverConfiguration
//
//        if (shouldDecorate && driverConfiguration.isMsvcLldb()) {
//            return decorate(value.type)
//        }
//        return super.getValueDisplayType(value, renderForUiLabel)
//    }
//
//    companion object {
//        @VisibleForTesting
//        fun decorate(typeName: String): String {
//            val typeNameParsed = CjTypeNameParserFacade.parse(typeName)
//                ?: return ValueRendererUtils.shortenTemplateType(typeName)
//            val visitor = CjMSVCTypeNameDecoratorVisitor()
//            typeNameParsed.accept(visitor)
//            return visitor.getDecoratedTypeName()
//        }
//
//        private fun DebuggerDriverConfiguration.isMsvcLldb(): Boolean =
//            hostMachine.osType == OSType.WIN && this is LLDBDriverConfiguration
//    }
//}

