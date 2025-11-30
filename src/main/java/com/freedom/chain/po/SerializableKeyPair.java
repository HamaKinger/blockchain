package com.freedom.chain.po;

import lombok.Data;

/**
 * @description: 密钥序列化实体
 * @author: freedom
 * @create: 2025-11-29
 **/
@Data
public class SerializableKeyPair {
    // 核心数据：Base64 编码的私钥（PKCS#8 格式）
    private String privateKeyBase64;
    // 核心数据：Base64 编码的公钥（X.509 格式）
    private String publicKeyBase64;
    // 辅助元数据（可选，避免后续解析歧义）
    private String algorithm; // 算法（如 RSA/Ed25519）
    private int keySize;      // 密钥长度（如 2048/4096）
    private String curveName; // 椭圆曲线算法特有（如 secp256k1/P-256，非 EC 算法可设为 null）
}
