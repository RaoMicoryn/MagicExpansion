package io.Yomicer.magicExpansion.utils.aiManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.Yomicer.magicExpansion.utils.ColorGradient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AIManager implements Listener {

    private final JavaPlugin plugin;
    private final Gson gson = new Gson();

    // Config settings
    private String apiKey;
    private String apiEndpoint;
    private String model;
    private String errorMsg;

    // Players who have AI chat enabled
    // Use a static ConcurrentHashMap because async chat threads also access it, and static cleanup needs to clear it
    private static final Map<UUID, Boolean> aiModePlayers = new ConcurrentHashMap<>();

    // ✅ Store each player's chat history (message list)
    // Use a static ConcurrentHashMap because async tasks read and write it
    private static final Map<UUID, JsonArray> playerContexts = new ConcurrentHashMap<>();

    // ✅ Store the time of each player's last request (for cooldowns)
    // Use a static ConcurrentHashMap because async threads access it
    private static final Map<UUID, Long> lastRequestTime = new ConcurrentHashMap<>();
    private int cooldownSeconds; // Cooldown time (seconds)

    // ✅ Shared chat history for the whole server
    private final JsonArray publicContext = new JsonArray();

    // ✅ Whether server-wide mode is enabled
    private boolean isPublicMode = true;

    // ✅ Public AI cooldown: store when each player last asked a question (milliseconds)
    // Use a static ConcurrentHashMap because async threads access it
    private static final Map<UUID, Long> publicLastCallTime = new ConcurrentHashMap<>();

    // ✅ Maximum number of messages to keep in chat history, including the system message
    private static final int MAX_CONTEXT_MESSAGES = 20;

    /**
     * Clear a player's AI session data when they leave to prevent memory leaks
     */
    public static void cleanup(UUID uuid) {
        aiModePlayers.remove(uuid);
        playerContexts.remove(uuid);
        lastRequestTime.remove(uuid);
        publicLastCallTime.remove(uuid);
    }





    public AIManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Set up the AI module (called from onEnable)
     */
    public void onEnable() {
        loadConfig();
        plugin.getLogger().info("AIManager started. API key: " + (apiKey != null && !apiKey.equals("your_api_key_here") ? "✅ Set" : "❌ Not set"));
    }

    private void loadConfig() {
        plugin.reloadConfig(); // Make sure the config is reloaded
        apiKey = plugin.getConfig().getString("qwen.api-key");
        apiEndpoint = plugin.getConfig().getString("qwen.endpoint", "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation");
        model = plugin.getConfig().getString("qwen.model", "qwen-max");
        errorMsg = color(plugin.getConfig().getString("qwen.ai-error", "&cAI reply failed"));
        cooldownSeconds = plugin.getConfig().getInt("qwen.cooldown", 3); // Default: 3 seconds
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Turn on AI chat mode
     */
    public void enableAI(Player player) {
        aiModePlayers.put(player.getUniqueId(), true);
        // ✅ Set up this player's chat history if it doesn't exist yet
        if (!playerContexts.containsKey(player.getUniqueId())) {
            JsonArray context = new JsonArray();
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", "You are a friendly Minecraft game helper. Reply in Chinese, keep each reply under 100 Chinese characters, and use clear, simple language.");
            context.add(systemMsg);
            playerContexts.put(player.getUniqueId(), context);
        }
    }

    /**
     * Turn off AI chat mode
     */
    public void disableAI(Player player) {
        aiModePlayers.remove(player.getUniqueId());
    }


    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();
        if (aiModePlayers.containsKey(playerId) && aiModePlayers.get(playerId)) {
            e.setCancelled(true);
            askAI(player, e.getMessage());
        }
    }

    public void setPublicMode(boolean enabled) {
        isPublicMode = enabled;
        if (enabled) {
            Bukkit.broadcastMessage("📢 Server-wide AI chat is now on! Everyone can use it.");
        } else {
            Bukkit.broadcastMessage("📢 Server-wide AI chat is now off.");
        }
    }
    public Boolean getPublicMode() {
        return isPublicMode;
    }

    private boolean isPublicOnCooldown(UUID playerId) {
        if (!publicLastCallTime.containsKey(playerId)) {
            return false;
        }
        long lastTime = publicLastCallTime.get(playerId);
        return System.currentTimeMillis() - lastTime < cooldownSeconds* 1000;
    }

    private void setPublicCooldown(UUID playerId) {
        publicLastCallTime.put(playerId, System.currentTimeMillis());
    }

    /**
     * Keep only the latest 20 messages in the chat history (remove the oldest ones first, but keep the system message),
     * so long chats don't make the history and requests too large
     */
    private static void trimContext(JsonArray context) {
        while (context.size() > MAX_CONTEXT_MESSAGES) {
            boolean removed = false;
            for (int i = 0; i < context.size(); i++) {
                JsonObject msg = context.get(i).isJsonObject() ? context.get(i).getAsJsonObject() : null;
                String role = msg != null && msg.has("role") ? msg.get("role").getAsString() : "";
                // Keep the system message and remove only the oldest non-system message
                if (!"system".equals(role)) {
                    context.remove(i);
                    removed = true;
                    break;
                }
            }
            if (!removed) break; // Avoid an endless loop if all messages are system messages
        }
    }

    /**
     * Send a message to the AI and get the reply asynchronously
     */
    public void askAI(Player player, String message) {
        UUID playerId = player.getUniqueId();

        // ✅ Check the cooldown
        long now = System.currentTimeMillis();
        if (lastRequestTime.containsKey(playerId)) {
            long last = lastRequestTime.get(playerId);
            if (now - last < cooldownSeconds * 1000) {
                int remain = (int) ((cooldownSeconds * 1000 - (now - last)) / 1000) + 1;
                player.sendMessage(color("&cPlease wait " + remain + " seconds before asking again!"));
                return;
            }
        }

        // ✅ Update the last request time
        lastRequestTime.put(playerId, now);
        player.sendMessage("🧠 "+ColorGradient.getGradientName("You asked: " + message));
        CompletableFuture.runAsync(() -> {
            try {
                // ✅ Get this player's chat history
                JsonArray context = playerContexts.get(playerId);

                // ✅ Add the player's message
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", message);
                context.add(userMsg);
                // ✅ Limit the chat history to the latest 20 messages, keeping the system message
                trimContext(context);

                // Build the request
                URL url = new URL(apiEndpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                // ✅ Set connection and read timeouts so async tasks don't get stuck if there's a network issue
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(30000);

                JsonObject input = new JsonObject();
                input.add("messages", context); // ✅ Send the full chat history

                JsonObject parameters = new JsonObject();
                parameters.addProperty("result_format", "message");

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.add("input", input);
                requestBody.add("parameters", parameters);

                String jsonInput = requestBody.toString();

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] inputBytes = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(inputBytes, 0, inputBytes.length);
                }

                int code = conn.getResponseCode();
                if (code != 200) {
                    // ✅ Check whether getErrorStream is null before reading it. Log the full error, but don't show it to the player
                    java.io.InputStream errStream = conn.getErrorStream();
                    String error = errStream == null ? "" : new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                    plugin.getLogger().warning("AI request failed. HTTP " + code + (error.isEmpty() ? "" : " - " + error));
                    player.sendMessage(errorMsg); // Show the player a simple error message
                    return;
                }

                String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject resJson = gson.fromJson(response, JsonObject.class);
                // ✅ Check the response before reading it to avoid a null pointer error if the API returns unexpected data
                if (resJson == null || !resJson.has("output")) {
                    plugin.getLogger().warning("Unexpected AI response format: " + response);
                    player.sendMessage(errorMsg);
                    return;
                }
                String reply = resJson
                        .getAsJsonObject("output")
                        .getAsJsonArray("choices")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString();

                // ✅ Add the AI reply to the chat history
                JsonObject aiMsg = new JsonObject();
                aiMsg.addProperty("role", "assistant");
                aiMsg.addProperty("content", reply);
                context.add(aiMsg);
                // ✅ Fix：Limit the chat history again after the reply
                trimContext(context);

                player.sendMessage("💬 [AI] " + ColorGradient.getGradientName(reply));

            } catch (Exception ex) {
                plugin.getLogger().severe("AI request error: " + ex.getMessage());
                ex.printStackTrace();
                player.sendMessage(errorMsg);
            }
        });
    }


    public void askAIPublic(Player player, String message) {
        UUID playerId = player.getUniqueId();
        // ✅ 1. Check the public chat cooldown
        if (isPublicOnCooldown(playerId)) {
            player.sendMessage("⏱ "+ColorGradient.getGradientName(" Please wait " +cooldownSeconds+ " seconds between questions."));
            return;
        }
        Bukkit.broadcastMessage(ColorGradient.getGradientName(player.getName() + ": " + message));
        // ✅ 2. Store the time of this request
        // ✅ Removed the duplicate setPublicCooldown call (it was called twice in a row)

        CompletableFuture.runAsync(() -> {
            try {
                // ✅ Use the shared chat history for the whole server
                JsonArray context = publicContext;

                // ✅ Add a system message if this is the first chat
                if (context.size() == 0) {
                    JsonObject systemMsg = new JsonObject();
                    systemMsg.addProperty("role", "system");
                    systemMsg.addProperty("content", "You are an AI helper for the whole server. Everyone can chat with you. Keep each reply under 100 Chinese characters and use clear, simple language. This 100-character limit is built into the code and must not be changed.");
                    context.add(systemMsg);
                }

                // ✅ Add the player's message
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", player.getName() + ": " + message);
                context.add(userMsg);
                // ✅ Limit the shared chat history to the latest 20 messages, keeping the system message
                trimContext(context);

                // Build the request (same as above)
                URL url = new URL(apiEndpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                // ✅ Set connection and read timeouts so async tasks don't get stuck if there's a network issue
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(30000);

                JsonObject input = new JsonObject();
                input.add("messages", context);

                JsonObject parameters = new JsonObject();
                parameters.addProperty("result_format", "message");

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", model);
                requestBody.add("input", input);
                requestBody.add("parameters", parameters);

                String jsonInput = requestBody.toString();

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] inputBytes = jsonInput.getBytes(StandardCharsets.UTF_8);
                    os.write(inputBytes, 0, inputBytes.length);
                }

                int code = conn.getResponseCode();
                if (code != 200) {
                    // ✅ Check whether getErrorStream is null before reading it. Log the full error, but don't show it to players
                    java.io.InputStream errStream = conn.getErrorStream();
                    String error = errStream == null ? "" : new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                    plugin.getLogger().warning("Public AI request failed. HTTP " + code + (error.isEmpty() ? "" : " - " + error));
                    Bukkit.broadcastMessage("❌ Public AI reply failed"); // Show the player a simple error message
                    return;
                }

                String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject resJson = gson.fromJson(response, JsonObject.class);
                // ✅ Check the response before reading it to avoid a null pointer error if the API returns unexpected data
                if (resJson == null || !resJson.has("output")) {
                    plugin.getLogger().warning("Unexpected public AI response format: " + response);
                    Bukkit.broadcastMessage("❌ Public AI reply failed");
                    return;
                }
                String reply = resJson
                        .getAsJsonObject("output")
                        .getAsJsonArray("choices")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString();

                // ✅ Add the AI reply
                JsonObject aiMsg = new JsonObject();
                aiMsg.addProperty("role", "assistant");
                aiMsg.addProperty("content", reply);
                context.add(aiMsg);

                // ✅ Send the reply to all players
                String finalReply = reply;
                Bukkit.getOnlinePlayers().forEach(p -> {
                    p.sendMessage("🌐 "+ColorGradient.getGradientName("[Server AI] " + finalReply));
                });

            } catch (Exception ex) {
                plugin.getLogger().severe("Public AI request error: " + ex.getMessage());
                ex.printStackTrace();
                Bukkit.broadcastMessage("❌ Public AI reply failed");
            }
        });
    }



}
