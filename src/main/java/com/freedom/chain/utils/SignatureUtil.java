package com.freedom.chain.utils;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.util.encoders.Hex;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * @description: 签名工具
 * @author: freedom
 * @create: 2025-11-21
 **/
public class SignatureUtil {


    // 加密算法配置（区块链标准）
    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;
    // 椭圆曲线名称
    public static final String CURVE_NAME = "secp256k1";
    private static final String SIGN_ALGORITHM = "SHA256withECDSA"; // 哈希+签名算法
    static {
        // 注册BouncyCastleProvider（必须初始化）
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * @description: ECDSA签名（私钥签名）
     * @author: freedom
     * @date: 2025/11/22 0:25
     * @param: [data, privateKey] 待签名数据（通常是交易哈希的字节数组） , 发起方私钥（KeyPair.getPrivate()）
     * @return: byte[] 签名字节数组（存储到交易的signature字段）
     **/
    public static byte[] ecdsaSign(byte[] data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGN_ALGORITHM, PROVIDER);
            signature.initSign(privateKey);
            signature.update(data); // 对哈希后的数据签名（避免原始数据过大）
            return signature.sign();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException("ECDSA签名失败", e);
        }
    }

    /**
     * @description: ECDSA验签（公钥验签）
     * @author: freedom
     * @date: 2025/11/22 0:26
     * @param: [data, signature, publicKeyHex]
     * @return: boolean
     **/
    public static boolean ecdsaVerify(byte[] data, byte[] signature, String publicKeyHex) {
        try {
            // 1. 将公钥十六进制字符串转为PublicKey对象
            byte[] publicKeyBytes = Hex.decode(publicKeyHex);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC", PROVIDER);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // 2. 验证签名
            Signature verifier = Signature.getInstance(SIGN_ALGORITHM, PROVIDER);
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException |
                 InvalidKeyException | SignatureException e) {
            throw new RuntimeException("ECDSA验签失败", e);
        }
    }

    /**
     * 生成ECDSA密钥对（公钥+私钥）
     * @return 密钥对（私钥用于签名，公钥用于验签/地址推导）
     */
    public static KeyPair generateKeyPair() {
        try {
            // 初始化EC密钥生成器（指定secp256k1曲线）
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", PROVIDER);
            ECGenParameterSpec ecSpec = new ECGenParameterSpec(CURVE_NAME);
            keyPairGenerator.initialize(ecSpec, new SecureRandom());
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("ECDSA密钥对生成失败", e);
        }
    }

    /**
     * 公钥转十六进制字符串（便于存储/传输）
     * @param publicKey 公钥对象
     * @return 公钥十六进制字符串
     */
    public static String publicKeyToHex(PublicKey publicKey) {
        return Hex.toHexString(publicKey.getEncoded());
    }

    /**
     * 私钥转十六进制字符串（仅用于测试/备份，实际不对外暴露）
     * @param privateKey 私钥对象
     * @return 私钥十六进制字符串
     */
    public static String privateKeyToHex(PrivateKey privateKey) {
        return Hex.toHexString(privateKey.getEncoded());
    }

    /**
     * 解析 EC 私钥（secp256k1 曲线）
     * @param ecPrivateKeyBytes PKCS#8 格式的 EC 私钥字节数组（Base64 解码后）
     * @param curveName 曲线名称（secp256k1）
     * @return 合法的 EC PrivateKey
     */
    public static PrivateKey parseECPrivateKey(byte[] ecPrivateKeyBytes, String curveName) throws Exception {
        // 1. 获取 secp256k1 曲线参数（依赖 BouncyCastle）
        ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(curveName);
        if (ecSpec == null) {
            throw new IllegalArgumentException("不支持的曲线：" + curveName + "，请确认 BouncyCastle 依赖已添加");
        }

        // 2. 两种解析方式（优先 PKCS#8 标准解析）
        try {
            // 方式 1：直接用 PKCS8EncodedKeySpec 解析（推荐，若私钥是标准 PKCS#8 格式）
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC"); // 指定 EC 算法和 BouncyCastle 提供者
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(ecPrivateKeyBytes));
        } catch (Exception e) {
            // 方式 2：手动解析 PKCS#8 中的私钥参数（兼容部分非标准格式）
            KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
            // 从 PKCS#8 字节数组中提取 EC 私钥的 scalar 值（核心参数）
            PKCS8EncodedKeySpec pkcs8Spec = new PKCS8EncodedKeySpec(ecPrivateKeyBytes);
            // 解析 PKCS#8 结构，获取私钥 scalar
            java.security.spec.ECPrivateKeySpec ecPrivateSpec = keyFactory.getKeySpec(
                    keyFactory.generatePrivate(pkcs8Spec),
                    java.security.spec.ECPrivateKeySpec.class
            );
            // 用 secp256k1 曲线参数构建私钥
            return keyFactory.generatePrivate(new ECPrivateKeySpec(
                    ecPrivateSpec.getS(), // 私钥 scalar 值
                    ecSpec                // secp256k1 曲线参数
            ));
        }
    }
}
