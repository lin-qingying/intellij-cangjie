repositories {
    intellijPlatform {
//        nightly()
    }
}
dependencies {
    intellijPlatform {

        plugins("com.redhat.devtools.lsp4ij:0.13.0")
    }
    implementation(project(":"))
}


project(":plugin") {
    dependencies {
        implementation(project(":lsp4ij"))
        intellijPlatform {
            plugins("com.redhat.devtools.lsp4ij:0.13.0")

        }
    }
}