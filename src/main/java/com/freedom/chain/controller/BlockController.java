package com.freedom.chain.controller;

import com.freedom.chain.dto.TransferRequest;
import com.freedom.chain.enumst.ResultCodeEnum;
import com.freedom.chain.error.Assert;
import com.freedom.chain.model.block.Block;
import com.freedom.chain.model.block.BlockCache;
import com.freedom.chain.model.ledger.PublicLedgerTransaction;
import com.freedom.chain.service.BlockService;
import com.freedom.chain.service.PowService;
import com.freedom.chain.service.TransactionService;
import com.freedom.chain.utils.LedgerUtil;
import com.freedom.chain.vo.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

/**
 * @description:
 * @author: freedom
 * @create: 2025-11-19
 **/
@RestController
public class BlockController {

    @Resource
    BlockService blockService;

    @Resource
    PowService powService;

    @Resource
    BlockCache blockCache;
    
    @Resource
    TransactionService transactionService;

    @Resource
    com.freedom.chain.websocket.P2PClient p2PClient;

    /**
     * 查看当前节点区数据
     * @return
     */
    @GetMapping("/scan")
    public Result<List<Block>> scanBlock() {
        return Result.success(blockCache.getBlockChain());
    }

    /**
     * 查看当前节点区块链数据
     * @return
     */
    @GetMapping("/data")
    public Result<List<PublicLedgerTransaction>> scanData() {
        return Result.success(blockCache.getPackedTransactions());
    }

    /**
     * 创建创世区块
     * @return
     */
    @GetMapping("/create")
    public Result<String> createFirstBlock() {
        blockService.createGenesisBlock();
        return Result.success();
    }

    /**
     * 工作量证明PoW
     * 挖矿生成新的区块
     */
    @GetMapping("/mine")
    public Result<Block> createNewBlock() {
        Block newBlock = powService.mine();
        Assert.notNull(newBlock, "挖矿失败，未生成新区块");
        return Result.success(newBlock);
    }

    /**
     * 获取钱包地址
     * @return
     */
    @GetMapping("/getWalletAddress")
    public Result<String> getWalletAddress() {
        String minerAddress = blockCache.getMinerAddress();
        Assert.notEmpty(minerAddress, "钱包地址尚未生成，请先创建创世区块");
        return Result.success(ResultCodeEnum.SUCCESS.getMsg(), minerAddress);
    }
    
    /**
     * 获取钱包余额（BTC）
     * @return
     */
    @GetMapping("/getWalletBalance")
    public Result<String> getWalletBalance() {
        String minerAddress = blockCache.getMinerAddress();
        Assert.notEmpty(minerAddress, "钱包地址尚未生成，请先创建创世区块");
        BigInteger satoshis = LedgerUtil.getAddressBalance(minerAddress);
        BigDecimal btc = new BigDecimal(satoshis).divide(new BigDecimal("100000000"), 8, RoundingMode.DOWN);
        return Result.success(ResultCodeEnum.SUCCESS.getMsg(), btc.toPlainString());
    }
    
    /**
     * 转账
     * @param request 转账请求
     * @return
     */
    @PostMapping("/transfer")
    public Result<String> transfer(@RequestBody TransferRequest request) {
        // 参数校验
        Assert.notEmpty(request.getFromAddress(), "发送地址不能为空");
        Assert.notEmpty(request.getToAddress(), "接收地址不能为空");
        Assert.notNull(request.getAmount(), "转账金额不能为空");
        Assert.greaterThanZero(request.getAmount(), "转账金额必须大于0");
        Assert.notNull(request.getFee(), "手续费不能为空");
        Assert.notNegative(request.getFee(), "手续费不能为负数");
        
        // 创建并提交交易
        String txHash = transactionService.createTransfer(request);
        
        return Result.success("交易创建成功，请等待矿工确认", txHash);
    }

    /**
     * 主动连接到指定的P2P节点
     */
    @PostMapping("/connectPeer")
    public Result<String> connectPeer(@RequestBody java.util.Map<String, String> req) {
        String addr = req.get("address");
        Assert.notEmpty(addr, "节点地址不能为空");
        p2PClient.connectToPeer(addr);
        return Result.success("连接请求已发送", addr);
    }

    /**
     * 查看最新交易数据 (最新节点)
     * @return
     */
    @GetMapping("/queryNewTran")
    public Result<List<PublicLedgerTransaction>> queryNewTran() {
        Block latestBlock = blockCache.getLatestBlock();
        return Result.success(latestBlock.getTransactions());
    }

    /**
     * 查询所有交易记录
     * @return
     */
    @GetMapping("/queryAllTran")
    public Result<List<PublicLedgerTransaction>> queryAllTran() {
        List<Block> blockChain = blockCache.getBlockChain();
        List<PublicLedgerTransaction> allTransactions = blockChain.stream()
                .map(Block::getTransactions)
                .flatMap(List::stream)
                .toList();
        return Result.success(allTransactions);
    }
}
