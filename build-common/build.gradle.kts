plugins {
    kotlin("jvm")
}

sourceSets {
    main {
        java.srcDirs("src")

        resources.srcDir("resources")
    }
    test {
        java.srcDirs("src")

        resources.srcDir("resources")
    }
}
