// 内容脚本 - 在网页上下文中运行
console.log('爬虫助手内容脚本已加载');

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
  }
  
  return false;
});

// 执行爬取脚本的函数
function executeCrawlScript(scriptContent, taskId) {
  try {
    // 创建一个函数来执行脚本内容
    // 注意：这里使用new Function是为了避免eval的安全风险
    const scriptFunction = new Function(scriptContent + '; return crawlProduct ? crawlProduct() : crawlArticle ? crawlArticle() : null;');
    const result = scriptFunction();
    
    // 将结果发送回background脚本
    chrome.runtime.sendMessage({
      type: 'crawl_result_from_content',
      taskId: taskId,
      result: result,
      status: 'success'
    });
    
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
      chrome.runtime.sendMessage({
        type: 'execute_script',
        scriptId: script.scriptId,
        scriptContent: script.scriptContent
      });
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
    chrome.runtime.sendMessage({
      type: 'crawl_result_from_content',
      taskId: event.data.taskId,
      result: event.data.result,
      status: event.data.status
    });
  }
});

// 当收到后台脚本的爬取结果消息时，转发给页面
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'execute_crawl_script') {
    // 执行爬取脚本
    const result = executeCrawlScript(message.scriptContent, message.taskId);
    sendResponse({ success: true, result: result });
    return true;
  }
});