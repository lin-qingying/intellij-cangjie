import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("cangjie.intellij-module")
    `java-test-fixtures`
}

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
    }
}

sourceSets {
    main {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}
