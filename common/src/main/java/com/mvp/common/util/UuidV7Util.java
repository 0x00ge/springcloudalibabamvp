package com.mvp.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUIDv7 工具类。
 *
 * <p>UUIDv7 的前 48 位是 Unix 毫秒时间戳，比 UUIDv4 更适合作为数据库主键：
 * 新数据大致按时间递增，B+Tree 索引分裂会少一些。</p>
 *
 * <p>这个工具类只负责生成字符串，不依赖 Spring 容器。真正接入 MyBatis-Plus 的地方是
 * {@link UuidV7IdentifierGenerator#nextUUID(Object)}。</p>
 */
public final class UuidV7Util {

    /**
     * 随机数来源。
     *
     * <p>UUIDv7 的时间戳部分保证大致有序，随机部分负责降低同一毫秒内生成多个 id 时发生冲突的概率。</p>
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 工具类不需要被实例化。
     */
    private UuidV7Util() {
    }

    /**
     * 生成 32 位无横杠 UUIDv7 字符串。
     *
     * <p>标准 UUID 文本格式是 8-4-4-4-12，例如 {@code xxxxxxxx-xxxx-7xxx-yxxx-xxxxxxxxxxxx}。
     * 当前项目落库时去掉横杠，最终格式为 32 位十六进制字符串，例如
     * {@code xxxxxxxxxxxx7xxxyxxxxxxxxxxxxxxx}。</p>
     *
     * @return 例如 018ff4f8bb8c7c9f9f226f66c6d20f8a
     */
    public static String generate() {
        /*
         * UUIDv7 布局简化理解：
         * 1. timestamp: 48 位毫秒时间戳，放在最高位，保证按时间大致递增。
         * 2. version: 固定写入 0111，也就是 UUID 版本号 7。
         * 3. randomA/randomB: 剩余随机位，保证同一毫秒内生成多个 id 时仍然足够分散。
         * 4. variant: 固定写入 RFC 4122 变体位，也就是最高两位 10。
         */
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;
        long randomA = RANDOM.nextLong() & 0xFFFL;
        long randomB = RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL;

        // mostSignificantBits 保存时间戳、版本号和前 12 位随机数。
        long mostSignificantBits = (timestamp << 16) | 0x7000L | randomA;

        // leastSignificantBits 保存 RFC 4122 variant 和剩余随机数。
        long leastSignificantBits = 0x8000000000000000L | randomB;

        return new UUID(mostSignificantBits, leastSignificantBits).toString().replace("-", "");
    }
}
