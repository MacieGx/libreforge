dependencies {
    compileOnly(project(":core"))

    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly("org.apache.maven:maven-artifact:3.8.5")
}

group = "com.willfp"
version = rootProject.version

tasks {
    build {
        dependsOn(publishToMavenLocal)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "libreforge-loader"
            groupId = "com.willfp"
        }
    }
}
