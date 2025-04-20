

dependencies {
    implementation(project(":"))
}


project(":plugin") {
    dependencies {
        implementation(project(":lsp4ij"))

    }
}