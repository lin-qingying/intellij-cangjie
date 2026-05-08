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
    testFixturesApi("org.cangnova.cangjie:cangjie-frontend-test-infrastructure:${providers.gradleProperty("cangjieVersion").get()}")
    testFixturesImplementation(libs.cangjieCommonForIde)
    testFixturesImplementation(libs.cangjiePsiForIde)
    testFixturesImplementation(libs.cangjieCfirForIde)
    testFixturesImplementation(libs.cangjieAnalysisApiForIde)
    testFixturesImplementation(libs.cangjieAnalysisApiCfirForIde)
    testFixturesImplementation(libs.cangjieAnalysisApiStandaloneForIde)
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation("junit:junit:4.13.2")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
}
