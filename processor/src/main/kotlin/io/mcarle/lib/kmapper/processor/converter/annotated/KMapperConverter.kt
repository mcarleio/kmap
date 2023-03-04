package io.mcarle.lib.kmapper.processor.converter.annotated

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.api.KMappers
import io.mcarle.lib.kmapper.api.annotation.KMapping
import io.mcarle.lib.kmapper.converter.api.ConverterConfig
import io.mcarle.lib.kmapper.converter.api.Priority
import io.mcarle.lib.kmapper.converter.api.TypeConverter

class KMapperConverter constructor(
    override val annotation: KMapping,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val mapKSClassDeclaration: KSClassDeclaration,
    val mapKSFunctionDeclaration: KSFunctionDeclaration,
) : TypeConverter, AnnotatedConverter<KMapping> {

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    val mapFunctionName: String = mapKSFunctionDeclaration.simpleName.asString()
    val paramName: String = mapKSFunctionDeclaration.parameters.first().name!!.asString()

    override val enabledByDefault: Boolean = true
    override val priority: Priority = annotation.priority

    override fun init(config: ConverterConfig) {
        // Nothing to initialize
    }

    override fun matches(source: KSType, target: KSType): Boolean {
        return sourceType == source && targetType == target
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        return "${KMappers::class.qualifiedName}.get<${mapKSClassDeclaration.qualifiedName?.asString()}>().$mapFunctionName($paramName = $fieldName)"
    }

}