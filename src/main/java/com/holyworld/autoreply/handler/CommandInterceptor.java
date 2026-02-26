package com.holyworld.autoreply.handler;

import com.holyworld.autoreply.HolyWorldAutoReply;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandInterceptor {

    private static final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HW-CmdInterceptor");
            t.setDaemon(true);
            return t;
        });

    /**
     * Вызывается из ChatHudMixin для каждого сообщения в чате.
     */
    public static void processMessage(String plain) {
        // Серверные ответы - пока не обрабатываем
    }

    /**
     * Вызывается из ChatScreenMixin когда игрок отправляет команду.
     */
    public static void onPlayerSendCommand(String command) {
        if (command == null || command.isEmpty()) return;

        String cmd = command.startsWith("/") ? command.substring(1) : command;
        String lower = cmd.toLowerCase().trim();

        HolyWorldAutoReply.LOGGER.debug("[CmdInterceptor] Command: {}", cmd);

        // ==========================================
        // /hm spy <ник> - запоминаем ник
        // ==========================================
        if (lower.startsWith("hm spy ") && !lower.startsWith("hm spyfrz")) {
            String afterSpy = cmd.substring(7).trim();
            String nick = afterSpy.split("\\s+")[0].trim();
            nick = nick.replaceAll("[^a-zA-Z0-9_]", "");

            if (!nick.isEmpty()) {
                HolyWorldAutoReply.setLastSpyNick(nick);
                HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] Saved spy nick: {}", nick);
                sendLocalMessage("\u00a76\u00a7l[Auto] \u00a7eSpy nick: \u00a7f" + nick);
            }
        }

        // ==========================================
        // /hm spyfrz - автоматический startcheckout
        // ==========================================
        if (lower.startsWith("hm spyfrz")) {
            String lastNick = HolyWorldAutoReply.getLastSpyNick();

            if (lastNick == null || lastNick.isEmpty()) {
                HolyWorldAutoReply.LOGGER.warn("[CmdInterceptor] spyfrz but no spy nick!");
                sendLocalMessage("\u00a7c\u00a7l[Auto] \u00a7eNo spy nick! /hm spy <nick> first");
                return;
            }

            if (HolyWorldAutoReply.isAutoReports()) {
                String autoCmd = "hm startcheckout " + lastNick + " report";
                HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] Auto: /{}", autoCmd);
                sendLocalMessage("\u00a7a\u00a7l[Auto] \u00a7f/" + autoCmd);
                scheduler.schedule(() -> sendCommand(autoCmd), 500, TimeUnit.MILLISECONDS);
            }
            else if (HolyWorldAutoReply.isAutoCheckout()) {
                String autoCmd = "hm startcheckout " + lastNick + " checkout";
                HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] Auto: /{}", autoCmd);
                sendLocalMessage("\u00a7a\u00a7l[Auto] \u00a7f/" + autoCmd);
                scheduler.schedule(() -> sendCommand(autoCmd), 500, TimeUnit.MILLISECONDS);
            }
        }

        // ==========================================
        // /hm sban <ник> ... - автоматический endcheckout
        // ==========================================
        if (lower.startsWith("hm sban ")) {
            if (HolyWorldAutoReply.isAutoOut()) {
                String afterSban = cmd.substring(8).trim();
                String[] parts = afterSban.split("\\s+");

                if (parts.length >= 1) {
                    String bannedNick = parts[0].replaceAll("[^a-zA-Z0-9_]", "");

                    if (!bannedNick.isEmpty()) {
                        String endCmd = "hm endcheckout ban " + bannedNick + " false";
                        HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] AutoOut: /{}", endCmd);
                        sendLocalMessage("\u00a7a\u00a7l[AutoOut] \u00a7f/" + endCmd);

                        scheduler.schedule(() -> sendCommand(endCmd), 1500, TimeUnit.MILLISECONDS);

                        // Очистить состояние игрока
                        if (HolyWorldAutoReply.getChatHandler() != null) {
                            HolyWorldAutoReply.getChatHandler()
                                .getResponseEngine().clearPlayerState(bannedNick);
                        }
                    }
                }
            }
        }
    }

    private static void sendCommand(String command) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        client.execute(() -> {
            try {
                if (client.player == null || client.getNetworkHandler() == null) return;
                String cmd = command.startsWith("/") ? command.substring(1) : command;
                client.getNetworkHandler().sendChatCommand(cmd);
                HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] SENT: /{}", cmd);
            } catch (Exception e) {
                HolyWorldAutoReply.LOGGER.error("[CmdInterceptor] Failed: {}", e.getMessage(), e);
            }
        });
    }

    private static void sendLocalMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(msg), false);
            }
        });
    }
}
