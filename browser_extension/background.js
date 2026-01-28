// 全局变量
let ws = null;
let isConnected = false;
let clientId = null; // 客户端唯一ID，通过浏览器指纹生成
let jwtToken = null;
let username = null;
let cachedScripts = {}; // 存储缓存的脚本
let domainScriptMap = {}; // 域名-脚本映射表
let activeTabUrl = null;
let clientIdleStatus = false; // 客户端空闲状态
let idleSince = null; // 开始空闲的时间
let heartbeatInterval = null; // 心跳定时器

// 页面脚本缓存
const pageScriptCache = new Map();

// 生成浏览器指纹的函数 (使用FingerprintJS)
async function generateBrowserFingerprint() {
  try {
    // 创建一个临时的script标签来加载FingerprintJS库
    // 由于Chrome扩展的安全策略，我们需要使用不同的方法
    // 这里我们创建一个更精确的浏览器指纹
    
    // 获取各种浏览器特征
    const components = [];
    
    // 获取运行时ID
    components.push(chrome.runtime.id || Math.random().toString(36).substring(2, 15));
    
    // 获取用户代理
    components.push(navigator.userAgent);
    
    // 获取语言
    components.push(navigator.language);
    
    // 获取平台
    components.push(navigator.platform);
    
    // 获取屏幕分辨率
    components.push(`${screen.width}x${screen.height}x${screen.colorDepth}`);
    
    // 获取可用屏幕尺寸
    components.push(`${screen.availWidth}x${screen.availHeight}`);
    
    // 获取时区
    components.push(Intl.DateTimeFormat().resolvedOptions().timeZone);
    
    // 获取时间偏移
    components.push(new Date().getTimezoneOffset());
    
    // 获取Session Storage支持
    try {
      components.push('sessionStorage' in window ? '1' : '0');
    } catch (e) {
      components.push('0');
    }
    
    // 获取Local Storage支持
    try {
      components.push('localStorage' in window ? '1' : '0');
    } catch (e) {
      components.push('0');
    }
    
    // 获取IndexedDB支持
    try {
      components.push('indexedDB' in window ? '1' : '0');
    } catch (e) {
      components.push('0');
    }
    
    // 获取Web SQL支持
    try {
      components.push('openDatabase' in window ? '1' : '0');
    } catch (e) {
      components.push('0');
    }
    
    // 获取GPU信息
    try {
      const canvas = document.createElement('canvas');
      const gl = canvas.getContext('webgl');
      if (gl) {
        components.push(gl.getParameter(gl.RENDERER) || '');
        components.push(gl.getParameter(gl.VENDOR) || '');
      }
    } catch (e) {
      components.push('');
    }
    
    // 生成哈希
    const combinedString = components.join('||');
    let hash = 0;
    for (let i = 0; i < combinedString.length; i++) {
      const char = combinedString.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // 转换为32位整数
    }
    
    return `client_fp_${Math.abs(hash).toString(36)}`;
  } catch (e) {
    console.error('生成浏览器指纹时出错:', e);
    // 回退到原始方法
    const runtimeId = chrome.runtime.id || Math.random().toString(36).substring(2, 15);
    const randomPart = Math.random().toString(36).substring(2, 10);
    
    // 创建一个组合字符串
    const fingerprintBase = `${runtimeId}_${randomPart}`;
    
    // 生成哈希值
    let hash = 0;
    for (let i = 0; i < fingerprintBase.length; i++) {
      const char = fingerprintBase.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // 转换为32位整数
    }
    
    return `client_${Math.abs(hash).toString(36)}`;
  }
}

// 初始化客户端ID
async function initializeClientId() {
  const result = await chrome.storage.local.get(['clientId']);
  if (result.clientId) {
    clientId = result.clientId;
  } else {
    clientId = await generateBrowserFingerprint();
    await chrome.storage.local.set({ clientId });
  }
  console.log('客户端ID:', clientId);
}

// 初始化
chrome.runtime.onInstalled.addListener(() => {
  console.log('爬虫助手扩展已安装');
});

// 在扩展加载时初始化客户端ID
initializeClientId();

// 监听来自popup的消息
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'login_success') {
    jwtToken = message.token;
    username = message.username;
    // 使用从login.js传递过来的客户端ID，确保与登录API使用的ID一致
    if (message.clientId) {
      clientId = message.clientId;
      // 保存客户端ID到本地存储
      chrome.storage.local.set({ clientId });
      console.log('登录时使用传递的客户端ID:', clientId);
      connectWebSocket();
      // 发送响应
      sendResponse({ success: true });
    } else {
      // 如果没有传递clientId，则生成新的客户端ID（向后兼容）
      generateBrowserFingerprint().then(newClientId => {
        clientId = newClientId;
        // 保存新的客户端ID到本地存储
        chrome.storage.local.set({ clientId });
        console.log('登录时生成新的客户端ID:', clientId);
        connectWebSocket();
        // 在客户端ID生成完成后发送响应
        sendResponse({ success: true });
      }).catch(error => {
        console.error('生成客户端ID时出错:', error);
        sendResponse({ success: false, error: error.message });
      });
    }
    return true; // 异步响应
  } else if (message.type === 'logout') {
    // 处理退出登录，关闭WebSocket连接
    disconnectWebSocket();
    sendResponse({ success: true });
    return true; // 异步响应
  } else if (message.type === 'execute_script') {
    // 执行特定脚本
    executeScriptOnActiveTab(message.scriptId, message.scriptContent);
    sendResponse({ success: true });
  } else if (message.type === 'get_status') {
    sendResponse({ 
      isConnected, 
      clientId, 
      activeTabUrl,
      username, // 添加用户名信息
      cachedScripts: Object.keys(cachedScripts),
      online: isConnected,
      idleStatus: clientIdleStatus,
      idleSince: idleSince
    });
  } else if (message.type === 'get_client_id') {
    // 返回客户端唯一标识
    sendResponse({ 
      clientId: clientId,
      success: true 
    });
  } else if (message.type === 'get_scheduled_tasks') {
    // 获取定时任务列表（现在由服务端管理，客户端不需要本地存储）
    const scheduledTasks = []; // 客户端不再管理定时任务
    sendResponse({ tasks: scheduledTasks });
  } else if (message.type === 'set_idle_threshold') {
    // 设置空闲阈值并通知content script
    chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
      if (tabs[0]) {
        chrome.tabs.sendMessage(tabs[0].id, {
          type: 'set_idle_threshold',
          threshold: message.threshold
        });
      }
    });
    sendResponse({ success: true });
  } else if (message.type === 'check_idle_status') {
    // 检查空闲状态
    chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
      if (tabs[0]) {
        chrome.tabs.sendMessage(tabs[0].id, {
          type: 'check_idle_status'
        }, function(response) {
          if (chrome.runtime.lastError) {
            // 如果content script未加载，返回本地状态
            sendResponse({ 
              isIdle: clientIdleStatus, 
              idleDuration: idleSince ? Date.now() - idleSince : 0,
              lastActivityTime: idleSince
            });
          } else {
            sendResponse(response);
          }
        });
      } else {
        sendResponse({ 
          isIdle: clientIdleStatus, 
          idleDuration: idleSince ? Date.now() - idleSince : 0,
          lastActivityTime: idleSince
        });
      }
    });
    return true; // 异步响应
  }
  
  return true; // 保持消息通道开放
});

// 监听来自content script的消息
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'crawl_result_from_content') {
    // 处理来自content script的爬取结果
    handleCrawlResult(message.taskId, message.result, message.status);
    sendResponse({ success: true });
  } else if (message.type === 'user_idle') {
    // 用户进入空闲状态
    clientIdleStatus = true;
    idleSince = message.timestamp;
    console.log('客户端进入空闲状态');
    
    // 通知后端客户端空闲状态
    notifyIdleStatusToBackend(true);
    
    sendResponse({ success: true });
    return true; // 异步响应
  } else if (message.type === 'user_active') {
    // 用户变为活跃状态
    clientIdleStatus = false;
    console.log('客户端变为活跃状态');
    
    // 通知后端客户端活跃状态
    notifyIdleStatusToBackend(false);
    
    sendResponse({ success: true });
    return true; // 异步响应
  } else if (message.type === 'execute_idle_tasks') {
    // 执行空闲任务
    executeIdleTasks(message.idleSince, message.currentTime);
    sendResponse({ success: true });
    return true; // 异步响应
  }
  
  return true;
});

// 监听标签页更新事件
chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.status === 'complete' && tab.url) {
    activeTabUrl = tab.url;
    // 同步当前网址到服务端
    if (isConnected && clientId) {
      sendUrlChange(tab.url);
    }
    
    // 检查是否匹配脚本域名
    checkDomainScriptMatch(tab.url);
  }
});

// 监听标签页激活事件
chrome.tabs.onActivated.addListener(async (activeInfo) => {
  const tab = await chrome.tabs.get(activeInfo.tabId);
  if (tab.url) {
    activeTabUrl = tab.url;
    // 同步当前网址到服务端
    if (isConnected && clientId) {
      sendUrlChange(tab.url);
    }
    
    // 检查是否匹配脚本域名
    checkDomainScriptMatch(tab.url);
  }
});

// 断开WebSocket连接
function disconnectWebSocket() {
  if (ws) {
    // 关闭WebSocket连接
    ws.close();
    ws = null;
  }
  
  // 重置连接状态
  isConnected = false;
  jwtToken = null;
  username = null;
  // 注意：不将clientId设为null，而是保留它以便下次连接时使用
  
  // 更新图标状态
  updateIcon('disconnected');
  
  // 停止心跳
  stopHeartbeat();
  
  console.log('WebSocket连接已关闭');
}

// 连接WebSocket
function connectWebSocket() {
  // 确保在连接前清理任何现有连接
  if (ws) {
    try {
      ws.close();
    } catch (e) {
      console.warn('关闭现有WebSocket连接时出错:', e);
    }
    ws = null;
  }
  
  if (!jwtToken) {
    console.log('缺少JWT令牌，无法建立WebSocket连接');
    return;
  }
  
  try {
    const wsUrl = `ws://localhost:8090/smarteCrawler/ws?token=${jwtToken}`;
    console.log('尝试连接WebSocket:', wsUrl);
    ws = new WebSocket(wsUrl);
    
    ws.onopen = function(event) {
      console.log('WebSocket连接已建立');
      isConnected = true;
      updateIcon('connected');
      
      // 连接成功后开始发送心跳
      startHeartbeat();
    };
    
    ws.onmessage = function(event) {
      try {
        const message = JSON.parse(event.data);
        handleMessage(message);
      } catch (error) {
        console.error('解析消息失败:', error);
      }
    };
    
    ws.onclose = function(event) {
      console.log('WebSocket连接已关闭', event);
      isConnected = false;
      updateIcon('disconnected');
      // 停止心跳
      stopHeartbeat();
      // 尝试重连
      setTimeout(connectWebSocket, 5000);
    };
    
    ws.onerror = function(error) {
      console.error('WebSocket错误:', error);
      isConnected = false;
      updateIcon('disconnected');
    };
  } catch (error) {
    console.error('WebSocket连接错误:', error);
    isConnected = false;
    updateIcon('disconnected');
  }
}

// 开始心跳
function startHeartbeat() {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
  }
  
  // 每30秒发送一次心跳
  heartbeatInterval = setInterval(() => {
    sendHeartbeat();
  }, 30000);
}

// 停止心跳
function stopHeartbeat() {
  if (heartbeatInterval) {
    clearInterval(heartbeatInterval);
    heartbeatInterval = null;
  }
}

// 发送心跳
function sendHeartbeat() {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.error('WebSocket未连接，无法发送心跳');
    stopHeartbeat();
    return;
  }
  
  // 确保clientId存在
  if (!clientId) {
    console.error('客户端ID未初始化，无法发送心跳');
    stopHeartbeat();
    return;
  }
  
  const heartbeatMessage = {
    type: 'heartbeat',
    payload: {
      timestamp: Date.now()
    },
    clientId: clientId,
    timestamp: Date.now()
  };
  
  try {
    ws.send(JSON.stringify(heartbeatMessage));
    console.log('发送心跳:', heartbeatMessage);
  } catch (error) {
    console.error('发送心跳失败:', error);
  }
}

// 处理WebSocket消息
function handleMessage(message) {
  console.log('收到消息:', message);
  
  switch (message.type) {
    case 'auth_success':
      // 不更新clientId，保持与登录API使用的ID一致
      // 服务器分配的clientId可能与本地不同，但我们使用本地生成的ID
      // 如果服务器在认证成功时提供了用户名信息，也要更新
      if (message.payload.username) {
        username = message.payload.username;
      }
      // 保存本地生成的clientId到本地存储
      chrome.storage.local.set({ 
        clientId: clientId,
        username: username // 同时保存用户名到本地存储
      });
      console.log('认证成功，客户端ID:', clientId);
      // 发送注册消息
      sendRegisterMessage();
      break;
      
    case 'script_push':
    case 'script_designated_push':
      // 处理脚本推送
      handleScriptPush(message.payload.scripts);
      break;
      
    // case 'task_command':
    case 'execute_script':
      // 处理任务指令
      handleTaskCommand(message.payload);
      break;
      
    case 'scheduled_task_config':
      // 服务端不再推送定时任务配置，因为定时任务完全在服务端管理
      console.log('客户端不再处理定时任务配置，所有定时任务在服务端管理');
      break;
      
    case 'idle_control_command':
      // 处理空闲控制命令（例如：设置空闲阈值、强制执行空闲任务等）
      handleIdleControlCommand(message.payload);
      break;
      
    case 'register_success':
      // 处理注册成功消息，确保用户名被正确设置
      console.log('客户端注册成功');
      break;
    case 'ping': // 服务端ping消息，需要回应pong
      // 发送pong回应
      sendPong(message.payload.requestId);
      break;
      
    default:
      console.log('未知消息类型:', message.type);
      break;
  }
}

// 发送Pong回应
function sendPong(requestId) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.error('WebSocket未连接，无法发送Pong回应');
    return;
  }
  
  // 确保clientId存在
  if (!clientId) {
    console.error('客户端ID未初始化，无法发送Pong回应');
    return;
  }
  
  const pongMessage = {
    type: 'pong',
    payload: {
      requestId: requestId,
      timestamp: Date.now()
    },
    clientId: clientId,
    timestamp: Date.now()
  };
  
  try {
    ws.send(JSON.stringify(pongMessage));
  } catch (error) {
    console.error('发送Pong回应失败:', error);
  }
}

// 处理空闲控制命令
function handleIdleControlCommand(payload) {
  console.log('收到空闲控制命令:', payload);
  
  switch (payload.command) {
    case 'set_idle_threshold':
      // 设置空闲阈值
      try {
        chrome.runtime.sendMessage({
          type: 'set_idle_threshold',
          threshold: payload.threshold || 300000 // 默认5分钟
        });
      } catch (error) {
        console.error('发送空闲阈值设置命令失败:', error);
      }
      break;
      
    case 'check_idle_status':
      // 检查空闲状态
      try {
        chrome.runtime.sendMessage({
          type: 'check_idle_status'
        });
      } catch (error) {
        console.error('发送检查空闲状态命令失败:', error);
      }
      break;
      
    case 'force_idle_execution':
      // 强制执行空闲任务
      executeIdleTasks(Date.now() - (payload.duration || 300000), Date.now());
      break;
      
    default:
      console.log('未知空闲控制命令:', payload.command);
      break;
  }
}

// 发送注册消息
function sendRegisterMessage() {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.error('WebSocket未连接，无法发送注册消息');
    return;
  }
  
  // 获取当前活动标签页的URL
  chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
    let currentUrl = tabs[0] ? tabs[0].url : '';
    
    // 如果没有活动标签页，尝试获取任意标签页的URL
    if (!currentUrl) {
      chrome.tabs.query({}, function(allTabs) {
        if (allTabs && allTabs.length > 0) {
          currentUrl = allTabs[0].url || '';
        }
        
        activeTabUrl = currentUrl;
        
        const registerMessage = {
          type: 'register',
          payload: {
            username: username,
            currentUrl: currentUrl,
            supportTaskTypes: 'product_crawl,article_crawl,idle_task', // 添加空闲任务类型
            idleStatus: clientIdleStatus // 发送当前空闲状态
          },
          clientId: clientId,
          timestamp: Date.now()
        };
        
        ws.send(JSON.stringify(registerMessage));
      });
    } else {
      activeTabUrl = currentUrl;
      
      const registerMessage = {
        type: 'register',
        payload: {
          username: username,
          currentUrl: currentUrl,
          supportTaskTypes: 'product_crawl,article_crawl,idle_task', // 添加空闲任务类型
          idleStatus: clientIdleStatus // 发送当前空闲状态
        },
        clientId: clientId,
        timestamp: Date.now()
      };
      
      ws.send(JSON.stringify(registerMessage));
    }
  });
}

// 处理脚本推送
function handleScriptPush(scripts) {
  console.log('收到脚本推送:', scripts);
  
  // 清空当前缓存
  cachedScripts = {};
  domainScriptMap = {};
  
  // 更新脚本缓存
  scripts.forEach(script => {
    cachedScripts[script.scriptId] = script;
    
    // 构建域名-脚本映射表
    if (script.domainPattern) {
      const domains = script.domainPattern.split('|'); // 支持多个域名
      domains.forEach(domain => {
        if (!domainScriptMap[domain]) {
          domainScriptMap[domain] = [];
        }
        domainScriptMap[domain].push(script);
      });
    }
  });
  
  console.log('脚本缓存已更新，当前缓存脚本数:', Object.keys(cachedScripts).length);
}

// 处理任务命令
async function handleTaskCommand(payload) {
  const { taskId, scriptId, executeOnIdle } = payload;
  console.log(`收到任务命令，任务ID: ${taskId}, 脚本ID: ${scriptId}, 空闲执行: ${executeOnIdle}`);
  
  // 如果任务配置为仅在空闲时执行，而当前不是空闲状态，则可能需要等待
  if (executeOnIdle && !clientIdleStatus) {
    console.log('任务配置为仅在空闲时执行，当前非空闲状态，可能需要等待...');
    // 注意：现在定时任务完全由服务端管理，客户端只需执行服务端发送的任务
  }
  
  const script = cachedScripts[scriptId];
  if (!script) {
    console.error(`未找到脚本ID为 ${scriptId} 的脚本`);
    // 发送错误结果
    sendCrawlResult(taskId, { error: `未找到脚本ID为 ${scriptId} 的脚本` }, 'fail');
    return;
  }
  
  // 在当前活动标签页执行脚本
  // 如果没有活动标签页，尝试获取所有标签页中的任意一个
  executeScriptOnActiveTab(scriptId, script.scriptContent, taskId);
}

// 在当前活动标签页执行脚本
async function executeScriptOnActiveTab(scriptId, scriptContent, taskId = null) {
  try {
    // 首先尝试获取活动标签页
    let tabs = await chrome.tabs.query({active: true, currentWindow: true});
    
    // 如果没有活动标签页，尝试获取任意一个标签页
    if (!tabs || tabs.length === 0) {
      console.log('没有找到活动的标签页，尝试获取任意标签页');
      tabs = await chrome.tabs.query({currentWindow: true});
    }
    
    // 如果当前窗口没有任何标签页，尝试获取任意窗口的任意标签页
    if (!tabs || tabs.length === 0) {
      console.log('当前窗口没有标签页，尝试获取任意窗口的标签页');
      tabs = await chrome.tabs.query({});
    }
    
    if (!tabs || tabs.length === 0) {
      console.error('没有找到任何标签页');
      // 发送错误结果到服务端
      if (taskId) {
        sendCrawlResult(taskId, { error: 'No tabs available to execute script' }, 'fail');
      }
      return;
    }
    
    const tab = tabs[0];
    
    // 调试：检查content script是否已注入
    console.log('准备向标签页发送消息:', {
      tabId: tab.id,
      url: tab.url,
      title: tab.title,
      status: tab.status
    });
    
    // 检查是否是允许发送消息的页面
    if (tab.url.startsWith('chrome://') || tab.url.startsWith('about:') || tab.url.startsWith('data:')) {
      console.error('无法向内部页面发送消息:', tab.url);
      if (taskId) {
        sendCrawlResult(taskId, { error: 'Cannot execute script on internal pages' }, 'fail');
      }
      return;
    }
    
    // 发送消息到content script执行脚本
    console.log('准备向标签页发送消息:', { tabId: tab.id, url: tab.url, title: tab.title });
    
    // 先检查content script是否已加载
    try {
      const result = await chrome.tabs.executeScript(tab.id, {
        code: 'typeof window.crawlerAssistantLoaded !== "undefined" && window.crawlerAssistantLoaded'
      });
      
      if (!result || result.length === 0 || !result[0]) {
        console.warn('content script未加载到标签页:', tab.id);
        // 尝试注入一次
        await chrome.tabs.executeScript(tab.id, {
          file: 'content.js'
        });
        
        // 再次检查
        const retryResult = await chrome.tabs.executeScript(tab.id, {
          code: 'typeof window.crawlerAssistantLoaded !== "undefined" && window.crawlerAssistantLoaded'
        });
        
        if (!retryResult || retryResult.length === 0 || !retryResult[0]) {
          console.error('content script注入失败，无法执行脚本');
          if (taskId) {
            sendCrawlResult(taskId, { error: 'Content script not loaded in target tab' }, 'fail');
          }
          return;
        }
      }
    } catch (checkError) {
      console.warn('检查content script状态失败:', checkError.message);
      // 继续尝试发送消息
    }
    
    try {
      chrome.tabs.sendMessage(tab.id, {
        type: 'execute_crawl_script',
        scriptId: scriptId,
        scriptContent: scriptContent,
        taskId: taskId
      }, function(response) {
        // 检查是否发送失败
        if (chrome.runtime.lastError) {
          console.error('发送消息失败:', chrome.runtime.lastError.message);
          if (taskId) {
            sendCrawlResult(taskId, { error: 'Failed to send message to tab: ' + chrome.runtime.lastError.message }, 'fail');
          }
        }
      });
    } catch (sendMessageError) {
      console.error('发送消息异常:', sendMessageError);
      if (taskId) {
        sendCrawlResult(taskId, { error: sendMessageError.message }, 'fail');
      }
    }
  } catch (error) {
    console.error('执行脚本时出错:', error);
    
    // 发送错误结果到服务端
    if (taskId) {
      sendCrawlResult(taskId, { error: error.message }, 'fail');
    }
  }
}

// 处理爬取结果
function handleCrawlResult(taskId, result, status) {
  console.log('处理爬取结果:', { taskId, result, status });
  
  // 发送爬取结果到服务端
  sendCrawlResult(taskId, result, status);
}

// 发送爬取结果到服务端
function sendCrawlResult(taskId, crawlData, crawlStatus, errorMessage = null) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.error('WebSocket未连接，无法发送爬取结果');
    return;
  }
  
  // 确保clientId存在
  if (!clientId) {
    console.error('客户端ID未初始化，无法发送爬取结果');
    return;
  }
  
  const resultMessage = {
    type: 'crawl_result',
    payload: {
      taskId: taskId,
      crawlData: crawlData,
      crawlStatus: crawlStatus,
      errorMessage: errorMessage
    },
    clientId: clientId,
    timestamp: Date.now()
  };
  
  ws.send(JSON.stringify(resultMessage));
}

// 发送网址变化消息
function sendUrlChange(url) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    return;
  }
  
  const urlChangeMessage = {
    type: 'url_change',
    payload: {
      currentUrl: url
    },
    clientId: clientId,
    timestamp: Date.now()
  };
  
  ws.send(JSON.stringify(urlChangeMessage));
}

// 检查域名脚本匹配
function checkDomainScriptMatch(url) {
  try {
    const urlObj = new URL(url);
    const hostname = urlObj.hostname;
    
    // 检查是否有匹配的脚本
    for (const domainPattern in domainScriptMap) {
      try {
        // 处理特殊字符
        const escapedPattern = domainPattern.replace(/\./g, '\\.').replace(/\*/g, '.*');
        const regex = new RegExp(escapedPattern);
        if (regex.test(hostname)) {
          const matchedScripts = domainScriptMap[domainPattern];
          
          // 发送匹配到的脚本到content script
          chrome.tabs.query({active: true, currentWindow: true}, function(tabs) {
            if (tabs[0]) {
              const tab = tabs[0];
                  
              // 检查是否是允许发送消息的页面
              if (tab.url.startsWith('chrome://') || tab.url.startsWith('about:') || tab.url.startsWith('data:')) {
                console.log('无法向内部页面发送域名匹配消息:', tab.url);
                return;
              }
                  
              try {
                chrome.tabs.sendMessage(tab.id, {
                  type: 'domain_script_matched',
                  matchedScripts: matchedScripts,
                  domain: hostname
                }, function(response) {
                  // 检查是否发送失败
                  if (chrome.runtime.lastError) {
                    console.log('发送域名匹配消息失败:', chrome.runtime.lastError.message);
                  }
                });
              } catch (error) {
                console.log('发送域名匹配消息异常:', error);
              }
            } else {
              // 如果没有活动标签页，尝试获取任意标签页
              chrome.tabs.query({}, function(allTabs) {
                if (allTabs && allTabs.length > 0) {
                  const tab = allTabs[0];
                      
                  // 检查是否是允许发送消息的页面
                  if (tab.url.startsWith('chrome://') || tab.url.startsWith('about:') || tab.url.startsWith('data:')) {
                    console.log('无法向内部页面发送域名匹配消息:', tab.url);
                    return;
                  }
                      
                  try {
                    chrome.tabs.sendMessage(tab.id, {
                      type: 'domain_script_matched',
                      matchedScripts: matchedScripts,
                      domain: hostname
                    }, function(response) {
                      // 检查是否发送失败
                      if (chrome.runtime.lastError) {
                        console.log('发送域名匹配消息失败:', chrome.runtime.lastError.message);
                      }
                    });
                  } catch (error) {
                    console.log('发送域名匹配消息异常:', error);
                  }
                }
              });
            }
          });
          
          console.log(`域名 ${hostname} 匹配到脚本:`, matchedScripts.map(s => s.scriptId));
          break; // 找到匹配后退出循环
        }
      } catch (e) {
        console.error('域名匹配错误:', e);
      }
    }
  } catch (e) {
    console.error('解析URL错误:', e);
  }
}

// 通知后端客户端空闲状态
function notifyIdleStatusToBackend(isIdle) {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.error('WebSocket未连接，无法通知空闲状态');
    return;
  }
  
  // 确保clientId存在
  if (!clientId) {
    console.error('客户端ID未初始化，无法发送空闲状态更新');
    return;
  }
  
  const idleStatusMessage = {
    type: 'idle_status_update',
    payload: {
      isIdle: isIdle,
      timestamp: Date.now()
    },
    clientId: clientId,
    timestamp: Date.now()
  };
  
  try {
    ws.send(JSON.stringify(idleStatusMessage));
    console.log('发送空闲状态更新:', idleStatusMessage);
  } catch (error) {
    console.error('发送空闲状态更新失败:', error);
  }
}

// 更新插件图标
function updateIcon(status) {
  const iconPath = status === 'connected' ? 
    { '16': 'icons/icon16.png', '48': 'icons/icon48.png', '128': 'icons/icon128.png' } :
    { '16': 'icons/icon16.png', '48': 'icons/icon48.png', '128': 'icons/icon128.png' };
  
  // 对于简单的连接状态指示，我们可以改变图标的样式
  // 但由于我们没有不同的连接/断开图标，这里只是记录状态
  console.log(`连接状态: ${status}`);
}

// 初始化时尝试从存储中恢复JWT令牌
chrome.storage.local.get(['jwtToken', 'username', 'clientId'], async function(result) {
  if (result.jwtToken) {
    jwtToken = result.jwtToken;
    username = result.username;
    // 确保客户端ID已被初始化
    await initializeClientId();
    connectWebSocket();
  } else {
    // 即使没有JWT令牌，也要确保客户端ID被初始化
    await initializeClientId();
  }
});

// 监听定时任务更新消息 (现在不需要了，因为定时任务在服务端管理)
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'scheduled_task_updated') {
    console.log('客户端不再处理定时任务更新消息，所有定时任务在服务端管理');
    sendResponse({ success: true });
  }
  
  return true;
});

// 移除之前与定时任务相关的存储监听
// chrome.storage.onChanged.addListener(function(changes, namespace) {
//   for (let [key, { oldValue, newValue }] of Object.entries(changes)) {
//     if (key === 'idleTaskQueue' && clientIdleStatus && newValue && newValue.length > 0) {
//       // 当队列有变化且客户端处于空闲状态时，执行队列中的任务
//       executeQueuedIdleTasks();
//     }
//   }
// });

// 从这里开始移除与客户端定时任务相关的函数
function executeIdleTasks(idleSince, currentTime) {
  console.log(`执行空闲任务，自 ${new Date(idleSince)} 开始空闲`);
  
  // 检查是否有适合空闲时执行的任务
  for (const scriptId in cachedScripts) {
    const script = cachedScripts[scriptId];
    
    // 检查脚本是否标记为可以在空闲时执行
    if (script.description.includes('idle') || script.scriptContent.includes('BACKGROUND_TASK')) {
      console.log(`发现空闲任务脚本: ${scriptId}`);
      
      // 生成任务ID
      const taskId = `idle_task_${scriptId}_${Date.now()}`;
      
      // 执行空闲脚本
      executeScriptOnActiveTab(scriptId, script.scriptContent, taskId);
    }
  }
}