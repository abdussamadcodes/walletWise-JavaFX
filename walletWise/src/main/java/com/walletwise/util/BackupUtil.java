package com.walletwise.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupUtil {

    public static void createBackup() {
        try {
            File dbFile = new File("walletwise.db");
            if (!dbFile.exists()) return;

            File backupDir = new File("backups");
            if (!backupDir.exists()) {
                backupDir.mkdir();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss"));
            File backupFile = new File(backupDir, "walletwise_backup_" + timestamp + ".db");

            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Automated backup saved: " + backupFile.getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Failed to create automated backup: " + e.getMessage());
        }
    }
}