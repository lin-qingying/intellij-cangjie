plugins {
    id("cangjie.test-support")
}

sourceSets {
    testFixtures {
        java.srcDirs("src/testFixtures")
        resources.srcDirs("src/testFixturesResources")
    }
}

dependencies {
    testFixturesImplementation(project(":modules:foundation"))
    testFixturesImplementation(project(":modules:ide:base"))
    testFixturesImplementation(project(":modules:domain:project-model"))
    testFixturesImplementation(project(":modules:domain:toolchain"))
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testFixturesImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.0")
}
