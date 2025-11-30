// API基础路径
const API_BASE = 'http://localhost:8090';

// 全局状态
let currentPage = 'overview';
let refreshInterval = null;

// 初始化应用
document.addEventListener('DOMContentLoaded', function() {
    initNavigation();
    loadOverviewData();
    startAutoRefresh();
});

// 导航功能
function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    
    navItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();
            
            // 更新导航状态
            navItems.forEach(nav => nav.classList.remove('active'));
            this.classList.add('active');
            
            // 切换页面
            const pageName = this.getAttribute('data-page');
            switchPage(pageName);
        });
    });
}

// 切换页面
function switchPage(pageName) {
    currentPage = pageName;
    
    // 隐藏所有页面
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });
    
    // 显示目标页面
    const targetPage = document.getElementById(`${pageName}-page`);
    if (targetPage) {
        targetPage.classList.add('active');
    }
    
    // 更新页面标题
    const titles = {
        'overview': '概览',
        'blocks': '区块浏览器',
        'transactions': '交易记录',
        'mining': '挖矿',
        'wallet': '钱包',
        'network': 'P2P网络'
    };
    document.getElementById('page-title').textContent = titles[pageName] || '概览';
    
    // 加载对应页面数据
    loadPageData(pageName);
}

// 加载页面数据
function loadPageData(pageName) {
    switch(pageName) {
        case 'overview':
            loadOverviewData();
            break;
        case 'blocks':
            loadBlocksData();
            break;
        case 'transactions':
            loadTransactionsData();
            break;
        case 'wallet':
            loadWalletData();
            break;
        case 'network':
            loadNetworkData();
            break;
    }
}

// 加载概览数据
async function loadOverviewData() {
    try {
        const response = await fetch(`${API_BASE}/scan`);
        const result = await response.json();
        
        // 检查返回结果 (code=200表示成功)
        if (result.code !== 200) {
            showError(result.msg || '获取数据失败');
            return;
        }
        
        const blockchain = result.data || [];
        
        // 更新统计数据
        updateStats(blockchain);
        
        // 更新最新区块列表
        updateLatestBlocks(blockchain);
        
        // 加载交易数据
        loadLatestTransactions();
        
    } catch (error) {
        console.error('加载概览数据失败:', error);
        showError('无法连接到区块链节点');
    }
}

// 更新统计数据
function updateStats(blockchain) {
    const blockHeight = blockchain.length;
    let totalTxs = 0;
    
    blockchain.forEach(block => {
        if (block.transactions) {
            totalTxs += block.transactions.length;
        }
    });
    
    document.getElementById('block-height').textContent = blockHeight;
    document.getElementById('tx-count').textContent = totalTxs;
    
    // 如果有区块，获取最新区块的难度
    if (blockchain.length > 0) {
        const latestBlock = blockchain[blockchain.length - 1];
        // 难度可以通过hash前缀0的数量判断
        const difficulty = countLeadingZeros(latestBlock.hash);
        document.getElementById('difficulty').textContent = difficulty;
        document.getElementById('current-difficulty').textContent = difficulty;
    }
}

// 计算hash前导0的数量
function countLeadingZeros(hash) {
    if (!hash) return 0;
    let count = 0;
    for (let char of hash) {
        if (char === '0') count++;
        else break;
    }
    return count;
}

// 更新最新区块列表
function updateLatestBlocks(blockchain) {
    const container = document.getElementById('latest-blocks-list');
    
    if (blockchain.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="fas fa-cube"></i><p>暂无区块数据</p></div>';
        return;
    }
    
    // 显示最新的5个区块
    const latestBlocks = blockchain.slice(-5).reverse();
    
    container.innerHTML = latestBlocks.map(block => `
        <div class="block-item">
            <div class="block-header">
                <span class="block-index"># ${block.index}</span>
                <span class="block-time">${formatTime(block.timestamp)}</span>
            </div>
            <div class="block-hash">${block.hash}</div>
            <div class="block-info">
                <div class="info-item">
                    <span class="label">交易:</span>
                    <span>${block.transactions ? block.transactions.length : 0}</span>
                </div>
                <div class="info-item">
                    <span class="label">Nonce:</span>
                    <span>${block.nonce}</span>
                </div>
            </div>
        </div>
    `).join('');
}

// 加载最新交易
async function loadLatestTransactions() {
    try {
        const response = await fetch(`${API_BASE}/queryNewTran`);
        const result = await response.json();
        
        // 检查返回结果 (code=200表示成功)
        if (result.code !== 200) {
            console.error('获取交易数据失败:', result.msg);
            return;
        }
        
        const transactions = result.data || [];
        
        const container = document.getElementById('latest-transactions-list');
        
        if (transactions.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-exchange-alt"></i><p>暂无交易数据</p></div>';
            return;
        }
        
        // 显示最新的5笔交易
        const latestTxs = transactions.slice(-5).reverse();
        
        container.innerHTML = latestTxs.map(tx => `
            <div class="transaction-item">
                <div class="transaction-header">
                    <span style="color: var(--primary-color);">
                        <i class="fas fa-exchange-alt"></i>
                    </span>
                    <span class="transaction-time">${formatTime(tx.timestamp)}</span>
                </div>
                <div class="transaction-hash">${tx.txHash}</div>
                <div class="block-info">
                    <div class="info-item">
                        <span class="label">发送:</span>
                        <span style="font-size: 12px;">${shortenAddress(tx.fromAddress)}</span>
                    </div>
                    <div class="info-item">
                        <span class="label">接收:</span>
                        <span style="font-size: 12px;">${shortenAddress(tx.toAddress)}</span>
                    </div>
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('加载交易数据失败:', error);
    }
}

// 加载区块数据
async function loadBlocksData() {
    try {
        const response = await fetch(`${API_BASE}/scan`);
        const result = await response.json();
        
        // 检查返回结果 (code=200表示成功)
        if (result.code !== 200) {
            showError(result.msg || '获取区块数据失败');
            return;
        }
        
        const blockchain = result.data || [];
        
        const container = document.getElementById('blocks-table');
        
        if (blockchain.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-cube"></i><p>暂无区块数据，请先创建创世区块</p></div>';
            return;
        }
        
        // 反转数组，最新的在前
        const reversedBlocks = [...blockchain].reverse();
        
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>高度</th>
                        <th>区块哈希</th>
                        <th>前块哈希</th>
                        <th>时间戳</th>
                        <th>交易数</th>
                        <th>Nonce</th>
                    </tr>
                </thead>
                <tbody>
                    ${reversedBlocks.map(block => `
                        <tr>
                            <td><strong>${block.index}</strong></td>
                            <td class="hash-cell" title="${block.hash}">${block.hash}</td>
                            <td class="hash-cell" title="${block.previousHash || '创世区块'}">${block.previousHash || '创世区块'}</td>
                            <td>${formatDateTime(block.timestamp)}</td>
                            <td>${block.transactions ? block.transactions.length : 0}</td>
                            <td>${block.nonce}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        
    } catch (error) {
        console.error('加载区块数据失败:', error);
        showError('加载区块数据失败');
    }
}

// 加载交易数据
async function loadTransactionsData() {
    try {
        const response = await fetch(`${API_BASE}/queryAllTran`);
        const result = await response.json();
        
        // 检查返回结果 (code=200表示成功)
        if (result.code !== 200) {
            showError(result.msg || '获取交易数据失败');
            return;
        }
        
        const transactions = result.data || [];
        
        const container = document.getElementById('transactions-table');
        
        if (transactions.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-exchange-alt"></i><p>暂无交易数据</p></div>';
            return;
        }
        
        // 反转数组，最新的在前
        const reversedTxs = [...transactions].reverse();
        
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>交易哈希</th>
                        <th>发送方</th>
                        <th>接收方</th>
                        <th>时间戳</th>
                        <th>状态</th>
                    </tr>
                </thead>
                <tbody>
                    ${reversedTxs.map(tx => `
                        <tr>
                            <td class="hash-cell" title="${tx.txHash}">${tx.txHash}</td>
                            <td class="hash-cell" title="${tx.fromAddress}">${shortenAddress(tx.fromAddress)}</td>
                            <td class="hash-cell" title="${tx.toAddress}">${shortenAddress(tx.toAddress)}</td>
                            <td>${formatDateTime(tx.timestamp)}</td>
                            <td><span style="color: var(--success-color);">${tx.status || 'CONFIRMED'}</span></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        
    } catch (error) {
        console.error('加载交易数据失败:', error);
        showError('加载交易数据失败');
    }
}

// 创建创世区块
async function createGenesisBlock() {
    const button = event.target;
    button.disabled = true;
    button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 创建中...';
    
    try {
        const response = await fetch(`${API_BASE}/create`);
        const result = await response.json();
        
        // 检查返回结果 (code=200表示成功)
        if (result.code !== 200) {
            showError(result.msg || '创建创世区块失败');
            return;
        }
        
        showSuccess('创世区块创建成功！');
        
        // 刷新区块数据
        setTimeout(() => {
            loadBlocksData();
            loadOverviewData();
        }, 1000);
        
    } catch (error) {
        console.error('创建创世区块失败:', error);
        showError('创建创世区块失败：' + error.message);
    } finally {
        button.disabled = false;
        button.innerHTML = '<i class="fas fa-plus"></i> 创建创世区块';
    }
}

// 开始挖矿
async function startMining() {
    try {
        const button = document.getElementById('start-mining');
        const statusSpan = document.getElementById('mining-status');
        
        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 挖矿中...';
        statusSpan.textContent = '挖矿中...';
        statusSpan.className = 'status-mining';
        
        addMiningLog('开始挖矿...');
        
        const startTime = Date.now();
        const response = await fetch(`${API_BASE}/mine`);
        const result = await response.json();
        const elapsed = ((Date.now() - startTime) / 1000).toFixed(2);
        
        // 检查返回结果
        if (!result.code) {
            addMiningLog('挖矿失败: ' + (result.message || '未知错误'));
            showError(result.message || '挖矿失败');
            return;
        }
        
        const newBlock = result.data;
        
        addMiningLog(`挖矿成功！耗时: ${elapsed}秒`);
        addMiningLog(`新区块高度: ${newBlock ? newBlock.index : 'N/A'}`);
        addMiningLog(`区块哈希: ${newBlock ? newBlock.hash : 'N/A'}`);
        
        showSuccess(`挖矿成功！新区块已添加到链上`);
        
        // 刷新数据
        loadOverviewData();
        
    } catch (error) {
        console.error('挖矿失败:', error);
        addMiningLog('挖矿失败: ' + error.message);
        showError('挖矿失败：' + error.message);
    } finally {
        const button = document.getElementById('start-mining');
        const statusSpan = document.getElementById('mining-status');
        
        button.disabled = false;
        button.innerHTML = '<i class="fas fa-play"></i> 开始挖矿';
        statusSpan.textContent = '空闲';
        statusSpan.className = 'status-idle';
    }
}

// 添加挖矿日志
function addMiningLog(message) {
    const logContainer = document.getElementById('mining-log-content');
    const timestamp = new Date().toLocaleTimeString();
    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry';
    logEntry.textContent = `[${timestamp}] ${message}`;
    logContainer.appendChild(logEntry);
    logContainer.scrollTop = logContainer.scrollHeight;
}

// 加载钱包数据
async function loadWalletData() {
    try {
        const addressElement = document.getElementById('address-text');
        
        // 从后端获取钱包地址
        const addressResponse = await fetch(`${API_BASE}/getWalletAddress`);
        const addressResult = await addressResponse.json();
        
        // 检查返回结果 (code=200表示成功)
        if (addressResult.code !== 200 || !addressResult.data) {
            console.error('获取钱包地址失败:', addressResult.msg);
            addressElement.textContent = '未生成';
            return;
        }
        
        const minerAddress = addressResult.data;
        addressElement.textContent = minerAddress;
        
        // 直接从后端获取余额（BTC）
        const balanceResponse = await fetch(`${API_BASE}/getWalletBalance`);
        const balanceResult = await balanceResponse.json();
        
        if (balanceResult.code !== 200 || balanceResult.data == null) {
            showError(balanceResult.msg || '获取钱包余额失败');
            return;
        }
        
        document.getElementById('wallet-balance').textContent = `${balanceResult.data} BTC`;
        
    } catch (error) {
        console.error('加载钱包数据失败:', error);
        document.getElementById('address-text').textContent = '加载失败';
    }
}

// 复制地址
function copyAddress() {
    const addressText = document.getElementById('address-text').textContent;
    
    if (addressText === '未生成') {
        showError('钱包地址尚未生成');
        return;
    }
    
    navigator.clipboard.writeText(addressText).then(() => {
        showSuccess('地址已复制到剪贴板');
    }).catch(err => {
        console.error('复制失败:', err);
        showError('复制失败');
    });
}

// 提交转账
async function submitTransfer() {
    const recipientAddress = document.getElementById('recipient-address').value.trim();
    const amount = document.getElementById('transfer-amount').value;
    const fee = document.getElementById('transfer-fee').value;
    
    // 表单验证
    if (!recipientAddress) {
        showError('请输入接收地址');
        return;
    }
    
    if (!amount || parseFloat(amount) <= 0) {
        showError('请输入有效的转账金额');
        return;
    }
    
    if (!fee || parseFloat(fee) < 0) {
        showError('请输入有效的手续费');
        return;
    }
    
    // 获取当前钱包地址
    const fromAddress = document.getElementById('address-text').textContent;
    if (fromAddress === '未生成' || fromAddress === '加载失败') {
        showError('钱包地址不可用，请先创建创世区块');
        return;
    }
    
    const button = event.target;
    button.disabled = true;
    button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 发送中...';
    
    try {
        const response = await fetch(`${API_BASE}/transfer`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                fromAddress: fromAddress,
                toAddress: recipientAddress,
                amount: parseFloat(amount),
                fee: parseFloat(fee)
            })
        });
        
        const result = await response.json();
        
        // 检查返回结果 (code=200表示成功)
        if (result.code !== 200) {
            showError(result.msg || '转账失败');
            return;
        }
        
        showSuccess('交易发送成功！请等待矿工打包确认');
        
        // 清空表单
        document.getElementById('recipient-address').value = '';
        document.getElementById('transfer-amount').value = '';
        document.getElementById('transfer-fee').value = '0.001';
        
        // 刷新余额
        setTimeout(() => {
            loadWalletData();
        }, 1000);
        
    } catch (error) {
        console.error('转账失败:', error);
        showError('转账失败：' + error.message);
    } finally {
        button.disabled = false;
        button.innerHTML = '<i class="fas fa-paper-plane"></i> 发送交易';
    }
}

// 加载网络数据
function loadNetworkData() {
    // 显示配置信息
    document.getElementById('local-port').textContent = '7001';
    document.getElementById('peer-address').textContent = 'ws://192.168.1.9:7002';
    
    // 模拟连接节点（实际应该从后端获取）
    const peersContainer = document.getElementById('peers-list');
    peersContainer.innerHTML = `
        <div class="empty-state">
            <i class="fas fa-network-wired"></i>
            <p>当前无连接节点</p>
            <p style="font-size: 12px; margin-top: 8px;">节点会自动发现并连接到网络中的其他节点</p>
        </div>
    `;
}

// 主动连接节点
async function connectPeer() {
    const input = document.getElementById('peer-input');
    const addr = input ? input.value.trim() : '';

    if (!addr) {
        showError('请输入节点地址');
        return;
    }
    if (!/^wss?:\/\//.test(addr)) {
        showError('地址必须以 ws:// 或 wss:// 开头');
        return;
    }

    const button = event.target;
    if (button && button.tagName === 'BUTTON') {
        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 连接中...';
    }

    try {
        const resp = await fetch(`${API_BASE}/connectPeer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ address: addr })
        });
        const result = await resp.json();

        if (result.code !== 200) {
            showError(result.msg || '连接节点失败');
            return;
        }

        showSuccess('连接请求已发送');
        if (input) input.value = '';
        // 可选：刷新网络数据显示
        // loadNetworkData();
    } catch (e) {
        console.error('连接节点失败:', e);
        showError('连接节点失败：' + e.message);
    } finally {
        if (button && button.tagName === 'BUTTON') {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-plug"></i> 连接节点';
        }
    }
}

// 刷新数据
function refreshData() {
    const button = event.target.closest('.btn-refresh');
    button.style.transform = 'rotate(360deg)';
    
    loadPageData(currentPage);
    
    setTimeout(() => {
        button.style.transform = '';
    }, 500);
}

// 自动刷新
function startAutoRefresh() {
    // 每10秒自动刷新一次
    refreshInterval = setInterval(() => {
        if (currentPage === 'overview') {
            loadOverviewData();
        }
    }, 10000);
}

// 工具函数：格式化时间
function formatTime(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = Math.floor((now - date) / 1000); // 秒
    
    if (diff < 60) return `${diff}秒前`;
    if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`;
    return `${Math.floor(diff / 86400)}天前`;
}

// 工具函数：格式化完整时间
function formatDateTime(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

// 工具函数：缩短地址
function shortenAddress(address) {
    if (!address) return 'N/A';
    if (address === 'COINBASE') return 'COINBASE';
    if (address.length <= 20) return address;
    return `${address.substring(0, 10)}...${address.substring(address.length - 8)}`;
}

// 显示成功消息
function showSuccess(message) {
    showNotification(message, 'success');
}

// 显示错误消息
function showError(message) {
    showNotification(message, 'error');
}

// 显示通知
function showNotification(message, type) {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'}"></i>
        <span>${message}</span>
    `;
    
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 16px 24px;
        background: ${type === 'success' ? '#2ecc71' : '#e74c3c'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        gap: 12px;
        z-index: 9999;
        animation: slideIn 0.3s ease;
        font-size: 14px;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}

// 添加动画样式
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);
