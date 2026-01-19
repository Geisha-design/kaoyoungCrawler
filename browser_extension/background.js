// 全局变量
let ws = null;
let isConnected = false;
let clientId = null;
let jwtToken = null;
let username = null;
let cachedScripts = {}; // 存储缓存的脚本
let domainScriptMap = {}; // 域名-脚本映射表
let activeTabUrl = null;

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
      online: isConnected 
    });
  } else if (message.type === 'get_scheduled_tasks') {
    // 获取定时任务列表（暂时返回空，实际从后端获取）
    // 这里可以维护一个定时任务列表
    const scheduledTasks = []; // 实际应用中应从存储中获取
    sendResponse({ tasks: scheduledTasks });
  }
  
  return true; // 保持消息通道开放
});

// 监听来自content script的消息
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'crawl_result_from_content') {
    // 处理来自content script的爬取结果
    handleCrawlResult(message.taskId, message.result, message.status);
    sendResponse({ success: true });
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

// 连接WebSocket
function connectWebSocket() {
  if (isConnected || !jwtToken) {
    return;
  }
  
  try {
    const wsUrl = `ws://localhost:8080/ws?token=${jwtToken}`;
    ws = new WebSocket(wsUrl);
    
    ws.onopen = function(event) {
      console.log('WebSocket连接已建立');
      isConnected = true;
      updateIcon('connected');
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
      // 处理定时任务配置
      handleScheduledTaskConfig(message.payload.tasks);
      break;
      
    default:
      console.log('未知消息类型:', message.type);
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
        supportTaskTypes: 'product_crawl,article_crawl' // 示例支持的任务类型
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
    const domains = script.domainPattern.split('|'); // 支持多个域名
    domains.forEach(domain => {
      if (!domainScriptMap[domain]) {
        domainScriptMap[domain] = [];
      }
      domainScriptMap[domain].push(script);
    });
  });
  
  console.log('脚本缓存已更新，当前缓存脚本数:', Object.keys(cachedScripts).length);
}

// 处理任务命令
async function handleTaskCommand(payload) {
  const { taskId, scriptId } = payload;
  console.log(`收到任务命令，任务ID: ${taskId}, 脚本ID: ${scriptId}`);
  
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

// 处理定时任务配置
function handleScheduledTaskConfig(tasks) {
  console.log('收到定时任务配置:', tasks);
  
  // 清除所有现有的定时任务
  chrome.alarms.clearAll();
  
  // 为每个启用的定时任务创建alarm
  tasks.forEach(task => {
    if (task.enabled) {
      // 确保间隔至少为1分钟（Chrome Alarms API的限制）
      const periodInMinutes = Math.max(1, Math.round(task.interval / 60000));
      
      const alarmInfo = {
        periodInMinutes: periodInMinutes
      };
      
      chrome.alarms.create(task.taskKey, alarmInfo);
      
      console.log(`创建定时任务: ${task.taskKey}, 间隔: ${periodInMinutes} 分钟`);
    }
  });
  
  // 监听alarm事件
  chrome.alarms.onAlarm.addListener(alarm => {
    handleAlarmTrigger(alarm.name);
  });
}

// 处理定时任务触发
async function handleAlarmTrigger(taskKey) {
  console.log(`定时任务触发: ${taskKey}`);
  
  // 获取当前活动标签页
  const tabs = await chrome.tabs.query({active: true, currentWindow: true});
  
  if (tabs.length > 0) {
    const activeTab = tabs[0];
    try {
      const url = new URL(activeTab.url);
      const hostname = url.hostname;
      
      // 获取定时任务配置
      const response = await fetch(`http://localhost:8080/api/scheduled-task/list/${clientId}`, {
        headers: {
          'Authorization': `Bearer ${jwtToken}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const result = await response.json();
        if (result.code === 200) {
          const task = result.data.find(t => t.taskKey === taskKey);
          
          if (task && task.enabled) {
            // 检查域名是否匹配
            if (new RegExp(task.domain).test(hostname)) {
              console.log(`域名匹配，执行定时任务 ${taskKey}`);
              
              // 执行任务
              const script = cachedScripts[task.scriptId];
              if (script) {
                // 生成任务ID
                const taskId = `scheduled_task_${taskKey}_${Date.now()}`;
                
                // 执行脚本
                executeScriptOnActiveTab(task.scriptId, script.scriptContent, taskId);
              }
            } else {
              console.log(`域名不匹配，跳过任务 ${taskKey}，当前域名: ${hostname}，任务域名: ${task.domain}`);
            }
          }
        }
      }
    } catch (e) {
      console.error('处理定时任务触发错误:', e);
    }
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

// 监听定时任务更新消息
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'scheduled_task_updated') {
    // 广播给所有打开的定时任务管理页面
    chrome.tabs.query({}, function(tabs) {
      for (let tab of tabs) {
        chrome.tabs.sendMessage(tab.id, message);
      }
    });
    sendResponse({ success: true });
  }
  
  return true;
});