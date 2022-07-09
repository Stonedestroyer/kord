package dev.kord.ksp.kordenum

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSType
import dev.kord.ksp.GenerateKordEnum
import dev.kord.ksp.GenerateKordEnum.ValueType
import dev.kord.ksp.GenerateKordEnum.ValueType.INT
import dev.kord.ksp.GenerateKordEnum.ValueType.STRING
import dev.kord.ksp.argumentsToMap
import dev.kord.ksp.get
import dev.kord.ksp.kordenum.KordEnum.Entry
import kotlin.DeprecationLevel.*

/** Mapping of [GenerateKordEnum] that is easier to work with in [KordEnumProcessor]. */
internal class KordEnum(
    val name: String,
    val kDoc: String?,
    val valueType: ValueType,
    val valueName: String,
    val entries: List<Entry>,
    val deprecatedEntries: List<Entry>,
) {
    internal class Entry(
        val name: String,
        val kDoc: String?,
        val value: Comparable<*>,
        val isDeprecated: Boolean,
        val deprecationMessage: String = "",
        val replaceWith: ReplaceWith? = null,
        val deprecationLevel: DeprecationLevel = WARNING,
    )
}

/**
 * Maps [KSAnnotation] for [GenerateKordEnum] to [KordEnum].
 *
 * Returns `null` if mapping fails.
 */
internal fun KSAnnotation.toKordEnumOrNull(logger: KSPLogger): KordEnum? {
    val args = argumentsToMap()

    val name = args[GenerateKordEnum::name] as String
    val valueType = args[GenerateKordEnum::valueType].toValueType()
    val entries = args[GenerateKordEnum::entries] as List<*>
    val kDoc = args[GenerateKordEnum::kDoc].toKDoc()
    val valueName = args[GenerateKordEnum::valueName] as String
    val deprecatedEntries = args[GenerateKordEnum::deprecatedEntries] as List<*>

    return KordEnum(
        name, kDoc, valueType, valueName,
        entries.map { it.toEntryOrNull(valueType, deprecated = false, logger) ?: return null },
        deprecatedEntries.map { it.toEntryOrNull(valueType, deprecated = true, logger) ?: return null },
    )
}

/** Maps [KSType] to [ValueType]. */
private fun Any?.toValueType() = when (val name = (this as KSType).declaration.qualifiedName?.asString()) {
    "dev.kord.ksp.GenerateKordEnum.ValueType.INT" -> INT
    "dev.kord.ksp.GenerateKordEnum.ValueType.STRING" -> STRING
    else -> error("Unknown GenerateKordEnum.ValueType: $name")
}

private fun Any?.toKDoc() = (this as String).trimIndent().ifBlank { null }

/**
 * Maps [KSAnnotation] for [GenerateKordEnum.Entry] to [Entry].
 *
 * Returns `null` if mapping fails.
 */
private fun Any?.toEntryOrNull(valueType: ValueType, deprecated: Boolean, logger: KSPLogger): Entry? {
    val args = (this as KSAnnotation).argumentsToMap()

    val name = args[GenerateKordEnum.Entry::name] as String
    val intValue = args[GenerateKordEnum.Entry::intValue] as Int
    val stringValue = args[GenerateKordEnum.Entry::stringValue] as String
    val kDoc = args[GenerateKordEnum.Entry::kDoc].toKDoc()
    val deprecationMessage = args[GenerateKordEnum.Entry::deprecationMessage] as String
    val replaceWith = args[GenerateKordEnum.Entry::replaceWith].toReplaceWith()
    val deprecationLevel = args[GenerateKordEnum.Entry::deprecationLevel].toDeprecationLevel()

    val value = when (valueType) {
        INT -> {
            if (stringValue != GenerateKordEnum.Entry.DEFAULT_STRING_VALUE) {
                logger.error("Specified stringValue for valueType $valueType", symbol = this)
                return null
            }
            if (intValue == GenerateKordEnum.Entry.DEFAULT_INT_VALUE) {
                logger.error("Didn't specify intValue for valueType $valueType", symbol = this)
                return null
            }

            intValue
        }
        STRING -> {
            if (intValue != GenerateKordEnum.Entry.DEFAULT_INT_VALUE) {
                logger.error("Specified intValue for valueType $valueType", symbol = this)
                return null
            }
            if (stringValue == GenerateKordEnum.Entry.DEFAULT_STRING_VALUE) {
                logger.error("Didn't specify stringValue for valueType $valueType", symbol = this)
                return null
            }

            stringValue
        }
    }

    return if (deprecated) {
        if (deprecationMessage.isBlank()) {
            logger.error("deprecationMessage is required", symbol = this)
            return null
        }

        Entry(
            name, kDoc, value, isDeprecated = true,
            deprecationMessage,
            replaceWith.takeIf { it.expression.isNotBlank() },
            deprecationLevel,
        )
    } else {
        if (deprecationMessage.isNotEmpty()) {
            logger.error("deprecationMessage is not allowed", symbol = this)
            return null
        }
        if (replaceWith.expression.isNotEmpty() || replaceWith.imports.isNotEmpty()) {
            logger.error("replaceWith is not allowed", symbol = this)
            return null
        }
        if (deprecationLevel != WARNING) {
            logger.error("deprecationLevel is not allowed", symbol = this)
            return null
        }

        Entry(name, kDoc, value, isDeprecated = false)
    }
}

/** Maps [KSAnnotation] to [ReplaceWith]. */
private fun Any?.toReplaceWith(): ReplaceWith {
    val args = (this as KSAnnotation).argumentsToMap()

    val expression = args[ReplaceWith::expression] as String
    val imports = @Suppress("UNCHECKED_CAST") (args[ReplaceWith::imports] as List<String>)

    return ReplaceWith(expression, *imports.toTypedArray())
}

/** Maps [KSType] to [DeprecationLevel]. */
private fun Any?.toDeprecationLevel() = when (val name = (this as KSType).declaration.qualifiedName?.asString()) {
    "kotlin.DeprecationLevel.WARNING" -> WARNING
    "kotlin.DeprecationLevel.ERROR" -> ERROR
    "kotlin.DeprecationLevel.HIDDEN" -> HIDDEN
    else -> error("Unknown DeprecationLevel: $name")
}
