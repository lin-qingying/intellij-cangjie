dependencies {
    intellijPlatform {
        plugins("com.redhat.devtools.lsp4ij:0.16.1")
    }
    implementation(project(":"))
    implementation(project(":telemetry"))

}
project(":") {
    dependencies {

        intellijPlatform {
            plugins("com.redhat.devtools.lsp4ij:0.16.1")
        }
    }
}

project(":plugin") {
    dependencies {
        implementation(project(":lsp4ij"))
        intellijPlatform {
            plugins("com.redhat.devtools.lsp4ij:0.16.1")
        }
    }
}