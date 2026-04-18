**方案 1：一键启动（推荐）**

1. 进入项目根目录
```bash
cd /Users/ruyne./Desktop/ljzp/njzp.tech
```

2. 首次赋权（只需一次）
```bash
chmod +x start_all.sh
```

3. 启动三端（前端+后端+Flask）
```bash
./start_all.sh --install-deps
```

4. 访问地址  
前端：`http://localhost:8888`  
后端：`http://localhost:9999/api`  
Flask：`http://localhost:5001`

5. 日志位置  
`/Users/ruyne./Desktop/ljzp/njzp.tech/.devlogs/frontend.log`  
`/Users/ruyne./Desktop/ljzp/njzp.tech/.devlogs/backend.log`  
`/Users/ruyne./Desktop/ljzp/njzp.tech/.devlogs/flask.log`

6. 停止服务  
终端按 `Ctrl + C`

---

**方案 2：分开启动（手动三终端）**

1. 终端 A：启动后端 Spring Boot
```bash
cd /Users/ruyne./Desktop/ljzp/njzp.tech/yolo_cropDisease_detection_springboot
./mvnw spring-boot:run
```

2. 终端 B：启动 Flask 推理服务
```bash
cd /Users/ruyne./Desktop/ljzp/njzp.tech/yolo_cropDisease_detection_flask
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python main.py
```

3. 终端 C：启动前端 Vue
```bash
cd /Users/ruyne./Desktop/ljzp/njzp.tech/yolo_cropDisease_detection_vue
npm install
npm run dev
```

4. 访问地址  
前端：`http://localhost:8888`  
后端：`http://localhost:9999/api`  
Flask：`http://localhost:5001`

---

**补充（按需）**

如果你只想启动某一端，可以用一键脚本的参数：
```bash
./start_all.sh --skip-frontend
./start_all.sh --skip-backend
./start_all.sh --skip-flask
```

<!-- 退出谷歌浏览
pkill -9 -x "Google Chrome"
 -->