plugins {
    `java-library`
}

dependencies {
    api(project(":trailmoney-api"))
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("TrailMoney")
    dependsOn(project(":trailmoney-api").tasks.named("jar"))
    from(zipTree(project(":trailmoney-api").tasks.named<Jar>("jar").flatMap { it.archiveFile }))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
