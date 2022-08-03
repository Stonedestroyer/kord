package dev.kord.core.gateway.handler

import dev.kord.cache.api.DataCache
import dev.kord.core.Kord
import dev.kord.core.event.kordCoroutineScope
import dev.kord.core.gateway.ShardEvent
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging
import dev.kord.core.event.Event as CoreEvent

private val logger = KotlinLogging.logger { }

public class DefaultGatewayEventInterceptor(
    cache: DataCache,
    private val eventScope: ((ShardEvent, Kord) -> CoroutineScope) = { _, kord -> kordCoroutineScope(kord) }
) : GatewayEventInterceptor {

    private val listeners = listOf(
        AutoModerationEventHandler(cache),
        ChannelEventHandler(cache),
        GuildEventHandler(cache),
        InteractionEventHandler(cache),
        LifeCycleEventHandler(cache),
        MessageEventHandler(cache),
        ThreadEventHandler(cache),
        UserEventHandler(cache),
        VoiceEventHandler(cache),
        WebhookEventHandler(cache),
    )

    override suspend fun handle(event: ShardEvent, kord: Kord): CoreEvent? {
        return runCatching {
            for (listener in listeners) {
                val coreEvent = listener.handle(
                    event.event,
                    event.shard,
                    kord,
                    eventScope.invoke(event, kord)
                )
                if (coreEvent != null) {
                    return coreEvent
                }
            }
            return null
        }.onFailure {
            logger.error(it)
        }.getOrNull()
    }
}
