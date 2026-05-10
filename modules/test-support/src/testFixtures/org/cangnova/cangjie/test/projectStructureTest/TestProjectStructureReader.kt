/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test.projectStructureTest

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Path

internal object TestProjectStructureReader {
    fun readJsonFile(jsonFile: Path): JsonObject {
        @Suppress("DEPRECATION")
        val json = JsonParser().parse(FileUtil.loadFile(jsonFile.toFile(), true))
        require(json is JsonObject)
        return json
    }

    fun <S : TestProjectStructure> parseTestStructure(
        json: JsonObject,
        parser: TestProjectStructureParser<S>,
    ): S {
        val libraries = json.getAsJsonArray(TestProjectStructureFields.LIBRARIES_FIELD)
            ?.map(TestProjectLibraryParser::parse)
            .orEmpty()
        val modules = json.getAsJsonArray(TestProjectStructureFields.MODULES_FIELD)
            ?.map(TestProjectModuleParser::parse)
            .orEmpty()
        return parser.parse(libraries, modules, json)
    }
}

