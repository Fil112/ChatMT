package mt.chat.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mt.chat.system.MonolithLoader;
import okhttp3.*;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GeminiManager {

    private final MonolithLoader loader;
    private final OkHttpClient client;

    private final String apiKey;
    private final boolean isEnabled;
    private final String apiUrl; // Полная ссылка для запроса

    public GeminiManager(MonolithLoader loader) {
        this.loader = loader;

        // Читаем всё из конфига
        this.apiKey = loader.getConfigManager().getConfig().getString("ai.gemini.api-key", "");
        this.isEnabled = loader.getConfigManager().getConfig().getBoolean("ai.gemini.enabled", true);
        String model = loader.getConfigManager().getConfig().getString("ai.gemini.model", "gemini-3.5-flash-lite");

        // Динамически склеиваем ссылку на основе выбранной модели и ключа
        this.apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + this.apiKey;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void askGemini(String prompt, Consumer<String> callback) {
        if (!isEnabled || apiKey.isEmpty()) {
            callback.accept("§cИИ в данный момент отключен или не настроен.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            try {
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", prompt);

                JsonArray partsArray = new JsonArray();
                partsArray.add(textPart);

                JsonObject contentObj = new JsonObject();
                contentObj.add("parts", partsArray);

                JsonArray contentsArray = new JsonArray();
                contentsArray.add(contentObj);

                JsonObject requestBodyObj = new JsonObject();
                requestBodyObj.add("contents", contentsArray);

                String json = requestBodyObj.toString();

                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

                // Используем нашу уже готовую полную ссылку
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        loader.getPlugin().getLogger().warning("Ошибка Gemini API: " + response.code());
                        callback.accept("§cИзвините, я сейчас не могу ответить. Ошибка: " + response.code());
                        return;
                    }

                    String responseString = response.body().string();
                    JsonObject jsonResponse = JsonParser.parseString(responseString).getAsJsonObject();

                    if (jsonResponse.has("candidates")) {
                        JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                        JsonObject content = firstCandidate.getAsJsonObject("content");
                        JsonArray parts = content.getAsJsonArray("parts");

                        String replyText = parts.get(0).getAsJsonObject().get("text").getAsString();

                        callback.accept(replyText.trim());
                    } else {
                        callback.accept("§cПустой ответ от нейросети.");
                    }
                }
            } catch (Exception e) {
                loader.getPlugin().getLogger().severe("Сбой при обращении к Gemini: " + e.getMessage());
                callback.accept("§cПроизошла ошибка при связи с ИИ.");
            }
        });
    }
}