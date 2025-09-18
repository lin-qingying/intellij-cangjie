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
package org.cangnova.cangjie.jsonSchema

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import org.cangnova.cangjie.utils.jsonSchema.SimpleJsonSchemaFileProvider

/**
 * 提供 cjpm.toml 的 json schema
 *
 * @author <a href="mailto:yms_hi@Outlook.com" rel="nofollow">yms</a>
 */
class SimpleJsonShemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): List<JsonSchemaFileProvider?> {
        val providers = arrayListOf<JsonSchemaFileProvider?>()

        providers.add(SimpleJsonSchemaFileProvider("cjpm.toml",
            "/jsonSchema/cjpmProjectConfig.schema.json","CJPM Package Config"))

        return providers
    }
}