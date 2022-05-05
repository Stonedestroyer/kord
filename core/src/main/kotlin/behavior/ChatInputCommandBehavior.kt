package dev.kord.core.behavior

import dev.kord.common.annotation.KordUnsafe
import dev.kord.core.cache.data.ApplicationCommandData
import dev.kord.core.entity.application.ChatInputCommandCommand
import dev.kord.core.entity.application.GlobalChatInputCommand
import dev.kord.core.entity.application.GuildChatInputCommand
import dev.kord.rest.builder.interaction.ChatInputModifyBuilder
import dev.kord.rest.builder.interaction.ChatInputModifyBuilderImpl


public interface ChatInputCommandBehavior : ApplicationCommandBehavior {

     public suspend fun edit(builder: suspend ChatInputModifyBuilder.() -> Unit): ChatInputCommandCommand

}


public interface GuildChatInputCommandBehavior : ChatInputCommandBehavior, GuildApplicationCommandBehavior {

     override suspend fun edit(builder: suspend ChatInputModifyBuilder.() -> Unit): GuildChatInputCommand {
          val request = ChatInputModifyBuilderImpl().apply { builder() }.toRequest()
          val response = service.modifyGuildApplicationCommand(applicationId, guildId, id, request)
          val data = ApplicationCommandData.from(response)
          return GuildChatInputCommand(data, service)
     }
}



public interface GlobalChatInputCommandBehavior : ChatInputCommandBehavior,GlobalApplicationCommandBehavior {
     @OptIn(KordUnsafe::class)
     override suspend fun edit(builder: suspend ChatInputModifyBuilder.() -> Unit): GlobalChatInputCommand {
          val request = ChatInputModifyBuilderImpl().apply { builder() }.toRequest()
          val response = service.modifyGlobalApplicationCommand(applicationId,id, request)
          val data = ApplicationCommandData.from(response)
          return GlobalChatInputCommand(data, service)
     }
}
