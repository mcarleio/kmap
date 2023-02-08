package io.mcarle.lib.kmapper.processor.converter

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.processor.isNullable
import java.time.*
import java.time.temporal.Temporal
import kotlin.reflect.KClass

abstract class XToTemporalConverter<T : Temporal>(
    internal val sourceClass: KClass<*>,
    internal val targetClass: KClass<T>,
) : AbstractTypeConverter() {

    private val temporalType: KSType by lazy {
        resolver.getClassDeclarationByName(targetClass.qualifiedName!!)!!.asType(emptyList()).makeNullable()
    }

    private val sourceType: KSType by lazy {
        resolver.getClassDeclarationByName(sourceClass.qualifiedName!!)!!.asType(emptyList())
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return (temporalType == target || temporalType == target.makeNullable()) &&
                (sourceType == source || sourceType == source.makeNotNullable())
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val sourceNullable = source.isNullable()
        val convertCode = convert(fieldName, if (sourceNullable) "?" else "")

        return if (sourceNullable && !target.isNullable()) {
            "$convertCode!!"
        } else {
            convertCode
        }
    }

    abstract fun convert(fieldName: String, nc: String): String
}

class StringToInstantConverter : XToTemporalConverter<Instant>(String::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String = "$fieldName$nc.let { java.time.Instant.parse(it) }"
}

class StringToZonedDateTimeConverter : XToTemporalConverter<ZonedDateTime>(String::class, ZonedDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.time.ZonedDateTime.parse(it) }"
}

class StringToOffsetDateTimeConverter : XToTemporalConverter<OffsetDateTime>(String::class, OffsetDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.time.OffsetDateTime.parse(it) }"
}

class StringToLocalDateTimeConverter : XToTemporalConverter<LocalDateTime>(String::class, LocalDateTime::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.time.LocalDateTime.parse(it) }"
}

class StringToLocalDateConverter : XToTemporalConverter<LocalDate>(String::class, LocalDate::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.time.LocalDate.parse(it) }"
}


class LongToInstantConverter : XToTemporalConverter<Instant>(Long::class, Instant::class) {
    override fun convert(fieldName: String, nc: String): String =
        "$fieldName$nc.let { java.time.Instant.ofEpochMilli(it) }"
}