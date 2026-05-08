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
 */

package org.cangnova.cangjie.test;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对位 Kotlin `TestFiles` 的多文件/多模块测试文本拆分器。
 */
public final class TestFiles {
    /**
     * Syntax:
     * <p>
     * // MODULE: name(dependency1, dependency2, ...)
     * <p>
     * // FILE: name
     * <p>
     * Several files may follow one module
     */
    private static final String MODULE_DELIMITER = ",\\s*";

    private static final Pattern MODULE_PATTERN = Pattern.compile("//\\s*MODULE:\\s*([^()\\r\\n]+)(?:\\(([^()]+(?:" + MODULE_DELIMITER + "[^()]+)*)\\))?\\s*(?:\\(([^()]+(?:" + MODULE_DELIMITER + "[^()]+)*)\\))?\\R");
    private static final Pattern FILE_PATTERN = Pattern.compile("//\\s*FILE:\\s*(.*)\\R");

    private TestFiles() {
    }

    @NotNull
    public static <M extends CangJieBaseTest.TestModule, F> List<F> createTestFiles(
            @Nullable String testFileName,
            String expectedText,
            TestFileFactory<M, ? extends F> factory
    ) {
        Map<String, M> modules = new HashMap<>();
        List<F> testFiles = new ArrayList<>();
        Matcher fileMatcher = FILE_PATTERN.matcher(expectedText);
        Matcher moduleMatcher = MODULE_PATTERN.matcher(expectedText);

        boolean fileFound = fileMatcher.find();
        boolean moduleFound = moduleMatcher.find();
        if (!fileFound && !moduleFound) {
            assert testFileName != null : "testFileName should not be null if no FILE directive defined";
            testFiles.add(factory.createFile(null, testFileName, expectedText, CangJieTestUtils.parseDirectives(expectedText)));
        } else {
            Directives allFilesOrCommonPrefixDirectives = CangJieTestUtils.parseDirectives(expectedText);
            int processedChars = 0;
            M module = null;

            while (true) {
                if (moduleFound) {
                    String moduleName = moduleMatcher.group(1);
                    String moduleDependencies = moduleMatcher.group(2);
                    String moduleFriends = moduleMatcher.group(3);
                    if (moduleName != null) {
                        moduleName = moduleName.trim();
                        module = factory.createModule(moduleName, parseModuleList(moduleDependencies), parseModuleList(moduleFriends));
                        M oldValue = modules.put(moduleName, module);
                        assert oldValue == null : "Module with name " + moduleName + " already present in file";
                    }
                }

                boolean nextModuleExists = moduleMatcher.find();
                moduleFound = nextModuleExists;
                while (true) {
                    String fileName = fileMatcher.group(1);
                    int start = processedChars;

                    boolean nextFileExists = fileMatcher.find();
                    int end;
                    if (nextFileExists && nextModuleExists) {
                        end = Math.min(fileMatcher.start(), moduleMatcher.start());
                    } else if (nextFileExists) {
                        end = fileMatcher.start();
                    } else {
                        end = expectedText.length();
                    }
                    String fileText = expectedText.substring(start, end);
                    testFiles.add(factory.createFile(module, fileName, fileText, allFilesOrCommonPrefixDirectives));
                    processedChars = end;
                    if (!nextFileExists && !nextModuleExists) break;
                    if (nextModuleExists && fileMatcher.start() > moduleMatcher.start()) break;
                }
                if (!nextModuleExists) break;
            }
        }

        for (M module : modules.values()) {
            if (module != null) {
                for (String name : module.dependenciesSymbols) {
                    M dependency = modules.get(name);
                    assert dependency != null : "Dependency not found:" + name + "for module " + module.name;
                    module.getDependencies().add(dependency);
                }

                for (String name : module.friendsSymbols) {
                    M friend = modules.get(name);
                    assert friend != null : "Dependency not found:" + name + "for module " + module.name;
                    module.getFriends().add(friend);
                }
            }
        }

        return testFiles;
    }

    private static List<String> parseModuleList(@Nullable String dependencies) {
        if (dependencies == null) return Collections.emptyList();
        return kotlin.text.StringsKt.split(dependencies, Pattern.compile(MODULE_DELIMITER), 0);
    }

    public interface TestFileFactory<M, F> {
        F createFile(@Nullable M module, @NotNull String fileName, @NotNull String text, @NotNull Directives directives);

        M createModule(@NotNull String name, @NotNull List<String> dependencies, @NotNull List<String> friends);
    }

    public static abstract class TestFileFactoryNoModules<F> implements TestFileFactory<CangJieBaseTest.TestModule, F> {
        @Override
        public final F createFile(
                @Nullable CangJieBaseTest.TestModule module,
                @NotNull String fileName,
                @NotNull String text,
                @NotNull Directives directives
        ) {
            return create(fileName, text, directives);
        }

        @NotNull
        public abstract F create(@NotNull String fileName, @NotNull String text, @NotNull Directives directives);

        @Override
        public CangJieBaseTest.TestModule createModule(
                @NotNull String name,
                @NotNull List<String> dependencies,
                @NotNull List<String> friends
        ) {
            return null;
        }
    }
}
