plugins {
    id("java")
    id("maven-publish")
}

group = "com.github.keyboardcat1"
version = "2.1"

java {
    withSourcesJar()
    withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

val extraLibs by configurations.creating
val implementation by configurations

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    extraLibs("org.kynosarges:tektosyne:6.2.0")
    extraLibs("org.ejml:ejml-all:0.43.1")
    implementation.extendsFrom(extraLibs)
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("github") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/keyboardcat1/erosio")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.jar {
    from(extraLibs.map { if (it.isDirectory) it else zipTree(it) })
}

