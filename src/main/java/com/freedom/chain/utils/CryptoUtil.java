package com.freedom.chain.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * @description: 密码学工具类
 * @author: freedom
 * @create: 2025-11-19
 **/
@Slf4j
public class CryptoUtil {

    private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase();

    /**
     * SHA256散列函数
     * @param str
     * @return
     */
    public static String sha256(String str) {
        MessageDigest messageDigest;
        String encodeStr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
            encodeStr = sha256(messageDigest.digest());
        } catch (Exception e) {
            log.error("getSHA256 is error :{}" , e.getMessage());
        }
        return encodeStr;

    }

    /**
     * 获取16进制字符串 包含哈希
     * @param bytes
     * @return
     */
    public static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 直接对二进制数据哈希
            byte[] hashBytes = digest.digest(bytes);
            // 转16进制字符串（用于脚本存储）
            return HEX_FORMAT.formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不支持", e);
        }
    }

    /**
     * @description: SHA-256哈希（返回字节数组）
     * @author: freedom 
     * @date: 2025/11/21 23:53
     * @param: bytes 待哈希的字节数组（如交易哈希的字节形式）
     * @return: 32字节哈希数组（签名时使用）
     **/
    public static byte[] sha256Bytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不支持", e);
        }
    }


    /**
     * SHA-256 + RIPEMD-160哈希（区块链地址推导核心算法）
     * 步骤：先SHA-256哈希，再对结果做RIPEMD-160哈希（压缩公钥长度）
     * @param bytes 待哈希的字节数组（如公钥字节数组）
     * @return 20字节哈希数组（地址核心部分）
     */
    public static byte[] sha256Ripemd160(byte[] bytes) {
        try {
            // 第一步：SHA-256哈希
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            byte[] sha256Bytes = sha256Digest.digest(bytes);

            // 第二步：RIPEMD-160哈希（需导入BouncyCastle依赖，Java标准库无此算法）
            MessageDigest ripemd160Digest = MessageDigest.getInstance("RIPEMD160");
            return ripemd160Digest.digest(sha256Bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("哈希算法不支持（需导入BouncyCastle）", e);
        }
    }

    /**
     * @description: 字节数组 → 十六进制字符串
     * @author: freedom
     * @date: 2025/11/22 0:51
     * @param: [bytes]
     * @return: java.lang.String
     **/
    public static String bytesToHex(byte[] bytes) {
        return HEX_FORMAT.formatHex(bytes);
    }

}
