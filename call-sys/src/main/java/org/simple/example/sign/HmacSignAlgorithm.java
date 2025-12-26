package org.simple.example.sign;

import org.simple.example.sign.hmac.HmacSigner;
import org.simple.example.sign.hmac.SecureCompareUtil;

public class HmacSignAlgorithm implements SignAlgorithm {


    private final String secretKey;

    public HmacSignAlgorithm(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String sign(String content) {
        return HmacSigner.sign(content, secretKey);
    }

    @Override
    public boolean verify(String content, String sign) {
        return SecureCompareUtil.equals(
                sign(content),
                sign
        );
    }
}
