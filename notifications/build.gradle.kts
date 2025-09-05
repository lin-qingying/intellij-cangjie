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
    implementation(project(":messages"))

}

tasks.test {
    useJUnitPlatform()
}