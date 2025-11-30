package com.freedom.chain.service;

import com.alibaba.fastjson2.JSON;
import com.freedom.chain.model.block.Block;
import com.freedom.chain.model.p2p.Message;
import com.freedom.chain.utils.BlockConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;

/**
 * @description: p2p网络服务类
 * @author: freedom
 * @create: 2025-11-19
 **/
@Service
@Slf4j
public class P2PService {

    @Resource
    private BlockService blockService;

    /**
     * 客户端和服务端共用的消息处理方法
     * @param webSocket
     * @param msg
     * @param sockets
     */
    public void handleMessage(WebSocket webSocket, String msg, List<WebSocket> sockets) {
        try {
            Message message = JSON.parseObject(msg, Message.class);
            log.info("接收到IP地址为：{},端口号为：{}的p2p消息：{}",webSocket.getRemoteSocketAddress().getAddress().toString(),
                    webSocket.getRemoteSocketAddress().getPort(),JSON.toJSONString(message));
            switch (message.getType()) {
                //客户端请求查询最新的区块:1
                case BlockConstant.QUERY_LATEST_BLOCK:
                    write(webSocket, responseLatestBlockMsg());//服务端调用方法返回最新区块:2
                    break;
                //接收到服务端返回的最新区块:2
                case BlockConstant.RESPONSE_LATEST_BLOCK:
                    handleBlockResponse(message.getData(), sockets);
                    break;
                //客户端请求查询整个区块链:3
                case BlockConstant.QUERY_BLOCKCHAIN:
                    write(webSocket, responseBlockChainMsg());//服务端调用方法返回最新区块:4
                    break;
                //直接接收到其他节点发送的整条区块链信息:4
                case BlockConstant.RESPONSE_BLOCKCHAIN:
                    handleBlockChainResponse(message.getData(), sockets);
                    break;
            }
        } catch (Exception e) {
            log.error("处理IP地址为：{}，端口号为：{}的p2p消息错误:{}",webSocket.getRemoteSocketAddress().getAddress().toString(),
                    webSocket.getRemoteSocketAddress().getPort() ,e.getMessage());
        }
    }

    /**
     * 处理其它节点发送过来的区块信息
     * @param blockData
     * @param sockets
     */
    public synchronized void handleBlockResponse(String blockData, List<WebSocket> sockets) {
        //反序列化得到其它节点的最新区块信息
        Block latestBlockReceived = JSON.parseObject(blockData, Block.class);
        //当前节点的最新区块
        Block latestBlock = blockService.getLatestBlock();

        if (latestBlockReceived != null) {
            if(latestBlock == null) {
                // 当本地没有区块时，直接尝试添加接收到的区块（可能是创世区块）
                if (blockService.addBlock(latestBlockReceived)) {
                    broatcast(responseLatestBlockMsg());
                    log.info("将接收到的创世区块加入到本地的区块链");
                }
                // 也可以同时查询完整链以确保同步
                broatcast(queryBlockChainMsg());
                log.info("重新查询所有节点上的整条区块链");
            } else {
                //如果接收到的区块高度比本地区块高度大的多
                if(latestBlockReceived.getIndex() > latestBlock.getIndex() + 1) {
                    broatcast(queryBlockChainMsg());
                    System.out.println("重新查询所有节点上的整条区块链");
                } else if (latestBlockReceived.getIndex() > latestBlock.getIndex() &&
                        latestBlock.getHash().equals(latestBlockReceived.getPreviousHash())) {
                    if (blockService.addBlock(latestBlockReceived)) {
                        broatcast(responseLatestBlockMsg());
                    }
                    log.info("将新接收到的区块加入到本地的区块链");
                }
            }
        }
    }


    /**
     * 处理其它节点发送过来的区块链信息
     * @param blockData
     * @param sockets
     */
    public synchronized void handleBlockChainResponse(String blockData, List<WebSocket> sockets) {
        //反序列化得到其它节点的整条区块链信息
        List<Block> receiveBlockchain = JSON.parseArray(blockData, Block.class);
        if(!CollectionUtils.isEmpty(receiveBlockchain) && blockService.isValidChain(receiveBlockchain)) {
            //根据区块索引先对区块进行排序
            receiveBlockchain.sort(Comparator.comparingInt(Block::getIndex));

            //其它节点的最新区块
            Block latestBlockReceived = receiveBlockchain.get(receiveBlockchain.size() - 1);
            //当前节点的最新区块
            Block latestBlock = blockService.getLatestBlock();

            if(latestBlock == null) {
                //替换本地的区块链
                blockService.replaceChain(receiveBlockchain);
            }else {
                //其它节点区块链如果比当前节点的长，则处理当前节点的区块链
                if (latestBlockReceived.getIndex() > latestBlock.getIndex()) {
                    if (latestBlock.getHash().equals(latestBlockReceived.getPreviousHash())) {
                        if (blockService.addBlock(latestBlockReceived)) {
                            broatcast(responseLatestBlockMsg());
                        }
                        log.info("将新接收到的区块加入到本地的区块链");
                    } else {
                        // 用长链替换本地的短链
                        blockService.replaceChain(receiveBlockchain);
                    }
                }
            }
        }
    }

    /**
     * 全网广播消息
     * @param message
     */
    public void broatcast(String message) {
        List<WebSocket> socketsList = blockService.getSockets();
        if (CollectionUtils.isEmpty(socketsList)) {
            return;
        }
        log.info("======全网广播消息开始：");
        for (WebSocket socket : socketsList) {
            this.write(socket, message);
        }
        log.info("======全网广播消息结束");
    }

    /**
     * 向其它节点发送消息
     * @param ws
     * @param message
     */
    public void write(WebSocket ws, String message) {
        log.info("发送给IP地址为：{},端口号为：{}的p2p消息:{}",ws.getRemoteSocketAddress().getAddress().toString(),
                ws.getRemoteSocketAddress().getPort(),message);
        ws.send(message);
    }

    /**
     * 查询整条区块链
     * @return
     */
    public String queryBlockChainMsg() {
        return JSON.toJSONString(new Message(BlockConstant.QUERY_BLOCKCHAIN));
    }

    /**
     * 返回整条区块链数据
     * @return
     */
    public String responseBlockChainMsg() {
        Message msg = new Message();
        msg.setType(BlockConstant.RESPONSE_BLOCKCHAIN);
        msg.setData(JSON.toJSONString(blockService.getBlockChain()));
        return JSON.toJSONString(msg);
    }

    /**
     * 查询最新的区块
     * @return
     */
    public String queryLatestBlockMsg() {
        return JSON.toJSONString(new Message(BlockConstant.QUERY_LATEST_BLOCK));
    }

    /**
     * 返回最新的区块
     * @return
     */
    public String responseLatestBlockMsg() {
        Message msg = new Message();
        msg.setType(BlockConstant.RESPONSE_LATEST_BLOCK);
        Block b = blockService.getLatestBlock();
        msg.setData(JSON.toJSONString(b));
        return JSON.toJSONString(msg);
    }

}
