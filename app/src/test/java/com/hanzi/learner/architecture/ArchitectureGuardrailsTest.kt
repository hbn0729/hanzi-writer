package com.hanzi.learner.architecture

import org.junit.Assert.fail
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.streams.asSequence

class ArchitectureGuardrailsTest {

    @Test
    fun uiScreens_mustNotDependOnAppContainer() {
        val base = projectRoot().resolve("app/src/main/java")
        val candidateFiles = listOf(
            base.resolve("com/hanzi/learner/ui/HanziLearnerApp.kt"),
        ) + kotlinFilesUnder(base.resolve("com/hanzi/learner/feature")).filter {
            it.invariantSeparatorsPathString.contains("/ui/")
        }

        val violations = mutableListOf<String>()
        for (file in candidateFiles) {
            val text = String(Files.readAllBytes(file))
            if (
                text.contains("import com.hanzi.learner.app.AppContainer") ||
                text.contains("container: AppContainer")
            ) {
                violations += file.invariantSeparatorsPathString
            }
        }

        if (violations.isNotEmpty()) {
            fail("UI must not depend on AppContainer directly. Violations:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun uiLayer_mustNotImportConcreteRepositoryImpls() {
        val base = projectRoot().resolve("app/src/main/java/com/hanzi/learner")
        val uiFiles = kotlinFilesUnder(base).filter {
            it.invariantSeparatorsPathString.contains("/ui/") &&
                !it.invariantSeparatorsPathString.endsWith("/ui/AppContainer.kt") &&
                !it.invariantSeparatorsPathString.endsWith("/ui/AppModules.kt")
        }

        val importRegex = Regex("^import\\s+.+RepositoryImpl\\s*$", RegexOption.MULTILINE)
        val violations = uiFiles.filter { file -> importRegex.containsMatchIn(String(Files.readAllBytes(file))) }
            .map { it.invariantSeparatorsPathString }

        if (violations.isNotEmpty()) {
            fail("UI layer must not import concrete RepositoryImpl types. Violations:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun nonUiLayers_mustNotImportUiPackage() {
        val base = projectRoot().resolve("app/src/main/java/com/hanzi/learner")
        val nonUiFiles = kotlinFilesUnder(base).filterNot {
            it.invariantSeparatorsPathString.contains("/ui/")
        }.filterNot {
            it.invariantSeparatorsPathString.endsWith("/MainActivity.kt")
        }

        val importRegex = Regex("^import\\s+com\\.hanzi\\.learner\\.ui\\..+$", RegexOption.MULTILINE)
        val violations = nonUiFiles.filter { file -> importRegex.containsMatchIn(String(Files.readAllBytes(file))) }
            .map { it.invariantSeparatorsPathString }

        if (violations.isNotEmpty()) {
            fail("Non-UI layers must not import com.hanzi.learner.app.*. Violations:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun uiLayers_mustNotImportDbEntitiesOrDaos() {
        val base = projectRoot().resolve("app/src/main/java/com/hanzi/learner")
        val uiFiles = kotlinFilesUnder(base).filter {
            it.invariantSeparatorsPathString.contains("/ui/") &&
                !it.invariantSeparatorsPathString.endsWith("/ui/AppContainer.kt") &&
                !it.invariantSeparatorsPathString.endsWith("/ui/AppModules.kt")
        }

        val importRegex = Regex("^import\\s+com\\.hanzi\\.learner\\.db\\..*(Entity|Dao)\\s*$", RegexOption.MULTILINE)
        val violations = uiFiles.filter { file -> importRegex.containsMatchIn(String(Files.readAllBytes(file))) }
            .map { it.invariantSeparatorsPathString }

        if (violations.isNotEmpty()) {
            fail("UI layers must not import db Entity/Dao types. Violations:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun localDateNow_mustOnlyAppearInTimeProvider() {
        val base = projectRoot().resolve("app/src/main/java")
        val allowed = base.resolve("com/hanzi/learner/db/TimeProvider.kt").invariantSeparatorsPathString

        val violations = kotlinFilesUnder(base)
            .filter { file ->
                val normalized = file.invariantSeparatorsPathString
                normalized != allowed && String(Files.readAllBytes(file)).contains("LocalDate.now(")
            }
            .map { it.invariantSeparatorsPathString }

        if (violations.isNotEmpty()) {
            fail("LocalDate.now() must only be used in TimeProvider. Violations:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun practiceViewModel_mustDependOnSessionEngineFactory_notConcreteOrchestrator() {
        val file = projectRoot()
            .resolve("app/src/main/java/com/hanzi/learner/feature/practice/viewmodel/PracticeViewModel.kt")
        val text = String(Files.readAllBytes(file))
        if (text.contains("PracticeSessionOrchestrator")) {
            fail("PracticeViewModel must depend on PracticeSessionEngineFactory, not PracticeSessionOrchestrator.")
        }
    }

    @Test
    fun adminBackupViewModel_mustDependOnSegregatedBackupPorts() {
        val file = projectRoot()
            .resolve("app/src/main/java/com/hanzi/learner/feature/admin/viewmodel/AdminBackupViewModel.kt")
        val text = String(Files.readAllBytes(file))
        if (text.contains("AdminBackupRepository")) {
            fail("AdminBackupViewModel must depend on segregated backup ports instead of AdminBackupRepository.")
        }
    }

    @Test
    fun dbRepositoryContracts_mustNotExposeEntityTypes() {
        val base = projectRoot().resolve("app/src/main/java/com/hanzi/learner/db")
        val contractFiles = kotlinFilesUnder(base).filter {
            it.fileName.toString().endsWith("RepositoryContract.kt")
        }
        val typeRegex = Regex(":\\s*[A-Za-z0-9_<>?,\\s.]*Entity\\b")
        val violations = contractFiles.filter { file ->
            typeRegex.containsMatchIn(String(Files.readAllBytes(file)))
        }.map { it.invariantSeparatorsPathString }

        if (violations.isNotEmpty()) {
            fail("DB repository contracts must not expose Entity types. Violations:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun practiceSessionEngine_contract_mustNotReferenceOrchestrator() {
        val file = projectRoot()
            .resolve("app/src/main/java/com/hanzi/learner/feature/practice/domain/PracticeSessionEngine.kt")
        val text = String(Files.readAllBytes(file))
        if (text.contains("PracticeSessionOrchestrator")) {
            fail("PracticeSessionEngine contract must not reference PracticeSessionOrchestrator types.")
        }
    }

    @Test
    fun viewModels_mustNotImportRepositoryImpls_orDaos() {
        val base = projectRoot().resolve("app/src/main/java/com/hanzi/learner")
        val viewModelFiles = kotlinFilesUnder(base).filter {
            it.invariantSeparatorsPathString.contains("/viewmodel/")
        }

        val implRegex = Regex("^import\\s+.+RepositoryImpl\\s*$", RegexOption.MULTILINE)
        val daoRegex = Regex("^import\\s+com\\.hanzi\\.learner\\.db\\..*Dao\\s*$", RegexOption.MULTILINE)
        val violations = viewModelFiles.filter { file ->
            val text = String(Files.readAllBytes(file))
            implRegex.containsMatchIn(text) || daoRegex.containsMatchIn(text)
        }.map { it.invariantSeparatorsPathString }

        if (violations.isNotEmpty()) {
            fail("ViewModels must not import RepositoryImpl/Dao types. Violations:\n${violations.joinToString("\n")}")
        }
    }

    @Test
    fun domain_and_application_layers_mustNotImportCompose() {
        val base = projectRoot().resolve("app/src/main/java/com/hanzi/learner")
        val candidateFiles = kotlinFilesUnder(base).filter { file ->
            val normalized = file.invariantSeparatorsPathString
            normalized.contains("/domain/") || normalized.contains("/application/")
        }
        val importRegex = Regex("^import\\s+androidx\\.compose\\..+$", RegexOption.MULTILINE)
        val violations = candidateFiles.filter { file ->
            importRegex.containsMatchIn(String(Files.readAllBytes(file)))
        }.map { it.invariantSeparatorsPathString }

        if (violations.isNotEmpty()) {
            fail("Domain/application layers must not import Compose APIs. Violations:\n${violations.joinToString("\n")}")
        }
    }

    private fun projectRoot(): Path {
        var cursor = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
        repeat(6) {
            if (Files.exists(cursor.resolve("settings.gradle.kts")) || Files.exists(cursor.resolve("settings.gradle"))) {
                return cursor
            }
            cursor = cursor.parent ?: return@repeat
        }
        throw IllegalStateException("Could not find project root from ${System.getProperty("user.dir")}")
    }

    private fun kotlinFilesUnder(root: Path): List<Path> {
        if (!Files.exists(root)) return emptyList()
        Files.walk(root).use { stream ->
            return stream.asSequence()
                .filter { Files.isRegularFile(it) && it.extension == "kt" }
                .toList()
        }
    }
    @Test
    fun dbPackage_mustNotImportFeaturePackage() {
        val base = projectRoot().resolve("app/src/main/java/com/hanzi/learner/db")
        val importRegex = Regex("^import\\s+com\\.hanzi\\.learner\\.feature\\..+$", RegexOption.MULTILINE)
        val violations = kotlinFilesUnder(base).filter { file ->
            importRegex.containsMatchIn(String(Files.readAllBytes(file)))
        }.map { it.invariantSeparatorsPathString }

        if (violations.isNotEmpty()) {
            fail("db package must not import feature package. Violations:\n${violations.joinToString("\n")}")
        }
    }
}
