import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import org.gradle.api.file.RelativePath

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

data class LinuxAgentDistribution(
    val name: String,
    val packageArch: String,
    val temurinArch: String
)

val linuxAgentDistributions = listOf(
    LinuxAgentDistribution("linuxAmd64", "amd64", "x64"),
    LinuxAgentDistribution("linuxArm64", "arm64", "aarch64")
)

val rootProjectDir = layout.projectDirectory.dir("..")
val linuxPackagingDir = rootProjectDir.dir("packaging/agent/linux")
val agentDistDir = rootProjectDir.dir("dist/agent")
val temurinCacheDir = layout.buildDirectory.dir("temurin-cache")
val agentPackageWorkDir = layout.buildDirectory.dir("agent-distributions")

fun downloadFile(url: String, targetFile: File) {
    targetFile.parentFile.mkdirs()

    var currentUrl = URI(url).toURL()
    repeat(8) {
        val connection = currentUrl.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000

        val status = connection.responseCode
        if (status in 300..399) {
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            require(!location.isNullOrBlank()) { "Redirect without Location header while downloading $url" }
            currentUrl = URI(location).toURL()
            return@repeat
        }

        require(status in 200..299) { "Download failed with HTTP $status: $currentUrl" }

        connection.inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        connection.disconnect()
        return
    }

    error("Too many redirects while downloading $url")
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead < 0) {
                break
            }
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val linuxPackageTasks = linuxAgentDistributions.map { distribution ->
    val capitalizedName = distribution.name.replaceFirstChar { it.uppercaseChar() }
    val temurinArchive = temurinCacheDir.map {
        it.file("temurin-21-linux-${distribution.packageArch}.tar.gz")
    }
    val packageName = "centaurus-agent-linux-${distribution.packageArch}-${project.version}"
    val stagingDir = agentPackageWorkDir.map {
        it.dir("${packageName}/centaurus-agent")
    }
    val downloadTemurinTask = tasks.register("downloadTemurin${capitalizedName}") {
        val downloadUrl = "https://api.adoptium.net/v3/binary/latest/21/ga/linux/${distribution.temurinArch}/jdk/hotspot/normal/eclipse?project=jdk"

        inputs.property("downloadUrl", downloadUrl)
        outputs.file(temurinArchive)

        doLast {
            val archiveFile = temurinArchive.get().asFile
            if (!archiveFile.exists()) {
                logger.lifecycle("Downloading Temurin 21 for linux/${distribution.packageArch}...")
                downloadFile(downloadUrl, archiveFile)
            }
        }
    }

    val preparePackageTask = tasks.register<Sync>("prepareAgent${capitalizedName}Package") {
        dependsOn(tasks.named("bootJar"))
        dependsOn(downloadTemurinTask)

        into(stagingDir)

        from(tasks.named("bootJar")) {
            rename { "centaurus-agent.jar" }
        }

        from(linuxPackagingDir) {
            include(".env.example")
            filter { line ->
                line
                    .replace("{{AGENT_VERSION}}", project.version.toString())
            }
        }

        from(linuxPackagingDir) {
            include("install.sh")
            filter { line ->
                line
                    .replace("{{PACKAGE_ARCH}}", distribution.packageArch)
                    .replace("{{AGENT_VERSION}}", project.version.toString())
            }
        }

        from(linuxPackagingDir) {
            include("run-agent.sh", "uninstall.sh")
        }

        from(tarTree(resources.gzip(temurinArchive))) {
            eachFile {
                val segments = relativePath.segments
                if (segments.size > 1) {
                    relativePath = RelativePath(true, "runtime", *segments.drop(1).toTypedArray())
                } else {
                    exclude()
                }
            }
            includeEmptyDirs = false
        }

        doFirst {
            delete(stagingDir.get().asFile)
        }

        doLast {
            val packageDir = stagingDir.get().asFile
            packageDir.resolve("install.sh").setExecutable(true, false)
            packageDir.resolve("run-agent.sh").setExecutable(true, false)
            packageDir.resolve("uninstall.sh").setExecutable(true, false)
            packageDir.resolve("runtime/bin").listFiles()
                ?.filter { it.isFile }
                ?.forEach { it.setExecutable(true, false) }
            packageDir.resolve("runtime/lib/jspawnhelper").takeIf { it.exists() }
                ?.setExecutable(true, false)
            packageDir.resolve("agent-data").mkdirs()
            packageDir.resolve("logs").mkdirs()
            packageDir.resolve("agent-data/.gitkeep").writeText("")
            packageDir.resolve("logs/.gitkeep").writeText("")
        }
    }

    val archiveFile = agentDistDir.file("${packageName}.tar.gz")

    val tarTask = tasks.register("packageAgent${capitalizedName}") {
        dependsOn(preparePackageTask)

        inputs.dir(agentPackageWorkDir.map { it.dir(packageName) })
        outputs.file(archiveFile)

        doLast {
            archiveFile.asFile.parentFile.mkdirs()
            archiveFile.asFile.delete()
            val process = ProcessBuilder(
                "tar",
                "-C",
                agentPackageWorkDir.get().dir(packageName).asFile.absolutePath,
                "-czf",
                archiveFile.asFile.absolutePath,
                "centaurus-agent"
            )
                .inheritIO()
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                error("tar failed with exit code $exitCode")
            }
        }
    }

    tasks.register("checksumAgent${capitalizedName}") {
        dependsOn(tarTask)

        val checksumFile = agentDistDir.file("${packageName}.tar.gz.sha256")

        inputs.file(archiveFile)
        outputs.file(checksumFile)

        doLast {
            val archive = archiveFile.asFile
            checksumFile.asFile.writeText("${sha256(archive)}  ${archive.name}\n")
        }
    }
}

tasks.register("buildAgentLinuxDistributions") {
    group = "distribution"
    description = "Builds Centaurus Agent release packages for supported Linux targets."
    dependsOn(linuxPackageTasks)
}

tasks.register("buildAgentDistributions") {
    group = "distribution"
    description = "Builds Centaurus Agent release packages."
    dependsOn("buildAgentLinuxDistributions")
}

tasks.named("build") {
    dependsOn("buildAgentDistributions")
}
