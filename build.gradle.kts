allprojects {
    group = "net.f02k"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
    }
}

// Module jars, by project path -> built jar file.
val moduleProjects = listOf(":modules:safety", ":modules:chat", ":modules:actionbar")

// Assembles the core jar plus every module jar into build/dist/
// (build/dist/4netlib-<version>.jar and build/dist/modules/*.jar).
tasks.register<Copy>("dist") {
    group = "build"
    description = "Collects the core plugin and all module jars into build/dist."

    val core = project(":safety-spy")
    dependsOn("${core.path}:jar")
    into(layout.buildDirectory.dir("dist"))
    from(core.tasks.named("jar"))

    moduleProjects.forEach { path ->
        dependsOn("$path:jar")
        from(project(path).tasks.named("jar")) {
            into("modules")
        }
    }
}
