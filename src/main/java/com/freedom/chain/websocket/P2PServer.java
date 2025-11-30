package com.freedom.chain.websocket;

import com.freedom.chain.service.BlockService;
import com.freedom.chain.service.P2PService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * @description: p2p服务端
 * @author: freedom
 * @create: 2025-11-19
 **/
@Component
@Slf4j
public class P2PServer {
    @Resource
    private P2PService p2pService;
    @Resource
    private BlockService blockService;

    /**
     * @description: 初始化p2p服务端
     * @author: freedom
     * @date: 2025/11/22 14:43
     * @param: [port]
     * @return: void
     **/
    public void initP2PServer(int port) {
        WebSocketServer socketServer = new WebSocketServer(new InetSocketAddress(port)) {

            /**
             * 连接建立后触发
             */
            @Override
            public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
                blockService.getSockets().add(webSocket);
            }

            /**
             * 连接关闭后触发
             */
            @Override
            public void onClose(WebSocket webSocket, int i, String s, boolean b) {
                blockService.getSockets().remove(webSocket);
                log.info("connection closed to address:{}" , webSocket.getRemoteSocketAddress());
            }

            /**
             * 接收到客户端消息时触发
             */
            @Override
            public void onMessage(WebSocket webSocket, String msg) {
                //作为服务端，业务逻辑处理
                p2pService.handleMessage(webSocket, msg, blockService.getSockets());
            }

            /**
             * 发生错误时触发
             */
            @Override
            public void onError(WebSocket webSocket, Exception e) {
                blockService.getSockets().remove(webSocket);
                log.info("connection failed to address:{}" , webSocket.getRemoteSocketAddress());
            }

            @Override
            public void onStart() {

            }

        };
        socketServer.start();
        log.info("listening websocket p2p port on:{} " , port);
    }

}
