package io.mcarle.konvert.converter

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.converter.api.TypeConverter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class EnumToXConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            EnumToStringConverter(),
            EnumToIntConverter(),
            EnumToUIntConverter(),
            EnumToLongConverter(),
            EnumToULongConverter(),
            EnumToShortConverter(),
            EnumToUShortConverter(),
            EnumToNumberConverter(),
            EnumToDoubleConverter(),
            EnumToByteConverter(),
            EnumToUByteConverter(),
            EnumToCharConverter(),
            EnumToFloatConverter(),
            EnumToBigIntegerConverter(),
            EnumToBigDecimalConverter(),
        ).toConverterTestArgumentsWithType {
            "MyEnum" to it.targetClass.qualifiedName
        }

        private val enumToXConverterClasses: Set<Class<out EnumToXConverter>> = Reflections(EnumToXConverter::class.java)
            .getSubTypesOf(EnumToXConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(
            enumToXConverterClasses.newConverterInstance(simpleConverterName),
            sourceTypeName,
            targetTypeName
        )
    }

    override fun generateAdditionalCode(): List<SourceFile> = listOf(
        SourceFile.kotlin(
            name = "MyEnum.kt",
            contents =
            """
enum class MyEnum {
    XXX,
    YYY,
    ZZZ
}
            """.trimIndent()
        )
    )

    override fun verifyMapper(
        converter: TypeConverter,
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val enumValue =
            (sourceKClass.members.first { it.name == "test" }.returnType.classifier as KClass<*>).java.enumConstants.random() as Enum<*>
        val sourceInstance = sourceKClass.constructors.first().call(
            enumValue
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }

        Assertions.assertEquals(
            when {
                targetTypeName.startsWith("java.math.BigInteger") -> enumValue.ordinal.toBigInteger()
                targetTypeName.startsWith("java.math.BigDecimal") -> enumValue.ordinal.toBigDecimal()
                targetTypeName.startsWith("kotlin.String") -> enumValue.name
                targetTypeName.startsWith("kotlin.Int") -> enumValue.ordinal
                targetTypeName.startsWith("kotlin.UInt") -> enumValue.ordinal.toUInt()
                targetTypeName.startsWith("kotlin.Long") -> enumValue.ordinal.toLong()
                targetTypeName.startsWith("kotlin.ULong") -> enumValue.ordinal.toULong()
                targetTypeName.startsWith("kotlin.Short") -> enumValue.ordinal.toShort()
                targetTypeName.startsWith("kotlin.UShort") -> enumValue.ordinal.toUShort()
                targetTypeName.startsWith("kotlin.Number") -> enumValue.ordinal
                targetTypeName.startsWith("kotlin.Double") -> enumValue.ordinal.toDouble()
                targetTypeName.startsWith("kotlin.Byte") -> enumValue.ordinal.toByte()
                targetTypeName.startsWith("kotlin.UByte") -> enumValue.ordinal.toUByte()
                targetTypeName.startsWith("kotlin.Char") -> enumValue.ordinal.toChar()
                targetTypeName.startsWith("kotlin.Float") -> enumValue.ordinal.toFloat()
                else -> null
            },
            targetValue
        )
    }

}

