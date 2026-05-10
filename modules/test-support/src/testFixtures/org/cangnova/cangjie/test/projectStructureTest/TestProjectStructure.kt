/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test.projectStructureTest

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.util.UUID

interface TestProjectStructure {
    val libraries: List<TestProjectLibrary>
    val modules: List<TestProjectModule>
}

object TestProjectStructureFields {
    const val IS_DISABLED_FIELD: String = "is_disabled"
    const val LIBRARIES_FIELD: String = "libraries"
    const val MODULES_FIELD: String = "modules"
}

fun interface TestProjectStructureParser<S : TestProjectStructure> {
    fun parse(libraries: List<TestProjectLibrary>, modules: List<TestProjectModule>, json: JsonObject): S
}

data class TestProjectLibrary(val name: String, val roots: List<String>)

object TestProjectLibraryParser {
    private const val ROOTS_FIELD = "roots"
    private const val NAME_FIELD = "name"

    fun parse(json: JsonElement): TestProjectLibrary {
        require(json is JsonObject)
        val roots = json.getStringList(ROOTS_FIELD) ?: listOf(UUID.randomUUID().toString())
        return TestProjectLibrary(json.getRequiredString(NAME_FIELD), roots)
    }
}

data class TestProjectModule(
    val name: String,
    val platform: String,
    val dependencies: List<Dependency>,
    val contentRoots: List<TestContentRoot>?,
)

data class Dependency(
    val name: String,
    val kind: DependencyKind,
    val scope: DependencyScope,
    val isExported: Boolean,
    val productionOnTest: Boolean,
)

enum class DependencyKind {
    REGULAR,
    FRIEND,
    REFINEMENT,
}

enum class DependencyScope {
    COMPILE,
    TEST,
    RUNTIME,
    PROVIDED,
}

data class TestContentRoot(
    val path: String?,
    val kind: TestContentRootKind,
)

enum class TestContentRootKind(val defaultDirectoryName: String, val alternativeName: String? = null) {
    PRODUCTION("src"),
    TESTS("test"),
    RESOURCES("resources"),
    TEST_RESOURCES("testResources", alternativeName = "testResources"),
}

object TestProjectModuleParser {
    private const val DEPENDENCIES_FIELD = "dependencies"
    private const val CONTENT_ROOTS_FIELD = "content_roots"
    private const val REFINEMENT_DEPENDENCIES_FIELD = "refinement_dependencies"
    private const val FRIEND_DEPENDENCIES_FIELD = "friend_dependencies"
    private const val MODULE_NAME_FIELD = "name"
    private const val PLATFORM_FIELD = "platform"
    private const val CONTENT_ROOT_PATH_FIELD = "path"
    private const val CONTENT_ROOT_KIND_FIELD = "kind"

    fun parse(json: JsonElement): TestProjectModule {
        require(json is JsonObject)
        val dependencies = buildList {
            addAll(parseDependencies(json, DEPENDENCIES_FIELD, DependencyKind.REGULAR))
            addAll(parseDependencies(json, REFINEMENT_DEPENDENCIES_FIELD, DependencyKind.REFINEMENT))
            addAll(parseDependencies(json, FRIEND_DEPENDENCIES_FIELD, DependencyKind.FRIEND))
        }

        return TestProjectModule(
            name = json.getRequiredString(MODULE_NAME_FIELD),
            platform = json.getNullableString(PLATFORM_FIELD) ?: "cangjie",
            dependencies = dependencies,
            contentRoots = json.getAsJsonArray(CONTENT_ROOTS_FIELD)?.let(::parseContentRoots),
        )
    }

    private fun parseDependencies(json: JsonObject, jsonField: String, dependencyKind: DependencyKind): List<Dependency> {
        return json.getAsJsonArray(jsonField)?.map { dependency ->
            when (dependency) {
                is JsonPrimitive -> Dependency(
                    name = dependency.asString,
                    kind = dependencyKind,
                    scope = DependencyScope.COMPILE,
                    isExported = false,
                    productionOnTest = false,
                )

                is JsonObject -> Dependency(
                    name = dependency.getRequiredString("name"),
                    kind = dependencyKind,
                    scope = dependency.getNullableString("scope")?.let(DependencyScope::valueOf) ?: DependencyScope.COMPILE,
                    isExported = dependency.get("exported")?.asBoolean == true,
                    productionOnTest = dependency.get("productionOnTest")?.asBoolean == true,
                )

                else -> error("Unexpected json element type: ${dependency::class.java}")
            }
        }.orEmpty()
    }

    private fun parseContentRoots(json: JsonArray): List<TestContentRoot> = json.map(::parseContentRoot)

    private fun parseContentRoot(element: JsonElement): TestContentRoot = when (element) {
        is JsonObject -> TestContentRoot(
            path = element.getRequiredString(CONTENT_ROOT_PATH_FIELD),
            kind = parseTestContentRootKind(element.getRequiredString(CONTENT_ROOT_KIND_FIELD)),
        )

        is JsonPrimitive -> {
            val kind = parseTestContentRootKind(element.asString)
            TestContentRoot(kind.defaultDirectoryName, kind)
        }

        else -> error("Unexpected content root JSON element type: ${element::class.java}")
    }

    private fun parseTestContentRootKind(name: String): TestContentRootKind {
        return TestContentRootKind.entries.firstOrNull { kind ->
            kind.alternativeName == name || kind.name == name.uppercase()
        } ?: error("Unexpected content root kind: $name")
    }
}

internal fun JsonObject.getRequiredString(name: String): String =
    requireNotNull(get(name)?.asString) { "Missing required JSON field `$name`" }

internal fun JsonObject.getNullableString(name: String): String? = get(name)?.asString

internal fun JsonObject.getStringList(name: String): List<String>? =
    getAsJsonArray(name)?.map { element -> element.asString }

