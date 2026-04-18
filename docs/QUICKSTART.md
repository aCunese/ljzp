# 快速启动指南（本地开发）

> 目标：用当前仓库的真实配置，在本机启动前端 + 后端 + Flask 推理服务。  
> 本文已按当前代码校准：前端默认 `8888`，后端 `9999`（上下文 `/api`），Flask `5001`。

## 1. 环境要求

- Node.js `>=16`（前端 `package.json` 要求）
- npm `>=7`
- Java `8+`（建议 Java 17，本项目可正常运行）
- Python `3.8+`
- MySQL `8.0+`

可先执行：

```bash
node -v
npm -v
java -version
python3 --version
mysql --version
```

## 2. 一次性准备数据库

在项目根目录 `njzp.tech` 下执行以下 SQL。

### 2.1 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS cropdisease
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE cropdisease;
```

### 2.2 初始化基础表（推荐）

仓库已有硬件相关 DDL，先导入：

```sql
SOURCE hardware_tables.sql;
```

再补齐核心业务表（首次本地搭建建议执行）：

```sql
CREATE TABLE IF NOT EXISTS `tb_user` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `name` VARCHAR(100) DEFAULT NULL,
  `sex` VARCHAR(10) DEFAULT NULL,
  `email` VARCHAR(150) DEFAULT NULL,
  `tel` VARCHAR(50) DEFAULT NULL,
  `role` VARCHAR(50) DEFAULT NULL,
  `avatar` VARCHAR(512) DEFAULT NULL,
  `time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tb_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_img_records` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `weight` VARCHAR(100) DEFAULT NULL,
  `input_img` TEXT,
  `out_img` TEXT,
  `confidence` VARCHAR(255) DEFAULT NULL,
  `all_time` VARCHAR(100) DEFAULT NULL,
  `conf` VARCHAR(100) DEFAULT NULL,
  `label` VARCHAR(255) DEFAULT NULL,
  `username` VARCHAR(100) DEFAULT NULL,
  `kind` VARCHAR(100) DEFAULT NULL,
  `start_time` VARCHAR(100) DEFAULT NULL,
  `task_id` INT DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_video_records` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `weight` VARCHAR(100) DEFAULT NULL,
  `input_video` TEXT,
  `out_video` TEXT,
  `conf` VARCHAR(100) DEFAULT NULL,
  `username` VARCHAR(100) DEFAULT NULL,
  `kind` VARCHAR(100) DEFAULT NULL,
  `start_time` VARCHAR(100) DEFAULT NULL,
  `task_id` INT DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_camera_records` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `weight` VARCHAR(100) DEFAULT NULL,
  `out_video` TEXT,
  `conf` VARCHAR(100) DEFAULT NULL,
  `username` VARCHAR(100) DEFAULT NULL,
  `kind` VARCHAR(100) DEFAULT NULL,
  `start_time` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `disease_knowledge_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `disease_code` VARCHAR(64) DEFAULT NULL,
  `disease_name` VARCHAR(128) NOT NULL,
  `crop_id` BIGINT DEFAULT NULL,
  `crop_name` VARCHAR(64) DEFAULT NULL,
  `description` TEXT,
  `symptom_summary` TEXT,
  `pathogen_type` VARCHAR(64) DEFAULT NULL,
  `risk_level` VARCHAR(32) DEFAULT NULL,
  `climate_risk_factors` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_disease_crop` (`crop_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_remedy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `remedy_code` VARCHAR(64) DEFAULT NULL,
  `remedy_name` VARCHAR(128) NOT NULL,
  `active_ingredient` VARCHAR(128) DEFAULT NULL,
  `target_pathogen` VARCHAR(128) DEFAULT NULL,
  `formulation` VARCHAR(64) DEFAULT NULL,
  `safe_dosage` DECIMAL(10,2) DEFAULT NULL,
  `dosage_unit` VARCHAR(32) DEFAULT NULL,
  `interval_days` INT DEFAULT NULL,
  `application_method` VARCHAR(128) DEFAULT NULL,
  `safety_interval_days` INT DEFAULT NULL,
  `max_applications_per_season` INT DEFAULT NULL,
  `caution` TEXT,
  `cost_per_unit` DECIMAL(10,2) DEFAULT NULL,
  `currency` VARCHAR(16) DEFAULT 'CNY',
  `last_price_update` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_solution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `disease_id` BIGINT DEFAULT NULL,
  `crop_id` BIGINT DEFAULT NULL,
  `crop_name` VARCHAR(64) DEFAULT NULL,
  `remedy_id` BIGINT DEFAULT NULL,
  `recommended_dosage` DECIMAL(10,2) DEFAULT NULL,
  `dosage_unit` VARCHAR(32) DEFAULT NULL,
  `application_stage` VARCHAR(64) DEFAULT NULL,
  `application_timing` VARCHAR(128) DEFAULT NULL,
  `notes` TEXT,
  `weather_constraints` TEXT,
  `expected_cost` DECIMAL(10,2) DEFAULT NULL,
  `status` VARCHAR(32) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_farm_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `field_id` BIGINT DEFAULT NULL,
  `task_type` VARCHAR(64) DEFAULT NULL,
  `plan_start_time` DATETIME DEFAULT NULL,
  `plan_end_time` DATETIME DEFAULT NULL,
  `solution_id` BIGINT DEFAULT NULL,
  `record_id` BIGINT DEFAULT NULL,
  `actual_dosage` DECIMAL(10,2) DEFAULT NULL,
  `actual_area` DECIMAL(10,2) DEFAULT NULL,
  `executor_id` INT DEFAULT NULL,
  `status` VARCHAR(32) DEFAULT 'PENDING',
  `resource_usage` TEXT,
  `description` TEXT,
  `feedback_text` TEXT,
  `feedback_images` TEXT,
  `progress_updated_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `archived_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tb_task_knowledge_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT DEFAULT NULL,
  `disease_id` BIGINT DEFAULT NULL,
  `crop_id` BIGINT DEFAULT NULL,
  `remedy_id` BIGINT DEFAULT NULL,
  `relation_type` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.3 导入仓库种子数据

```sql
SOURCE disease_knowledge_data.sql;
```

### 2.4 创建默认管理员账号（若不存在）

```sql
INSERT INTO tb_user (username, password, name, role, time)
SELECT 'admin', 'admin123', '管理员', 'admin', NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM tb_user WHERE username = 'admin'
);
```

## 3. 配置后端连接信息

编辑文件：`yolo_cropDisease_detection_springboot/src/main/resources/application.properties`

至少确认：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/cropdisease?serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的MySQL密码
server.port=9999
server.servlet.context-path=/api
```

提示：当前仓库 `application.properties` 含敏感配置（数据库口令、LLM Key），建议你本地改为自己的值，并避免把真实密钥提交到仓库。

## 4. 启动方式 A：一键启动（推荐）

在 `njzp.tech` 根目录执行：

```bash
chmod +x start_all.sh
./start_all.sh --install-deps
```

说明：
- 会并行启动三端：前端 + 后端 + Flask。
- 日志在 `.devlogs/frontend.log`、`.devlogs/backend.log`、`.devlogs/flask.log`。
- 关闭时直接 `Ctrl+C`。

## 5. 启动方式 B：手动分端启动

### 5.1 启动后端（Spring Boot）

```bash
cd yolo_cropDisease_detection_springboot
./mvnw spring-boot:run
```

Windows：

```powershell
cd yolo_cropDisease_detection_springboot
.\mvnw.cmd spring-boot:run
```

### 5.2 启动推理服务（Flask）

```bash
cd yolo_cropDisease_detection_flask
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py
```

Windows：

```powershell
cd yolo_cropDisease_detection_flask
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
python main.py
```

### 5.3 启动前端（Vue3 + Vite）

```bash
cd yolo_cropDisease_detection_vue
npm install
npm run dev
```

## 6. 启动后访问地址

- 前端首页：`http://localhost:8888`
- 后端健康检查（示例接口）：`http://localhost:9999/api/sensor/latest`
- Flask 服务：`http://localhost:5001`

## 7. 联调快速自检

```bash
# 后端接口
curl http://localhost:9999/api/sensor/latest

# Flask 权重列表
curl http://localhost:5001/file_names
```

若两条命令都能返回 JSON，通常说明后端和 Flask 已正常运行。

## 8. 常见问题

### 8.1 前端打不开（`localhost:8888`）

- 确认执行的是 `npm run dev`（不是 `npm run serve`）。
- 检查 `yolo_cropDisease_detection_vue/.env` 中 `VITE_PORT` 是否改过。

### 8.2 后端启动报数据库错误

- 检查 `application.properties` 中账号密码。
- 确认已执行第 2 节 SQL（尤其是 `tb_user`、`tb_img_records`、`tb_video_records`、`tb_camera_records`、`tb_sensor_data`）。

### 8.3 图像识别报错或超时

- 先单独启动 Flask 并观察控制台日志。
- 首次加载 YOLO 权重较慢，属于正常现象。
- 如未使用 GPU，可先用轻量权重并减少并发请求。

## 9. 你最常用的一套命令（复制即用）

```bash
cd /你的路径/njzp.tech

# 只需首次执行一次（后续仅改配置或更新数据时再做）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cropdisease DEFAULT CHARSET utf8mb4;"

# 推荐一键启动
./start_all.sh --install-deps
```
