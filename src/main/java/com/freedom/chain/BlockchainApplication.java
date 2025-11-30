package com.freedom.chain;

import com.freedom.chain.service.BlockService;
import com.freedom.chain.websocket.P2PClient;
import com.freedom.chain.websocket.P2PServer;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlockchainApplication implements ApplicationRunner {
    @Resource
    private P2PServer p2PServer;

    @Resource
    private P2PClient p2PClient;

    @Resource
    private BlockService blockService;

    public static void main(String[] args) {
        SpringApplication.run(BlockchainApplication.class, args);
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        p2PServer.initP2PServer(blockService.getP2pport());
        p2PClient.connectToPeer(blockService.getAddress());
        System.out.println("*****难度系数******"+blockService.getDifficulty());
        System.out.println("*****端口号******"+blockService.getP2pport());
        System.out.println("*****节点地址******"+blockService.getAddress());
    }
}
