package io.mcarle.konvert.processor.codegen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSValueParameter
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.api.classDeclaration
import io.mcarle.konvert.processor.SourceDataExtractionStrategy

class PropertyMappingResolver(
    private val logger: KSPLogger,
    private val sourceDataExtractionStrategy: SourceDataExtractionStrategy
) {
    fun determinePropertyMappings(
        source: Source,
        mappings: List<Mapping>,
        additionalSourceParameters: List<KSValueParameter>
    ): List<PropertyMappingInfo> {
        val sourceDataList = sourceDataExtractionStrategy.extract(source.type.classDeclaration()!!)

        val propertiesWithoutSource = getPropertyMappingsWithoutSource(mappings, source.paramName)
        val propertiesWithSource = getPropertyMappingsWithSource(mappings, sourceDataList, source.paramName)
        val propertiesFromAdditionalParameters = getPropertyMappingsFromAdditionalParameters(additionalSourceParameters)
        val propertiesWithoutMappings = getPropertyMappingsWithoutMappings(sourceDataList, source.paramName)

        return propertiesWithoutSource + propertiesWithSource + propertiesFromAdditionalParameters + propertiesWithoutMappings
    }

    private fun getPropertyMappingsFromAdditionalParameters(
        properties: List<KSValueParameter>,
    ) = properties
        .map { property ->
            val paramName = property.name!!.asString()
            PropertyMappingInfo(
                mappingParamName = null,
                sourceName = null,
                targetName = paramName,
                constant = paramName,
                expression = null,
                ignore = false,
                enableConverters = emptyList(),
                sourceData = null,
                isBasedOnAnnotation = false
            )
        }

    private fun getPropertyMappingsWithoutMappings(
        sourceDataList: List<SourceDataExtractionStrategy.SourceData>,
        mappingParamName: String?
    ) = sourceDataList
        .map { sourceData ->
            PropertyMappingInfo(
                mappingParamName = mappingParamName,
                sourceName = sourceData.name,
                targetName = sourceData.name,
                constant = null,
                expression = null,
                ignore = false,
                enableConverters = emptyList(),
                sourceData = sourceData,
                isBasedOnAnnotation = false
            )
        }

    private fun getPropertyMappingsWithSource(
        mappings: List<Mapping>,
        sourceDataList: List<SourceDataExtractionStrategy.SourceData>,
        mappingParamName: String?
    ) = mappings.filter { it.source.isNotEmpty() }.mapNotNull { annotation ->
        sourceDataList.firstOrNull { property ->
            property.name == annotation.source
        }?.let { annotation to it }
    }.map { (annotation, sourceData) ->
        PropertyMappingInfo(
            mappingParamName = mappingParamName,
            sourceName = sourceData.name,
            targetName = annotation.target,
            constant = annotation.constant.takeIf { it.isNotEmpty() },
            expression = annotation.expression.takeIf { it.isNotEmpty() },
            ignore = annotation.ignore,
            enableConverters = annotation.enable.toList(),
            sourceData = sourceData,
            isBasedOnAnnotation = true
        )
    }

    private fun getPropertyMappingsWithoutSource(
        mappings: List<Mapping>,
        mappingParamName: String?
    ) = mappings.filter { it.source.isEmpty() }.map { annotation ->
        PropertyMappingInfo(
            mappingParamName = mappingParamName,
            sourceName = null,
            targetName = annotation.target,
            constant = annotation.constant.takeIf { it.isNotEmpty() },
            expression = annotation.expression.takeIf { it.isNotEmpty() },
            ignore = annotation.ignore,
            enableConverters = annotation.enable.toList(),
            sourceData = null,
            isBasedOnAnnotation = true
        )
    }
}
