package com.holyworld.autoreply.handler;

import com.holyworld.autoreply.HolyWorldAutoReply;
import com.holyworld.autoreply.ai.ResponseEngine;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.*;

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

    public void processIncoming(String raw) {
        if (raw == null || raw.isEmpty()) return;

        String clean = stripColors(raw);
        if (!clean.contains("[CHECK]")) return;

        HolyWorldAutoReply.LOGGER.info("[Handler] Processing: '{}'", clean);

        int checkIdx = clean.indexOf("[CHECK]");
        String afterCheck = clean.substring(checkIdx + 7).trim();

        int arrowIdx = afterCheck.indexOf("->");
        if (arrowIdx <= 0) {
            HolyWorldAutoReply.LOGGER.warn("[Handler] No '->' found in: '{}'", afterCheck);
            return;
        }

        String playerName = afterCheck.substring(0, arrowIdx).trim();
        String playerMessage = afterCheck.substring(arrowIdx + 2).trim();

        playerName = playerName.replaceAll("[^a-zA-Z0-9_]", "");

        if (playerName.isEmpty() || playerMessage.isEmpty()) {
            HolyWorldAutoReply.LOGGER.warn("[Handler] Empty: name='{}' msg='{}'",
                playerName, playerMessage);
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            String myName = client.player.getName().getString();
            if (playerName.equalsIgnoreCase(myName)) return;
        }

        HolyWorldAutoReply.LOGGER.info("[Handler] Player='{}' Msg='{}'", playerName, playerMessage);

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(playerName);
        if (last != null && (now - last) < COOLDOWN_MS) return;
        cooldowns.put(playerName, now);

        String response = responseEngine.getResponse(playerMessage, playerName);

        if (response != null && !response.isEmpty()) {
            HolyWorldAutoReply.LOGGER.info("[Handler] Will send: '{}'", response);

            final String resp = response;
            long delay = 800 + (long)(Math.random() * 1200);

            scheduler.schedule(() -> sendPublicChat(resp), delay, TimeUnit.MILLISECONDS);
        }
    }

    private void sendPublicChat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        client.execute(() -> {
            try {
                if (client.player == null || client.getNetworkHandler() == null) return;
                client.getNetworkHandler().sendChatMessage(message);
                HolyWorldAutoReply.LOGGER.info("[Handler] SENT: '{}'", message);
            } catch (Exception e) {
                HolyWorldAutoReply.LOGGER.error("[Handler] Send failed: {}", e.getMessage(), e);
            }
        });
    }

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
