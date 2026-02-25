package com.holyworld.autoreply;

import com.holyworld.autoreply.command.AICommand;
import com.holyworld.autoreply.handler.ChatHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HolyWorldAutoReply implements ClientModInitializer {
    public static final String MOD_ID = "holyworld-autoreply";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean enabled = false;
    private static ChatHandler chatHandler;

    @Override
    public void onInitializeClient() {
        LOGGER.info("===========================================");
        LOGGER.info("[HolyWorldAutoReply] v1.0.0 Fabric 1.20.1");
        LOGGER.info("[HolyWorldAutoReply] Chat mode: PUBLIC CHAT");
        LOGGER.info("===========================================");
        chatHandler = new ChatHandler();
        AICommand.register();
        LOGGER.info("[HolyWorldAutoReply] Ready! /ai start");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean state) {
        enabled = state;
        if (chatHandler != null && !state) {
            chatHandler.getResponseEngine().clearAllStates();
        }
    }

    public static ChatHandler getChatHandler() {
        return chatHandler;
    }
}
