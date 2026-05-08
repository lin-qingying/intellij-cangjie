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

/**
 * 对位 Kotlin `org.jetbrains.kotlin.test.JUnit3RunnerWithInners` 的桥接入口。
 *
 * 编译器/生成测试更习惯引用独立的 JUnit3 runner 名称，
 * 这里保留一个显式别名，避免所有 generated 测试都直接绑定框架内部实现类名。
 */
public class CangJieJUnit3RunnerWithInners extends CangJieJUnit4TestRunner {
    public CangJieJUnit3RunnerWithInners(Class<?> testClass) throws org.junit.runners.model.InitializationError {
        super(testClass);
    }
}
