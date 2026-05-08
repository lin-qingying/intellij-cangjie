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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.RunAll;
import com.intellij.testFramework.TempFiles;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;

/**
 * 对位 Kotlin `KotlinLightCodeInsightFixtureTestCaseBase` 的仓颉 code insight fixture 基座。
 *
 * 这一层只负责 light fixture 的公共行为：
 * 1. 统一 IntelliJ platform light fixture 能力面；
 * 2. 提供临时文件创建与清理；
 * 3. 保持后续高层 fixture 可在其上叠加仓颉特定装配逻辑。
 */
public abstract class CangJieLightCodeInsightFixtureTestCaseBase extends CangJieLightPlatformCodeInsightFixtureTestCase {

    @NotNull
    @Override
    public Project getProject() {
        return super.getProject();
    }

    protected final Collection<Path> myFilesToDelete = new HashSet<>();
    private final TempFiles myTempFiles = new TempFiles(myFilesToDelete);

    @Override
    protected void tearDown() {
        RunAll.runAll(myTempFiles::deleteAll, super::tearDown);
    }

    @NotNull
    public VirtualFile createTempFile(
            @NonNls @NotNull String ext,
            byte @Nullable [] bom,
            @NonNls @NotNull String content,
            @NotNull Charset charset
    ) throws IOException {
        File temp = FileUtil.createTempFile("copy", "." + ext);
        setContentOnDisk(temp, bom, content, charset);

        myFilesToDelete.add(temp.toPath());
        VirtualFile file = getVirtualFile(temp);
        assert file != null : temp;
        return file;
    }

    public static void setContentOnDisk(
            @NotNull File file,
            byte @Nullable [] bom,
            @NotNull String content,
            @NotNull Charset charset
    ) throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        if (bom != null) {
            stream.write(bom);
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(stream, charset)) {
            writer.write(content);
        }
    }

    protected static VirtualFile getVirtualFile(@NotNull File file) {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    }
}
