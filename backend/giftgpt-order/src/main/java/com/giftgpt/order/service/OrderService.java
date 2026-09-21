package com.giftgpt.order.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.giftgpt.common.ai.PythonAiClient;
import com.giftgpt.common.exception.BusinessException;
import com.giftgpt.common.result.ResultCode;
import com.giftgpt.goods.entity.Product;
import com.giftgpt.goods.mapper.ProductMapper;
import com.giftgpt.order.dto.*;
import com.giftgpt.order.entity.Feedback;
import com.giftgpt.order.entity.FeedbackAccessToken;
import com.giftgpt.order.entity.GreetingCard;
import com.giftgpt.order.entity.LogisticsEvent;
import com.giftgpt.order.entity.Order;
import com.giftgpt.order.entity.Packaging;
import com.giftgpt.order.mapper.FeedbackMapper;
import com.giftgpt.order.mapper.FeedbackAccessTokenMapper;
import com.giftgpt.order.mapper.GreetingCardMapper;
import com.giftgpt.order.mapper.LogisticsEventMapper;
import com.giftgpt.order.mapper.OrderMapper;
import com.giftgpt.order.mapper.PackagingMapper;
import com.giftgpt.user.entity.GiftRecord;
import com.giftgpt.user.entity.Recipient;
import com.giftgpt.user.mapper.GiftRecordMapper;
import com.giftgpt.user.mapper.RecipientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final PackagingMapper packagingMapper;
    private final GreetingCardMapper greetingCardMapper;
    private final FeedbackMapper feedbackMapper;
    private final FeedbackAccessTokenMapper feedbackAccessTokenMapper;
    private final GiftRecordMapper giftRecordMapper;
    private final RecipientMapper recipientMapper;
    private final LogisticsEventMapper logisticsEventMapper;
    private final ProductMapper productMapper;
    private final PythonAiClient pythonAiClient;

    @Value("${giftgpt.public-web-url:http://localhost:3000}")
    private String publicWebUrl;

    @Value("${giftgpt.upload.path:./data/uploads}")
    private String uploadPath;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long MAX_VOICE_SIZE = 5L * 1024 * 1024;

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ORDERED = "ordered";
    public static final String STATUS_PACKAGED = "packaged";
    public static final String STATUS_SHIPPED = "shipped";
    public static final String STATUS_DELIVERED = "delivered";
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
            STATUS_PENDING, Set.of(STATUS_ORDERED),
            STATUS_ORDERED, Set.of(STATUS_PACKAGED, STATUS_SHIPPED),
            STATUS_PACKAGED, Set.of(STATUS_SHIPPED),
            STATUS_SHIPPED, Set.of(STATUS_DELIVERED),
            STATUS_DELIVERED, Set.of());

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        GiftRecord giftRecord = giftRecordMapper.selectById(request.getGiftRecordId());
        if (giftRecord == null || !giftRecord.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.GIFT_RECORD_NOT_FOUND);
        }

        List<Order> existingOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getGiftRecordId, giftRecord.getId())
                        .orderByDesc(Order::getId)
                        .last("LIMIT 1"));
        if (!existingOrders.isEmpty()) {
            return existingOrders.get(0);
        }

        Packaging packaging = packagingMapper.selectOne(
                new LambdaQueryWrapper<Packaging>()
                        .eq(Packaging::getGiftRecordId, giftRecord.getId())
                        .orderByDesc(Packaging::getId)
                        .last("LIMIT 1"));
        if (packaging == null && request.getPackagingThemeId() != null) {
            Packaging candidate = packagingMapper.selectById(request.getPackagingThemeId());
            if (candidate == null || !userId.equals(candidate.getUserId())
                    || (candidate.getGiftRecordId() != null
                    && !giftRecord.getId().equals(candidate.getGiftRecordId()))) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            packaging = candidate;
        }

        BigDecimal productAmount = giftRecord.getBudget() == null
                ? BigDecimal.ZERO : giftRecord.getBudget().max(BigDecimal.ZERO);
        if (giftRecord.getProductId() != null) {
            Product product = productMapper.selectById(giftRecord.getProductId());
            if (product != null && Integer.valueOf(1).equals(product.getStatus())
                    && product.getPrice() != null && product.getPrice().signum() >= 0) {
                productAmount = product.getPrice();
            }
        }
        BigDecimal packagingAmount = packaging == null || packaging.getPrice() == null
                ? BigDecimal.ZERO : packaging.getPrice().max(BigDecimal.ZERO);

        Order order = new Order();
        order.setGiftRecordId(giftRecord.getId());
        order.setOrderNo(IdUtil.fastSimpleUUID().substring(0, 20));
        order.setTotalAmount(productAmount.add(packagingAmount));
        order.setStatus(STATUS_ORDERED);
        order.setLogisticsNo("DEMO-" + IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase());
        order.setLogisticsCompany("模拟物流");
        orderMapper.insert(order);

        if (packaging != null) {
            packaging.setOrderId(order.getId());
            packaging.setGiftRecordId(giftRecord.getId());
            packagingMapper.updateById(packaging);
        }

        if (request.getCustomMessage() != null && !request.getCustomMessage().isBlank()) {
            GreetingCard greeting = giftRecord.getGreetingCardId() == null
                    ? new GreetingCard() : greetingCardMapper.selectById(giftRecord.getGreetingCardId());
            if (greeting == null) {
                greeting = new GreetingCard();
            }
            greeting.setContent(request.getCustomMessage().trim());
            greeting.setStyleTemplate(normalizeGreetingStyle(request.getGreetingStyle()));
            if (greeting.getId() == null) {
                greetingCardMapper.insert(greeting);
                giftRecord.setGreetingCardId(greeting.getId());
            } else {
                greetingCardMapper.updateById(greeting);
            }
        }

        insertMockLogisticsEvents(order.getId());
        changeOrderStatus(order, STATUS_SHIPPED);

        giftRecord.setBudget(productAmount);
        giftRecord.setStatus(STATUS_SHIPPED);
        giftRecordMapper.updateById(giftRecord);

        return order;
    }

    private void changeOrderStatus(Order order, String newStatus) {
        String currentStatus = order.getStatus();
        if (newStatus.equals(currentStatus)) {
            return;
        }
        if (!STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                    "订单状态不能从 " + currentStatus + " 变更为 " + newStatus);
        }
        order.setStatus(newStatus);
        orderMapper.updateById(order);
        log.info("order_status_changed orderId={} from={} to={}",
                order.getId(), currentStatus, newStatus);
    }

    private GiftRecord loadOwnGiftRecord(Long giftRecordId) {
        Long userId = StpUtil.getLoginIdAsLong();
        GiftRecord record = giftRecordMapper.selectById(giftRecordId);
        if (record == null) {
            throw new BusinessException(ResultCode.GIFT_RECORD_NOT_FOUND);
        }
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return record;
    }

    public OrderDetailResponse getOrderDetail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        GiftRecord giftRecord = loadOwnGiftRecord(order.getGiftRecordId());
        Recipient recipient = recipientMapper.selectById(giftRecord.getRecipientId());
        Packaging packaging = packagingMapper.selectOne(
                new LambdaQueryWrapper<Packaging>().eq(Packaging::getOrderId, order.getId()));
        GreetingCard greeting = null;
        if (giftRecord.getGreetingCardId() != null) {
            greeting = greetingCardMapper.selectById(giftRecord.getGreetingCardId());
        }

        OrderDetailResponse resp = new OrderDetailResponse();
        resp.setOrderId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setLogisticsNo(order.getLogisticsNo());
        resp.setLogisticsCompany(order.getLogisticsCompany());
        resp.setRecipientName(recipient != null ? recipient.getName() : "");
        resp.setPackagingTheme(packaging != null ? packaging.getTheme() : null);
        if (greeting != null) {
            resp.setGreetingContent(greeting.getContent());
            resp.setGreetingVoiceUrl(greeting.getVoiceUrl());
            resp.setGreetingQrCodeUrl(greeting.getQrCodeUrl());
        }
        return resp;
    }

    public LogisticsResponse getLogistics(Long giftRecordId) {
        loadOwnGiftRecord(giftRecordId);
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>().eq(Order::getGiftRecordId, giftRecordId));
        if (order == null) throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        LogisticsResponse resp = new LogisticsResponse();
        resp.setOrderNo(order.getOrderNo());
        resp.setStatus(order.getStatus());
        resp.setLogisticsNo(order.getLogisticsNo());
        resp.setLogisticsCompany(order.getLogisticsCompany());
        List<LogisticsEvent> evs = logisticsEventMapper.selectList(
            new LambdaQueryWrapper<LogisticsEvent>()
                .eq(LogisticsEvent::getOrderId, order.getId())
                .orderByAsc(LogisticsEvent::getEventTime));
        resp.setEvents(evs.stream().map(e -> {
            LogisticsResponse.Event ev = new LogisticsResponse.Event();
            ev.setEventTime(e.getEventTime() == null ? "" : e.getEventTime().toString());
            ev.setLocation(e.getLocation());
            ev.setStatus(e.getStatus());
            ev.setDescription(e.getDescription());
            ev.setSource(e.getSource());
            return ev;
        }).collect(Collectors.toList()));
        resp.setSimulated(evs.stream().anyMatch(e -> "simulation".equals(e.getSource())));
        return resp;
    }

    public GreetingResponse generateGreeting(GreetingGenerateRequest request) {
        GreetingResponse resp = new GreetingResponse();
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("recipientName", request.getRecipientName());
            payload.put("relation", request.getRelation() == null ? "" : request.getRelation());
            payload.put("occasion", request.getOccasion() == null ? "" : request.getOccasion());
            payload.put("senderName", request.getSenderName());
            PythonAiClient.GreetingResult r = pythonAiClient.generateGreeting(payload);
            resp.setContent(r.getContent());
            resp.setStyleTemplate(r.getStyleTemplate());
            resp.setAiGenerated(true);
            return resp;
        } catch (Exception e) {
            log.warn("Python greeting unavailable, fallback to template");
            resp.setContent(generateMockGreeting(request));
            resp.setStyleTemplate("classic");
            resp.setAiGenerated(false);
            return resp;
        }
    }

    public Feedback submitFeedback(Long giftRecordId, FeedbackRequest request) {
        loadOwnGiftRecord(giftRecordId);
        Feedback feedback = new Feedback();
        feedback.setGiftRecordId(giftRecordId);
        feedback.setContent(request.getContent().trim());
        feedback.setType(normalizeFeedbackType(request.getType()));
        feedback.setIsPublic(Integer.valueOf(1).equals(request.getIsPublic()) ? 1 : 0);
        feedback.setRole("sender");
        feedbackMapper.insert(feedback);
        return feedback;
    }

    public List<Feedback> listFeedback(Long giftRecordId) {
        loadOwnGiftRecord(giftRecordId);
        return feedbackMapper.selectList(
            new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getGiftRecordId, giftRecordId)
                .orderByAsc(Feedback::getCreateTime));
    }

    @Transactional
    public FeedbackLinkResponse createFeedbackLink(Long giftRecordId) {
        GiftRecord giftRecord = loadOwnGiftRecord(giftRecordId);
        feedbackAccessTokenMapper.update(null,
                new LambdaUpdateWrapper<FeedbackAccessToken>()
                        .eq(FeedbackAccessToken::getGiftRecordId, giftRecordId)
                        .eq(FeedbackAccessToken::getUsed, 0)
                        .set(FeedbackAccessToken::getUsed, 1));

        byte[] random = new byte[32];
        SECURE_RANDOM.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        FeedbackAccessToken accessToken = new FeedbackAccessToken();
        accessToken.setGiftRecordId(giftRecordId);
        accessToken.setTokenHash(hashToken(rawToken));
        accessToken.setExpireAt(expiresAt);
        accessToken.setUsed(0);
        feedbackAccessTokenMapper.insert(accessToken);

        String link = stripTrailingSlash(publicWebUrl) + "/feedback/" + rawToken;
        String qrCodeUrl = null;
        try {
            qrCodeUrl = saveBytes("qrcodes", ".png", pythonAiClient.generateQrCode(link));
            GreetingCard greeting = getOrCreateGreeting(giftRecord);
            greeting.setQrCodeUrl(qrCodeUrl);
            saveGreeting(giftRecord, greeting);
        } catch (Exception e) {
            log.warn("Feedback link created without QR image: {}", e.getMessage());
        }

        FeedbackLinkResponse response = new FeedbackLinkResponse();
        response.setUrl(link);
        response.setQrCodeUrl(qrCodeUrl);
        response.setExpiresAt(expiresAt);
        return response;
    }

    public PublicFeedbackGiftResponse getPublicFeedbackGift(String rawToken) {
        FeedbackAccessToken token = loadFeedbackToken(rawToken, true);
        GiftRecord giftRecord = giftRecordMapper.selectById(token.getGiftRecordId());
        if (giftRecord == null) {
            throw invalidFeedbackLink();
        }
        Recipient recipient = recipientMapper.selectById(giftRecord.getRecipientId());
        GreetingCard greeting = giftRecord.getGreetingCardId() == null
                ? null : greetingCardMapper.selectById(giftRecord.getGreetingCardId());
        Product product = giftRecord.getProductId() == null
                ? null : productMapper.selectById(giftRecord.getProductId());

        PublicFeedbackGiftResponse response = new PublicFeedbackGiftResponse();
        response.setRecipientName(recipient == null ? "朋友" : recipient.getName());
        response.setOccasion(giftRecord.getOccasion());
        response.setGreetingContent(greeting == null ? null : greeting.getContent());
        response.setGreetingVoiceUrl(greeting == null ? null : greeting.getVoiceUrl());
        response.setProductName(product == null ? null : product.getName());
        response.setSubmitted(Integer.valueOf(1).equals(token.getUsed()));
        return response;
    }

    @Transactional
    public Feedback submitPublicFeedback(String rawToken, FeedbackRequest request) {
        FeedbackAccessToken token = loadFeedbackToken(rawToken, false);
        if (feedbackAccessTokenMapper.consume(token.getId()) != 1) {
            throw invalidFeedbackLink();
        }
        Feedback feedback = new Feedback();
        feedback.setGiftRecordId(token.getGiftRecordId());
        feedback.setContent(request.getContent().trim());
        feedback.setType(normalizeFeedbackType(request.getType()));
        feedback.setIsPublic(Integer.valueOf(1).equals(request.getIsPublic()) ? 1 : 0);
        feedback.setRole("receiver");
        feedbackMapper.insert(feedback);
        return feedback;
    }

    public GreetingCard getGreeting(Long giftRecordId) {
        GiftRecord giftRecord = loadOwnGiftRecord(giftRecordId);
        return giftRecord.getGreetingCardId() == null
                ? null : greetingCardMapper.selectById(giftRecord.getGreetingCardId());
    }

    @Transactional
    public GreetingCard uploadGreetingVoice(Long giftRecordId, MultipartFile file) {
        GiftRecord giftRecord = loadOwnGiftRecord(giftRecordId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "语音文件不能为空");
        }
        if (file.getSize() > MAX_VOICE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "语音文件不能超过5MB");
        }
        String extension = audioExtension(file.getContentType());
        String voiceUrl;
        try {
            Path directory = Paths.get(uploadPath).toAbsolutePath().normalize().resolve("voices");
            Files.createDirectories(directory);
            String filename = IdUtil.fastSimpleUUID() + extension;
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) {
                throw new IOException("invalid upload path");
            }
            try (java.io.InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            voiceUrl = "/uploads/voices/" + filename;
        } catch (IOException e) {
            log.error("Greeting voice upload failed", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "语音保存失败");
        }

        GreetingCard greeting = getOrCreateGreeting(giftRecord);
        greeting.setVoiceUrl(voiceUrl);
        saveGreeting(giftRecord, greeting);
        return greeting;
    }

    private void insertMockLogisticsEvents(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        List<LogisticsEvent> evs = List.of(
            buildEvent(orderId, now.minusDays(2), "上海仓", "已下单", "订单已创建，等待发货"),
            buildEvent(orderId, now.minusDays(1), "上海转运中心", "已发货", "包裹已从仓库发出"),
            buildEvent(orderId, now, "运输中", "运输中", "包裹运往目的地")
        );
        evs.forEach(logisticsEventMapper::insert);
    }

    private LogisticsEvent buildEvent(Long orderId, LocalDateTime t, String loc, String st, String desc) {
        LogisticsEvent e = new LogisticsEvent();
        e.setOrderId(orderId);
        e.setEventTime(t);
        e.setLocation(loc);
        e.setStatus(st);
        e.setDescription(desc);
        e.setSource("simulation");
        return e;
    }

    private String normalizeFeedbackType(String type) {
        if (type == null || type.isBlank()) {
            return "comment";
        }
        String normalized = type.trim().toLowerCase();
        return List.of("comment", "thanks", "suggestion", "like").contains(normalized)
                ? normalized : "comment";
    }

    private FeedbackAccessToken loadFeedbackToken(String rawToken, boolean allowUsed) {
        if (rawToken == null || !rawToken.matches("[A-Za-z0-9_-]{40,64}")) {
            throw invalidFeedbackLink();
        }
        List<FeedbackAccessToken> tokens = feedbackAccessTokenMapper.selectList(
                new LambdaQueryWrapper<FeedbackAccessToken>()
                        .eq(FeedbackAccessToken::getTokenHash, hashToken(rawToken))
                        .last("LIMIT 1"));
        if (tokens.isEmpty()) {
            throw invalidFeedbackLink();
        }
        FeedbackAccessToken token = tokens.get(0);
        if (token.getExpireAt() == null || !token.getExpireAt().isAfter(LocalDateTime.now())
                || (!allowUsed && Integer.valueOf(1).equals(token.getUsed()))) {
            throw invalidFeedbackLink();
        }
        return token;
    }

    private BusinessException invalidFeedbackLink() {
        return new BusinessException(ResultCode.NOT_FOUND.getCode(), "反馈链接无效、已使用或已过期");
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String saveBytes(String directoryName, String extension, byte[] data) throws IOException {
        Path directory = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(directoryName);
        Files.createDirectories(directory);
        String filename = IdUtil.fastSimpleUUID() + extension;
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            throw new IOException("invalid upload path");
        }
        Files.write(target, data);
        return "/uploads/" + directoryName + "/" + filename;
    }

    private GreetingCard getOrCreateGreeting(GiftRecord giftRecord) {
        GreetingCard greeting = giftRecord.getGreetingCardId() == null
                ? null : greetingCardMapper.selectById(giftRecord.getGreetingCardId());
        if (greeting == null) {
            greeting = new GreetingCard();
            greeting.setStyleTemplate("classic");
        }
        return greeting;
    }

    private void saveGreeting(GiftRecord giftRecord, GreetingCard greeting) {
        if (greeting.getId() == null) {
            greetingCardMapper.insert(greeting);
            giftRecord.setGreetingCardId(greeting.getId());
            giftRecordMapper.updateById(giftRecord);
        } else {
            greetingCardMapper.updateById(greeting);
        }
    }

    private String audioExtension(String contentType) {
        if (contentType == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的语音格式");
        }
        Map<String, String> extensions = Map.of(
                "audio/webm", ".webm",
                "audio/ogg", ".ogg",
                "audio/mpeg", ".mp3",
                "audio/wav", ".wav",
                "audio/x-wav", ".wav",
                "audio/mp4", ".m4a");
        String extension = extensions.get(contentType.toLowerCase().split(";", 2)[0]);
        if (extension == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "仅支持 webm、ogg、mp3、wav、m4a 语音");
        }
        return extension;
    }

    private String normalizeGreetingStyle(String style) {
        if (style == null || style.isBlank()) {
            return "classic";
        }
        String normalized = style.trim().toLowerCase();
        return Set.of("classic", "warm", "literary", "funny").contains(normalized)
                ? normalized : "classic";
    }

    private String stripTrailingSlash(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:3000" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String generateMockGreeting(GreetingGenerateRequest request) {
        return "亲爱的" + request.getRecipientName() + "，\n\n" +
                "在这个特别的日子里，愿这份礼物为你带来温暖与惊喜。" +
                "感谢你一直以来的陪伴，祝你幸福快乐！\n\n" +
                "—— " + request.getSenderName();
    }
}
