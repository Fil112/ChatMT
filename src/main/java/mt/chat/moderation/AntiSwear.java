package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import org.bukkit.entity.Player;

import java.util.List;

public class AntiSwear {

    private final MonolithLoader loader;

    public AntiSwear(MonolithLoader loader) {
        this.loader = loader;
    }

    public String filterSwear(Player player, String message) {
        if (player.hasPermission("chatmt.bypass.swear")) return message;

        boolean enabled = loader.getConfigManager().getConfig().getBoolean("filters.swear.enabled", true);
        if (!enabled) return message;

        List<String> badWords = loader.getConfigManager().getConfig().getStringList("filters.swear.words");

        // 1. Создаем "грязную" копию текста и нормализуем её для проверки
        String normalizedMessage = normalizeText(message);
        String filteredMessage = message;

        // 2. Ищем совпадения
        for (String word : badWords) {
            String normalizedWord = normalizeText(word); // Нормализуем и само слово из конфига на всякий случай

            // Если в сжатом и очищенном тексте найден мат
            if (normalizedMessage.contains(normalizedWord)) {

                // Генерируем звёздочки по длине слова (мат -> ***)
                String stars = new String(new char[word.length()]).replace("\0", "*");

                // Генерируем умную регулярку, чтобы заменить мат в ОРИГИНАЛЬНОМ сообщении,
                // даже если он написан как "с_у.к  а"
                String regexPattern = buildRegexPattern(word);
                filteredMessage = filteredMessage.replaceAll("(?i)" + regexPattern, stars);
            }
        }

        return filteredMessage;
    }

    /**
     * МОДУЛЬ ДЕОБФУСКАЦИИ
     * Приводит текст к "голому" виду, снимая все попытки обхода фильтра.
     */
    private String normalizeText(String text) {
        // 1. Нижний регистр
        text = text.toLowerCase();

        // 2. Подмена английских букв-обманок на русскую кириллицу
        text = text.replace('a', 'а').replace('o', 'о').replace('e', 'е')
                .replace('c', 'с').replace('p', 'р').replace('x', 'х')
                .replace('y', 'у').replace('m', 'м').replace('t', 'т')
                .replace('b', 'в').replace('k', 'к');

        // 3. Жесткая очистка: удаляем абсолютно всё, кроме русских букв
        // (убирает пробелы, точки, цифры, символы)
        text = text.replaceAll("[^а-яё]", "");

        // 4. Сжатие дубликатов: "ссуууккаа" превратится в "сука"
        // Регулярка (.)\1+ ищет любой символ, который повторяется подряд, и заменяет его на один такой символ
        text = text.replaceAll("(.)\\1+", "$1");

        return text;
    }

    /**
     * УМНАЯ ЗАМЕНА
     * Строит регулярное выражение для поиска мата со вставленными между буквами символами.
     * Пример для слова "мат": "м[^а-яё]*а[^а-яё]*т"
     */
    private String buildRegexPattern(String word) {
        StringBuilder regex = new StringBuilder();
        char[] chars = word.toLowerCase().toCharArray();

        for (int i = 0; i < chars.length; i++) {
            // Добавляем текущую букву мата
            regex.append(chars[i]);
            regex.append("+"); // Позволяем этой букве дублироваться в оригинале

            if (i < chars.length - 1) {
                // Между буквами разрешаем любое количество мусорных не-буквенных символов
                regex.append("[^а-яёa-z]*");
            }
        }
        return regex.toString();
    }
}