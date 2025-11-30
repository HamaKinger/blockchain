package com.freedom.chain.model.block;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.freedom.chain.model.ledger.PublicLedgerTransaction;
import com.freedom.chain.utils.LedgerUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @description:
 * @author: freedom
 * @create: 2025-11-19
 **/
@Setter
@Getter
@ConfigurationProperties(prefix = "block")
@Component
@Slf4j
public class BlockCache implements CommandLineRunner {

    /**
     * 当前节点的区块链结构
     */
    private List<Block> blockChain = new CopyOnWriteArrayList<>();

    /**
     * 已打包保存的业务数据集合
     */
    private List<PublicLedgerTransaction> packedTransactions = new CopyOnWriteArrayList<>();

    /**
     * 当前节点的socket对象
     */
    private List<WebSocket> socketsList = new CopyOnWriteArrayList<>();
    /**
     * 挖矿地址
     */
    private String minerAddress;
    /**
     * 挖矿的难度系数
     */
    @Value("${block.difficulty}")
    private int difficulty;

    /**
     * 当前节点p2pserver端口号
     */
    @Value("${block.p2pport}")
    private int p2pport;

    /**
     * 要连接的节点地址
     */
    @Value("${block.address}")
    private String address;

    /**
     * 获取最新的区块，即当前链上最后一个区块
     *
     * @return
     */
    public Block getLatestBlock() {
        return !blockChain.isEmpty() ? blockChain.get(blockChain.size() - 1) : null;
    }

    @Override
    public void run(String... args) throws Exception {
        String blocks = Files.readString(Paths.get("file/block.json"));
        JSONArray blockDatas = JSON.parseArray(blocks);
        if(CollUtil.isNotEmpty(blockDatas)){
            log.info("init local block file... ");
            //缓存到内存
            for (int i = 0; i < blockDatas.size(); i++) {
                JSONObject jsonObject = blockDatas.getJSONObject(i);
                jsonObject.keySet().forEach(key -> {
                    Block block = JSON.to( Block.class,jsonObject.getJSONObject(key));
                    this.blockChain.add(block);
                    // 同步记录交易到内存的已打包集合
                    if (block.getTransactions() != null) {
                        this.packedTransactions.addAll(block.getTransactions());
                    }
                });
            }
        }
        String mineInfo = Files.readString(Paths.get("file/mineInfo.json"));
        JSONObject mineInfoDatas = JSON.parseObject(mineInfo);
        if(CollUtil.isNotEmpty(mineInfoDatas)){
            log.info("init local mineInfo file... ");
            this.minerAddress = mineInfoDatas.keySet().iterator().next();
        }
        
        // 加载UTXO快照（如不存在则等待后续通过区块回放重建）
        LedgerUtil.loadUtxoSnapshot();
    }
}
