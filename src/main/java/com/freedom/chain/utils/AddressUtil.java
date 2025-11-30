package com.freedom.chain.utils;

import cn.hutool.core.codec.Base58;

import java.security.PublicKey;
import java.util.Arrays;

/**
 * @description: 块链地址工具类：公钥→地址（Base58Check编码）、地址→公钥哈希
 * 地址生成流程（区块链标准）：公钥 → SHA-256 → RIPEMD-160 → 加版本号 → 两次SHA-256取前4字节（校验位） → Base58编码
 * @author: freedom
 * @date: 2025/11/22 0:32
 **/
public class AddressUtil {
    // 地址版本号（不同区块链不同，如比特币主网0x00，测试网0x6F）
    private static final byte VERSION = 0x00;
    // 校验位长度（4字节，Base58Check核心，防地址输入错误）
    private static final int CHECKSUM_LENGTH = 4;

    /**
     * 公钥→区块链地址（Base58Check编码）
     * @param publicKey 公钥对象（从密钥对获取）
     * @return 区块链地址（如比特币地址：1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa）
     */
    public static String publicKeyToAddress(PublicKey publicKey) {
        // 1. 公钥字节数组（X.509编码）
        byte[] publicKeyBytes = publicKey.getEncoded();

        // 2. SHA-256 + RIPEMD-160 哈希（压缩公钥）
        byte[] pubKeyHash = CryptoUtil.sha256Ripemd160(publicKeyBytes);

        // 3. 拼接版本号（区分主网/测试网）
        byte[] versionedPubKeyHash = new byte[pubKeyHash.length + 1];
        versionedPubKeyHash[0] = VERSION;
        System.arraycopy(pubKeyHash, 0, versionedPubKeyHash, 1, pubKeyHash.length);

        // 4. 生成校验位（两次SHA-256取前4字节）
        byte[] checksum = generateChecksum(versionedPubKeyHash);

        // 5. 拼接校验位（versionedPubKeyHash + checksum）
        byte[] addressBytes = new byte[versionedPubKeyHash.length + CHECKSUM_LENGTH];
        System.arraycopy(versionedPubKeyHash, 0, addressBytes, 0, versionedPubKeyHash.length);
        System.arraycopy(checksum, 0, addressBytes, versionedPubKeyHash.length, CHECKSUM_LENGTH);

        // 6. Base58编码（最终地址，防输入错误）
        return Base58.encode(addressBytes);
    }

    /**
     * 地址→公钥哈希（反向解析，用于验签时获取公钥哈希）
     * @param address 区块链地址（Base58编码）
     * @return 20字节公钥哈希（SHA-256+RIPEMD-160结果）
     * @throws IllegalArgumentException 地址非法（校验位错误）
     */
    public static byte[] addressToPubKeyHash(String address) {
        // 1. Base58解码
        byte[] addressBytes = Base58.decode(address);

        // 2. 验证长度（版本号1字节 + 公钥哈希20字节 + 校验位4字节 = 25字节）
        if (addressBytes.length != 1 + 20 + CHECKSUM_LENGTH) {
            throw new IllegalArgumentException("非法地址：长度错误");
        }

        // 3. 拆分版本号、公钥哈希、校验位
        byte[] versionedPubKeyHash = Arrays.copyOfRange(addressBytes, 0, addressBytes.length - CHECKSUM_LENGTH);
        byte[] checksum = Arrays.copyOfRange(addressBytes, addressBytes.length - CHECKSUM_LENGTH, addressBytes.length);

        // 4. 验证校验位（防地址篡改/输入错误）
        byte[] expectedChecksum = generateChecksum(versionedPubKeyHash);
        if (!Arrays.equals(checksum, expectedChecksum)) {
            throw new IllegalArgumentException("非法地址：校验位错误");
        }

        // 5. 返回公钥哈希（去掉版本号）
        return Arrays.copyOfRange(versionedPubKeyHash, 1, versionedPubKeyHash.length);
    }


    /**
     * 生成校验位（Base58Check核心）
     * 逻辑：对输入字节数组做两次SHA-256，取前4字节作为校验位
     */
    private static byte[] generateChecksum(byte[] data) {
        byte[] firstHash = CryptoUtil.sha256Bytes(data);
        byte[] secondHash = CryptoUtil.sha256Bytes(firstHash);
        return Arrays.copyOfRange(secondHash, 0, CHECKSUM_LENGTH);
    }

    /**
     * 验证地址合法性（校验位+长度）
     * @param address 区块链地址
     * @return true=合法，false=非法
     */
    public static boolean isValidAddress(String address) {
        try {
            addressToPubKeyHash(address);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
