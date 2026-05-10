/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VirtualFile

/**
 * 对位 Kotlin `ConfigLibraryUtil` 的仓颉 library 装配工具。
 *
 * 该层只负责 IntelliJ project/module library 的创建、挂载与移除，
 * 不引入 Kotlin runtime/JDK 语义。
 */
object CangJieConfigLibraryUtil {
    fun addProjectLibrary(
        project: Project,
        name: String,
        configure: Library.ModifiableModel.() -> Unit,
    ): Library = runWriteAction {
        val library = LibraryTablesRegistrar.getInstance().getLibraryTable(project).createLibrary(name)
        library.modifiableModel.apply {
            configure()
            commit()
        }
        library
    }

    fun addLibrary(
        module: Module,
        name: String,
        classesRoot: VirtualFile,
        sourcesRoot: VirtualFile? = null,
        dependencyScope: DependencyScope = DependencyScope.COMPILE,
        exported: Boolean = false,
    ): Library {
        val library = addProjectLibrary(module.project, name) {
            addRoot(classesRoot, OrderRootType.CLASSES)
            if (sourcesRoot != null) {
                addRoot(sourcesRoot, OrderRootType.SOURCES)
            }
        }
        module.addDependency(library, dependencyScope, exported)
        return library
    }

    fun removeLibrary(module: Module, libraryName: String) {
        runWriteAction {
            val rootModel = ModuleRootManager.getInstance(module).modifiableModel
            val orderEntry = rootModel.orderEntries.firstOrNull { entry ->
                entry.presentableName == libraryName
            }
            if (orderEntry != null) {
                rootModel.removeOrderEntry(orderEntry)
            }
            rootModel.commit()

            LibraryTablesRegistrar.getInstance().getLibraryTable(module.project).libraries
                .firstOrNull { library -> library.name == libraryName }
                ?.let { library ->
                    LibraryTablesRegistrar.getInstance().getLibraryTable(module.project).removeLibrary(library)
                }
        }
    }
}
