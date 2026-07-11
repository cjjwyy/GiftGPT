package com.giftgpt.goods.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giftgpt.goods.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Pinduoduo Duoduoke (拼多多多多客) open API — pdd.ddk.goods.search
 *
 * PDD keyword search requires the PID to have completed "member authority"
 * (站长授权): the developer opens the authority URL in the Pinduoduo app and
 * confirms. Until bind==1, pdd.ddk.goods.search returns error 50001
 * ("未绑定已经授权的推广位"). This service exposes the authority URL so the
 * one-time authorization can be completed; after that, search works.
 */
@Slf4j
@Service
public class PddService {

    @Value("${giftgpt.commerce.pinduoduo.client-id:}")
    private String clientId;

    @Value("${giftgpt.commerce.pinduoduo.client-secret:}")
    private String clientSecret;

    private static final String GATEWAY = "https://gw-api.pinduoduo.com/api/router";
    private static final String MEDIA_ID = "11177534914";
    private static final String USER_UID = "giftgpt_user_001";
    private static final String PID_NAME = "GiftGPT_default";
    private static final Path PID_FILE = Paths.get("data", "pdd_pid.txt");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String cachedPid;
    private volatile Boolean cachedAuthorized;
    private volatile String cachedAuthorityUrl;

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    /** Generate (or reuse) a single PID used for both authority and search. Persisted to data/pdd_pid.txt so it survives restarts. */
    private String getOrCreatePid() {
        if (cachedPid != null) return cachedPid;

        // 1. Reuse a previously persisted pid
        String saved = loadSavedPid();
        if (saved != null) {
            cachedPid = saved;
            log.info("Pinduoduo PID loaded from file: {}", saved);
            return saved;
        }

        // 2. Try to recover an already-authorized pid (e.g. user authorized before persistence existed)
        String authed = findAuthorizedPid();
        if (authed != null) {
            cachedPid = authed;
            savePid(authed);
            log.info("Pinduoduo recovered an already-authorized PID: {}", authed);
            return authed;
        }

        // 3. Otherwise generate a new one and persist it
        String pid = generatePid();
        if (pid != null) {
            cachedPid = pid;
            savePid(pid);
        }
        return pid;
    }

    /** Call pdd.ddk.goods.pid.generate to create a new PID. */
    private String generatePid() {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("type", "pdd.ddk.goods.pid.generate");
            params.put("client_id", clientId);
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            params.put("number", "1");
            params.put("p_id_name", PID_NAME);
            params.put("media_id", MEDIA_ID);
            params.put("sign", PlatformApiSigner.signPdd(params, clientSecret));

            String body = httpPost(GATEWAY, params);
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.get("error_response");
            if (error != null) {
                log.error("Pinduoduo PID generate error: {}", error);
                return null;
            }
            JsonNode resp = root.get("p_id_generate_response");
            if (resp != null) {
                JsonNode pIdList = resp.get("p_id_list");
                if (pIdList != null && pIdList.isArray() && pIdList.size() > 0) {
                    String pid = pIdList.get(0).get("p_id").asText();
                    log.info("Pinduoduo PID generated: {}", pid);
                    return pid;
                }
            }
        } catch (Exception e) {
            log.error("Pinduoduo PID generate failed", e);
        }
        return null;
    }

    /** List existing PIDs via pdd.ddk.goods.pid.query and return the first authorized one. */
    private String findAuthorizedPid() {
        try {
            for (int page = 1; page <= 5; page++) {
                Map<String, String> params = new LinkedHashMap<>();
                params.put("type", "pdd.ddk.goods.pid.query");
                params.put("client_id", clientId);
                params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
                params.put("page", String.valueOf(page));
                params.put("page_size", "100");
                params.put("sign", PlatformApiSigner.signPdd(params, clientSecret));

                String body = httpPost(GATEWAY, params);
                JsonNode root = objectMapper.readTree(body);
                if (root.has("error_response")) break;
                JsonNode pidList = root.at("/p_id_query_response/p_id_list");
                if (pidList == null || !pidList.isArray() || pidList.size() == 0) break;
                for (JsonNode item : pidList) {
                    String pid = item.get("p_id").asText();
                    if (isAuthorized(pid)) return pid;
                }
            }
        } catch (Exception e) {
            log.warn("Pinduoduo findAuthorizedPid failed", e);
        }
        return null;
    }

    private String loadSavedPid() {
        try {
            if (Files.exists(PID_FILE)) {
                String pid = Files.readString(PID_FILE).trim();
                return pid.isEmpty() ? null : pid;
            }
        } catch (Exception e) {
            log.warn("Failed to read saved PDD pid", e);
        }
        return null;
    }

    private void savePid(String pid) {
        try {
            Files.createDirectories(PID_FILE.getParent());
            Files.writeString(PID_FILE, pid);
        } catch (Exception e) {
            log.warn("Failed to persist PDD pid", e);
        }
    }

    /** Check whether the PID has been authorized (bind==1). */
    private boolean isAuthorized(String pid) {
        if (pid == null) return false;
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("type", "pdd.ddk.member.authority.query");
            params.put("client_id", clientId);
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            params.put("pid", pid);
            params.put("sign", PlatformApiSigner.signPdd(params, clientSecret));

            String body = httpPost(GATEWAY, params);
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.get("error_response");
            if (error != null) {
                log.warn("Authority query error: {}", error);
                return false;
            }
            JsonNode resp = root.get("authority_query_response");
            if (resp != null) {
                return resp.get("bind").asInt(0) == 1;
            }
        } catch (Exception e) {
            log.warn("Authority query failed", e);
        }
        return false;
    }

    /**
     * Generate the authority URL the user must open in the Pinduoduo app.
     * After the user confirms, bind becomes 1 and keyword search works.
     */
    public String generateAuthorityUrl() {
        if (!isConfigured()) return null;
        String pid = getOrCreatePid();
        if (pid == null) return null;
        try {
            String customParams = "{\"uid\":\"" + USER_UID + "\"}";
            Map<String, String> params = new LinkedHashMap<>();
            params.put("type", "pdd.ddk.rp.prom.url.generate");
            params.put("client_id", clientId);
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            params.put("p_id_list", "[\"" + pid + "\"]");
            params.put("channel_type", "10");
            params.put("custom_parameters", customParams);
            params.put("sign", PlatformApiSigner.signPdd(params, clientSecret));

            String body = httpPost(GATEWAY, params);
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.get("error_response");
            if (error != null) {
                log.error("Generate authority URL error: {}", error);
                return null;
            }
            JsonNode urlList = root.at("/rp_promotion_url_generate_response/url_list");
            if (urlList.isArray() && urlList.size() > 0) {
                String authUrl = urlList.get(0).get("mobile_url").asText();
                cachedAuthorityUrl = authUrl;
                log.info("PDD authority URL: {}", authUrl);
                return authUrl;
            }
        } catch (Exception e) {
            log.error("Failed to generate PDD authority URL", e);
        }
        return null;
    }

    /**
     * Authority status for the frontend: whether the PID is authorized and,
     * if not, the URL the user should open to authorize.
     */
    public Map<String, Object> getAuthorityStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", isConfigured());
        if (!isConfigured()) {
            status.put("authorized", false);
            status.put("message", "拼多多 client_id / client_secret 未配置");
            return status;
        }
        String pid = getOrCreatePid();
        status.put("pid", pid);
        boolean authorized = pid != null && isAuthorized(pid);
        cachedAuthorized = authorized;
        status.put("authorized", authorized);
        if (authorized) {
            status.put("message", "已授权，可正常搜索");
        } else {
            String url = cachedAuthorityUrl != null ? cachedAuthorityUrl : generateAuthorityUrl();
            status.put("authorityUrl", url);
            status.put("message", "拼多多推广位未授权：请在手机端拼多多 App 中打开授权链接完成站长授权，授权后即可搜索。");
        }
        return status;
    }

    public List<Product> searchGoods(String keyword, int page, int pageSize) {
        if (!isConfigured()) {
            log.info("Pinduoduo keys not configured, skipping PDD search");
            return Collections.emptyList();
        }

        String pid = getOrCreatePid();
        if (pid == null) {
            log.warn("Failed to get PDD PID, skipping search");
            return Collections.emptyList();
        }

        // PDD keyword search works with a pid + custom_parameters even before
        // the member-authority (bind) flow is completed, so we do NOT gate on
        // isAuthorized here. Searching directly returns goods; if the API ever
        // replies with error 50001 (auth required for this account), parseResponse
        // logs it and we return empty — the authority URL is still available via
        // getAuthorityStatus() for the user to authorize if needed.
        String customParams = "{\"uid\":\"" + USER_UID + "\"}";
        List<Product> products = new ArrayList<>();
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("type", "pdd.ddk.goods.search");
            params.put("client_id", clientId);
            params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            params.put("keyword", keyword);
            params.put("page", String.valueOf(page));
            params.put("page_size", String.valueOf(Math.max(10, Math.min(pageSize, 100))));
            params.put("sort_type", "0");
            params.put("pid", pid);
            params.put("custom_parameters", customParams);
            params.put("sign", PlatformApiSigner.signPdd(params, clientSecret));

            String body = httpPost(GATEWAY, params);
            parseResponse(body, products);
        } catch (Exception e) {
            log.error("Pinduoduo search failed for keyword: {}", keyword, e);
        }
        return products;
    }

    private String httpPost(String urlStr, Map<String, String> params) throws Exception {
        StringBuilder qs = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (qs.length() > 0) qs.append('&');
            qs.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8))
              .append('=')
              .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }

        byte[] body = qs.toString().getBytes(StandardCharsets.UTF_8);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.getOutputStream().write(body);

        int code = conn.getResponseCode();
        if (code == 200) {
            return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
        String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        log.warn("Pinduoduo HTTP {}: {}", code, err);
        throw new RuntimeException("Pinduoduo HTTP " + code);
    }

    private void parseResponse(String body, List<Product> products) throws Exception {
        JsonNode root = objectMapper.readTree(body);

        JsonNode error = root.get("error_response");
        if (error != null) {
            log.warn("Pinduoduo API error: {}", error);
            // Authorization lost — force re-check on next search so the auth URL is surfaced again.
            cachedAuthorized = false;
            return;
        }

        JsonNode resp = root.get("goods_search_response");
        if (resp == null) {
            log.warn("PDD unexpected response: {}", body.substring(0, Math.min(300, body.length())));
            return;
        }

        JsonNode goodsList = resp.get("goods_list");
        if (goodsList == null || !goodsList.isArray()) return;

        for (JsonNode item : goodsList) {
            Product p = new Product();
            p.setName(optText(item, "goods_name"));
            long priceInCents = item.get("min_group_price").asLong(0);
            p.setPrice(BigDecimal.valueOf(priceInCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            p.setPlatform("拼多多");
            String goodsId = optText(item, "goods_id");
            String goodsSign = optText(item, "goods_sign");
            String url;
            if (!goodsId.isEmpty()) {
                url = "https://mobile.yangkeduo.com/goods.html?goods_id=" + goodsId;
            } else if (!goodsSign.isEmpty()) {
                url = "https://mobile.yangkeduo.com/goods.html?goods_sign=" + goodsSign;
            } else {
                url = "";
            }
            p.setPlatformUrl(url);
            p.setImageUrl(optText(item, "goods_thumbnail_url"));
            p.setCategory(optText(item, "opt_name"));
            p.setDescription(optText(item, "goods_desc"));
            p.setSalesCount(parseInt(optText(item, "sales_tip")));
            p.setStatus(1);
            products.add(p);
        }
        log.info("Pinduoduo returned {} products", products.size());
    }

    private String optText(JsonNode node, String key) {
        JsonNode n = node.get(key);
        return n != null && !n.isNull() ? n.asText() : "";
    }

    private int parseInt(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
