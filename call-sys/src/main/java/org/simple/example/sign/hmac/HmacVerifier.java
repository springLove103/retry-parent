package org.simple.example.sign.hmac;

import java.util.Objects;

public class HmacVerifier {

    public static boolean verify(
            String content,
            String sign,
            String secretKey
    ) {
        if (Objects.isNull(sign)) {
            return false;
        }
        String expectedSign = HmacSigner.sign(content, secretKey);
        return expectedSign.equals(sign);
        //return SecureCompareUtil.equals(expectedSign, sign);
    }
}
