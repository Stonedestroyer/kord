package dev.kord.rest.builder.auto_moderation

import dev.kord.common.annotation.KordDsl
import dev.kord.common.entity.AutoModerationActionType.*
import dev.kord.common.entity.AutoModerationRuleEventType
import dev.kord.common.entity.AutoModerationRuleKeywordPresetType
import dev.kord.common.entity.AutoModerationRuleTriggerType
import dev.kord.common.entity.AutoModerationRuleTriggerType.*
import dev.kord.common.entity.Permission.ModerateMembers
import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.AuditBuilder
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.time.Duration

@KordDsl
public sealed interface AutoModerationRuleBuilder : AuditBuilder {

    /** The rule name. */
    public val name: String?

    /** Use this to set [name][AutoModerationRuleBuilder.name] for [AutoModerationRuleBuilder]. */
    public fun assignName(name: String)

    /** The rule [event type][AutoModerationRuleEventType]. */
    public val eventType: AutoModerationRuleEventType?

    /** Use this to set [eventType][AutoModerationRuleBuilder.eventType] for [AutoModerationRuleBuilder]. */
    public fun assignEventType(eventType: AutoModerationRuleEventType)

    /** The rule [trigger type][AutoModerationRuleTriggerType]. */
    public val triggerType: AutoModerationRuleTriggerType

    /** The actions which will execute when the rule is triggered. */
    public val actions: MutableList<AutoModerationActionBuilder>?

    /** Use this to set [actions][AutoModerationRuleBuilder.actions] for [AutoModerationRuleBuilder]. */
    public fun assignActions(actions: MutableList<AutoModerationActionBuilder>)

    /** Whether the rule is enabled (`false` by default). */
    public var enabled: Boolean?

    /** The IDs of the roles that should not be affected by the rule. */
    public var exemptRoles: MutableList<Snowflake>?

    /** Exempt a [role][roleId] from being affected by the rule. */
    public fun exemptRole(roleId: Snowflake) {
        exemptRoles?.add(roleId) ?: run { exemptRoles = mutableListOf(roleId) }
    }

    /** The IDs of the channels that should not be affected by the rule. */
    public var exemptChannels: MutableList<Snowflake>?

    /** Exempt a [channel][channelId] from being affected by the rule. */
    public fun exemptChannel(channelId: Snowflake) {
        exemptChannels?.add(channelId) ?: run { exemptChannels = mutableListOf(channelId) }
    }
}

/** Add a [BlockMessage] action which will execute whenever the rule is triggered. */
public inline fun AutoModerationRuleBuilder.blockMessage(
    builder: BlockMessageAutoModerationActionBuilder.() -> Unit = {},
) {
    contract { callsInPlace(builder, EXACTLY_ONCE) }
    val action = BlockMessageAutoModerationActionBuilder().apply(builder)
    actions?.add(action) ?: assignActions(mutableListOf(action))
}

/**
 * Add a [SendAlertMessage] action which will execute whenever the rule is triggered.
 *
 * @param channelId the ID of the channel to which user content should be logged.
 */
public inline fun AutoModerationRuleBuilder.sendAlertMessage(
    channelId: Snowflake,
    builder: SendAlertMessageAutoModerationActionBuilder.() -> Unit = {},
) {
    contract { callsInPlace(builder, EXACTLY_ONCE) }
    val action = SendAlertMessageAutoModerationActionBuilder(channelId).apply(builder)
    actions?.add(action) ?: assignActions(mutableListOf(action))
}

@KordDsl
public sealed interface KeywordAutoModerationRuleBuilder : AutoModerationRuleBuilder {

    override val triggerType: Keyword get() = Keyword

    /**
     * Substrings which will be searched for in content.
     *
     * A keyword can be a phrase which contains multiple words. Wildcard symbols can be used to customize how each
     * keyword will be matched. See
     * [keyword matching strategies](https://discord.com/developers/docs/resources/auto-moderation#auto-moderation-rule-object-keyword-matching-strategies).
     */
    public val keywords: MutableList<String>?

    /** Use this to set [keywords][KeywordAutoModerationRuleBuilder.keywords] for [KeywordAutoModerationRuleBuilder]. */
    public fun assignKeywords(keywords: MutableList<String>)

    /**
     * Add a [keyword] to [keywords].
     *
     * A keyword can be a phrase which contains multiple words. Wildcard symbols can be used to customize how each
     * keyword will be matched. See
     * [keyword matching strategies](https://discord.com/developers/docs/resources/auto-moderation#auto-moderation-rule-object-keyword-matching-strategies).
     */
    public fun keyword(keyword: String) {
        keywords?.add(keyword) ?: assignKeywords(mutableListOf(keyword))
    }
}

/**
 * Add a [Timeout] action which will execute whenever the rule is triggered.
 *
 * The [ModerateMembers] permission is required to use this action.
 *
 * @param duration the timeout duration.
 */
public inline fun KeywordAutoModerationRuleBuilder.timeout(
    duration: Duration,
    builder: TimeoutAutoModerationActionBuilder.() -> Unit = {},
) {
    contract { callsInPlace(builder, EXACTLY_ONCE) }
    val action = TimeoutAutoModerationActionBuilder(duration).apply(builder)
    actions?.add(action) ?: assignActions(mutableListOf(action))
}

@KordDsl
public sealed interface HarmfulLinkAutoModerationRuleBuilder : AutoModerationRuleBuilder {
    override val triggerType: HarmfulLink get() = HarmfulLink
}

@KordDsl
public sealed interface SpamAutoModerationRuleBuilder : AutoModerationRuleBuilder {
    override val triggerType: Spam get() = Spam
}

@KordDsl
public sealed interface KeywordPresetAutoModerationRuleBuilder : AutoModerationRuleBuilder {

    override val triggerType: KeywordPreset get() = KeywordPreset

    /** The internally pre-defined wordsets which will be searched for in content. */
    public val presets: MutableList<AutoModerationRuleKeywordPresetType>?

    /**
     * Use this to set [presets][KeywordPresetAutoModerationRuleBuilder.presets] for
     * [KeywordPresetAutoModerationRuleBuilder].
     */
    public fun assignPresets(presets: MutableList<AutoModerationRuleKeywordPresetType>)

    /** Add a [preset] to [presets]. */
    public fun preset(preset: AutoModerationRuleKeywordPresetType) {
        presets?.add(preset) ?: assignPresets(mutableListOf(preset))
    }

    /**
     * Substrings which will be exempt from triggering the [presets].
     *
     * A keyword can be a phrase which contains multiple words.
     */
    public var allowList: MutableList<String>?

    /**
     * Add a [keyword] to [allowList].
     *
     * A keyword can be a phrase which contains multiple words.
     */
    public fun allow(keyword: String) {
        allowList?.add(keyword) ?: run { allowList = mutableListOf(keyword) }
    }
}
