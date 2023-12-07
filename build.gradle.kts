plugins {
    id("java")
}

group = "com.github.keyboardcat"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    implementation("org.kynosarges:tektosyne:6.2.0")
}

tasks.test {
    useJUnitPlatform()
}
