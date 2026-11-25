package com.example.lifemanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.lifemanager.utils.AuthValidator;
import com.example.lifemanager.utils.PasswordHasher;

import org.junit.Test;

/**
 * Kiểm thử phần tài khoản: băm mật khẩu PBKDF2 và các quy tắc kiểm tra dữ liệu đăng ký.
 */
public class AuthTest {

    private static final int FAST_ITERATIONS = 1_000;

    @Test
    public void hasher_verifiesCorrectPasswordOnly() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("matkhau123", salt, FAST_ITERATIONS);
        assertTrue(PasswordHasher.verify("matkhau123", salt, FAST_ITERATIONS, hash));
        assertFalse(PasswordHasher.verify("matkhau124", salt, FAST_ITERATIONS, hash));
        assertFalse(PasswordHasher.verify("MATKHAU123", salt, FAST_ITERATIONS, hash));
        // Khóa 256 bit mã hóa Base64 dài 44 ký tự, không chứa mật khẩu gốc
        assertEquals(44, hash.length());
        assertFalse(hash.contains("matkhau123"));
    }

    @Test
    public void hasher_samePasswordDifferentSaltGivesDifferentHash() {
        String salt1 = PasswordHasher.newSalt();
        String salt2 = PasswordHasher.newSalt();
        assertNotEquals(salt1, salt2);
        assertEquals(PasswordHasher.hash("abc123", salt1, FAST_ITERATIONS),
                PasswordHasher.hash("abc123", salt1, FAST_ITERATIONS));
        assertNotEquals(PasswordHasher.hash("abc123", salt1, FAST_ITERATIONS),
                PasswordHasher.hash("abc123", salt2, FAST_ITERATIONS));
    }

    @Test
    public void validator_username() {
        assertEquals("nguyen.van_dai", AuthValidator.normalizeUsername("  Nguyen.Van_Dai "));
        assertEquals(AuthValidator.Error.NONE, AuthValidator.checkUsername("Dai2026"));
        assertEquals(AuthValidator.Error.USERNAME_REQUIRED, AuthValidator.checkUsername("   "));
        assertEquals(AuthValidator.Error.USERNAME_LENGTH, AuthValidator.checkUsername("abc"));
        assertEquals(AuthValidator.Error.USERNAME_LENGTH,
                AuthValidator.checkUsername("a123456789012345678901234567890"));
        assertEquals(AuthValidator.Error.USERNAME_INVALID_CHARS, AuthValidator.checkUsername("đại123"));
        assertEquals(AuthValidator.Error.USERNAME_INVALID_CHARS, AuthValidator.checkUsername("van dai"));
    }

    @Test
    public void validator_passwordAndConfirm() {
        assertEquals(AuthValidator.Error.NONE, AuthValidator.checkPassword("abc123"));
        assertEquals(AuthValidator.Error.PASSWORD_REQUIRED, AuthValidator.checkPassword(""));
        assertEquals(AuthValidator.Error.PASSWORD_LENGTH, AuthValidator.checkPassword("ab12"));
        assertEquals(AuthValidator.Error.PASSWORD_WEAK, AuthValidator.checkPassword("abcdefgh"));
        assertEquals(AuthValidator.Error.PASSWORD_WEAK, AuthValidator.checkPassword("12345678"));
        assertEquals(AuthValidator.Error.NONE, AuthValidator.checkConfirm("abc123", "abc123"));
        assertEquals(AuthValidator.Error.CONFIRM_MISMATCH, AuthValidator.checkConfirm("abc123", "abc124"));
    }

    @Test
    public void validator_displayName() {
        assertEquals(AuthValidator.Error.NONE, AuthValidator.checkDisplayName("Nguyễn Văn Đại"));
        assertEquals(AuthValidator.Error.DISPLAY_NAME_REQUIRED, AuthValidator.checkDisplayName("  "));
        assertEquals(AuthValidator.Error.DISPLAY_NAME_TOO_LONG,
                AuthValidator.checkDisplayName("Một cái tên hiển thị rất rất dài vượt quá bốn mươi ký tự"));
    }
}
