# 🛠️ ChatMT — Ultimate Documentation (v1.4.2)

ChatMT is a modern and flexible chat management solution for Minecraft servers (1.16.5+). It combines powerful MiniMessage formatting, protection systems, and an intuitive GUI for moderation.

---

## 🚀 Installation
1. Download ChatMT.jar and place it in the /plugins/ folder.
2. Install dependencies: Vault and PlaceholderAPI.
3. (Optional) Install WorldGuard to control chat in specific regions.
4. Restart your server.

---

## ⚙️ Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| /mt help | Show help menu | chatmt.user |
| /mt reload | Reload config and language files | chatmt.admin.reload |
| /mt punish <player> | Open Punishment GUI | chatmt.staff.punish |
| /mt history <player> | View player violation history | chatmt.staff.history |
| /mt clear | Clear global chat | chatmt.staff.clear |

### 🔑 Extra Permissions:
* chatmt.bypass.spam — Bypass anti-spam cooldown.
* chatmt.bypass.caps — Bypass anti-caps filter.
* chatmt.color — Permission to use colors in chat.

---

## 🎨 Formatting & HEX
The plugin supports the MiniMessage system. Use them in chat or config:
* HEX Colors: <#55FFBB>Hello! or &#55FFBBHello!
* Gradients: <gradient:#55FFBB:#00AAFF>Beautiful Text</gradient>
* Effects: <rainbow>Rainbow Message</rainbow>, <b>Bold</b>, <i>Italic</i>.

---

## 🧩 Placeholders
All placeholders are supported via PlaceholderAPI:
* %player% — Player's name.
* %message% — Message content.
* Any external: %luckperms_prefix%, %vault_rank%, %player_ping%.

---

## 📦 Modules
1. Anti-Spam: Chat cooldown management.
2. Anti-Caps: Automatically lowers uppercase messages.
3. Auto-Messages: Scheduled announcements with HEX support.
4. Replacer: Auto-replace words or symbols (e.g., swear filters).

---

## 🤝 Integrations
Officially supports and integrates with:
* Vault (Groups & Prefixes).
* PlaceholderAPI (Dynamic data).
* WorldGuard (Quiet mode in specific regions).

---

## 📜 License
This project is licensed under the MIT License. You are free to use and modify the code.