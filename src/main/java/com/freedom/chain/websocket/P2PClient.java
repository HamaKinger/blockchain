package com.freedom.chain.websocket;

import com.freedom.chain.service.BlockService;
import com.freedom.chain.service.P2PService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @description: p2p客户端
 * @author: freedom
 * @create: 2025-11-19
 **/
@Component
@Slf4j
public class P2PClient {
    @Resource
    P2PService p2pService;
    @Resource
    private BlockService blockService;

    /**
     * @description: 连接节点
     * @author: freedom
     * @date: 2025/11/22 14:43
     * @param: [addr]
     * @return: void
     **/
    public void connectToPeer(String addr) {
        try {
            final WebSocketClient socketClient = new WebSocketClient(new URI(addr)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    //客户端发送请求，查询最新区块
                    p2pService.write(this, p2pService.queryLatestBlockMsg());
                    blockService.getSockets().add(this);
                }

                /**
                 * 接收到消息时触发
                 * @param msg
                 */
                @Override
                public void onMessage(String msg) {
                    p2pService.handleMessage(this, msg, blockService.getSockets());
                }

                @Override
                public void onClose(int i, String msg, boolean b) {
                    blockService.getSockets().remove(this);
                    log.info("connection closed");
                }

                @Override
                public void onError(Exception e) {
                    blockService.getSockets().remove(this);
                    log.info("connection failed");
                }
            };
            socketClient.connect();
        } catch (URISyntaxException e) {
            log.error("p2p connect is error:{}" , e.getMessage());
        }
    }
}
