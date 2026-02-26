package com.holyworld.autoreply.handler;

import com.holyworld.autoreply.HolyWorldAutoReply;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Перехватывает сообщения в чате и реагирует на:
 * 1. /hm spy <ник> - запоминает ник
 * 2. /hm spyfrz - если auto включен, шлёт startcheckout
 * 3. /hm sban - если autoout включен, шлёт endcheckout
 */
public class CommandInterceptor {

    private static final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HW-CmdInterceptor");
            t.setDaemon(true);
            return t;
        });

    // Паттерны для поиска в ОТПРАВЛЕННЫХ командах (перехватываем через ChatScreen)
    // Но мы также ловим серверные ответы в чате

    /**
     * Вызывается из ChatHudMixin для КАЖДОГО сообщения в чате.
     * Здесь мы не можем видеть что МЫ отправили как команду,
     * но можем видеть серверные ответы.
     *
     * Поэтому дополнительно перехватываем отправку через ChatScreenMixin.
     */
    public static void processMessage(String plain) {
        // Тут можно ловить серверные ответы если нужно
        // Например подтверждение бана и т.д.
    }

    /**
     * Вызывается из ChatScreenMixin когда игрок ОТПРАВЛЯЕТ сообщение/команду.
     * Возвращает true если мы обработали и НЕ надо блокировать отправку.
     */
    public static void onPlayerSendCommand(String command) {
        if (command == null || command.isEmpty()) return;

        String cmd = command.startsWith("/") ? command.substring(1) : command;
        String lower = cmd.toLowerCase().trim();

        HolyWorldAutoReply.LOGGER.debug("[CmdInterceptor] Command: {}", cmd);

        // === /hm spy <ник> ===
        if (lower.startsWith("hm spy ") && !lower.startsWith("hm spyfrz")) {
            String afterSpy = cmd.substring(7).trim(); // после "hm spy "
            String nick = afterSpy.split("\\s+")[0].trim();
            nick = nick.replaceAll("[^a-zA-Z0-9_]", "");

            if (!nick.isEmpty()) {
                HolyWorldAutoReply.setLastSpyNick(nick);
                HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] Saved spy nick: {}", nick);

                // Уведомление игроку
                sendLocalMessage("\u00a76\u00a7l[Auto] \u00a7eSpy nick saved: \u00a7f" + nick);
            }
        }

        // === /hm spyfrz ===
        if (lower.startsWith("hm spyfrz")) {
            String lastNick = HolyWorldAutoReply.getLastSpyNick();

            if (lastNick == null || lastNick.isEmpty()) {
                HolyWorldAutoReply.LOGGER.warn("[CmdInterceptor] spyfrz but no spy nick saved!");
                sendLocalMessage("\u00a7c\u00a7l[Auto] \u00a7eNo spy nick! Use /hm spy <nick> first");
                return;
            }

            if (HolyWorldAutoReply.isAutoReports()) {
                // /hm startcheckout <ник> report
                String autoCmd = "hm startcheckout " + lastNick + " report";
                HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] Auto sending: /{}", autoCmd);
                sendLocalMessage("\u00a7a\u00a7l[Auto] \u00a7eSending: \u00a7f/" + autoCmd);

                scheduler.schedule(() -> sendCommand(autoCmd), 500, TimeUnit.MILLISECONDS);
            }
            else if (HolyWorldAutoReply.isAutoCheckout()) {
                // /hm startcheckout <ник> checkout
                String autoCmd = "hm startcheckout " + lastNick + " checkout";
                HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] Auto sending: /{}", autoCmd);
                sendLocalMessage("\u00a7a\u00a7l[Auto] \u00a7eSending: \u00a7f/" + autoCmd);

                scheduler.schedule(() -> sendCommand(autoCmd), 500, TimeUnit.MILLISECONDS);
            }
        }

        // === /hm sban <ник> <время> <причина> ===
        if (lower.startsWith("hm sban ")) {
            if (HolyWorldAutoReply.isAutoOut()) {
                // Парсим ник из команды: hm sban <ник> <время> <причина>
                String afterSban = cmd.substring(8).trim(); // после "hm sban "
                String[] parts = afterSban.split("\\s+");

                if (parts.length >= 1) {
                    String bannedNick = parts[0].replaceAll("[^a-zA-Z0-9_]", "");

                    if (!bannedNick.isEmpty()) {
                        // /hm endcheckout ban <ник> false
                        String endCmd = "hm endcheckout ban " + bannedNick + " false";
                        HolyWorldAutoReply.LOGGER.info("[CmdInterceptor] AutoOut: /{}", endCmd);
                        sendLocalMessage("\u00a7a\u00a7l[AutoOut] \u00a7eSending: \u00a7f/" + endCmd);

                        // Задержка чуть больше чтобы бан успел пройти
                        scheduler.schedule(() -> sendCommand(endCmd), 1500, TimeUnit.MILLISECONDS);

                        // Очищаем состояние игрока в ResponseEngine
                        if (HolyWorldAutoReply.getChatHandler() != null) {
                            HolyWorldAutoReply.getChatHandler()
                                .getResponseEngine().clearPlayerState(bannedNick);
                        }
                    }
                }
            }
        }
    }

    /**
     * Отправить команду на сервер (без /)
     */
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

    /**
     * Показать локальное сообщение игроку (только ему, не в чат сервера)
     */
    private static void sendLocalMessage(String msg) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal(msg), false);
            }
        });
    }

    // Нужен импорт Text
    private static class Text {
        static net.minecraft.text.Text literal(String s) {
            return net.minecraft.text.Text.literal(s);
        }
    }
}
