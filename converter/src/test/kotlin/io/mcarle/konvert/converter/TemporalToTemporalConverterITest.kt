package io.mcarle.konvert.converter

import io.mcarle.konvert.converter.utils.ConverterITest
import io.mcarle.konvert.converter.utils.VerificationData
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.reflections.Reflections
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@OptIn(ExperimentalCompilerApi::class)
class TemporalToTemporalConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            OffsetDateTimeToInstantConverter(),
            OffsetDateTimeToZonedDateTimeConverter(),
            OffsetDateTimeToLocalDateTimeConverter(),
            OffsetDateTimeToLocalDateConverter(),
            OffsetDateTimeToLocalTimeConverter(),
            OffsetDateTimeToOffsetTimeConverter(),
            ZonedDateTimeToInstantConverter(),
            ZonedDateTimeToOffsetDateTimeConverter(),
            ZonedDateTimeToLocalDateTimeConverter(),
            ZonedDateTimeToLocalDateConverter(),
            ZonedDateTimeToLocalTimeConverter(),
            ZonedDateTimeToOffsetTimeConverter(),
            LocalDateTimeToLocalDateConverter(),
            LocalDateTimeToLocalTimeConverter(),
            OffsetTimeToLocalTimeConverter(),
        ).toConverterTestArgumentsWithType {
            it.sourceClass.qualifiedName to it.targetClass.qualifiedName
        }

        private val temporalToTemporalConverterClasses: Set<Class<out TemporalToTemporalConverter>> =
            Reflections(TemporalToTemporalConverter::class.java)
                .getSubTypesOf(TemporalToTemporalConverter::class.java)
    }

    @ParameterizedTest
    @MethodSource("converterList")
    fun converterTest(simpleConverterName: String, sourceTypeName: String, targetTypeName: String) {
        executeTest(
            sourceTypeName = sourceTypeName,
            targetTypeName = targetTypeName,
            converter = temporalToTemporalConverterClasses.newConverterInstance(simpleConverterName)
        )
    }

    override fun verify(verificationData: VerificationData) {
        val instant = Instant.now()
        val sourceValues = verificationData.sourceVariables.map { sourceVariable ->
            val sourceTypeName = sourceVariable.second
            when {
                sourceTypeName.startsWith("java.time.ZonedDateTime") -> instant.atOffset(ZoneOffset.UTC).toZonedDateTime()
                sourceTypeName.startsWith("java.time.OffsetDateTime") -> instant.atOffset(ZoneOffset.UTC)
                sourceTypeName.startsWith("java.time.LocalDateTime") -> instant.atOffset(ZoneOffset.UTC).toLocalDateTime()
                sourceTypeName.startsWith("java.time.OffsetTime") -> instant.atOffset(ZoneOffset.UTC).toOffsetTime()
                else -> null
            }
        }
        val sourceInstance = verificationData.sourceKClass.constructors.first().call(*sourceValues.toTypedArray())

        val targetInstance = verificationData.mapperFunction.call(verificationData.mapperInstance, sourceInstance)

        verificationData.targetVariables.forEachIndexed { index, targetVariable ->
            val targetName = targetVariable.first
            val targetTypeName = targetVariable.second
            val targetValue = assertDoesNotThrow {
                verificationData.targetKClass.members.first { it.name == targetName }.call(targetInstance)
            }
            val sourceTypeName = verificationData.sourceVariables[index].second
            when {
                targetTypeName.startsWith("java.time.Instant") -> {
                    targetValue as Instant
                    Assertions.assertEquals(instant, targetValue)
                }

                targetTypeName.startsWith("java.time.ZonedDateTime") -> {
                    targetValue as ZonedDateTime
                    Assertions.assertEquals(instant, targetValue.toInstant())
                }

                targetTypeName.startsWith("java.time.OffsetDateTime") -> {
                    targetValue as OffsetDateTime
                    Assertions.assertEquals(instant, targetValue.toInstant())
                }

                targetTypeName.startsWith("java.time.LocalDateTime") -> {
                    targetValue as LocalDateTime
                    Assertions.assertEquals(
                        when {
                            sourceTypeName.startsWith("java.time.ZonedDateTime") -> instant.atOffset(ZoneOffset.UTC).toZonedDateTime()
                                .toLocalDateTime()
                            sourceTypeName.startsWith("java.time.OffsetDateTime") -> instant.atOffset(ZoneOffset.UTC).toLocalDateTime()
                            else -> null
                        },
                        targetValue
                    )
                }

                targetTypeName.startsWith("java.time.LocalDate") -> {
                    targetValue as LocalDate
                    Assertions.assertEquals(
                        when {
                            sourceTypeName.startsWith("java.time.ZonedDateTime") -> instant.atOffset(ZoneOffset.UTC).toZonedDateTime()
                                .toLocalDate()
                            sourceTypeName.startsWith("java.time.OffsetDateTime") -> instant.atOffset(ZoneOffset.UTC).toLocalDate()
                            sourceTypeName.startsWith("java.time.LocalDateTime") -> instant.atOffset(ZoneOffset.UTC).toLocalDate()
                            else -> null
                        },
                        targetValue
                    )
                }

                targetTypeName.startsWith("java.time.OffsetTime") -> {
                    targetValue as OffsetTime
                    Assertions.assertEquals(
                        when {
                            sourceTypeName.startsWith("java.time.ZonedDateTime") -> instant.atOffset(ZoneOffset.UTC).toZonedDateTime()
                                .toOffsetDateTime().toOffsetTime()
                            sourceTypeName.startsWith("java.time.OffsetDateTime") -> instant.atOffset(ZoneOffset.UTC).toOffsetTime()
                            else -> null
                        },
                        targetValue
                    )
                }

                targetTypeName.startsWith("java.time.LocalTime") -> {
                    targetValue as LocalTime
                    Assertions.assertEquals(
                        when {
                            sourceTypeName.startsWith("java.time.ZonedDateTime") -> instant.atOffset(ZoneOffset.UTC).toZonedDateTime()
                                .toLocalTime()
                            sourceTypeName.startsWith("java.time.OffsetDateTime") -> instant.atOffset(ZoneOffset.UTC).toLocalTime()
                            sourceTypeName.startsWith("java.time.LocalDateTime") -> instant.atOffset(ZoneOffset.UTC).toLocalDateTime()
                                .toLocalTime()
                            sourceTypeName.startsWith("java.time.OffsetTime") -> instant.atOffset(ZoneOffset.UTC).toOffsetTime()
                                .toLocalTime()

                            else -> null
                        },
                        targetValue
                    )
                }
            }
        }
    }

}

