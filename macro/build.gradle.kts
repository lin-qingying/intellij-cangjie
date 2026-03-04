

dependencies {
    implementation(project(":toolchain"))
    implementation(project(":common"))
    implementation(project(":messages"))
    implementation(project(":psi"))
    implementation(project(":icon"))
    implementation(project(":namedpipe"))
    implementation(project(":flatbuffers-gen"))
    implementation(libs.flatbuffers.java)

    compileOnly(project(":cangjie-project"))
}
