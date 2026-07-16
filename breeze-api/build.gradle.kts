plugins {
    java
    `maven-publish`
}

dependencies {
    compileOnly("io.canvasmc.canvas:canvas-api:1.21.11-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "ru.breezeproject"
            artifactId = "breeze-api"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
