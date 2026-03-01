package com.reimo.reimohelper.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.reimo.reimohelper.config.ReimoHelperConfig;
import com.reimo.reimohelper.feature.BpsTracker;
import com.reimo.reimohelper.handler.MacroHandler;
import com.reimo.reimohelper.hud.StatusHud;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public final class DiscordWebhookService {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");
    private static final Minecraft MC = Minecraft.getInstance();
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static long nextSendAtMs = 0L;

    private DiscordWebhookService() {
    }

    public static void sendMacroStarted() {
        try {
            ReimoHelperConfig cfg = ReimoHelperConfig.getInstance();
            if (!cfg.discordWebhookEnabled || cfg.discordWebhookUrl == null || cfg.discordWebhookUrl.trim().isEmpty()) {
                return;
            }
            sendEventEmbed(cfg.discordWebhookUrl.trim(), "Macro Started", "Macro farming session has begun", 0x00FF00);
        } catch (Exception e) {
            LOGGER.warn("Failed sending macro start webhook", e);
        }
    }

    public static void sendMacroStopped(long runtimeSeconds) {
        try {
            ReimoHelperConfig cfg = ReimoHelperConfig.getInstance();
            if (!cfg.discordWebhookEnabled || cfg.discordWebhookUrl == null || cfg.discordWebhookUrl.trim().isEmpty()) {
                return;
            }
            String runtime = formatTime(runtimeSeconds);
            sendEventEmbed(cfg.discordWebhookUrl.trim(), "Macro Stopped", "Runtime: " + runtime, 0xFFAA00);
        } catch (Exception e) {
            LOGGER.warn("Failed sending macro stop webhook", e);
        }
    }

    public static void sendFailsafeTriggered(String reason) {
        try {
            ReimoHelperConfig cfg = ReimoHelperConfig.getInstance();
            if (!cfg.discordWebhookEnabled || cfg.discordWebhookUrl == null || cfg.discordWebhookUrl.trim().isEmpty()) {
                return;
            }
            sendEventEmbed(cfg.discordWebhookUrl.trim(), "Failsafe Triggered", reason, 0xFF0000);
        } catch (Exception e) {
            LOGGER.warn("Failed sending failsafe webhook", e);
        }
    }

    private static void sendEventEmbed(String url, String title, String description, int color) {
        String playerName = MC.getUser() != null ? MC.getUser().getName() : "Unknown";

        JsonObject payload = new JsonObject();
        payload.addProperty("username", "ReimoHelper");

        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("description", description);
        embed.addProperty("color", color);
        embed.addProperty("timestamp", Instant.now().toString());
                
        String avatarUrl = "https://mc-heads.net/avatar/" + playerName + "/100";
        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", avatarUrl);
        embed.add("thumbnail", thumbnail);

        JsonObject author = new JsonObject();
        author.addProperty("name", playerName);
        author.addProperty("icon_url", avatarUrl);
        embed.add("author", author);

        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Player: " + playerName);
        embed.add("footer", footer);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        CLIENT.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .thenAccept(resp -> {
                    if (resp.statusCode() >= 300) {
                        LOGGER.warn("Webhook responded with status {}", resp.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.warn("Webhook send failed", ex);
                    return null;
                });
    }

    public static void onClientTick() {
        ReimoHelperConfig cfg = ReimoHelperConfig.getInstance();
        if (!cfg.discordWebhookEnabled) {
            nextSendAtMs = 0L;
            return;
        }

        // Only send status updates if macro is running
        if (!MacroHandler.getInstance().isMacroActive()) {
            return;
        }

        String url = cfg.discordWebhookUrl == null ? "" : cfg.discordWebhookUrl.trim();
        if (url.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextSendAtMs) {
            return;
        }

        int interval = Math.max(10, cfg.discordWebhookIntervalSeconds);
        nextSendAtMs = now + interval * 1000L;

        try {
            sendStatusEmbed(url);
        } catch (Exception e) {
            LOGGER.warn("Failed preparing webhook payload", e);
        }
    }

    private static void sendStatusEmbed(String url) {
        ReimoHelperConfig cfg = ReimoHelperConfig.getInstance();
        List<String> lines = StatusHud.getStatusLines(cfg, cfg.showDebugHud);

        JsonObject payload = new JsonObject();
        payload.addProperty("username", "ReimoHelper");

        JsonObject embed = new JsonObject();
        embed.addProperty("title", "ReimoHelper Status Update");
        embed.addProperty("color", 0x2EC4B6);
        embed.addProperty("timestamp", Instant.now().toString());
        
        String playerName = MC.getUser() != null ? MC.getUser().getName() : "Unknown";
        String avatarUrl = "https://mc-heads.net/avatar/" + playerName + "/100";
        JsonObject thumbnail = new JsonObject();
        thumbnail.addProperty("url", avatarUrl);
        embed.add("thumbnail", thumbnail);
        JsonObject author = new JsonObject();
        author.addProperty("name", playerName);
        author.addProperty("icon_url", avatarUrl);
        embed.add("author", author);

        JsonArray fields = new JsonArray();
        for (String line : lines) {
            JsonObject field = new JsonObject();
            int sep = line.indexOf(':');
            if (sep > 0 && sep < line.length() - 1) {
                field.addProperty("name", line.substring(0, sep).trim());
                field.addProperty("value", line.substring(sep + 1).trim());
            } else {
                field.addProperty("name", "Info");
                field.addProperty("value", line);
            }
            field.addProperty("inline", true);
            fields.add(field);
        }
        embed.add("fields", fields);

        // footer (reuse playerName defined above)
        embed.addProperty("footer", "");
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Player: " + playerName + " | BPS: " + String.format("%.2f", BpsTracker.getBps()));
        embed.add("footer", footer);

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        CLIENT.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .thenAccept(resp -> {
                    if (resp.statusCode() >= 300) {
                        LOGGER.warn("Webhook responded with status {}", resp.statusCode());
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.warn("Webhook send failed", ex);
                    return null;
                });
    }

    private static String formatTime(long totalSeconds) {
        long h = totalSeconds / 3600L;
        long m = (totalSeconds % 3600L) / 60L;
        long s = totalSeconds % 60L;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        return String.format("%02dm %02ds", m, s);
    }
}

