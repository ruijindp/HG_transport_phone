package com.hgkefang.transport.util;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Create by admin on 2018/7/3
 * AES加密
 */
public class AESUtil {
    public static String encode(String stringToEncode, String IV, String key) throws NullPointerException {
        try {
            SecretKeySpec secretKeySpec = getKey(key);
            byte[] clearText = stringToEncode.getBytes("UTF-8");
            Arrays.fill(new byte[16], (byte) 0x00);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(createIV(IV));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            String value = Base64.encodeToString(cipher.doFinal(clearText), Base64.NO_WRAP);
            return new String(Base64.encode(value.getBytes("utf-8"), Base64.NO_WRAP), "utf-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static byte[] createIV(String password) {
        byte[] data = null;
        if (password == null) {
            password = "";
        }
        StringBuilder sb = new StringBuilder(16);
        sb.append(password);
        while (sb.length() < 16) {
            sb.append("0");
        }
        if (sb.length() > 16) {
            sb.setLength(16);
        }
        try {
            data = sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static SecretKeySpec getKey(String password) throws UnsupportedEncodingException {
        int keyLength = 256;
        byte[] keyBytes = new byte[keyLength / 8];
        Arrays.fill(keyBytes, (byte) 0x0);
        byte[] passwordBytes = password.getBytes("UTF-8");
        int length = passwordBytes.length < keyBytes.length ? passwordBytes.length : keyBytes.length;
        System.arraycopy(passwordBytes, 0, keyBytes, 0, length);
        return  new SecretKeySpec(keyBytes, "AES");
    }
}
