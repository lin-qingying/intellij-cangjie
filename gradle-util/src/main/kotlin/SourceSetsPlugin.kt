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


import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * SourceSets 插件
 *
 * 提供便捷的 SourceSet 配置功能。
 */
class SourceSetsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 应用 Java 插件
        project.plugins.apply("java")
        
        // 扩展项目对象
        project.extensions.create("sourceSetsBuilder", SourceSetsBuilder::class.java, project)
        
        // 添加扩展函数到项目
        project.extensions.add("sourceSets", project.sourceSets)
    }
} 