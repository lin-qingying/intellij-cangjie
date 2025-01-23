// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;




public abstract class CangJieDirectoryIndex {
  public static CangJieDirectoryIndex getInstance(Project project) {

    return project.getService(CangJieDirectoryIndex.class);
  }

  @NotNull
  public abstract
  Query<VirtualFile> getDirectoriesByPackageName(@NotNull String packageName, boolean includeLibrarySources);

  public Query<VirtualFile> getDirectoriesByPackageName(@NotNull String packageName, @NotNull GlobalSearchScope scope) {
    return getDirectoriesByPackageName(packageName, true).filtering(scope::contains);
  }

  @Nullable
  public abstract String getPackageName(@NotNull VirtualFile dir);

  @NotNull
  public abstract List<OrderEntry> getOrderEntries(@NotNull VirtualFile fileOrDir);


  @NotNull
  public abstract Set<String> getDependentUnloadedModules(@NotNull Module module);
}
