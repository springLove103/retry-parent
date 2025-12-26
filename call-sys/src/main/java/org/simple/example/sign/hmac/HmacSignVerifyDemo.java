package org.simple.example.sign.hmac;

import org.simple.example.sign.rsa.SignContentUtil;

import java.util.HashMap;
import java.util.Map;

public class HmacSignVerifyDemo {


    // 双方共享密钥（⚠️生产必须安全存储）
    private static final String SECRET_KEY = "hmac-secret-2025";

    public static void main(String[] args) {

        // 1. 模拟回调参数
        Map<String, String> params = new HashMap<>();
        params.put("biz_order_no", "202512250001");
        params.put("pay_order_no", "P2025122500001");
        params.put("amount", "100.00");
        params.put("pay_status", "SUCCESS");
        params.put("notify_time", "2025-12-25T16:00:00");

        // 2. 平台侧签名
        String content = SignContentUtil.build(params);
        String sign = HmacSigner.sign(content, SECRET_KEY);

        params.put("sign", sign);
        params.put("sign_type", "HMAC");

        System.out.println("sign content = " + content);
        System.out.println("sign = " + sign);

        // 3. 商户侧验签
        boolean verify = HmacVerifier.verify(
                SignContentUtil.build(params),
                params.get("sign"),
                SECRET_KEY
        );

        System.out.println("verify result = " + verify);
    }
}
