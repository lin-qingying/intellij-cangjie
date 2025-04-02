dependencies {
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.15.0")
    implementation("com.vladsch.flexmark:flexmark:0.34.60")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.9.0")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.9")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")



//        implementation("com.github.ballerina-platform:lsp4intellij:0.96.0")
        implementation(project(":"))

}

project(":plugin"){
    dependencies {
        implementation(project(":lsp4intellij"))

    }
}