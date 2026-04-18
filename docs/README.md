# 作物病害智能诊断与农情决策平台

本项目聚合了 YOLO 病虫害识别、知识图谱决策和农事任务管理等能力，构建面向智慧农业场景的“一体化数字农服平台”。通过“感知 → 诊断 → 决策 → 执行”的闭环设计，帮助种植主体实现病害早预警、精准处置和任务协同。

## 项目亮点

- **全流程能力**：集成传感器监测、图像/视频识别、病害知识库、智能防治方案与农事计划跟踪，覆盖日常管理的关键环节。
- **三端联动架构**：前端（Vue3 + Element Plus）负责可视化门户，中台（Spring Boot）承载业务逻辑与数据服务，AI 服务（Flask + YOLO）提供实时推理能力。
- **工程化实践**：模型热加载、推理任务状态回传、文件上传网关、缓存策略与日志监控等设计方便生产落地。
- **丰富的初始化数据**：仓库附带病害知识、解决方案、农事任务与传感器样例数据，可一键导入，快速搭建演示环境。

## 总体架构

```
┌─────────────────────────┐
│  Web 前端（Vue3 + Vite） │
│  · 数据大屏 / 实时监控   │
│  · 智能方案 / 识别记录   │
│  · 用户、任务、聊天等    │
└──────────▲──────────┘
           │REST / WS
┌──────────┴──────────┐
│ Spring Boot 中台服务 │
│ · 用户与权限管理     │
│ · 病害/方案知识库    │
│ · 传感器 & 农事任务  │
│ · AI/天气/LLM 调用   │
└──────────▲──────────┘
           │HTTP
┌──────────┴──────────┐
│ Flask + YOLO 推理服务 │
│ · 图片/视频/摄像头识别 │
│ · 结果存档 + SocketIO │
└──────────▲──────────┘
           │
┌──────────┴──────────┐
│      MySQL 数据库     │
│      Redis（可选）     │
└──────────────────────┘
```

## 功能清单

- **病害识别中心**
  - 支持上传图片、离线视频或摄像头流进行 YOLO 推理。
  - 实时显示检测框、置信度与处理耗时，自动保存识别记录。
  - 权重文件按需切换，支持远程资源下载、本地缓存。
- **传感器监测**
  - 看板展示温度、湿度、土壤含水量、光照、CO₂ 等指标。
  - 内置 ECharts 历史分析，支持 1/3/7 天时间维度切换与自动刷新。
- **硬件控制中心**
  - 后端新增 TCP 网关，完全复刻 Android APP 的 Socket 协议，可直接对接 `192.168.4.1:8080` 设备。
  - 支持多设备多选、指令回执（OK/ERR 自动落入 `tb_device_control_log`）以及 `/device/connections` 实时查询在线状态。
- **知识与方案中心**
  - 维护病害百科、典型症状、危害等级与防治要点。
  - `/solution/generate` 联合知识库、实时天气（Open-Meteo 缓存）与 Spark LLM 输出个性化施药建议、风险评估、投入产出分析。
- **农事任务协同**
  - 任务列表、执行看板、甘特时间线，支持责任人、优先级与进度追踪。
  - 可关联识别结果或防治方案，实现闭环管理。
- **运营与辅助模块**
  - 用户/角色管理、个人中心、文件上传下载、智能客服聊天（HTTP/WebSocket 双通道）。
  - 系统通知、日志查询、运行监控（预留）。

## 核心技术栈

| 层次 | 技术栈 | 说明 |
| ---- | ------ | ---- |
| 前端 | Vue 3, Vite, TypeScript, Element Plus, Pinia, ECharts | 组件化大屏、实时图表、表单/流程页面 |
| 中台 | Spring Boot, MyBatis, Lombok, Maven Wrapper, Spark LLM SDK | 业务服务、知识库/方案/任务 API、第三方调用封装 |
| AI 服务 | Flask, Ultralytics YOLO, OpenCV, Torch, SocketIO | 推理调用、模型管理、任务进度推送 |
| 数据层 | MySQL 8（脚本在根目录 SQL 文件），Redis（可选缓存） | 结构化存储与缓存扩展 |

## 目录结构概览

```
njzp.tech/
├─ yolo_cropDisease_detection_vue/       # Vue3 前端
│  ├─ src/views/sensor/                  # 传感器可视化
│  ├─ src/views/solution/                # 智能方案页面
│  ├─ src/views/imgPredict/、videoPredict 等
│  └─ src/api/、stores/、utils/ ...
├─ yolo_cropDisease_detection_springboot/ # Spring Boot 中台
│  ├─ src/main/java/com/...              # 控制器、服务、实体
│  ├─ src/main/resources/                # 配置、Mapper、SQL
│  └─ files/                             # 上传文件持久化目录
├─ yolo_cropDisease_detection_flask/     # Flask + YOLO 推理服务
│  ├─ main.py                            # 入口，含 SocketIO、任务调度
│  ├─ predict/                           # 图像推理封装
│  └─ weights/、runs/、static/           # 模型权重与输出
├─ disease_knowledge_data.sql            # 病害知识初始化数据
├─ hardware_tables.sql                   # 传感器与设备控制表结构
├─ start_all.sh                          # 一键启动脚本
└─ docs/QUICKSTART.md                    # 启动与排障说明
```

## 数据库与业务模型

- **病害知识**：`disease_knowledge_data` 记录作物、病害名称、症状描述、风险等级等，用于知识展示与方案生成。
- **解决方案**：`tb_solution`、`tb_remedy` 存储防治策略、药剂成分、成本与执行步骤，可与识别结果或农事任务关联。
- **传感器数据**：`tb_sensor_data` 支持实时上报与历史查询，前端进行图表渲染。
- **农事任务**：`tb_farm_task` 包含任务类型、执行人、节点时间、状态等，支撑调度与提醒。
- **识别记录**：`tb_img_records`、`tb_video_records`、`tb_camera_records` 追溯各类推理结果并支持二次分析。


## AI 推理服务设计

- **模型管理**：支持多权重共存（默认 `yolo11n.pt`），首次调用时自动加载并缓存至 GPU；根据环境自动选择 CUDA/Half 精度。
- **输入适配**：可处理本地文件、远程 URL（自动下载缓存）以及 Base64 数据。
- **任务事件**：通过 SocketIO 向前端推送 `processing/completed/failed` 状态，便于做进度提示和失败补救。
- **输出存档**：预测结果保存至 `static/results`，并将路径、置信度、标签、耗时等写入数据库供前端展示。

## 快速上手（本地开发）

推荐直接看 `docs/QUICKSTART.md`，该文档已按当前仓库实际配置校准（可直接执行）。

最短路径如下：

1. **初始化数据库（首次）**
   - 创建 `cropdisease` 数据库。
   - 导入 `hardware_tables.sql`（传感器与设备控制表）。
   - 补齐核心业务表并导入 `disease_knowledge_data.sql`（详见 `docs/QUICKSTART.md`）。
2. **一键启动三端（推荐）**
   ```bash
   chmod +x start_all.sh
   ./start_all.sh --install-deps
   ```
3. **访问系统**
   - 前端：`http://localhost:8888`
   - 后端：`http://localhost:9999/api`
   - Flask：`http://localhost:5001`
4. **默认账号**
   - 建议创建：`admin / admin123`（对应 `tb_user` 表）。

### 硬件控制配置

1. 若硬件设备以 AP 形式暴露 `192.168.4.1:8080`（与 Android APP 同协议），请在 `yolo_cropDisease_detection_springboot/src/main/resources/application.properties` 中开启：
   ```properties
   hardware.tcp.enabled=true
   hardware.tcp.host=192.168.4.1
   hardware.tcp.port=8080
   hardware.tcp.default-device-id=DEVICE_001
   ```
   可按需为多台设备追加 `hardware.tcp.devices[n].device-id/host/port`。
2. 前端 `deviceControl` 页面当前通过单设备选择下发指令（`deviceId` 字段），后台会逐台记录 `tb_device_control_log`，TCP 发送后等待硬件返回 `OK/ERR` 并自动更新状态；多设备批量控制已在规划中。
3. 调用 `GET /api/device/connections` 可实时查看连接是否在线、最近心跳时间，便于在大屏上展示硬件运行状况。

完整排障与分端启动命令请参考 `docs/QUICKSTART.md`。

## 常见问题

- **模型推理速度慢**：确保安装 CUDA 环境并在 `main.py` 中检测到 GPU；必要时更换更轻量的 YOLO 权重或开启 FP16。
- **跨服务调用失败**：检查 `application.properties`、`.env` 中的 Flask/Base URL 设置，确认端口映射及防火墙策略。
- **WebSocket 无法连接**：确保前后端域名一致或正确配置 CORS，查看 Flask 控制台是否有连接日志。
- **天气或 LLM 能力不可用**：在 Spring Boot 配置文件中补齐 `weather.*` 与 `llm.spark.*` 参数，或在无网络环境下关闭相关功能。

## 未来拓展建议

1. **智能硬件接入**：将真实物联网设备数据接入 `/api/sensor/data` 接口，实现生产级监测。
2. **告警与自动化**：基于识别结果、风险等级触发短信/钉钉/企业微信通知，与任务系统联动。
3. **算法迭代**：引入多模型 ensemble、自适应阈值或轻量化部署方案，提升检测准确率与效率。
4. **数据闭环**：对识别结果与实际防治效果进行标注和回流，用于持续训练与模型优化。

---

如需二次开发或部署支持，可先阅读 `yolo_cropDisease_detection_springboot/README.md`、`docs/QUICKSTART.md` 获取更细节的环境配置与调试指南。
