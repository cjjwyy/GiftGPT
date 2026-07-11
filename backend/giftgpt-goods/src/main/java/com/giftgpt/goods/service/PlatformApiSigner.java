package com.giftgpt.goods.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * MD5 signature utility for Pinduoduo affiliate API.
 */
public final class PlatformApiSigner {

    private PlatformApiSigner() {}

    /**
     * Pinduoduo signing: MD5(secret + sortedKeyValueConcat + secret), uppercase.
     * Concatenation: key1value1key2value2... (no separator, no equals sign).
     */
    public static String signPdd(Map<String, String> params, String clientSecret) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder(clientSecret);
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            String v = e.getValue();
            if (v != null && !v.isEmpty()) {
                sb.append(e.getKey()).append(v);
            }
        }
        sb.append(clientSecret);
        return md5Upper(sb.toString());
    }

    private static String md5Upper(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}
