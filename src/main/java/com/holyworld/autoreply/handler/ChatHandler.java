package com.holyworld.autoreply.handler;

import com.holyworld.autoreply.HolyWorldAutoReply;
import com.holyworld.autoreply.ai.ResponseEngine;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.*;

/**
 * Processes [CHECK] messages from public chat and replies in public chat.
 *
 * Message flow on HolyWorld:
 * 1. Player on check writes in NORMAL CHAT
 * 2. Server formats it as: §d§l[CHECK] §fPlayerName §5-> message
 * 3. getString() returns: [CHECK] PlayerName -> message  (colors stripped)
 * 4. We parse player name and message
 * 5. We reply in NORMAL CHAT (not /r, not /msg - just regular chat message)
 */
public class ChatHandler {

    private final ResponseEngine responseEngine;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 2500;

    public ChatHandler() {
        this.responseEngine = new ResponseEngine();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HW-AutoReply");
            t.setDaemon(true);
            return t;
        });
        HolyWorldAutoReply.LOGGER.info("[ChatHandler] Ready (PUBLIC CHAT mode)");
    }

    public ResponseEngine getResponseEngine() {
        return responseEngine;
    }

    /**
     * Called from Mixin when any chat message containing [CHECK] appears.
     */
    public void processIncoming(String raw) {
        if (raw == null || raw.isEmpty()) return;

        // Strip color codes just in case getString() still has some
        String clean = stripColors(raw);

        if (!clean.contains("[CHECK]")) return;

        HolyWorldAutoReply.LOGGER.info("[Handler] Processing: '{}'", clean);

        // Find [CHECK] marker
        int checkIdx = clean.indexOf("[CHECK]");
        String afterCheck = clean.substring(checkIdx + 7).trim();

        HolyWorldAutoReply.LOGGER.info("[Handler] After [CHECK]: '{}'", afterCheck);

        // Find "->" separator
        int arrowIdx = afterCheck.indexOf("->");
        if (arrowIdx <= 0) {
            HolyWorldAutoReply.LOGGER.warn("[Handler] No '->' found in: '{}'", afterCheck);
            return;
        }

        String playerName = afterCheck.substring(0, arrowIdx).trim();
        String playerMessage = afterCheck.substring(arrowIdx + 2).trim();

        // Clean player name - only valid MC characters
        playerName = playerName.replaceAll("[^a-zA-Z0-9_]", "");

        if (playerName.isEmpty() || playerMessage.isEmpty()) {
            HolyWorldAutoReply.LOGGER.warn("[Handler] Empty: name='{}' msg='{}'",
                playerName, playerMessage);
            return;
        }

        // Don't reply to ourselves
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            String myName = client.player.getName().getString();
            if (playerName.equalsIgnoreCase(myName)) {
                return;
            }
        }

        HolyWorldAutoReply.LOGGER.info("[Handler] Player='{}' Msg='{}'", playerName, playerMessage);

        // Cooldown
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(playerName);
        if (last != null && (now - last) < COOLDOWN_MS) {
            HolyWorldAutoReply.LOGGER.debug("[Handler] Cooldown active for {}", playerName);
            return;
        }
        cooldowns.put(playerName, now);

        // Get response from engine
        String response = responseEngine.getResponse(playerMessage, playerName);

        if (response != null && !response.isEmpty()) {
            HolyWorldAutoReply.LOGGER.info("[Handler] Will send: '{}'", response);

            final String resp = response;
            // Natural delay 0.8-2.0 seconds
            long delay = 800 + (long)(Math.random() * 1200);

            scheduler.schedule(() -> sendPublicChat(resp), delay, TimeUnit.MILLISECONDS);
        } else {
            HolyWorldAutoReply.LOGGER.info("[Handler] NULL response (ban signal) for {}: '{}'",
                playerName, playerMessage);
        }
    }

    /**
     * Send a message in PUBLIC CHAT.
     * This is what the player sees as a normal chat message from the moderator.
     *
     * In 1.20.1:
     * - sendChatMessage(String) sends a regular chat message (what you type in chat)
     * - sendChatCommand(String) sends a command (without the leading /)
     */
    private void sendPublicChat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            HolyWorldAutoReply.LOGGER.error("[Handler] Client is null!");
            return;
        }

        client.execute(() -> {
            try {
                if (client.player == null) {
                    HolyWorldAutoReply.LOGGER.error("[Handler] Player is null!");
                    return;
                }
                if (client.getNetworkHandler() == null) {
                    HolyWorldAutoReply.LOGGER.error("[Handler] NetworkHandler is null!");
                    return;
                }

                // =============================================
                // KEY FIX: Send as REGULAR CHAT MESSAGE
                // NOT as command, NOT as /r
                // Just a normal message that appears in chat
                // =============================================
                client.getNetworkHandler().sendChatMessage(message);

                HolyWorldAutoReply.LOGGER.info("[Handler] SENT in chat: '{}'", message);
            } catch (Exception e) {
                HolyWorldAutoReply.LOGGER.error("[Handler] Send failed: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Strip Minecraft color codes.
     */
    private static String stripColors(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\u00A7' && i + 1 < input.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
