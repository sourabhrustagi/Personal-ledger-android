package com.ledger.personal

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class ModuleBoundaryTest {

    @Test
    fun featuresDoNotDependOnEachOther() {
        val features = repoRoot().resolve("feature").listFiles { file -> file.isDirectory }.orEmpty()
        features.forEach { feature ->
            val gradle = feature.resolve("build.gradle.kts").readText()
            features.filter { it != feature }.forEach { other ->
                assertFalse(gradle.contains("\":feature:${other.name}\""))
            }
        }
    }

    @Test
    fun featureSourcesDoNotUseRawHexColors() {
        val features = repoRoot().resolve("feature")
        features.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            assertFalse(file.readText().contains("Color(0x"))
        }
    }

    private fun repoRoot(): File {
        val dir = File(System.getProperty("user.dir"))
        return if (dir.resolve("settings.gradle.kts").exists()) dir else dir.parentFile
    }
}
