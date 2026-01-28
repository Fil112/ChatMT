# 💬 ChatMT

![Version](https://img.shields.io/badge/version-1.4.3-green.svg)
![Platform](https://img.shields.io/badge/platform-Spigot-orange.svg)

ChatMT — это мощный модульный плагин для управления чатом на серверах Minecraft (1.16.5+).

## ✨ Основные функции
* HEX & Gradients: Полная поддержка MiniMessage и Legacy HEX цветов.
* Modular: Включение/выключение функций (Анти-спам, Анти-капс, Авто-сообщения).
* GUI Punishment: Удобное меню наказаний (/mt punish).
* Optional Dependencies: Интеграция с Vault, PAPI и WorldGuard.

## 🛠 Команды
| Команда | Описание | Право |
| :--- | :--- | :--- |
| /mt help | Список всех команд | chatmt.user |
| /mt reload | Перезагрузка плагина | chatmt.admin.reload |
| /mt punish <игрок> | Открыть меню наказаний | chatmt.staff.punish |
| /mt history <игрок> | История нарушений игрока | chatmt.staff.history |
| /mt clear | Полная очистка чата | chatmt.staff.clear |

## 🧩 Плейсхолдеры
Плагин поддерживает все плейсхолдеры PlaceholderAPI, а также встроенные:
* %player% — Имя отправителя.
* %message% — Текст сообщения.

## 📥 Установка
1. Соберите проект через mvn clean package.
2. Поместите ChatMT.jar в папку plugins.
3. Установите зависимости (Vault, PAPI) для расширенной работы.