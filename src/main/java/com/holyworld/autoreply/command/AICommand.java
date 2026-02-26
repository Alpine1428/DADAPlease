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

                    // === /ai start ===
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

                    // === /ai stop ===
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

                    // === /ai status ===
                    .then(ClientCommandManager.literal("status")
                        .executes(ctx -> {
                            boolean on = HolyWorldAutoReply.isEnabled();
                            boolean ar = HolyWorldAutoReply.isAutoReports();
                            boolean ac = HolyWorldAutoReply.isAutoCheckout();
                            boolean ao = HolyWorldAutoReply.isAutoOut();
                            String spy = HolyWorldAutoReply.getLastSpyNick();

                            ctx.getSource().sendFeedback(Text.literal(
                                "\u00a76\u00a7l=== AutoReply Status ===\n" +
                                "\u00a7eAutoReply: " + (on ? "\u00a7aON" : "\u00a7cOFF") + "\n" +
                                "\u00a7eAuto Reports: " + (ar ? "\u00a7aON" : "\u00a7cOFF") + "\n" +
                                "\u00a7eAuto Checkout: " + (ac ? "\u00a7aON" : "\u00a7cOFF") + "\n" +
                                "\u00a7eAuto Out: " + (ao ? "\u00a7aON" : "\u00a7cOFF") + "\n" +
                                "\u00a7eLast Spy: " + (spy != null ? "\u00a7f" + spy : "\u00a77none")
                            ));
                            return 1;
                        })
                    )

                    // === /ai clear ===
                    .then(ClientCommandManager.literal("clear")
                        .executes(ctx -> {
                            if (HolyWorldAutoReply.getChatHandler() != null)
                                HolyWorldAutoReply.getChatHandler().getResponseEngine().clearAllStates();
                            HolyWorldAutoReply.setLastSpyNick(null);
                            ctx.getSource().sendFeedback(Text.literal("\u00a7eCleared!"));
                            return 1;
                        })
                    )

                    // === /ai test ===
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

                    // =============================================
                    // /ai auto reports  - включить auto reports
                    // /ai auto checkout - включить auto checkout
                    // /ai auto off      - выключить всё auto
                    // =============================================
                    .then(ClientCommandManager.literal("auto")

                        // /ai auto reports
                        .then(ClientCommandManager.literal("reports")
                            .executes(ctx -> {
                                HolyWorldAutoReply.setAutoReports(true);
                                HolyWorldAutoReply.setAutoCheckout(false);
                                ctx.getSource().sendFeedback(Text.literal(
                                    "\u00a7a\u00a7l[Auto] \u00a7eMode: \u00a7fREPORTS\n" +
                                    "\u00a77При /hm spyfrz будет: /hm startcheckout <ник> report"
                                ));
                                HolyWorldAutoReply.LOGGER.info("[Auto] Mode set to REPORTS");
                                return 1;
                            })
                        )

                        // /ai auto checkout
                        .then(ClientCommandManager.literal("checkout")
                            .executes(ctx -> {
                                HolyWorldAutoReply.setAutoCheckout(true);
                                HolyWorldAutoReply.setAutoReports(false);
                                ctx.getSource().sendFeedback(Text.literal(
                                    "\u00a7a\u00a7l[Auto] \u00a7eMode: \u00a7fCHECKOUT\n" +
                                    "\u00a77При /hm spyfrz будет: /hm startcheckout <ник> checkout"
                                ));
                                HolyWorldAutoReply.LOGGER.info("[Auto] Mode set to CHECKOUT");
                                return 1;
                            })
                        )

                        // /ai auto off
                        .then(ClientCommandManager.literal("off")
                            .executes(ctx -> {
                                HolyWorldAutoReply.disableAllAuto();
                                ctx.getSource().sendFeedback(Text.literal(
                                    "\u00a7c\u00a7l[Auto] \u00a7eAll auto modes \u00a7cOFF"
                                ));
                                return 1;
                            })
                        )

                        // /ai auto (без аргументов - показать статус)
                        .executes(ctx -> {
                            boolean ar = HolyWorldAutoReply.isAutoReports();
                            boolean ac = HolyWorldAutoReply.isAutoCheckout();
                            String mode = "OFF";
                            if (ar) mode = "REPORTS";
                            if (ac) mode = "CHECKOUT";

                            ctx.getSource().sendFeedback(Text.literal(
                                "\u00a76\u00a7l[Auto] \u00a7eCurrent mode: \u00a7f" + mode + "\n" +
                                "\u00a77/ai auto reports - startcheckout report\n" +
                                "\u00a77/ai auto checkout - startcheckout checkout\n" +
                                "\u00a77/ai auto off - disable"
                            ));
                            return 1;
                        })
                    )

                    // =============================================
                    // /ai autoout on  - включить auto endcheckout
                    // /ai autoout off - выключить auto endcheckout
                    // =============================================
                    .then(ClientCommandManager.literal("autoout")

                        // /ai autoout on
                        .then(ClientCommandManager.literal("on")
                            .executes(ctx -> {
                                HolyWorldAutoReply.setAutoOut(true);
                                ctx.getSource().sendFeedback(Text.literal(
                                    "\u00a7a\u00a7l[AutoOut] \u00a7eON\n" +
                                    "\u00a77После /hm sban будет: /hm endcheckout ban <ник> false"
                                ));
                                HolyWorldAutoReply.LOGGER.info("[AutoOut] ENABLED");
                                return 1;
                            })
                        )

                        // /ai autoout off
                        .then(ClientCommandManager.literal("off")
                            .executes(ctx -> {
                                HolyWorldAutoReply.setAutoOut(false);
                                ctx.getSource().sendFeedback(Text.literal(
                                    "\u00a7c\u00a7l[AutoOut] \u00a7eOFF"
                                ));
                                HolyWorldAutoReply.LOGGER.info("[AutoOut] DISABLED");
                                return 1;
                            })
                        )

                        // /ai autoout (без аргументов)
                        .executes(ctx -> {
                            boolean ao = HolyWorldAutoReply.isAutoOut();
                            ctx.getSource().sendFeedback(Text.literal(
                                "\u00a76\u00a7l[AutoOut] \u00a7e" + (ao ? "\u00a7aON" : "\u00a7cOFF") + "\n" +
                                "\u00a77/ai autoout on - auto endcheckout after ban\n" +
                                "\u00a77/ai autoout off - disable"
                            ));
                            return 1;
                        })
                    )
            );
        });
    }
}
