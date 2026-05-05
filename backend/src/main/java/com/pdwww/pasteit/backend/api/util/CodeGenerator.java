package com.pdwww.pasteit.backend.api.util;

import com.pdwww.pasteit.backend.api.exception.ServerResourceException;
import com.pdwww.pasteit.backend.api.storage.StashStorage;

public class CodeGenerator {
    public static String generateUniqueCode() {
        String code = null;
        for (int len = 8; len <= 40; len++) {
            code = generateRandomCode(len);
            if (!StashStorage.exists(code)) {
                break;
            }
            code = null;
        }

        if (code == null) {
            throw new ServerResourceException("Failed to generate unique code after multiple attempts");
        }

        return code;
    }

    private static String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }
}
