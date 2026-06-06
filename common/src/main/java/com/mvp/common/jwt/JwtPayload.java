package com.mvp.common.jwt;

import lombok.Data;

import java.util.Map;

/**
 * common 解析后的最小 JWT payload。
 *
 * <p>JWT payload 只是 Base64Url 编码，不是加密。</p>
 */
@Data
public class JwtPayload {

    private static final String CLAIM_JTI = "jti";
    private static final String CLAIM_TYP = "typ";
    private static final String CLAIM_SUB = "sub";
    private static final String CLAIM_IAT = "iat";
    private static final String CLAIM_EXP = "exp";

    /** token 唯一 id，用于区分每一次签发。 */
    private final String jti;

    /** token 类型，默认 access 表示访问业务接口。 */
    private final String typ;

    /** subject，当前项目里存 userId。 */
    private final String sub;

    /** issued at，签发时间，秒级 Unix 时间戳。 */
    private final long iat;

    /** expiration time，过期时间，秒级 Unix 时间戳。 */
    private final long exp;

    public JwtPayload(String jti, String typ, String sub, long iat, long exp) {
        this.jti = requireText(CLAIM_JTI, jti);
        this.typ = requireText(CLAIM_TYP, typ);
        this.sub = requireText(CLAIM_SUB, sub);
        this.iat = requirePositive(CLAIM_IAT, iat);
        this.exp = requirePositive(CLAIM_EXP, exp);

        if (this.exp <= this.iat) {
            throw new IllegalArgumentException("token payload exp 必须大于 iat");
        }
    }

    /**
     * 把 Fastjson2 解析出来的 Map 转成强类型 payload。
     *
     * <p>转换时顺便完成必填字段校验。这样 JwtUtil 只需要关心签名、类型和过期时间，
     * 字段完整性由 payload 自己负责。</p>
     */
    public static JwtPayload fromClaims(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            throw new IllegalArgumentException("token payload 不能为空");
        }
        return new JwtPayload(
                requireString(claims, CLAIM_JTI),
                requireString(claims, CLAIM_TYP),
                requireString(claims, CLAIM_SUB),
                requireLong(claims, CLAIM_IAT),
                requireLong(claims, CLAIM_EXP)
        );
    }

    private static String requireString(Map<String, Object> claims, String key) {
        // jti、typ、sub 按字符串处理，避免不同模块对用户 id 类型理解不一致。
        Object value = requireClaim(claims, key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new IllegalArgumentException("token payload 字段必须是非空字符串: " + key);
    }

    private static long requireLong(Map<String, Object> claims, String key) {
        // iat、exp 正常是 JSON number；兼容字符串数字，便于排查手工构造 token 的场景。
        Object value = requireClaim(claims, key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("token payload 字段必须是数字: " + key, e);
            }
        }
        throw new IllegalArgumentException("token payload 字段必须是数字: " + key);
    }

    private static Object requireClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            throw new IllegalArgumentException("token payload 缺少字段: " + key);
        }
        return value;
    }

    private static String requireText(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("token payload 字段不能为空: " + key);
        }
        return value;
    }

    private static long requirePositive(String key, long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("token payload 字段必须大于 0: " + key);
        }
        return value;
    }
}
