dependencies {
    intellijPlatform {
        plugins("com.redhat.devtools.lsp4ij:0.13.0")
    }
    implementation(project(":"))
    implementation(project(":telemetry"))

}


project(":plugin") {
    dependencies {
        implementation(project(":lsp4ij"))
        intellijPlatform {
            plugins("com.redhat.devtools.lsp4ij:0.13.0")
        }
    }
}