package org.simple.example.sign.rsa;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class KeyGenDemo {


    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();

        String publicKey = Base64.getEncoder()
                .encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder()
                .encodeToString(keyPair.getPrivate().getEncoded());

        System.out.println("====== PUBLIC KEY ======");
        System.out.println(publicKey);
        System.out.println("====== PRIVATE KEY ======");
        System.out.println(privateKey);
    }
}
