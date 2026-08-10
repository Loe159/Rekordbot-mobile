plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("formatCheck") {
    group = "verification"
    description = "Checks Kotlin and Gradle Kotlin files for basic formatting issues."

    doLast {
        val violations = fileTree(rootDir) {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/.gradle/**")
        }.files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                when {
                    line != line.trimEnd() -> "${file.relativeTo(rootDir)}:${index + 1}: trailing whitespace"
                    '\t' in line -> "${file.relativeTo(rootDir)}:${index + 1}: tab character"
                    else -> null
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(violations.joinToString(separator = "\n"))
        }
    }
}
