
dependencies {
  intellijPlatform{
      plugins("com.redhat.devtools.lsp4ij:0.11.0")
  }
    implementation(project(":"))

}
project(":plugin"){
    dependencies {
        intellijPlatform{
            plugins("com.redhat.devtools.lsp4ij:0.11.0")
        }
        implementation(project(":lsp4ij"))
    }
}