package com.mvp.common.jwt;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 生成和校验工具。
 *
 * <p>使用 JDK 自带 HMAC-SHA256 实现 HS256 签名。
 * 具体业务模块只需要配置同一份 jwt.secret 即可签发和校验 token。</p>
 *
 * <p>当前项目没有引入额外 JWT 框架，目的是把 JWT 的三段结构和验签流程写清楚：
 * header.payload.signature，其中 header 和 payload 是 Base64Url 编码，signature 是 HMAC 签名。</p>
 */
@Component
public class JwtUtil {

    /** accessToken：访问业务接口使用。 */
    public static final String TYPE_ACCESS = "access";

    /** refreshToken：换取新 accessToken 使用。 */
    public static final String TYPE_REFRESH = "refresh";

    /** JWT 当前使用的签名算法，对应 header.alg=HS256。 */
    private static final String HMAC_SHA256 = "HmacSHA256";

    /** Fastjson2 反序列化 payload JSON 时使用的 Map 类型声明。 */
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 创建默认 access 类型 token。
     */
    public String createAccessToken(String userId) {
        return createToken(userId, TYPE_ACCESS, jwtProperties.getAccessTokenSeconds());
    }

    /**
     * 按指定有效期创建 accessToken。
     *
     * <p>refreshToken 剩余时间小于默认 accessToken 有效期时使用，避免 accessToken 活得比当前会话更久。</p>
     */
    public String createAccessToken(String userId, long expiresInSeconds) {
        return createToken(userId, TYPE_ACCESS, expiresInSeconds);
    }

    /**
     * 创建 refresh 类型 token。
     */
    public String createRefreshToken(String userId) {
        return createToken(userId, TYPE_REFRESH, jwtProperties.getRefreshTokenSeconds());
    }

    public JwtPayload parseAndValidate(String token) {
        return parseAndValidate(token, TYPE_ACCESS);
    }

    /**
     * 解析并校验 JWT。
     *
     * <p>校验顺序很重要：
     * 1. 先检查 token 三段格式；
     * 2. 再验签，确认 header 和 payload 没被篡改；
     * 3. 签名通过后再解析 payload；
     * 4. 最后校验 typ 和 exp。</p>
     */
    public JwtPayload parseAndValidate(String token, String expectedType) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token 不能为空");
        }

        // JWT 必须是 header.payload.signature 三段。
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("token 格式错误");
        }

        // 签名原文只包含前两段：base64Url(header) + "." + base64Url(payload)。
        String signingInput = parts[0] + "." + parts[1];

        // 重新用服务端密钥计算签名，再和 token 第三段比较。
        // 如果 payload 被人改过，重新计算出来的签名一定对不上。
        String expectedSignature = sign(signingInput);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new IllegalArgumentException("token 签名错误");
        }

        // 签名通过后再解析 payload，读取用户 id、jti、过期时间等业务字段。
        JwtPayload payload = JwtPayload.fromClaims(readJson(base64UrlDecode(parts[1])));

        // 校验 token 的业务类型，默认要求 typ=access。
        if (!expectedType.equals(payload.getTyp())) {
            throw new IllegalArgumentException("token 类型错误");
        }

        // exp 是秒级时间戳。当前时间大于等于 exp 时，token 失效。
        if (payload.getExp() <= Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("token 已过期");
        }
        return payload;
    }

    public long getAccessTokenSeconds() {
        return jwtProperties.getAccessTokenSeconds();
    }

    public long getRefreshTokenSeconds() {
        return jwtProperties.getRefreshTokenSeconds();
    }

    private String createToken(String userId,
                               String type,
                               long expiresInSeconds) {
        long now = Instant.now().getEpochSecond();

        // header 描述 token 类型和签名算法。
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        // payload 只保留最小声明：jti、typ、sub、iat、exp。
        // 注意：JWT payload 只是 Base64Url 编码，不是加密，不能放手机号、昵称、身份证号等敏感信息。
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("typ", type);
        payload.put("sub", userId);
        payload.put("iat", now);
        payload.put("exp", now + expiresInSeconds);

        // JWT 前两段分别是 header 和 payload 的 Base64Url 编码。
        String signingInput = base64UrlEncode(writeJson(header)) + "." + base64UrlEncode(writeJson(payload));

        // 第三段是签名，用于证明前两段没有被篡改。
        return signingInput + "." + sign(signingInput);
    }

    private String sign(String signingInput) {
        try {
            // HMAC-SHA256 是对称签名：生成和校验都使用同一个 secret。
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] signature = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signature);
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    private byte[] writeJson(Map<String, Object> value) {
        try {
            // 使用 Fastjson2 输出紧凑 JSON，避免空格、换行影响 Base64Url 编码结果。
            return JSON.toJSONBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException("JWT JSON 序列化失败", e);
        }
    }

    private Map<String, Object> readJson(byte[] value) {
        try {
            // payload 解码后是 JSON 对象，这里先读成 Map，再由 JwtPayload 做强类型字段校验。
            return JSON.parseObject(value, MAP_TYPE.getType());
        } catch (Exception e) {
            throw new IllegalArgumentException("token 内容解析失败", e);
        }
    }

    private String base64UrlEncode(byte[] value) {
        // JWT 使用 Base64Url 且去掉 padding，避免普通 Base64 中的 +、/、= 影响 URL 或 Header 传输。
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64UrlDecode(String value) {
        // JWT 三段使用 URL 安全 Base64；如果传入非法字符，JDK 解码器会抛出异常并被上层转成 401。
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            // 用固定时间比较减少根据响应时间猜测签名内容的风险。
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }
}
