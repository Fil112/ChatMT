package mt.chat.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mt.chat.system.MonolithLoader;
import okhttp3.*;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AiManager {

    private final MonolithLoader loader;
    private final OkHttpClient client;

    public AiManager(MonolithLoader loader) {
        this.loader = loader;

        // 60 секунд на чтение — локалки и тяжелые ответы не должны падать по таймауту
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
    }

    public boolean isEnabled() {
        return loader.getConfigManager().getConfig().getBoolean("ai.enabled", true);
    }

    public String getTrigger() {
        return loader.getConfigManager().getConfig().getString("ai.trigger", "Бот,");
    }

    /**
     * Основная точка входа для запросов к ИИ
     */
    public void askAi(String prompt, Consumer<String> callback) {
        if (!isEnabled()) {
            callback.accept("§cИИ-ассистент отключен в конфигурации.");
            return;
        }

        String provider = loader.getConfigManager().getConfig().getString("ai.provider", "gemini").toLowerCase();

        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), new Runnable() {
            @Override
            public void run() {
                try {
                    switch (provider) {
                        case "gemini":
                            executeGeminiRequest(prompt, callback);
                            break;
                        case "claude":
                            executeClaudeRequest(prompt, callback);
                            break;
                        case "openai-compatible":
                        case "local":
                        case "ollama":
                        default:
                            executeOpenAiCompatibleRequest(prompt, callback);
                            break;
                    }
                } catch (Exception e) {
                    loader.getPlugin().getLogger().warning("Сбой при вызове ИИ (" + provider + "): " + e.getMessage());
                    callback.accept("§cНе удалось связаться с нейросетью. Подробности в консоли сервера.");
                }
            }
        });
    }

    public void askGemini(String prompt, Consumer<String> callback) {
        askAi(prompt, callback);
    }

    // =========================================================================
    // 1. OPENAI-СОВМЕСТИМЫЙ РЕЖИМ (DEEPSEEK, GROK, OPENAI, OLLAMA, LM STUDIO)
    // =========================================================================
    private void executeOpenAiCompatibleRequest(String prompt, Consumer<String> callback) throws Exception {
        String endpoint = loader.getConfigManager().getConfig().getString("ai.universal.endpoint",
                loader.getConfigManager().getConfig().getString("ai.endpoint", "https://api.deepseek.com/v1/chat/completions"));

        String apiKey = loader.getConfigManager().getConfig().getString("ai.universal.api-key",
                loader.getConfigManager().getConfig().getString("ai.api-key", ""));

        String model = loader.getConfigManager().getConfig().getString("ai.universal.model",
                loader.getConfigManager().getConfig().getString("ai.model", "deepseek-chat"));

        String systemPrompt = loader.getConfigManager().getConfig().getString("ai.system-prompt", "");
        double temperature = loader.getConfigManager().getConfig().getDouble("ai.temperature", 0.7);
        int maxTokens = loader.getConfigManager().getConfig().getInt("ai.max-tokens", 250);

        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("temperature", temperature);
        root.addProperty("max_tokens", maxTokens);

        JsonArray messages = new JsonArray();

        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt.trim());
            messages.add(sysMsg);
        }

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt.trim());
        messages.add(userMsg);

        root.add("messages", messages);

        RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request.Builder requestBuilder = new Request.Builder()
                .url(endpoint)
                .post(body);

        // Для локальных сетей (Ollama) токен пустой, хидер не вешаем
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey.trim());
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                int code = response.code();
                loader.getPlugin().getLogger().warning("Ошибка OpenAI-compatible API [" + code + "]");
                callback.accept("§cОшибка нейросети (HTTP " + code + ").");
                return;
            }

            String respStr = response.body().string();
            JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();

            if (json.has("choices") && json.getAsJsonArray("choices").size() > 0) {
                JsonObject firstChoice = json.getAsJsonArray("choices").get(0).getAsJsonObject();
                if (firstChoice.has("message") && firstChoice.getAsJsonObject("message").has("content")) {
                    callback.accept(firstChoice.getAsJsonObject("message").get("content").getAsString().trim());
                    return;
                }
            }
            callback.accept("§cПустой ответ от модели.");
        }
    }

    // =========================================================================
    // 2. NATIVE GOOGLE GEMINI API
    // =========================================================================
    private void executeGeminiRequest(String prompt, Consumer<String> callback) throws Exception {
        String apiKey = loader.getConfigManager().getConfig().getString("ai.gemini.api-key",
                loader.getConfigManager().getConfig().getString("ai.api-key", ""));

        String model = loader.getConfigManager().getConfig().getString("ai.gemini.model",
                loader.getConfigManager().getConfig().getString("ai.model", "gemini-3.5-flash-lite"));

        String systemPrompt = loader.getConfigManager().getConfig().getString("ai.system-prompt", "");

        if (apiKey.isEmpty() || apiKey.contains("ВСТАВЬ_СЮДА")) {
            callback.accept("§cAPI-ключ Gemini не настроен в config.yml!");
            return;
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        JsonObject root = new JsonObject();

        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JsonObject sysObj = new JsonObject();
            JsonArray sysParts = new JsonArray();
            JsonObject sysText = new JsonObject();
            sysText.addProperty("text", systemPrompt);
            sysParts.add(sysText);
            sysObj.add("parts", sysParts);
            root.add("system_instruction", sysObj);
        }

        JsonArray contents = new JsonArray();
        JsonObject userContent = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        userContent.add("parts", parts);
        contents.add(userContent);
        root.add("contents", contents);

        RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                callback.accept("§cОшибка Gemini API (HTTP " + response.code() + ").");
                return;
            }

            String respStr = response.body().string();
            JsonObject json = JsonParser.parseString(respStr).getAsJsonObject();

            if (json.has("candidates") && json.getAsJsonArray("candidates").size() > 0) {
                JsonObject firstCandidate = json.getAsJsonArray("candidates").get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray resParts = content.getAsJsonArray("parts");
                callback.accept(resParts.get(0).getAsJsonObject().get("text").getAsString().trim());
            } else {
                callback.accept("§cПустой ответ от Gemini.");
            }
        }
    }

    // =========================================================================
    // 3. ANTHROPIC CLAUDE API
    // =========================================================================
    private void executeClaudeRequest(String prompt, Consumer<String> callback) throws Exception {
        String apiKey = loader.getConfigManager().getConfig().getString("ai.claude.api-key", "");
        String model = loader.getConfigManager().getConfig().getString("ai.claude.model", "claude-3-5-sonnet-20241022");
        String systemPrompt = loader.getConfigManager().getConfig().getString("ai.system-prompt", "");
        int maxTokens = loader.getConfigManager().getConfig().getInt("ai.max-tokens", 250);

        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            root.addProperty("system", systemPrompt);
        }

        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        root.add("messages", messages);

        RequestBody body = RequestBody.create(root.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                callback.accept("§cОшибка Claude API (HTTP " + response.code() + ").");
                return;
            }

            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if (json.has("content")) {
                JsonArray arr = json.getAsJsonArray("content");
                for (JsonElement el : arr) {
                    JsonObject item = el.getAsJsonObject();
                    if ("text".equals(item.get("type").getAsString())) {
                        callback.accept(item.get("text").getAsString().trim());
                        return;
                    }
                }
            }
            callback.accept("§cПустой ответ от Claude.");
        }
    }
}