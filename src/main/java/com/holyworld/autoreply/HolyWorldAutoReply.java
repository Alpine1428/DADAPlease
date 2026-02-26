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

    // === AUTO SETTINGS ===
    private static boolean autoReports = false;
    private static boolean autoCheckout = false;
    private static boolean autoOut = false;
    private static String lastSpyNick = null;

    @Override
    public void onInitializeClient() {
        LOGGER.info("===========================================");
        LOGGER.info("[HolyWorldAutoReply] v1.1.0 Fabric 1.20.1");
        LOGGER.info("[HolyWorldAutoReply] Chat mode: PUBLIC CHAT");
        LOGGER.info("===========================================");
        chatHandler = new ChatHandler();
        AICommand.register();
        LOGGER.info("[HolyWorldAutoReply] Ready! /ai start");
    }

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean state) {
        enabled = state;
        if (chatHandler != null && !state) {
            chatHandler.getResponseEngine().clearAllStates();
        }
    }

    public static ChatHandler getChatHandler() { return chatHandler; }

    // --- Auto Reports ---
    public static boolean isAutoReports() { return autoReports; }
    public static void setAutoReports(boolean v) { autoReports = v; }

    // --- Auto Checkout ---
    public static boolean isAutoCheckout() { return autoCheckout; }
    public static void setAutoCheckout(boolean v) { autoCheckout = v; }

    // --- Auto Out ---
    public static boolean isAutoOut() { return autoOut; }
    public static void setAutoOut(boolean v) { autoOut = v; }

    // --- Last Spy Nick ---
    public static String getLastSpyNick() { return lastSpyNick; }
    public static void setLastSpyNick(String nick) { lastSpyNick = nick; }

    // --- Отключить всё auto ---
    public static void disableAllAuto() {
        autoReports = false;
        autoCheckout = false;
        LOGGER.info("[Auto] All auto modes disabled");
    }
}
