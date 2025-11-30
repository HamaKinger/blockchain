package com.freedom.chain.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @description: 文件工具
 * @author: freedom
 * @create: 2025-11-29
 **/
@Slf4j
public class FileUtil {

    /**
     * @description: 判断文件内容是否为空
     * @author: freedom
     * @date: 2025/11/29 0:29
     * @param: [filePath]
     * @return: boolean
     **/
    public static boolean isEmpty(String filePath) {
        String mineInfo = null;
        try {
            mineInfo = Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            log.error("文件读取异常", e);
        }
        return StrUtil.isEmpty(mineInfo);
    }
}
