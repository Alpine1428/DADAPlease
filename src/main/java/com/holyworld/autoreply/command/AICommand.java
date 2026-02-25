package com.holyworld.autoreply.command;

import com.holyworld.autoreply.HolyWorldAutoReply;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.text.Text;

public class AICommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("ai")
                    .then(ClientCommandManager.literal("start")
                        .executes(ctx -> {
                            HolyWorldAutoReply.setEnabled(true);
                            ctx.getSource().sendFeedback(
                                Text.literal("\u00a7a\u00a7l[AutoReply] \u00a7eON!")
                            );
                            HolyWorldAutoReply.LOGGER.info("[AI] ENABLED");
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("stop")
                        .executes(ctx -> {
                            HolyWorldAutoReply.setEnabled(false);
                            ctx.getSource().sendFeedback(
                                Text.literal("\u00a7c\u00a7l[AutoReply] \u00a7eOFF!")
                            );
                            HolyWorldAutoReply.LOGGER.info("[AI] DISABLED");
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("status")
                        .executes(ctx -> {
                            boolean on = HolyWorldAutoReply.isEnabled();
                            ctx.getSource().sendFeedback(
                                Text.literal("\u00a7b[AutoReply] " + (on ? "\u00a7aON" : "\u00a7cOFF"))
                            );
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("clear")
                        .executes(ctx -> {
                            if (HolyWorldAutoReply.getChatHandler() != null)
                                HolyWorldAutoReply.getChatHandler().getResponseEngine().clearAllStates();
                            ctx.getSource().sendFeedback(Text.literal("\u00a7eCleared!"));
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("test")
                        .executes(ctx -> {
                            if (HolyWorldAutoReply.getChatHandler() != null) {
                                HolyWorldAutoReply.getChatHandler().processIncoming(
                                    "[CHECK] TestPlayer -> привет"
                                );
                                ctx.getSource().sendFeedback(
                                    Text.literal("\u00a7eTest sent!")
                                );
                            }
                            return 1;
                        })
                    )
            );
        });
    }
}
