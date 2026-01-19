-- 1. 用户表
CREATE TABLE `crawler_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（生产环境加密存储）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫系统用户表';

-- 2. 客户端注册表
CREATE TABLE `crawler_client` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_id` varchar(100) NOT NULL COMMENT '客户端唯一标识',
  `username` varchar(50) NOT NULL COMMENT '关联用户名',
  `current_url` varchar(500) DEFAULT '' COMMENT '当前浏览器网址',
  `support_task_types` varchar(200) DEFAULT '' COMMENT '支持的任务类型（逗号分隔）',
  `browser` varchar(20) DEFAULT 'Chrome' COMMENT '浏览器类型',
  `connect_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '连接时间',
  `status` varchar(20) DEFAULT 'online' COMMENT '状态（online/offline）',
  `last_update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫客户端注册表';

-- 3. 爬取脚本表（新增domain_pattern字段，用于网站脚本调度）
CREATE TABLE `crawler_script` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `script_id` varchar(100) NOT NULL COMMENT '脚本唯一标识',
  `task_type` varchar(50) NOT NULL COMMENT '关联任务类型',
  `script_content` text NOT NULL COMMENT 'JS脚本内容',
  `description` varchar(200) DEFAULT '' COMMENT '脚本描述',
  `domain_pattern` varchar(200) DEFAULT '.*' COMMENT '匹配域名正则表达式，默认匹配所有域名',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_script_id` (`script_id`),
  KEY `idx_task_type` (`task_type`),
  KEY `idx_domain_pattern` (`domain_pattern`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬取JS脚本表';

-- 4. 爬取任务表
CREATE TABLE `crawler_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(100) NOT NULL COMMENT '任务唯一标识',
  `client_id` varchar(100) NOT NULL COMMENT '关联客户端ID',
  `task_type` varchar(50) NOT NULL COMMENT '任务类型（即时/定时）',
  `script_id` varchar(100) NOT NULL COMMENT '关联脚本ID',
  `timeout` int(11) DEFAULT 30000 COMMENT '任务超时时间（毫秒）',
  `params` varchar(500) DEFAULT '' COMMENT '任务额外参数（JSON格式）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` varchar(20) DEFAULT 'pending' COMMENT '任务状态（pending/processing/success/fail）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬取任务表';

-- 5. 爬取结果表
CREATE TABLE `crawler_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `result_id` varchar(100) NOT NULL COMMENT '结果唯一标识',
  `task_id` varchar(100) NOT NULL COMMENT '关联任务ID',
  `client_id` varchar(100) NOT NULL COMMENT '关联客户端ID',
  `crawl_data` text NOT NULL COMMENT '爬取数据（JSON格式）',
  `crawl_status` varchar(20) DEFAULT 'success' COMMENT '爬取状态（success/fail）',
  `crawl_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '爬取时间',
  `storage_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '存储时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_result_id` (`result_id`),
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬取结果表';

-- 6. 脚本下发日志表
CREATE TABLE `crawler_script_push_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `push_id` varchar(100) NOT NULL COMMENT '下发记录唯一标识',
  `client_id` varchar(100) NOT NULL COMMENT '目标客户端ID',
  `script_ids` varchar(200) NOT NULL COMMENT '下发的脚本ID（逗号分隔）',
  `push_type` varchar(20) NOT NULL COMMENT '下发类型（batch/designated）',
  `push_status` varchar(20) NOT NULL COMMENT '下发状态（success/fail）',
  `push_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_push_id` (`push_id`),
  KEY `idx_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JS脚本下发日志表';

-- 7. 定时任务表（新增，存储定时任务配置）
CREATE TABLE `crawler_scheduled_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_key` varchar(100) NOT NULL COMMENT '定时任务唯一标识',
  `client_id` varchar(100) NOT NULL COMMENT '关联客户端ID',
  `username` varchar(50) NOT NULL COMMENT '关联用户名',
  `script_id` varchar(100) NOT NULL COMMENT '关联脚本ID',
  `domain` varchar(200) NOT NULL COMMENT '目标域名正则',
  `interval` bigint(20) NOT NULL COMMENT '执行间隔（毫秒）',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用（1=启用，0=禁用）',
  `task_name` varchar(100) DEFAULT '' COMMENT '任务名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_key` (`task_key`),
  KEY `idx_client_id` (`client_id`),
  KEY `idx_script_id` (`script_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时爬取任务表';

-- 初始化测试数据
INSERT INTO `crawler_user` (`username`, `password`) VALUES ('admin', '123456');
INSERT INTO `crawler_script` (`script_id`, `task_type`, `script_content`, `description`, `domain_pattern`) VALUES 
('script_1', 'product_crawl', 'function crawlProduct() { return {name: document.querySelector(".product-name")?.innerText || "", price: document.querySelector(".product-price")?.innerText || ""}; } crawlProduct();', '电商商品基础信息爬取', 'taobao\\.com|tmall\\.com'),
('script_2', 'article_crawl', 'function crawlArticle() { return {title: document.querySelector("h1")?.innerText || "", author: document.querySelector(".article-author")?.innerText || ""}; } crawlArticle();', '资讯文章基础信息爬取', 'sohu\\.com|sina\\.com\\.cn');
-- 初始化测试定时任务
INSERT INTO `crawler_scheduled_task` (`task_key`, `client_id`, `username`, `script_id`, `domain`, `interval`, `enabled`, `task_name`) 
VALUES ('taobao_product_01', 'client_test_001', 'admin', 'script_1', 'taobao\\.com', 300000, 1, '淘宝商品5分钟定时爬取');