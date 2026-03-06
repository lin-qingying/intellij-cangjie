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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi.stubs.impl

import org.cangnova.cangjie.psi.CjElementImplStub
import org.cangnova.cangjie.psi.stubs.CangJieClassifierStub
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import java.lang.reflect.Method

val STUB_TO_STRING_PREFIX = "CangJieStub$"

open class CangJieStubBaseImpl<T : CjElementImplStub<*>>(parent: StubElement<*>?, elementType: IStubElementType<*, *>) :
    StubBase<T>(parent, elementType) {

    companion object {
        private val LOGGER: Logger = Logger.getInstance(CangJieStubBaseImpl::class.java)
        private val BASE_STUB_INTERFACES = listOf(
//                CangJieStubWithFqName::class.java,
            CangJieClassifierStub::class.java,
            CangJieTypeStatementStub::class.java,
            NamedStub::class.java,
//                CangJieCallableStubBase::class.java
        )
    }

    @Deprecated("Deprecated in Java")
    override fun getStubType(): IStubElementType<out StubElement<*>, *> =
        super.getStubType() as IStubElementType<out StubElement<*>, *>

    private fun renderPropertyValues(stubInterface: Class<out Any?>): List<String> {
        return collectProperties(stubInterface).mapNotNull { property -> renderProperty(property) }.sorted()
    }

    private fun getPropertyName(method: Method): String {
        val methodName = method.name
        if (methodName.startsWith("get")) {
            return methodName.substring(3).replaceFirstChar(Char::lowercaseChar)
        }
        return methodName
    }

    private fun renderProperty(property: Method): String? {
        return try {
            val value = property.invoke(this)
            val name = getPropertyName(property)
            "$name=$value"
        } catch (e: java.lang.reflect.InvocationTargetException) {
            // 打印真正的根因，而不是包装异常
            LOGGER.error("Failed to invoke ${property.name}: ${e.cause}", e.cause)
            null
        } catch (e: Exception) {
            LOGGER.error("Reflection error on ${property.name}", e)
            null
        }
    }

    private fun collectProperties(stubInterface: Class<*>): Collection<Method> {
        val result = ArrayList<Method>()
        result.addAll(stubInterface.declaredMethods.filter { it.parameterTypes.isEmpty() })
        for (baseInterface in stubInterface.interfaces) {
            if (baseInterface in BASE_STUB_INTERFACES) {
                result.addAll(collectProperties(baseInterface))
            }
        }
        return result
    }

    override fun toString(): String {
        val stubInterface = this::class.java.interfaces.single { it.name.contains("Stub") }
        val propertiesValues = renderPropertyValues(stubInterface)
        if (propertiesValues.isEmpty()) {
            return "$STUB_TO_STRING_PREFIX$stubType"
        }
        val properties = propertiesValues.joinToString(separator = ", ", prefix = "[", postfix = "]")
        return "$STUB_TO_STRING_PREFIX$stubType$properties"
    }
}
