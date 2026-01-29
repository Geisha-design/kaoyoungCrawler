// 内容脚本 - 在网页上下文中运行
console.log('爬虫助手内容脚本已加载');

// 空闲监控相关变量
let idleTimer = null;
let isIdle = false;
let idleThreshold = 300000; // 5分钟空闲阈值（毫秒）
let lastActivityTime = Date.now();

// 监听用户的活动
function resetIdleTimer() {
  isIdle = false;
  lastActivityTime = Date.now();
  
  // 清除之前的计时器
  if (idleTimer) {
    clearTimeout(idleTimer);
  }
  
  // 设置新的空闲检测计时器
  idleTimer = setTimeout(goIdle, idleThreshold);
  
  // 通知background脚本用户变为活跃状态
  chrome.runtime.sendMessage({
    type: 'user_active',
    timestamp: Date.now()
  });
}

// 进入空闲状态
function goIdle() {
  isIdle = true;
  console.log('用户进入空闲状态');
  
  // 通知background脚本用户进入空闲状态
  try {
    chrome.runtime.sendMessage({
      type: 'user_idle',
      timestamp: Date.now()
    });
  } catch (error) {
    console.warn('无法发送用户空闲状态消息:', error.message);
  }
  
  // 触发空闲时的任务执行
  executeIdleTasks();
}

// 执行空闲时的任务
function executeIdleTasks() {
  console.log('检测到空闲状态，准备执行后台任务...');
  
  // 通知background脚本执行空闲任务
  chrome.runtime.sendMessage({
    type: 'execute_idle_tasks',
    idleSince: lastActivityTime,
    currentTime: Date.now()
  });
}

// 监听来自background的消息
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'execute_crawl_script') {
    // 执行爬取脚本
    const result = executeCrawlScript(message.scriptContent, message.taskId);
    sendResponse({ success: true, result: result });
    return true; // 保持消息通道开放以进行异步响应
  } else if (message.type === 'domain_script_matched') {
    // 处理域名匹配到脚本的情况
    handleDomainScriptMatch(message.matchedScripts, message.domain);
    sendResponse({ success: true });
  } else if (message.type === 'set_idle_threshold') {
    // 设置空闲阈值
    idleThreshold = message.threshold || 300000; // 默认5分钟
    console.log(`空闲阈值已设置为 ${idleThreshold} 毫秒`);
    sendResponse({ success: true });
  } else if (message.type === 'check_idle_status') {
    // 检查空闲状态
    sendResponse({ 
      isIdle: isIdle, 
      idleDuration: isIdle ? (Date.now() - lastActivityTime) : 0,
      lastActivityTime: lastActivityTime
    });
    return true;
  }
  
  return false;
});

// 监听用户的活动事件
function attachActivityListeners() {
  const events = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click', 'wheel'];
  
  events.forEach(event => {
    document.addEventListener(event, resetIdleTimer, true);
  });
  
  // 初始化空闲计时器
  resetIdleTimer();
}

// 执行爬取脚本的函数
function executeCrawlScript(scriptContent, taskId) {
  try {
    // 创建一个函数来执行脚本内容
    // 注意：这里使用new Function是为了避免eval的安全风险
    const scriptFunction = new Function(`
      ${scriptContent};
      return typeof crawlProduct !== 'undefined' ? crawlProduct() : 
             typeof crawlArticle !== 'undefined' ? crawlArticle() : 
             typeof crawlData !== 'undefined' ? crawlData() : null;
    `);
    const result = scriptFunction();
    
    // 将结果发送回background脚本
    try {
      chrome.runtime.sendMessage({
        type: 'crawl_result_from_content',
        taskId: taskId,
        result: result,
        status: 'success'
      });
    } catch (error) {
      console.warn('无法发送爬取结果消息:', error.message);
    }
    
    return result;
  } catch (error) {
    console.error('执行爬取脚本时出错:', error);
    
    // 发送错误结果
    chrome.runtime.sendMessage({
      type: 'crawl_result_from_content',
      taskId: taskId,
      result: { error: error.message },
      status: 'fail'
    });
    
    return { error: error.message };
  }
}

// 处理域名匹配到脚本的情况
function handleDomainScriptMatch(matchedScripts, domain) {
  console.log(`域名 ${domain} 匹配到脚本:`, matchedScripts);
  
  // 可以在这里显示一个提示，让用户选择是否执行脚本
  // 或者根据设置自动执行脚本
  
  // 示例：显示一个临时的通知
  showNotification(`检测到 ${domain} 匹配的爬取脚本，是否执行？`, matchedScripts);
}

// 显示通知（简单实现）
function showNotification(message, scripts) {
  // 创建一个简单的通知元素
  let notification = document.getElementById('crawler-assistant-notification');
  
  if (!notification) {
    notification = document.createElement('div');
    notification.id = 'crawler-assistant-notification';
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      background: #333;
      color: white;
      padding: 15px;
      border-radius: 5px;
      z-index: 10000;
      font-family: Arial, sans-serif;
      max-width: 300px;
      box-shadow: 0 4px 8px rgba(0,0,0,0.3);
    `;
    document.body.appendChild(notification);
  }
  
  notification.innerHTML = `
    <div>${message}</div>
    <div style="margin-top: 10px;">
      <button onclick="this.parentElement.parentElement.remove()" 
              style="background: #555; color: white; border: none; padding: 5px 10px; cursor: pointer; margin-right: 5px;">取消</button>
      <button onclick="executeMatchingScripts(arguments[0])" 
              style="background: #4CAF50; color: white; border: none; padding: 5px 10px; cursor: pointer;">执行</button>
    </div>
  `;
  
  // 添加执行匹配脚本的函数到全局作用域
  window.executeMatchingScripts = function() {
    scripts.forEach(script => {
      try {
        chrome.runtime.sendMessage({
          type: 'execute_script',
          scriptId: script.scriptId,
          scriptContent: script.scriptContent
        });
      } catch (error) {
        console.error('发送执行脚本消息失败:', error);
      }
    });
    notification.remove();
  };
}

// 监听来自content script内部的结果消息
window.addEventListener('message', function(event) {
  // 只处理来自自己的消息
  if (event.source !== window) return;
  
  if (event.data.type === 'CRAWLER_RESULT') {
    // 将结果转发给background脚本
    try {
      chrome.runtime.sendMessage({
        type: 'crawl_result_from_content',
        taskId: event.data.taskId,
        result: event.data.result,
        status: event.data.status
      });
    } catch (error) {
      console.warn('无法转发爬取结果消息:', error.message);
    }
  }
});

// 初始化空闲监控
attachActivityListeners();

// 当收到后台脚本的爬取结果消息时，转发给页面
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'execute_crawl_script') {
    // 执行爬取脚本
    const result = executeCrawlScript(message.scriptContent, message.taskId);
    sendResponse({ success: true, result: result });
    return true;
  }
});