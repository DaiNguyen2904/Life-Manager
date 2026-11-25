package com.example.lifemanager.utils;

import androidx.annotation.NonNull;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Băm mật khẩu bằng PBKDF2-HMAC-SHA256 với salt ngẫu nhiên 16 byte.
 * Mật khẩu không bao giờ được lưu dưới dạng gốc; khi đăng nhập, mật khẩu nhập vào được băm lại
 * với cùng salt và số vòng lặp rồi so sánh trong thời gian hằng (chống đoán theo thời gian).
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    /** Số vòng lặp mặc định, được lưu kèm từng tài khoản để có thể tăng về sau. */
    public static final int DEFAULT_ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** Tạo salt ngẫu nhiên, mã hóa Base64. */
    @NonNull
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Băm mật khẩu với salt (Base64) và số vòng lặp cho trước, kết quả mã hóa Base64. */
    @NonNull
    public static String hash(@NonNull String password, @NonNull String saltBase64, int iterations) {
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, Base64.getDecoder().decode(saltBase64), iterations,
                KEY_LENGTH_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Thiết bị không hỗ trợ " + ALGORITHM, e);
        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }

    /** Kiểm tra mật khẩu nhập vào có khớp với giá trị băm đã lưu hay không. */
    public static boolean verify(@NonNull String password, @NonNull String saltBase64, int iterations,
                                 @NonNull String expectedHashBase64) {
        byte[] actual = Base64.getDecoder().decode(hash(password, saltBase64, iterations));
        byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
        return MessageDigest.isEqual(actual, expected);
    }
}
