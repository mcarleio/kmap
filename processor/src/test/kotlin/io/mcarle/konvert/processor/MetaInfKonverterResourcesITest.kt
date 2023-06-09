package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MetaInfKonverterResourcesITest : KonverterITest() {

    override var addGeneratedKonverterAnnotation = true

    @ParameterizedTest
    @ValueSource(strings = ["", "a.b"])
    fun generateKonvertToFile(packageName: String) {
        val codeFileName = packageName.replace(".", "/").let {
            (if (it.isNotEmpty()) "$it/" else it) + "TestCode.kt"
        }
        val packageCodeLine = if (packageName.isNotEmpty()) "package $packageName" else ""
        val expectedPackagePrefix = packageName + if (packageName.isNotEmpty()) "." else ""
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = codeFileName,
                contents =
                """
$packageCodeLine
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(val property: String)
class TargetClass(val property: String)

                """.trimIndent()
            )
        )
        val metaInfFileContent = compilation.generatedSourceFor("io.mcarle.konvert.api.KonvertTo")
        println(metaInfFileContent)

        assertContentEquals(
            """
            ${expectedPackagePrefix}toTargetClass
            """.trimIndent(),
            metaInfFileContent
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a.b"])
    fun generateKonvertFromFile(packageName: String) {
        val codeFileName = packageName.replace(".", "/").let {
            (if (it.isNotEmpty()) "$it/" else it) + "TestCode.kt"
        }
        val packageCodeLine = if (packageName.isNotEmpty()) "package $packageName" else ""
        val expectedPackagePrefix = packageName + if (packageName.isNotEmpty()) "." else ""
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = codeFileName,
                contents =
                """
$packageCodeLine
import io.mcarle.konvert.api.KonvertFrom

class SourceClass(val property: String)
@KonvertFrom(SourceClass::class)
class TargetClass(val property: String) {
    companion object
}

                """.trimIndent()
            )
        )
        val metaInfFileContent = compilation.generatedSourceFor("io.mcarle.konvert.api.KonvertFrom")
        println(metaInfFileContent)

        assertContentEquals(
            """
            ${expectedPackagePrefix}fromSourceClass
            """.trimIndent(),
            metaInfFileContent
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "a.b"])
    fun generateKonvertFile(packageName: String) {
        val codeFileName = packageName.replace(".", "/").let {
            (if (it.isNotEmpty()) "$it/" else it) + "TestCode.kt"
        }
        val packageCodeLine = if (packageName.isNotEmpty()) "package $packageName" else ""
        val expectedPackagePrefix = packageName + if (packageName.isNotEmpty()) "." else ""
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = codeFileName,
                contents =
                """
$packageCodeLine
import io.mcarle.konvert.api.Konverter

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val metaInfFileContent = compilation.generatedSourceFor("io.mcarle.konvert.api.Konvert")
        println(metaInfFileContent)

        assertContentEquals(
            """
            ${expectedPackagePrefix}Mapper.toTarget
            """.trimIndent(),
            metaInfFileContent
        )
    }

}
