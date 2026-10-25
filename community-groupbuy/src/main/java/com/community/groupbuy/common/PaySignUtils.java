package com.community.groupbuy.common;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * 支付回调验签工具（HMAC-SHA256）
 *
 * 说明：payment 表没有签名字段，验签采用「请求参数 + 服务端密钥」计算摘要比对的方式，
 * 密钥来自配置 app.pay.secret（生产环境用环境变量注入，不写死在配置文件里）。
 */
public final class PaySignUtils {

    private static final String ALGORITHM = "HmacSHA256";

    private PaySignUtils() {
    }

    /**
     * 计算签名：把参与签名的字段用 | 拼接后做 HMAC-SHA256，输出小写十六进制。
     * 字段顺序固定为 payNo | orderNo | amount，回调方必须按同样顺序拼接。
     */
    public static String sign(String secret, String... parts) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(String.join("|", parts).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "支付签名计算失败");
        }
    }

    /** 校验签名（忽略大小写，兼容大写十六进制） */
    public static boolean verify(String secret, String expected, String... parts) {
        if (expected == null || expected.isEmpty()) {
            return false;
        }
        return sign(secret, parts).equalsIgnoreCase(expected);
    }
}
