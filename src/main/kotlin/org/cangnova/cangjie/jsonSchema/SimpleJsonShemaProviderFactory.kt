package org.cangnova.cangjie.jsonSchema

import com.intellij.openapi.project.Project
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import org.cangnova.cangjie.utils.jsonSchema.SimpleJsonSchemaFileProvider

class SimpleJsonShemaProviderFactory : JsonSchemaProviderFactory {
    override fun getProviders(project: Project): List<JsonSchemaFileProvider?> {
        val providers = arrayListOf<JsonSchemaFileProvider?>()

        providers.add(SimpleJsonSchemaFileProvider("cjpm.toml",
            "/jsonSchema/cjpmProjectConfig.schema.json","CJPM Package Config"))

        return providers
    }
}