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

package org.cangnova.cangjie.debugger.service

import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cangnova.cangjie.debugger.core.DebugAdapter
import org.cangnova.cangjie.debugger.core.Scope
import org.cangnova.cangjie.debugger.core.Variable
import org.cangnova.cangjie.debugger.exception.VariableException

/**
 * 变量服务
 *
 * 管理变量的查看和展开
 */
class VariableService(
    private val adapter: DebugAdapter
) {

    companion object {
        private val LOG = Logger.getInstance(VariableService::class.java)
    }

    /**
     * 获取作用域
     */
    suspend fun getScopes(frameId: Long): Result<List<Scope>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting scopes for frame: $frameId")

                val scopes = adapter.getScopes(frameId).getOrThrow()

                LOG.debug("Retrieved ${scopes.size} scopes for frame $frameId")
                Result.success(scopes)
            } catch (e: Exception) {
                LOG.error("Failed to get scopes", e)
                Result.failure(VariableException("Failed to get scopes", e))
            }
        }
    }

    /**
     * 获取变量
     */
    suspend fun getVariables(variablesReference: Long): Result<List<Variable>> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.debug("Getting variables: reference=$variablesReference")

                val variables = adapter.getVariables(variablesReference).getOrThrow()

                LOG.debug("Got ${variables.size} variables")
                Result.success(variables)
            } catch (e: Exception) {
                LOG.error("Failed to get variables", e)
                Result.failure(VariableException("Failed to get variables", e))
            }
        }
    }

    /**
     * 设置变量值
     */
    suspend fun setVariable(
        variablesReference: Long,
        name: String,
        value: String
    ): Result<Variable> {
        return withContext(Dispatchers.IO) {
            try {
                LOG.info("Setting variable: $name = $value")

                val variable = adapter.setVariable(variablesReference, name, value).getOrThrow()

                LOG.debug("Variable set successfully: $variable")
                Result.success(variable)
            } catch (e: Exception) {
                LOG.error("Failed to set variable", e)
                Result.failure(VariableException("Failed to set variable", e))
            }
        }
    }
}