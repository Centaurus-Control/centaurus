plugins {
    id("java")
    id("org.springframework.boot").version("4.1.0")
    id("io.spring.dependency-management").version("1.1.7")
}

group = "de.shadowsoft.centaurus"
version = "2.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.github.oshi:oshi-core:7.3.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.bootJar {
    archiveBaseName.set("centaurus-agent")
}

tasks.test {
    useJUnitPlatform()
}
