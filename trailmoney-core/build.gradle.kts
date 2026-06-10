plugins {
    `java-library`
}

dependencies {
    api(project(":trailmoney-api"))
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.53.2.0")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.jar {
    archiveBaseName.set("TrailMoney")
    dependsOn(project(":trailmoney-api").tasks.named("jar"))
    from(zipTree(project(":trailmoney-api").tasks.named<Jar>("jar").flatMap { it.archiveFile }))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
