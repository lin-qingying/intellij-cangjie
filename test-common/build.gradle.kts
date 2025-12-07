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

import org.jetbrains.intellij.platform.gradle.TestFrameworkType


plugins {
    `java-test-fixtures`
}
sourceSets {
    main {
        none()
    }
    test {
        none()

    }
    testFixtures {
        projectDefault()
    }
}


fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}


val SourceSet.projectDefault: Project.() -> Unit
    get() = {
        when (val name = this@projectDefault.name) {
            "main" -> {
                this@projectDefault.java.srcDir("src")

                this@projectDefault.resources.srcDir("resources")
            }

            "test" -> {
                this@projectDefault.java.srcDirs("test", "tests")
                this@projectDefault.resources.srcDir("resources")
            }

            "testFixtures" -> {
                this@projectDefault.java.srcDirs("testFixtures")
                this@projectDefault.resources.srcDir("testFixturesResources")
            }

            else -> error("Unknown source set $name")
        }
    }

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)


    }
    testFixturesImplementation("junit:junit:4.13.2")
// https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")


    testFixturesImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(kotlin("test-junit"))
    testFixturesImplementation(project(":"))


}