import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm")
    `kotlin-dsl`

    `java-gradle-plugin`
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
                this@projectDefault.resources.srcDir("testResources")
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