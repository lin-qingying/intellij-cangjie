plugins {
    id("java")
}

group = "org.cangnova"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":notifications"))
    implementation(project(":messages"))
    implementation(project(":util"))
    implementation(project(":icon"))

    implementation(project(":psi"))

}

tasks.test {
    useJUnitPlatform()
}