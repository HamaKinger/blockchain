package com.freedom.chain.utils;

/**
 * @description: 常量工具类
 * @author: freedom
 * @create: 2025-11-19
 **/
public class BlockConstant {
    // 查询最新的区块
    public final static int QUERY_LATEST_BLOCK = 1;

    // 返回最新的区块
    public final static int RESPONSE_LATEST_BLOCK = 2;

    // 查询整个区块链
    public final static int QUERY_BLOCKCHAIN = 3;

    // 返回整个区块链
    public final static int RESPONSE_BLOCKCHAIN = 4;

    // 最大区块大小（字节）
    public static final long MAX_BLOCK_SIZE = 1_000_000;

    // 难度调整窗口（区块数量）
    public static final int DIFFICULTY_ADJUST_WINDOW = 10;
    // 期望出块时间（毫秒）- 10分钟
    public static final long EXPECTED_BLOCK_TIME_MS = 600_000;

}
