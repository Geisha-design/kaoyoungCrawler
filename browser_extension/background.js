// 全局变量
let ws = null;
let isConnected = false;
let clientId = null;
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

// 初始化
chrome.runtime.onInstalled.addListener(() => {
  console.log('爬虫助手扩展已安装');
});

// 监听来自popup的消息
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'login_success') {
    jwtToken = message.token;
    username = message.username;
    connectWebSocket();
    sendResponse({ success: true });
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
  clientId = null;
  username = null;
  
  // 更新图标状态
  updateIcon('disconnected');
  
  // 停止心跳
  stopHeartbeat();
  
  console.log('WebSocket连接已关闭');
}

// 连接WebSocket
function connectWebSocket() {
  if (isConnected || !jwtToken) {
    return;
  }
  
  try {
    const wsUrl = `ws://localhost:8090/smarteCrawler/ws?token=${jwtToken}`;
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
      clientId = message.payload.clientId;
      // 保存clientId到本地存储
      chrome.storage.local.set({ clientId: clientId });
      // 发送注册消息
      sendRegisterMessage();
      break;
      
    case 'script_push':
    case 'script_designated_push':
      // 处理脚本推送
      handleScriptPush(message.payload.scripts);
      break;
      
    case 'task_command':
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
      chrome.runtime.sendMessage({
        type: 'set_idle_threshold',
        threshold: payload.threshold || 300000 // 默认5分钟
      });
      break;
      
    case 'check_idle_status':
      // 检查空闲状态
      chrome.runtime.sendMessage({
        type: 'check_idle_status'
      });
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
    const currentUrl = tabs[0] ? tabs[0].url : '';
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
  executeScriptOnActiveTab(scriptId, script.scriptContent, taskId);
}

// 在当前活动标签页执行脚本
async function executeScriptOnActiveTab(scriptId, scriptContent, taskId = null) {
  try {
    const [tab] = await chrome.tabs.query({active: true, currentWindow: true});
    
    // 发送消息到content script执行脚本
    chrome.tabs.sendMessage(tab.id, {
      type: 'execute_crawl_script',
      scriptId: scriptId,
      scriptContent: scriptContent,
      taskId: taskId
    });
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
              chrome.tabs.sendMessage(tabs[0].id, {
                type: 'domain_script_matched',
                matchedScripts: matchedScripts,
                domain: hostname
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
chrome.storage.local.get(['jwtToken', 'username', 'clientId'], function(result) {
  if (result.jwtToken) {
    jwtToken = result.jwtToken;
    username = result.username;
    clientId = result.clientId;
    connectWebSocket();
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