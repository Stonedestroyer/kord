package dev.kord.core.event.guild

import dev.kord.common.entity.Snowflake
import dev.kord.common.exception.RequestException
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.ChannelBehavior
import dev.kord.core.cache.data.InviteDeleteData
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Strategizable
import dev.kord.core.entity.channel.Channel
import dev.kord.core.event.Event
import dev.kord.core.event.kordCoroutineScope
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.supplier.EntitySupplier
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.coroutines.CoroutineScope
import kotlin.DeprecationLevel.HIDDEN

/**
 * Sent when an invite is deleted.
 */
public class InviteDeleteEvent(
    public val data: InviteDeleteData,
    override val kord: Kord,
    override val shard: Int,
    override val supplier: EntitySupplier = kord.defaultSupplier,
    public val coroutineScope: CoroutineScope = kordCoroutineScope(kord)
) : Event, CoroutineScope by coroutineScope, Strategizable {

    /**
     * The id of the [Channel] the invite is for.
     */
    public val channelId: Snowflake get() = data.channelId

    /**
     * The behavior of the [Channel] the invite is for.
     */
    public val channel: ChannelBehavior get() = ChannelBehavior(id = channelId, kord = kord)

    /**
     * The id of the [Guild] of the invite.
     */
    public val guildId: Snowflake? get() = data.guildId.value

    /**
     * The behavior of the [Guild] of the invite.
     */
    public val guild: GuildBehavior? get() = guildId?.let { GuildBehavior(id = it, kord = kord) }

    /**
     * The unique invite code.
     */
    public val code: String get() = data.code

    /**
     * Requests to get the [Channel] this invite is for.
     *
     * @throws [RequestException] if anything went wrong during the request.
     * @throws [EntityNotFoundException] if the  wasn't present.
     */
    public suspend fun getChannel(): Channel = supplier.getChannel(channelId)

    /**
     * Requests to get the [Channel] this invite is for,
     * returns null if the channel isn't present.
     *
     * @throws [RequestException] if anything went wrong during the request.
     */
    public suspend fun getChannelOrNull(): Channel? = supplier.getChannelOrNull(channelId)

    /**
     * Requests to get the [Guild] of the invite.
     */
    @Deprecated(
        "'guildId' might not be present, use 'getGuildOrNull' instead.",
        ReplaceWith("this.getGuildOrNull()"),
        level = HIDDEN,
    )
    public suspend fun getGuild(): Guild = supplier.getGuild(guildId!!)

    /**
     * Requests to get the [Guild] of the invite.
     * returns null if the guild isn't present, or if invite does not target a guild.
     *
     * @throws [RequestException] if anything went wrong during the request.
     */
    public suspend fun getGuildOrNull(): Guild? = guildId?.let { supplier.getGuildOrNull(it) }

    override fun withStrategy(strategy: EntitySupplyStrategy<*>): InviteDeleteEvent =
        InviteDeleteEvent(data, kord, shard, strategy.supply(kord))

    override fun toString(): String {
        return "InviteDeleteEvent(data=$data, kord=$kord, shard=$shard, supplier=$supplier)"
    }

}
