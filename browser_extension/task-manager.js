// 定时任务管理页面逻辑
document.addEventListener('DOMContentLoaded', function() {
  // DOM元素
  const addTaskBtn = document.getElementById('addTaskBtn');
  const taskForm = document.getElementById('taskForm');
  const cancelBtn = document.getElementById('cancelBtn');
  const saveTaskBtn = document.getElementById('saveTaskBtn');
  const taskTableBody = document.getElementById('taskTableBody');
  const formTitle = document.getElementById('formTitle');
  
  // 表单元素
  const taskNameInput = document.getElementById('taskName');
  const scriptIdInput = document.getElementById('scriptId');
  const domainInput = document.getElementById('domain');
  const intervalInput = document.getElementById('interval');
  const enabledCheckbox = document.getElementById('enabled');
  
  // 当前编辑的任务
  let currentEditTask = null;
  
  // 初始化页面
  loadTasks();
  
  // 监听添加任务按钮
  addTaskBtn.addEventListener('click', function() {
    resetForm();
    formTitle.textContent = '添加定时任务';
    currentEditTask = null;
    taskForm.classList.remove('hidden');
  });
  
  // 监听取消按钮
  cancelBtn.addEventListener('click', function() {
    taskForm.classList.add('hidden');
  });
  
  // 监听保存任务按钮
  saveTaskBtn.addEventListener('click', saveTask);
  
  // 加载任务列表
  async function loadTasks() {
    try {
      // 从后台脚本获取任务列表
      chrome.runtime.sendMessage({ type: 'get_scheduled_tasks' }, function(response) {
        if (response && response.tasks) {
          renderTaskList(response.tasks);
        } else {
          // 如果后台没有返回任务，则尝试从后端获取
          fetchTasksFromBackend();
        }
      });
    } catch (error) {
      console.error('加载任务列表失败:', error);
      // 如果失败，也尝试从后端获取
      fetchTasksFromBackend();
    }
  }
  
  // 从后端获取任务列表
  async function fetchTasksFromBackend() {
    try {
      // 获取JWT令牌
      const storage = await chrome.storage.local.get(['jwtToken', 'clientId']);
      if (!storage.jwtToken || !storage.clientId) {
        console.error('未登录或缺少必要信息');
        return;
      }
      
      const response = await fetch(`http://localhost:8080/api/scheduled-task/list/${storage.clientId}`, {
        headers: {
          'Authorization': `Bearer ${storage.jwtToken}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const result = await response.json();
        if (result.code === 200) {
          renderTaskList(result.data);
        } else {
          console.error('获取任务列表失败:', result.message);
        }
      } else {
        console.error('请求失败:', response.statusText);
      }
    } catch (error) {
      console.error('获取任务列表时出错:', error);
    }
  }
  
  // 渲染任务列表
  function renderTaskList(tasks) {
    taskTableBody.innerHTML = '';
    
    if (!tasks || tasks.length === 0) {
      const row = document.createElement('tr');
      row.innerHTML = '<td colspan="6" style="text-align: center;">暂无定时任务</td>';
      taskTableBody.appendChild(row);
      return;
    }
    
    tasks.forEach(task => {
      const row = document.createElement('tr');
      
      row.innerHTML = `
        <td>${task.taskName || task.taskKey}</td>
        <td>${task.scriptId}</td>
        <td>${task.domain}</td>
        <td>${Math.floor(task.interval / 1000)}</td>
        <td>${task.enabled ? '启用' : '禁用'}</td>
        <td>
          <button class="btn btn-warning" onclick="editTask('${task.taskKey}')">编辑</button>
          <button class="btn btn-danger" onclick="deleteTask('${task.taskKey}')">删除</button>
          <button class="btn btn-primary" onclick="toggleTask('${task.taskKey}', ${task.enabled})">${task.enabled ? '禁用' : '启用'}</button>
        </td>
      `;
      
      taskTableBody.appendChild(row);
    });
    
    // 将函数附加到window对象，以便在HTML中调用
    window.editTask = function(taskKey) {
      editExistingTask(taskKey);
    };
    
    window.deleteTask = function(taskKey) {
      deleteExistingTask(taskKey);
    };
    
    window.toggleTask = function(taskKey, isEnabled) {
      toggleTaskStatus(taskKey, !isEnabled);
    };
  }
  
  // 编辑现有任务
  async function editExistingTask(taskKey) {
    try {
      // 获取任务详情
      const storage = await chrome.storage.local.get(['jwtToken', 'clientId']);
      if (!storage.jwtToken || !storage.clientId) {
        console.error('未登录或缺少必要信息');
        return;
      }
      
      const response = await fetch(`http://localhost:8080/api/scheduled-task/list/${storage.clientId}`, {
        headers: {
          'Authorization': `Bearer ${storage.jwtToken}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const result = await response.json();
        if (result.code === 200) {
          const task = result.data.find(t => t.taskKey === taskKey);
          if (task) {
            // 填充表单
            taskNameInput.value = task.taskName || '';
            scriptIdInput.value = task.scriptId;
            domainInput.value = task.domain;
            intervalInput.value = task.interval;
            enabledCheckbox.checked = task.enabled;
            
            formTitle.textContent = '编辑定时任务';
            currentEditTask = taskKey;
            taskForm.classList.remove('hidden');
          }
        }
      }
    } catch (error) {
      console.error('编辑任务时出错:', error);
    }
  }
  
  // 删除任务
  async function deleteExistingTask(taskKey) {
    if (!confirm('确定要删除这个任务吗？')) {
      return;
    }
    
    try {
      const storage = await chrome.storage.local.get(['jwtToken', 'clientId']);
      if (!storage.jwtToken || !storage.clientId) {
        console.error('未登录或缺少必要信息');
        return;
      }
      
      // 构造要同步的数据（移除要删除的任务）
      const response = await fetch(`http://localhost:8080/api/scheduled-task/list/${storage.clientId}`, {
        headers: {
          'Authorization': `Bearer ${storage.jwtToken}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const result = await response.json();
        if (result.code === 200) {
          const tasks = result.data.filter(task => task.taskKey !== taskKey);
          await syncTasksToBackend(tasks);
          loadTasks(); // 重新加载任务列表
        }
      }
    } catch (error) {
      console.error('删除任务时出错:', error);
    }
  }
  
  // 切换任务状态
  async function toggleTaskStatus(taskKey, newStatus) {
    try {
      const storage = await chrome.storage.local.get(['jwtToken', 'clientId']);
      if (!storage.jwtToken || !storage.clientId) {
        console.error('未登录或缺少必要信息');
        return;
      }
      
      // 获取当前所有任务
      const response = await fetch(`http://localhost:8080/api/scheduled-task/list/${storage.clientId}`, {
        headers: {
          'Authorization': `Bearer ${storage.jwtToken}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const result = await response.json();
        if (result.code === 200) {
          const tasks = result.data.map(task => {
            if (task.taskKey === taskKey) {
              task.enabled = newStatus;
            }
            return task;
          });
          
          await syncTasksToBackend(tasks);
          loadTasks(); // 重新加载任务列表
        }
      }
    } catch (error) {
      console.error('切换任务状态时出错:', error);
    }
  }
  
  // 保存任务
  async function saveTask() {
    const taskData = {
      taskName: taskNameInput.value.trim(),
      scriptId: scriptIdInput.value.trim(),
      domain: domainInput.value.trim(),
      interval: parseInt(intervalInput.value) || 300000, // 默认5分钟
      enabled: enabledCheckbox.checked
    };
    
    if (!taskData.scriptId || !taskData.domain) {
      alert('请填写必需字段：脚本ID和目标域名');
      return;
    }
    
    try {
      const storage = await chrome.storage.local.get(['jwtToken', 'clientId']);
      if (!storage.jwtToken || !storage.clientId) {
        console.error('未登录或缺少必要信息');
        return;
      }
      
      // 获取当前所有任务
      let tasks = [];
      const response = await fetch(`http://localhost:8080/api/scheduled-task/list/${storage.clientId}`, {
        headers: {
          'Authorization': `Bearer ${storage.jwtToken}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const result = await response.json();
        if (result.code === 200) {
          tasks = result.data || [];
        }
      }
      
      // 如果是编辑现有任务，则更新它；否则添加新任务
      if (currentEditTask) {
        // 更新现有任务
        tasks = tasks.map(task => {
          if (task.taskKey === currentEditTask) {
            return {
              ...task,
              taskName: taskData.taskName,
              scriptId: taskData.scriptId,
              domain: taskData.domain,
              interval: taskData.interval,
              enabled: taskData.enabled
            };
          }
          return task;
        });
      } else {
        // 添加新任务
        const newTask = {
          taskKey: `task_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
          clientId: storage.clientId,
          username: storage.username || 'unknown',
          ...taskData
        };
        tasks.push(newTask);
      }
      
      // 同步到后端
      await syncTasksToBackend(tasks);
      
      // 重置表单并隐藏
      taskForm.classList.add('hidden');
      resetForm();
      
      // 重新加载任务列表
      loadTasks();
    } catch (error) {
      console.error('保存任务时出错:', error);
      alert('保存任务失败: ' + error.message);
    }
  }
  
  // 同步任务到后端
  async function syncTasksToBackend(tasks) {
    try {
      const storage = await chrome.storage.local.get(['jwtToken', 'clientId']);
      if (!storage.jwtToken || !storage.clientId) {
        console.error('未登录或缺少必要信息');
        return;
      }
      
      const response = await fetch('http://localhost:8080/smarteCrawler/api/scheduled-task/sync', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${storage.jwtToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          clientId: storage.clientId,
          tasks: tasks
        })
      });
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      
      const result = await response.json();
      if (result.code !== 200) {
        throw new Error(result.message || '同步任务失败');
      }
      
      console.log('任务同步成功');
    } catch (error) {
      console.error('同步任务到后端时出错:', error);
      throw error;
    }
  }
  
  // 重置表单
  function resetForm() {
    taskNameInput.value = '';
    scriptIdInput.value = '';
    domainInput.value = '';
    intervalInput.value = '';
    enabledCheckbox.checked = true;
  }
  
  // 监听定时任务更新消息
  chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === 'scheduled_task_updated') {
      loadTasks(); // 重新加载任务列表
    }
    return true;
  });
});