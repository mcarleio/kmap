plugins {
    id("konvert.kotlin")
}

dependencies {
    api(project(":converter"))

    // Generate docs...
    implementation("net.steppschuh.markdowngenerator:markdowngenerator:${Versions.markdownGenerator}")
}

val generateMarkdownTablesTask = tasks.register<JavaExec>("generateMarkdownTables") {
    description = "Generates the markdown tables for available converters"
    group = JavaBasePlugin.DOCUMENTATION_GROUP

    classpath = sourceSets.main.get().runtimeClasspath

    mainClass.set("io.mcarle.konvert.internal.docs.GenerateDocumentationKt")
}

tasks.named("build") {
    dependsOn += generateMarkdownTablesTask
}
