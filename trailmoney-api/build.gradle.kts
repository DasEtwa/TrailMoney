plugins {
    `java-library`
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
