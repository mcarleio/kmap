package io.mcarle.konvert.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.StringToIntConverter
import io.mcarle.konvert.processor.exceptions.AmbiguousConstructorException
import io.mcarle.konvert.processor.exceptions.NoMatchingConstructorException
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals


@Suppress("RedundantVisibilityModifier")
class TargetStructureITest : KonverterITest() {

    @Test
    fun setPropertiesAfterConstructor() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(source="sourceProperty1", target="targetProperty1"), Mapping(source="sourceProperty2", target="targetProperty2"), Mapping(source="sourceProperty3", target="targetProperty3")
])
class SourceClass(
    val sourceProperty1: String,
    val sourceProperty2: String,
    val sourceProperty3: String
)
class TargetClass(
    var targetProperty1: String,
) {
    var targetProperty2: String = ""
    var targetProperty3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty1 = sourceProperty1
            ).also { targetClass ->
              targetClass.targetProperty2 = sourceProperty2
              targetClass.targetProperty3 = sourceProperty3
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun ignoreDefinedProperties() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(source="sourceProperty1", target="targetProperty1"),
    Mapping(source="sourceProperty2", target="targetProperty2"),
    Mapping(target="targetProperty3", ignore = true)
])
class SourceClass(
    val sourceProperty1: String,
    val sourceProperty2: String,
    val sourceProperty3: String
)
class TargetClass(
    var targetProperty1: String,
) {
    var targetProperty2: String = ""
    var targetProperty3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty1 = sourceProperty1
            ).also { targetClass ->
              targetClass.targetProperty2 = sourceProperty2
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useSameSourcePropertyForDifferentTargets() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(source="sourceProperty", target="targetProperty1"),
    Mapping(source="sourceProperty", target="targetProperty2"),
    Mapping(source="sourceProperty", target="targetProperty3")
])
class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    var targetProperty1: String,
    var targetProperty2: String,
    var targetProperty3: String
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty1 = sourceProperty,
              targetProperty2 = sourceProperty,
              targetProperty3 = sourceProperty
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useConstantAndExpression() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="targetProperty1", expression = "it.sourceProperty.lowercase()"),
    Mapping(target="targetProperty2", constant = "\"Hello\""),
])
class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    var targetProperty1: String,
    var targetProperty2: String
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty1 = let { it.sourceProperty.lowercase() },
              targetProperty2 = "Hello"
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun enableProvidedConverter() {
        val (compilation) = super.compileWith(
            emptyList(),
            listOf(StringToIntConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.api.converter.STRING_TO_INT_CONVERTER

@KonvertTo(TargetClass::class, mappings=[
    Mapping(source="sourceProperty", target="targetProperty", enable=[STRING_TO_INT_CONVERTER])
])
class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    var targetProperty: Int
)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty = sourceProperty.toInt()
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotSetPropertiesNotPartOfNonEmptyConstructor() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="targetProperty", source = "property")
])
class SourceClass(
    val property: String
)
class TargetClass(
    var targetProperty: String
) {
    var property: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty = property
            )
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun setExplicitDefinedPropertiesNotPartOfNonEmptyConstructor() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="targetProperty", source = "property"),
    Mapping(target="property", source = "property")
])
class SourceClass(
    val property: String
)
class TargetClass(
    var targetProperty: String
) {
    var property: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              targetProperty = property
            ).also { targetClass ->
              targetClass.property = property
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun setNotDefinedPropertiesOnEmptyConstructor() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="targetProperty", source = "property")
])
class SourceClass(
    val property: String
)
class TargetClass {
    var targetProperty: String = ""
    var property: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass().also { targetClass ->
              targetClass.targetProperty = property
              targetClass.property = property
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun setMutableProperties() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: String,
    val property3: String
)
class TargetClass {
    var property1: String = ""
    var property2: String = ""
    var property3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass().also { targetClass ->
              targetClass.property1 = property1
              targetClass.property2 = property2
              targetClass.property3 = property3
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotSetFinalProperties() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: String,
    val property3: String
)
class TargetClass {
    val property1: String = ""
    val property2: String = ""
    val property3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass()
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun useDefinedConstructor() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, constructor = [MyInt::class], mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: Int,
    val property3: String
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    constructor(property1: String) {
        this.property1 = property1
    }
    constructor(property2: Int) {
        this.property2 = property2
    }
    val property3: String = ""
}
typealias MyInt = Int
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property2 = property2
            ).also { targetClass ->
              targetClass.property1 = property1
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun enforceUsingEmptyConstructor() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, constructor = [])
class SourceClass(
    val property1: String,
    val property2: Int,
    val property3: String
)
class TargetClass(
    var property1: String,
    var property2: Int,
    var property3: String,
) {
    constructor(): this("",-1,"")
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass().also { targetClass ->
              targetClass.property1 = property1
              targetClass.property2 = property2
              targetClass.property3 = property3
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun doNotUseEmptyConstructorWhenAnotherDefined() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, constructor = [Int::class], mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: Int,
    val property3: String
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    constructor()
    constructor(property2: MyInt) {
        this.property2 = property2
    }
    val property3: String = ""
}
typealias MyInt = Int
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property2 = property2
            ).also { targetClass ->
              targetClass.property1 = property1
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun ignorePrivateConstructor() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings=[
    Mapping(target="property1", source = "property1")
])
class SourceClass(
    val property1: String,
    val property2: Int,
    val property3: String
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    private constructor()
    private constructor(property1: String) {
        this.property1 = property1
    }
    constructor(property2: Int) {
        this.property2 = property2
    }
    val property3: String = ""
}
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              property2 = property2
            ).also { targetClass ->
              targetClass.property1 = property1
            }
            """.trimIndent(),
            extensionFunctionCode
        )
    }

    @Test
    fun throwAmbiguousConstructorException() {
        val (_, compilationResult) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            false,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(
    val property1: String,
    val property2: Int,
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    constructor(property1: String) {
        this.property1 = property1
    }
    constructor(property2: Int) {
        this.property2 = property2
    }
}
                """.trimIndent()
            )
        )
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = compilationResult.exitCode)
        assertContains(compilationResult.messages, AmbiguousConstructorException::class.qualifiedName!!)
    }

    @Test
    fun throwNoMatchingConstructorException() {
        val (_, compilationResult) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            false,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class)
class SourceClass(
    val property1: String,
    val property2: Int,
)
class TargetClass {
    var property1: String = ""
    var property2: Int = -1
    constructor(property3: String) {
        this.property1 = property3
    }
    constructor(property3: Int) {
        this.property2 = property3
    }
}
                """.trimIndent()
            )
        )
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = compilationResult.exitCode)
        assertContains(compilationResult.messages, NoMatchingConstructorException::class.qualifiedName!!)
    }

    @Test
    fun throwNoMatchingConstructorExceptionWhenConstructorTypesWrong() {
        val (_, compilationResult) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            false,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo

@KonvertTo(TargetClass::class, constructor = [String::class, Long::class])
class SourceClass(
    val property1: String,
    val property2: Int,
)
class TargetClass(
    val property1: String,
    val property2: Int,
)
                """.trimIndent()
            )
        )
        assertEquals(expected = KotlinCompilation.ExitCode.COMPILATION_ERROR, actual = compilationResult.exitCode)
        assertContains(compilationResult.messages, NoMatchingConstructorException::class.qualifiedName!!)
    }

    @Test
    fun workWithValueClasses() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            emptyList(),
            true,
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.KonvertTo
import io.mcarle.konvert.api.Mapping

@KonvertTo(TargetClass::class, mappings = [
    Mapping(source = "source", target ="target")
])
data class SourceClass(val source: SourceValueClass)
data class TargetClass(val target: TargetValueClass)


@KonvertTo(TargetValueClass::class)
@JvmInline
value class SourceValueClass(val value: String)

@JvmInline
value class TargetValueClass(val value: String)
                """.trimIndent()
            )
        )
        val extensionFunctionCode = compilation.generatedSourceFor("SourceClassKonverter.kt")
        println(extensionFunctionCode)

        assertSourceEquals(
            """
            public fun SourceClass.toTargetClass(): TargetClass = TargetClass(
              target = source.toTargetValueClass()
            )
            """.trimIndent(),
            extensionFunctionCode
        )

        val extensionFunctionValueClassCode = compilation.generatedSourceFor("SourceValueClassKonverter.kt")
        println(extensionFunctionValueClassCode)

        assertSourceEquals(
            """
            public fun SourceValueClass.toTargetValueClass(): TargetValueClass = TargetValueClass(
              value = value
            )
            """.trimIndent(),
            extensionFunctionValueClassCode
        )
    }

}
