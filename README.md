#(用于学习) 区块链客户端 (Blockchain Client)

一个基于 Java 实现的轻量级区块链系统，采用 UTXO 模型、PoW 共识机制和 P2P 网络，支持完整的钱包、转账、挖矿等功能。

---

## 📋 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [功能模块](#功能模块)
- [API 接口](#api-接口)
- [配置说明](#配置说明)
- [数据持久化](#数据持久化)
- [开发规范](#开发规范)

---

## 项目简介

本项目是一个教学级区块链系统，实现了区块链的核心概念与机制：

- **UTXO 模型**：采用比特币同款的未花费交易输出模型管理账户余额
- **PoW 共识**：基于工作量证明的挖矿机制，通过调整难度值控制出块速度
- **P2P 网络**：基于 WebSocket 的去中心化节点通信，支持区块同步与广播
- **钱包系统**：ECDSA 非对称加密、Base58 地址编码、数字签名验证
- **Web 客户端**：提供直观的 Web UI，实时查看区块链状态、发起转账、挖矿

---

## 核心特性

### ✅ 区块链核心功能
- 🔗 **区块链管理**：创世区块创建、区块添加、链校验、链替换
- ⛏️ **PoW 挖矿**：可配置难度的工作量证明算法
- 💰 **UTXO 模型**：输入/输出模型、找零机制、手续费计算
- 🔐 **密码学**：ECDSA 签名、SHA-256 哈希、Base58 编码
- 📡 **P2P 网络**：WebSocket 节点通信、区块同步、全网广播

### 💼 钱包与交易
- 👛 **钱包地址生成**：ECDSA 密钥对生成、公钥哈希、Base58Check 编码
- 💸 **转账功能**：UTXO 选择、交易构建、签名验证、交易池管理
- 💵 **余额查询**：基于 UTXO 的精确余额计算（聪↔BTC 转换）

### 🖥️ Web 客户端
- 📊 **概览页面**：区块高度、交易总数、挖矿难度、连接节点统计
- 🔍 **区块浏览器**：完整区块信息展示、哈希查看、交易记录
- 📝 **交易记录**：所有交易历史、发送方/接收方地址、状态追踪
- ⛏️ **挖矿控制台**：一键挖矿、实时日志、挖矿状态监控
- 💰 **钱包页面**：余额显示、地址管理、转账表单
- 🌐 **P2P 网络**：节点状态、主动连接节点

---

## 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| **Java** | 17 | 核心开发语言 |
| **Spring Boot** | 3.5.7 | 应用框架 |
| **BouncyCastle** | 1.78.1 | 密码学算法库（ECDSA、RIPEMD-160） |
| **Java-WebSocket** | 1.6.0 | P2P 网络通信 |
| **Fastjson2** | 2.0.60 | JSON 序列化 |
| **Hutool** | 5.8.41 | 工具类库 |
| **Guava** | 33.4.0-jre | Google 核心库 |
| **Lombok** | 1.18.42 | 代码简化 |

### 前端技术

| 技术 | 说明 |
|------|------|
| **原生 JavaScript** | 前端逻辑 |
| **HTML5 + CSS3** | 页面结构与样式 |
| **Font Awesome** | 图标库 |
| **Fetch API** | HTTP 请求 |

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                      Web 客户端                          │
│    (HTML + CSS + JavaScript - 前端交互)                  │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTP REST API
                        ↓
┌─────────────────────────────────────────────────────────┐
│                 Spring Boot 后端                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ BlockController│ │ P2P WebSocket │ │ BlockService  │  │
│  │  (REST API)   │ │  (Client/Srv) │ │  (区块管理)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │TransactionSvc │ │  PowService   │ │  P2PService   │  │
│  │  (交易管理)   │ │  (PoW挖矿)    │ │  (消息处理)   │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│  ┌──────────────┐  ┌──────────────┐                    │
│  │  LedgerUtil   │ │  BlockCache   │                    │
│  │  (UTXO管理)   │ │  (内存缓存)   │                    │
│  └──────────────┘  └──────────────┘                    │
└───────────────────────┬─────────────────────────────────┘
                        │ 持久化
                        ↓
┌─────────────────────────────────────────────────────────┐
│                   本地文件存储                           │
│   file/block.json  │  file/utxo.json  │  file/mineInfo.json │
└─────────────────────────────────────────────────────────┘
```

### 核心模块说明

#### 1. 区块链层 (Blockchain Layer)
- **Block**：区块数据结构（索引、时间戳、交易列表、哈希、前块哈希、Nonce）
- **BlockCache**：内存缓存，启动时加载持久化数据
- **BlockService**：区块创建、添加、校验、链替换、持久化

#### 2. 共识层 (Consensus Layer)
- **PowService**：工作量证明挖矿、难度调整、Nonce 计算、UTXO 更新

#### 3. 交易层 (Transaction Layer)
- **TransactionService**：转账交易创建、UTXO 选择、签名、验证、交易池管理
- **LedgerUtil**：UTXO 存储、余额计算、快照保存/加载
- **PublicLedgerTransaction**：交易数据结构（输入、输出、签名）

#### 4. 网络层 (Network Layer)
- **P2PClient**：WebSocket 客户端，主动连接其他节点
- **P2PServer**：WebSocket 服务端，接受连接请求
- **P2PService**：消息处理、区块同步、全网广播

#### 5. 密码学层 (Cryptography Layer)
- **WalletUtil**：密钥对生成、地址编码、签名/验签
- **BouncyCastle**：ECDSA、SHA-256、RIPEMD-160 算法实现

---

## 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **IDE**: IntelliJ IDEA / Eclipse (推荐 IntelliJ IDEA)

### 安装步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd Blockchain
```

2. **配置文件**

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8091                    # HTTP 服务端口

block:
  difficulty: 4                 # 挖矿难度（前导零数量）
  p2pport: 7001                 # P2P 监听端口
  address: ws://192.168.1.9:7002  # 要连接的远程节点地址
```

3. **编译运行**

```bash
# 方式1：Maven 命令
mvn clean package
java -jar target/Blockchain-0.0.1-SNAPSHOT.jar

# 方式2：IDE 直接运行
# 运行 BlockchainApplication.java 主类
```

4. **访问 Web 客户端**

浏览器打开：`http://localhost:8091/index.html`

### 快速体验

1. **创建创世区块**：进入「区块浏览器」页面，点击「创建创世区块」
2. **查看钱包**：进入「钱包」页面，查看地址和初始余额（创世奖励）
3. **开始挖矿**：进入「挖矿」页面，点击「开始挖矿」，观察挖矿日志
4. **转账测试**：进入「钱包」页面，填写接收地址、金额、手续费，发送交易
5. **P2P 连接**：进入「P2P 网络」页面，输入其他节点地址进行连接

---

## 功能模块

### 1. 区块浏览器

- **创建创世区块**：初始化区块链，生成第一个区块，包含创世交易（50 BTC 奖励）
- **区块列表展示**：显示所有区块的索引、哈希、前块哈希、时间戳、交易数、Nonce
- **区块详情**：点击可查看完整哈希与交易列表

### 2. 挖矿

- **PoW 挖矿算法**：
  - 收集待打包交易
  - 构建新区块（包含 Coinbase 交易，奖励 50 BTC）
  - 计算满足难度要求的 Nonce（前导零数量）
  - 更新 UTXO 集合
  - 持久化到本地文件
  - 广播到 P2P 网络

- **挖矿日志**：实时显示挖矿状态、耗时、新区块信息

### 3. 钱包

#### 地址生成流程
```
ECDSA 私钥 → 公钥 → SHA-256 → RIPEMD-160 → 添加版本前缀 
→ 双 SHA-256 校验和 → Base58Check 编码 → 钱包地址
```

#### 转账流程
1. **输入参数**：发送地址、接收地址、金额（BTC）、手续费（BTC）
2. **UTXO 选择**：从发送地址选择足够的 UTXO 作为输入
3. **构建输出**：
   - 输出1：接收方地址 + 转账金额
   - 输出2：发送方地址 + 找零金额（输入总额 - 转账金额 - 手续费）
4. **签名验证**：用私钥对交易哈希签名，验证签名合法性
5. **加入交易池**：等待矿工打包确认

#### 余额计算
- 扫描 UTXO 集合，累加属于该地址的所有未花费输出
- 聪（Satoshi）单位：1 BTC = 100,000,000 聪
- 前端显示：保留 8 位小数的 BTC 金额

### 4. P2P 网络

#### 节点通信
- **WebSocket 协议**：全双工通信，支持实时消息推送
- **客户端模式**：主动连接其他节点（`P2PClient.connectToPeer`）
- **服务端模式**：监听端口，接受其他节点连接（`P2PServer`）

#### 消息类型
| 类型 | 常量 | 说明 |
|------|------|------|
| QUERY_LATEST_BLOCK | 1 | 客户端请求查询最新区块 |
| RESPONSE_LATEST_BLOCK | 2 | 服务端返回最新区块 |
| QUERY_BLOCKCHAIN | 3 | 客户端请求查询整条链 |
| RESPONSE_BLOCKCHAIN | 4 | 服务端返回整条区块链 |

#### 区块同步策略
- **接收到更高区块**：如果远端区块高度大于本地，查询整条链
- **链替换**：验证远端链合法性，用更长的链替换本地短链
- **全网广播**：挖出新区块后，广播给所有已连接节点

---

## API 接口

### 区块相关

#### 查询区块链
```http
GET /scan
```
**响应示例**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": [
    {
      "index": 0,
      "timestamp": 1732780800000,
      "transactions": [...],
      "hash": "0000abc...",
      "previousHash": "",
      "nonce": 12345
    }
  ]
}
```

#### 创建创世区块
```http
GET /create
```

#### 挖矿
```http
GET /mine
```

### 交易相关

#### 查询所有交易
```http
GET /queryAllTran
```

#### 查询最新交易
```http
GET /queryNewTran
```

#### 转账
```http
POST /transfer
Content-Type: application/json

{
  "fromAddress": "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa",
  "toAddress": "1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2",
  "amount": 1.5,
  "fee": 0.001
}
```

### 钱包相关

#### 获取钱包地址
```http
GET /getWalletAddress
```

#### 获取钱包余额
```http
GET /getWalletBalance
```
**响应示例**：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": "50.00000000"  // BTC
}
```

### P2P 网络相关

#### 主动连接节点
```http
POST /connectPeer
Content-Type: application/json

{
  "address": "ws://127.0.0.1:7002"
}
```

---

## 配置说明

### application.yml 配置项

```yaml
server:
  port: 8091                    # HTTP API 服务端口

block:
  difficulty: 4                 # 挖矿难度（推荐范围 3-6）
                                # 难度越高，挖矿耗时越长
  p2pport: 7001                 # P2P WebSocket 监听端口
                                # 多节点部署时需要不同端口
  address: ws://192.168.1.9:7002  # 启动时自动连接的远程节点地址
                                  # 如果无需连接，可留空或注释

logging:
  config: classpath:config/logback-spring.xml  # 日志配置文件路径
```

### 多节点部署示例

**节点1**（端口 8091 / 7001）
```yaml
server:
  port: 8091
block:
  difficulty: 4
  p2pport: 7001
  address: ws://127.0.0.1:7002  # 连接节点2
```

**节点2**（端口 8092 / 7002）
```yaml
server:
  port: 8092
block:
  difficulty: 4
  p2pport: 7002
  address: ws://127.0.0.1:7001  # 连接节点1
```

**启动顺序**：先启动节点1，再启动节点2，两节点会自动建立 P2P 连接

---

## 数据持久化

### 文件结构

```
file/
├── block.json       # 区块链数据（所有区块）
├── utxo.json        # UTXO 快照（未花费输出）
└── mineInfo.json    # 矿工信息（私钥、公钥、地址）
```

### 持久化策略

遵循**「先内存，后文件」**原则：

1. **区块持久化**：
   - 创建创世区块 → 内存 BlockCache → `block.json`
   - 挖矿生成新区块 → 内存 BlockCache → 追加到 `block.json`
   - 链替换 → 内存 BlockCache → 全量写入 `block.json`

2. **UTXO 持久化**：
   - 挖矿完成 → 更新内存 UTXO_STORAGE → 快照写入 `utxo.json`
   - 启动加载 → 读取 `utxo.json` → 重建内存 UTXO_STORAGE

3. **钱包持久化**：
   - 创建创世区块时生成密钥对 → 写入 `mineInfo.json`
   - 启动加载 → 读取 `mineInfo.json` → BlockCache.minerAddress

### 数据恢复

应用启动时（`BlockCache.run()`）自动执行：
```java
1. 读取 block.json → 重建 blockChain 内存缓存
2. 读取交易记录 → 填充 packedTransactions（包括创世交易）
3. 读取 utxo.json → 重建 UTXO_STORAGE
4. 读取 mineInfo.json → 加载矿工地址
```

---

## 开发规范

### 代码规范

1. **Result 类使用规范**：
   - ✅ 正确：根据 `code` 字段判断接口成功（`code == 200`）
   - ❌ 错误：依赖 `success` 字段（已弃用）

2. **数据操作规范**：
   - 创世区块、挖矿、交易等操作**必须先写入内存，再持久化到文件**
   - 确保数据一致性与程序启动时的可恢复性

3. **异常处理规范**：
   - 全局自定义异常放置在 `error` 目录
   - 使用 `Assert` 工具类进行参数校验（`Assert.notNull`、`Assert.notEmpty` 等）

4. **交易记录规范**：
   - 创世区块发放的交易**必须被系统记录**，确保所有发行行为可追溯

### 前端规范

1. **异步操作按钮状态管理**：
   ```javascript
   // ✅ 正确：在 try 块外定义按钮变量
   const button = event.target;
   button.disabled = true;
   try {
     // 异步操作
   } finally {
     button.disabled = false;  // 确保恢复状态
   }
   ```

2. **API 调用规范**：
   ```javascript
   const result = await response.json();
   // ✅ 正确：根据 code 判断
   if (result.code !== 200) {
     showError(result.msg);
     return;
   }
   ```

### Git 提交规范

```
feat: 新增功能
fix: 修复 Bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
test: 测试相关
chore: 构建/工具链相关
```

---

## 项目结构

```
Blockchain/
├── file/                          # 持久化文件目录
│   ├── block.json                 # 区块链数据
│   ├── utxo.json                  # UTXO 快照
│   └── mineInfo.json              # 矿工信息
├── src/
│   ├── main/
│   │   ├── java/com/freedom/chain/
│   │   │   ├── controller/        # REST 控制器
│   │   │   │   └── BlockController.java
│   │   │   ├── service/           # 业务逻辑层
│   │   │   │   ├── BlockService.java
│   │   │   │   ├── PowService.java
│   │   │   │   ├── P2PService.java
│   │   │   │   └── TransactionService.java
│   │   │   ├── websocket/         # WebSocket 通信
│   │   │   │   ├── P2PClient.java
│   │   │   │   └── P2PServer.java
│   │   │   ├── model/             # 数据模型
│   │   │   │   ├── block/         # 区块相关
│   │   │   │   ├── ledger/        # 账本相关
│   │   │   │   └── p2p/           # P2P 消息
│   │   │   ├── utils/             # 工具类
│   │   │   │   ├── LedgerUtil.java      # UTXO 管理
│   │   │   │   ├── WalletUtil.java      # 钱包工具
│   │   │   │   └── BlockConstant.java   # 常量定义
│   │   │   ├── dto/               # 数据传输对象
│   │   │   ├── vo/                # 视图对象
│   │   │   ├── error/             # 异常处理
│   │   │   └── enumst/            # 枚举类型
│   │   └── resources/
│   │       ├── application.yml    # 应用配置
│   │       ├── config/
│   │       │   └── logback-spring.xml  # 日志配置
│   │       └── static/            # 前端资源
│   │           ├── index.html     # 主页面
│   │           ├── css/
│   │           │   └── style.css  # 样式文件
│   │           └── js/
│   │               └── app.js     # 前端逻辑
│   └── test/                      # 测试代码
├── pom.xml                        # Maven 配置
└── README.md                      # 项目文档
```

---

## 常见问题

### Q1: 创建创世区块失败？
**A**: 确保 `file` 目录存在且有写入权限，首次运行会自动创建相关文件。

### Q2: 挖矿速度太慢？
**A**: 降低 `application.yml` 中的 `difficulty` 值（推荐 3-4），难度每增加 1，平均耗时约增加 16 倍。

### Q3: 转账提示余额不足？
**A**: 
- 确保已挖出至少一个区块（获得 Coinbase 奖励）
- 余额 = 所有区块的 Coinbase 奖励 - 已花费的交易

### Q4: P2P 连接失败？
**A**: 
- 检查目标节点 IP 和端口是否正确
- 确保目标节点已启动且 WebSocket 端口可访问
- 防火墙是否放行对应端口

### Q5: 前端页面无数据？
**A**: 
- 打开浏览器开发者工具（F12）查看 Console 和 Network 面板
- 确认后端服务已启动（`http://localhost:8091`）
- 检查 `app.js` 中的 `API_BASE` 配置是否正确

---

## 许可证

MIT License

---

## 联系方式

- **作者**: freedom
- **创建时间**: 2025-11-19

---

## 更新日志

### v0.0.1-SNAPSHOT (2025-11-28)
- ✅ 实现 UTXO 模型与 PoW 共识机制
- ✅ 完成钱包系统（密钥生成、地址编码、签名验证）
- ✅ 实现转账功能（UTXO 选择、找零、手续费）
- ✅ 实现 P2P 网络（区块同步、全网广播）
- ✅ 实现数据持久化（区块、UTXO、钱包信息）
- ✅ 完成 Web 客户端（区块浏览器、挖矿、钱包、P2P 管理）
- ✅ 新增主动连接 P2P 节点功能

---

**🎉 感谢使用本项目！欢迎 Star 和提交 Issue/PR！**
