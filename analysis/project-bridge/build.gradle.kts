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

/**
 * Analysis Project Bridge 模块
 *
 * 此模块作为 analysis 和 cangjie-project 之间的桥接层，
 * 负责将项目模型（CjModule、CjProject 等）适配为分析上下文（AnalysisContext）。
 *
 * 依赖关系：
 * - analysis：提供 AnalysisContext 接口
 * - cangjie-project：提供 CjModule、CjProject 等项目模型
 */

dependencies {
    // 核心依赖：分析接口和项目模型
    api(project(":analysis"))
    api(project(":cangjie-project"))

    // 基础设施依赖
    implementation(project(":util"))
    implementation(project(":common"))
    implementation(project(":psi"))
}
