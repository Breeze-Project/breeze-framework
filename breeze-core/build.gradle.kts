buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.flywaydb:flyway-mysql:10.15.0")
        classpath("org.postgresql:postgresql:42.7.3")
    }
}

plugins {
    java
    id("org.flywaydb.flyway") version "10.15.0"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    implementation(project(":breeze-api"))

    implementation("org.yaml:snakeyaml:2.2")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.4.0")
    implementation("org.postgresql:postgresql:42.7.3")

    implementation("org.flywaydb:flyway-core:10.15.0")
    implementation("org.flywaydb:flyway-mysql:10.15.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

flyway {
    val vendor = (project.findProperty("dbVendor") as String?) ?: "mysql"
    locations = arrayOf("classpath:migrations/$vendor/")
    url = (project.findProperty("flyway.url") as String?) ?: "jdbc:mysql://localhost:3306/breezecore"
    user = (project.findProperty("flyway.user") as String?) ?: "root"
    password = (project.findProperty("flyway.password") as String?) ?: ""
}

tasks.named("flywayMigrate") {
    dependsOn(tasks.processResources)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
        showCauses = true
        showExceptions = true
    }
}

tasks {
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(project(":breeze-api").extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java)["main"].output)
        from({
            configurations.runtimeClasspath.get().filter { it.exists() }.map {
                if (it.isDirectory) it else zipTree(it)
            }
        })
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        }
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
