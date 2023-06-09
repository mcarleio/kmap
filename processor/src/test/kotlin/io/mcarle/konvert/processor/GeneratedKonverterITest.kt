package io.mcarle.konvert.processor

import com.squareup.kotlinpoet.ksp.toClassName
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.GeneratedKonverter
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.converter.api.config.ADD_GENERATED_KONVERTER_ANNOTATION_OPTION
import io.mcarle.konvert.processor.konvert.KonvertTypeConverter
import io.mcarle.konvert.processor.konvertfrom.KonvertFromTypeConverter
import io.mcarle.konvert.processor.konvertto.KonvertToTypeConverter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GeneratedKonverterITest : KonverterITest() {

    override var addGeneratedKonverterAnnotation = true

    @Test
    fun loadGeneratedKonvertTypeConverter() {
        super.compileWith(emptyList())
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(1, alreadyGeneratedKonverterList.size, "missing generated konverter")
        val converter = alreadyGeneratedKonverterList.first()
        assertEquals("toSomeOtherTestClass", converter.mapFunctionName)
        assertEquals("SomeTestClass", converter.sourceType.toClassName().simpleName)
        assertEquals("SomeOtherTestClass", converter.targetType.toClassName().simpleName)
        assertEquals("source", converter.paramName)
        assertEquals("SomeTestMapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(12, converter.priority)
    }

    @Test
    fun loadGeneratedKonvertToTypeConverter() {
        super.compileWith(emptyList())
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertToTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(1, alreadyGeneratedKonverterList.size, "missing generated konverter")
        val converter = alreadyGeneratedKonverterList.first()
        assertEquals("toSomeOtherTestClass", converter.mapFunctionName)
        assertEquals("SomeTestClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("SomeOtherTestClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(10, converter.priority)
    }

    @Test
    fun loadGeneratedKonvertFromTypeConverter() {
        super.compileWith(emptyList())
        val alreadyGeneratedKonverterList = TypeConverterRegistry
            .filterIsInstance<KonvertFromTypeConverter>()
            .filter { it.alreadyGenerated }
        assertEquals(1, alreadyGeneratedKonverterList.size, "missing generated konverter")
        val converter = alreadyGeneratedKonverterList.first()
        assertEquals("fromSomeTestClass", converter.mapFunctionName)
        assertEquals("SomeTestClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("SomeOtherTestClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals("source", converter.paramName)
        assertEquals(true, converter.enabledByDefault)
        assertEquals(11, converter.priority)
    }

    @Test
    fun useGeneratedKonverterWithHighestPriority() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.processor.SomeTestClass
import io.mcarle.konvert.processor.SomeOtherTestClass

@KonvertTo(TargetClass::class)
class SourceClass(val property: SomeTestClass)
class TargetClass(val property: SomeOtherTestClass)

                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter
            import io.mcarle.konvert.processor.toSomeOtherTestClass

            @GeneratedKonverter(priority = 3_000)
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property.toSomeOtherTestClass()
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun generateGeneratedKonverterAnnotationForKonvertTo() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class, priority = 123)
class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter

            @GeneratedKonverter(priority = 123)
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun generateGeneratedKonverterAnnotationForKonvertFrom() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom

class SourceClass(val property: String)

@KonvertFrom(SourceClass::class, priority = 123)
class TargetClass(val property: String) {
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter

            @GeneratedKonverter(priority = 123)
            public fun TargetClass.Companion.fromSourceClass(sourceClass: SourceClass): TargetClass =
                TargetClass(
              property = sourceClass.property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun generateGeneratedKonverterAnnotationForKonverter() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    @Konvert(priority = 123)
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            import io.mcarle.konvert.api.GeneratedKonverter

            public object MapperImpl : Mapper {
              @GeneratedKonverter(priority = 123)
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotGenerateGeneratedKonverterAnnotationIfOptionDisabledInKonvertToOptions() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Konfig

@KonvertTo(TargetClass::class, options=[Konfig(key = "${ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key}", value = "false")])
class SourceClass(val property: String)
class TargetClass(val property: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotGenerateGeneratedKonverterAnnotationIfOptionDisabledInKonvertFromOptions() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertFrom
import io.mcarle.konvert.api.Konfig

class SourceClass(val property: String)

@KonvertFrom(SourceClass::class, options=[Konfig(key = "${ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key}", value = "false")])
class TargetClass(val property: String) {
    companion object
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("TargetClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun TargetClass.Companion.fromSourceClass(sourceClass: SourceClass): TargetClass =
                TargetClass(
              property = sourceClass.property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotGenerateGeneratedKonverterAnnotationIfOptionDisabledInKonvertOptions() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Konfig

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter
interface Mapper {
    @Konvert(options=[Konfig(key = "${ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key}", value = "false")])
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotGenerateGeneratedKonverterAnnotationIfOptionDisabledInKonverterOptions() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konfig

class SourceClass(val property: String)
class TargetClass(val property: String)

@Konverter(options=[Konfig(key = "${ADD_GENERATED_KONVERTER_ANNOTATION_OPTION.key}", value = "false")])
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public object MapperImpl : Mapper {
              override fun toTarget(source: SourceClass): TargetClass = TargetClass(
                property = source.property
              )
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

}


data class SomeTestClass(val s: String)
data class SomeOtherTestClass(val s: Int) {
    companion object
}

interface SomeTestMapper {
    fun toSomeOtherTestClass(source: SomeTestClass): SomeOtherTestClass
}

/**
 * Is referenced by META-INF/konvert/io.mcarle.konvert.api.KonvertTo
 */
@GeneratedKonverter(priority = 10)
fun SomeTestClass.toSomeOtherTestClass() = SomeOtherTestClass(s.toInt())

/**
 * Is referenced by META-INF/konvert/io.mcarle.konvert.api.KonvertFrom
 */
@GeneratedKonverter(priority = 11)
fun SomeOtherTestClass.Companion.fromSomeTestClass(source: SomeTestClass) = SomeOtherTestClass(
    source.s.toInt()
)

object SomeTestMapperImpl : SomeTestMapper {
    /**
     * Is referenced by META-INF/konvert/io.mcarle.konvert.api.Konvert
     */
    @GeneratedKonverter(priority = 12)
    override fun toSomeOtherTestClass(source: SomeTestClass): SomeOtherTestClass {
        return SomeOtherTestClass(source.s.toInt())
    }
}


