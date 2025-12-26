package org.simple.example.sign.rsa;

import java.util.HashMap;
import java.util.Map;

public class SignVerifyDemo {



    // ← 用 KeyGenDemo 生成的
    private static final String PUBLIC_KEY = "你的publicKey";
    private static final String PRIVATE_KEY = "你的privateKey";

    public static void main(String[] args) {

        // 1. 模拟支付回调参数
        Map<String, String> params = new HashMap<>();
        params.put("biz_order_no", "202512250001");
        params.put("pay_order_no", "P2025122500001");
        params.put("amount", "100.00");
        params.put("pay_status", "SUCCESS");
        params.put("notify_time", "2025-12-25T15:30:00");

        // 2. 平台侧：签名
        String content = SignContentUtil.build(params);
        System.out.println("sign content = " + content);

        String sign = RsaSigner.sign(content, PRIVATE_KEY);
        params.put("sign", sign);
        params.put("sign_type", "RSA2");

        System.out.println("sign = " + sign);

        // 3. 商户侧：验签
        boolean verify = RsaVerifier.verify(
                SignContentUtil.build(params),
                params.get("sign"),
                PUBLIC_KEY
        );

        System.out.println("verify result = " + verify);
    }
}
