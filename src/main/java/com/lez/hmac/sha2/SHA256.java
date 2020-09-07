package com.lez.hmac.sha2;


import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

/**
 * HMAC: Hash-based Message Authentication Code
 * SHA2: Secure Hash Algorithm 2
 */
public class SHA256 {
    public static void main(String[] args) {
        try {
            byte[] hmacSha256 = HMAC.calcHmacSha256("Ziu61T9xY227aazS530Pk8C5424y663r".getBytes("UTF-8"), "10003f2504e04f8911d39a0c0305e82c3301MYR12321144221Product Ahmac-sha256TRX1708901http://yoursite.com/result?referenceId=TRX1708901v1".getBytes("UTF-8"));
            System.out.println(String.format("Hex: %032x", new BigInteger(1, hmacSha256)));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}

class HMAC {
    static byte[] calcHmacSha256(byte[] secretKey, byte[] message) {
        byte[] hmacSha256 = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(secretKeySpec);
            hmacSha256 = mac.doFinal(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hmac-sha256", e);
        }
        return hmacSha256;
    }
}