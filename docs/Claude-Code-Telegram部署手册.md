# Claude Code Telegram 远程操控部署手册

## 文档信息

- **文档版本**: v1.0
- **创建日期**: 2026-06-29
- **适用场景**: 通过手机 Telegram 远程操控电脑上的 Claude Code
- **目标用户**: 需要移动办公或远程协作的开发者

---

## 一、系统架构

### 1.1 架构图

```
┌─────────────────┐
│  手机 Telegram  │  发送指令
│    客户端       │
└────────┬────────┘
         │
         ↓ (Telegram API)
┌─────────────────┐
│ Telegram 服务器 │
└────────┬────────┘
         │
         ↓ (轮询/Webhook)
┌─────────────────────────┐
│  你的电脑               │
│  ┌──────────────────┐   │
│  │ claude-code-     │   │
│  │ telegram bot     │   │
│  └────────┬─────────┘   │
│           │              │
│           ↓ (调用)       │
│  ┌──────────────────┐   │
│  │ Claude Code      │   │
│  │ CLI/SDK          │   │
│  └────────┬─────────┘   │
│           │              │
│           ↓ (操作)       │
│  ┌──────────────────┐   │
│  │ 项目文件         │   │
│  └──────────────────┘   │
└─────────────────────────┘
```

### 1.2 工作原理

1. 你在手机 Telegram 发送消息
2. Bot 程序（运行在电脑上）接收消息
3. Bot 调用本地 Claude Code 处理请求
4. Claude Code 操作项目文件
5. 结果返回到手机 Telegram

### 1.3 优势

- ✅ **随时随地**: 手机即可操控电脑上的代码项目
- ✅ **安全隔离**: 电脑本地运行，不上传代码到云端
- ✅ **上下文保持**: 每个项目自动保存会话历史
- ✅ **自然交互**: 用自然语言描述需求，无需记命令

---

## 二、前置准备

### 2.1 硬件要求

| 项目 | 要求 |
|------|------|
| 电脑系统 | Windows 10+, macOS 11+, Linux |
| 内存 | 最低 4GB，推荐 8GB+ |
| 磁盘空间 | 至少 2GB 可用空间 |
| 网络 | 稳定的互联网连接 |
| 手机 | 安装 Telegram 的任意设备 |

### 2.2 软件依赖

**必需**:
- Python 3.11 或更高版本
- Claude Code CLI
- Telegram 账号

**可选**:
- Git（用于版本控制功能）
- 代码编辑器（VSCode、PyCharm 等）

### 2.3 账号准备

- Anthropic 账号（用于 Claude Code 认证）
- Telegram 账号

---

## 三、Telegram Bot 创建

### 3.1 创建 Bot

**步骤 1**: 打开 Telegram，搜索 `@BotFather`

**步骤 2**: 发送命令 `/newbot`

**步骤 3**: 按提示操作：

```
BotFather: Alright, a new bot. How are we going to call it?
你: My Claude Code Bot

BotFather: Good. Now let's choose a username for your bot.
你: my_claude_code_bot
```

**注意事项**:
- Bot 用户名必须以 `bot` 结尾
- 用户名必须唯一，已被占用需换一个
- 显示名称可以随意修改

**步骤 4**: 保存返回的信息

BotFather 会返回类似内容：

```
Done! Congratulations on your new bot.
Token: 1234567890:ABCdefGHIjklMNOpqrsTUVwxyz123456789
```

**重要**: 将 `Token` 保存到安全位置，后续配置需要用到。

### 3.2 获取你的 User ID

**步骤 1**: 在 Telegram 搜索 `@userinfobot`

**步骤 2**: 发送任意消息（如 `hi`）

**步骤 3**: Bot 会回复你的信息，记录 **Id** 字段：

```
Id: 123456789
First name: 张三
Username: @zhangsan
```

**重要**: 将 `Id` 数字保存下来（如 `123456789`），用于配置用户白名单。

### 3.3 配置 Bot 设置（可选）

**启用线程模式**（如果想为每个项目创建独立话题）：

1. 向 `@BotFather` 发送 `/mybots`
2. 选择你的 bot
3. 进入 `Bot Settings` → `Threaded mode`
4. 选择 `Enable`

**设置 Bot 头像**：

1. 向 `@BotFather` 发送 `/mybots`
2. 选择你的 bot
3. 选择 `Edit Bot` → `Edit Botpic`
4. 上传图片

### 3.4 配置信息汇总

完成后，你应该有以下信息：

| 项目 | 示例 | 用途 |
|------|------|------|
| Bot Token | `1234567890:ABC...` | 连接 Bot 的凭证 |
| Bot Username | `my_claude_code_bot` | Bot 的用户名 |
| Your User ID | `123456789` | 授权访问的用户 ID |

---

## 四、电脑端环境配置

### 4.1 安装 Python

**Windows**:

1. 访问 https://www.python.org/downloads/
2. 下载 Python 3.11+ 安装包
3. **重要**: 勾选 "Add Python to PATH"
4. 点击 "Install Now"

验证安装：
```bash
python --version
# 输出: Python 3.11.x 或更高
```

**macOS**:

```bash
# 使用 Homebrew
brew install python@3.11

# 验证
python3 --version
```

**Linux (Ubuntu/Debian)**:

```bash
sudo apt update
sudo apt install python3.11 python3-pip

# 验证
python3 --version
```

### 4.2 安装 Claude Code CLI

**方式 1**: 通过官网安装

1. 访问 https://claude.ai/code
2. 下载对应系统的安装包
3. 按提示安装

**方式 2**: 使用包管理器（macOS/Linux）

```bash
# macOS
brew install anthropics/tap/claude

# Linux (使用 curl)
curl -fsSL https://claude.ai/install.sh | sh
```

验证安装：
```bash
claude --version
# 输出: claude-cli x.x.x
```

### 4.3 Claude Code 认证

**方式 1**: 使用 API Key（推荐）

1. 访问 https://console.anthropic.com/settings/keys
2. 创建 API Key
3. 执行认证：

```bash
claude auth login --api-key sk-ant-api03-xxxxx
```

**方式 2**: 浏览器登录

```bash
claude auth login
# 会打开浏览器，按提示登录
```

验证认证：
```bash
claude auth status
# 输出: Authenticated as: your-email@example.com
```

### 4.4 创建项目目录

**Windows**:
```cmd
mkdir C:\Users\你的用户名\claude-projects
mkdir C:\claude-telegram-bot
cd C:\claude-telegram-bot
```

**macOS/Linux**:
```bash
mkdir -p ~/claude-projects
mkdir -p ~/claude-telegram-bot
cd ~/claude-telegram-bot
```

**目录说明**:
- `claude-projects`: 存放你的代码项目
- `claude-telegram-bot`: Bot 程序的配置目录

---

## 五、安装和配置 Bot

### 5.1 安装 claude-code-telegram

**Windows**:
```cmd
cd C:\claude-telegram-bot
pip install git+https://github.com/RichardAtCT/claude-code-telegram@v1.3.0
```

**macOS/Linux**:
```bash
cd ~/claude-telegram-bot
pip install git+https://github.com/RichardAtCT/claude-code-telegram@v1.3.0
```

验证安装：
```bash
pip show claude-code-telegram
```

### 5.2 下载配置文件模板

**Windows**:
```cmd
curl -o .env https://raw.githubusercontent.com/RichardAtCT/claude-code-telegram/main/.env.example
```

**macOS/Linux**:
```bash
curl -o .env https://raw.githubusercontent.com/RichardAtCT/claude-code-telegram/main/.env.example
```

如果没有 `curl`，可以手动创建 `.env` 文件。

### 5.3 配置 .env 文件

使用文本编辑器打开 `.env` 文件，填入以下配置：

```bash
# ========================================
# Telegram 配置
# ========================================
TELEGRAM_BOT_TOKEN=你的Bot_Token
TELEGRAM_BOT_USERNAME=你的Bot用户名

# ========================================
# 安全配置
# ========================================
ALLOWED_USERS=你的User_ID
APPROVED_DIRECTORY=项目目录路径

# ========================================
# Claude 配置
# ========================================
# 如果已通过 CLI 认证，可不填
# ANTHROPIC_API_KEY=sk-ant-...

# ========================================
# 模式配置
# ========================================
AGENTIC_MODE=true
VERBOSE_LEVEL=1

# ========================================
# 限流配置
# ========================================
CLAUDE_MAX_COST_PER_USER=10.0
RATE_LIMIT_REQUESTS=10
RATE_LIMIT_WINDOW=60
```

**配置说明**：

| 配置项 | 说明 | 示例值 |
|-------|------|--------|
| `TELEGRAM_BOT_TOKEN` | 从 BotFather 获取的 Token | `1234567890:ABC...` |
| `TELEGRAM_BOT_USERNAME` | Bot 用户名 | `my_claude_code_bot` |
| `ALLOWED_USERS` | 授权的用户 ID（多个用逗号分隔） | `123456789,987654321` |
| `APPROVED_DIRECTORY` | 允许访问的项目根目录 | Windows: `C:\Users\xxx\claude-projects`<br>macOS: `/Users/xxx/claude-projects` |
| `ANTHROPIC_API_KEY` | Anthropic API Key（可选） | `sk-ant-api03-...` |
| `AGENTIC_MODE` | 使用自然对话模式 | `true` |
| `VERBOSE_LEVEL` | 详细程度（0=安静，1=正常，2=详细） | `1` |
| `CLAUDE_MAX_COST_PER_USER` | 每用户最大花费（USD） | `10.0` |

**Windows 路径示例**：
```bash
APPROVED_DIRECTORY=C:\Users\YourName\claude-projects
```

**macOS/Linux 路径示例**：
```bash
APPROVED_DIRECTORY=/Users/yourname/claude-projects
```

### 5.4 验证配置

确认以下文件和目录存在：

**Windows**:
```cmd
dir C:\claude-telegram-bot\.env
dir C:\Users\你的用户名\claude-projects
```

**macOS/Linux**:
```bash
ls -la ~/claude-telegram-bot/.env
ls -la ~/claude-projects
```

---

## 六、启动 Bot

### 6.1 首次启动（调试模式）

**Windows**:
```cmd
cd C:\claude-telegram-bot
python -m claude_code_telegram --log-level DEBUG
```

**macOS/Linux**:
```bash
cd ~/claude-telegram-bot
python -m claude_code_telegram --log-level DEBUG
```

**预期输出**：
```
INFO:claude_code_telegram:Bot started successfully
INFO:claude_code_telegram:Agentic mode enabled
INFO:claude_code_telegram:Approved directory: /path/to/claude-projects
INFO:claude_code_telegram:Listening for messages...
```

**如果出现错误**：
- 检查 `.env` 文件配置
- 确认网络连接正常
- 查看错误信息并参考故障排查章节

### 6.2 后台运行

启动成功后，可以设置为后台运行。

**Windows - 使用任务计划程序**：

1. 打开"任务计划程序"
2. 创建基本任务
3. 触发器：用户登录时
4. 操作：启动程序
   - 程序：`python`
   - 参数：`-m claude_code_telegram`
   - 起始位置：`C:\claude-telegram-bot`

**macOS/Linux - 使用 screen**：

```bash
# 创建新会话
screen -S claude-bot

# 启动 Bot
cd ~/claude-telegram-bot
python -m claude_code_telegram

# 分离会话（保持运行）
# 按 Ctrl+A，然后按 D

# 重新连接会话
screen -r claude-bot
```

**macOS/Linux - 使用 nohup**：

```bash
cd ~/claude-telegram-bot
nohup python -m claude_code_telegram > bot.log 2>&1 &

# 查看日志
tail -f bot.log
```

### 6.3 停止 Bot

**前台运行**：
- 按 `Ctrl+C`

**后台运行（nohup）**：
```bash
# 查找进程
ps aux | grep claude_code_telegram

# 停止进程
kill -9 <进程ID>
```

**screen 会话**：
```bash
# 重新连接
screen -r claude-bot

# 停止
按 Ctrl+C

# 退出 screen
exit
```

---

## 七、手机端测试

### 7.1 首次连接

1. 在 Telegram 搜索你的 Bot 用户名（如 `@my_claude_code_bot`）
2. 点击 **Start** 或发送 `/start`
3. Bot 应该回复欢迎消息

**预期响应**：
```
欢迎使用 Claude Code Telegram Bot！

我可以帮你：
- 分析和编辑代码
- 运行测试和命令
- 管理项目文件

发送消息开始对话，或输入 /help 查看帮助。
```

### 7.2 基础功能测试

**测试 1：查看状态**
```
你：/status

Bot：
📊 系统状态
━━━━━━━━━━━━━━
✅ Claude Code: 已连接
📂 当前目录: /path/to/projects
💰 成本使用: $0.05 / $10.00
⏱️ 会话时长: 5分钟
```

**测试 2：列出文件**
```
你：列出当前目录的文件

Bot：[Working... 正在处理]
Bot：
📁 当前目录文件：
- project1/
- project2/
- README.md
```

**测试 3：创建测试文件**
```
你：创建一个 Python 文件，打印 Hello World

Bot：[Working... 正在处理]
Bot：
✅ 已创建文件 hello.py
```python
print("Hello World")
```
```

### 7.3 项目操作测试

**准备测试项目**（在电脑端）：

**Windows**:
```cmd
cd C:\Users\你的用户名\claude-projects
mkdir test-project
cd test-project
echo print("Hello") > main.py
```

**macOS/Linux**:
```bash
cd ~/claude-projects
mkdir test-project
cd test-project
echo 'print("Hello")' > main.py
```

**在手机 Telegram 测试**：

```
你：切换到 test-project 目录

Bot：✅ 已切换到目录：test-project/

你：读取 main.py

Bot：
📄 main.py:
```python
print("Hello")
```
```

你：在文件开头添加注释说明这是测试文件

Bot：[Working... 正在编辑]
Bot：
✅ 已更新 main.py
```python
# 测试文件
print("Hello")
```
```

### 7.4 调整详细程度

```
你：/verbose 2

Bot：详细度设置为 2（详细模式）

你：创建一个函数计算斐波那契数列

Bot：
🔧 Tool: Read (main.py)
💭 Reasoning: 我需要先查看现有代码...
🔧 Tool: Edit (main.py)
💭 Reasoning: 添加斐波那契函数...
🔧 Tool: Bash (python main.py)
💭 Reasoning: 测试代码是否正常运行...

✅ 已添加斐波那契函数并测试通过
```

```
你：/verbose 0

Bot：详细度设置为 0（安静模式）

你：优化这个函数

Bot：[显示输入指示器，完成后直接返回结果]
Bot：✅ 已优化函数，使用记忆化提升性能
```

---

## 八、故障排查

### 8.1 Bot 不响应

**现象**：发送消息后 Bot 没有任何反应

**排查步骤**：

1. **确认 Bot 进程运行**

**Windows**:
```cmd
tasklist | findstr python
```

**macOS/Linux**:
```bash
ps aux | grep claude_code_telegram
```

2. **检查日志**

```bash
# 如果使用 nohup
tail -f bot.log

# 或重新启动调试模式
python -m claude_code_telegram --log-level DEBUG
```

3. **测试 Bot Token**

```bash
# Windows (PowerShell)
Invoke-WebRequest -Uri "https://api.telegram.org/bot你的Token/getMe"

# macOS/Linux
curl https://api.telegram.org/bot你的Token/getMe
```

4. **验证用户白名单**

检查 `.env` 文件：
```bash
grep ALLOWED_USERS .env
# 确认你的 User ID 在列表中
```

5. **检查网络连接**

```bash
# 测试是否能访问 Telegram API
ping api.telegram.org
```

### 8.2 Claude 认证失败

**现象**：Bot 提示 "Claude authentication failed"

**解决方案**：

1. **重新认证 CLI**

```bash
claude auth logout
claude auth login
```

2. **使用 API Key**

在 `.env` 中添加：
```bash
ANTHROPIC_API_KEY=sk-ant-api03-你的API_Key
```

3. **验证认证状态**

```bash
claude auth status
# 应该显示：Authenticated as: your-email@example.com
```

### 8.3 权限错误

**现象**：Bot 提示 "Permission denied" 或 "Path not allowed"

**解决方案**：

1. **检查目录权限**

**Windows**:
```cmd
icacls C:\Users\你的用户名\claude-projects
```

**macOS/Linux**:
```bash
ls -la ~/claude-projects
# 确保当前用户有读写权限
```

2. **验证 APPROVED_DIRECTORY 配置**

```bash
grep APPROVED_DIRECTORY .env
# 确认路径正确且存在
```

3. **修正路径**

确保使用绝对路径，不要使用 `~` 或相对路径：

**正确**：
```bash
# Windows
APPROVED_DIRECTORY=C:\Users\YourName\claude-projects

# macOS/Linux
APPROVED_DIRECTORY=/Users/yourname/claude-projects
```

**错误**：
```bash
APPROVED_DIRECTORY=~/claude-projects  # 不要使用 ~
APPROVED_DIRECTORY=./projects         # 不要使用相对路径
```

### 8.4 Rate Limit 超限

**现象**：Bot 提示 "Rate limit exceeded"

**解决方案**：

1. **调整限流配置** (`.env`)

```bash
RATE_LIMIT_REQUESTS=20    # 增加到 20
RATE_LIMIT_WINDOW=60      # 保持 60 秒
```

2. **等待限流窗口过期**

通常等待 1 分钟后自动恢复。

3. **检查是否被滥用**

查看日志，确认没有意外的大量请求。

### 8.5 成本超限

**现象**：Bot 提示 "Cost limit exceeded"

**解决方案**：

1. **查看当前使用量**

在 Telegram 发送：
```
/status
```

2. **调整成本限制** (`.env`)

```bash
CLAUDE_MAX_COST_PER_USER=20.0  # 提高到 $20
```

3. **重置成本计数**

删除数据库文件（会清空所有会话历史）：

**Windows**:
```cmd
del C:\claude-telegram-bot\bot.db
```

**macOS/Linux**:
```bash
rm ~/claude-telegram-bot/bot.db
```

### 8.6 连接超时

**现象**：操作长时间无响应，最后提示超时

**解决方案**：

1. **增加超时时间** (`.env`)

```bash
CLAUDE_TIMEOUT_SECONDS=600  # 增加到 10 分钟
```

2. **检查网络稳定性**

```bash
ping anthropic.com -c 10
```

3. **分解复杂操作**

将大型任务拆分成多个小步骤。

### 8.7 常见错误代码

| 错误码 | 含义 | 解决方案 |
|-------|------|---------|
| 401 | Bot Token 无效 | 检查 `TELEGRAM_BOT_TOKEN` 配置 |
| 403 | 用户未授权 | 将 User ID 添加到 `ALLOWED_USERS` |
| 404 | Bot 不存在 | 确认 Bot 用户名正确 |
| 429 | 请求过于频繁 | 等待或调整 Rate Limit |
| 500 | 服务器内部错误 | 查看日志，可能是 Claude API 问题 |

---

## 九、高级配置

### 9.1 项目线程模式

为每个项目创建独立的 Telegram 话题。

**启用配置** (`.env`)：

```bash
ENABLE_PROJECT_THREADS=true
PROJECT_THREADS_MODE=private
PROJECTS_CONFIG_PATH=config/projects.yaml
```

**创建项目配置文件**：

创建 `config/projects.yaml`：

```yaml
projects:
  - name: "医工宝"
    path: "D:/01_Project/02_Personal/医工宝"
    description: "医疗设备管理系统"
  
  - name: "测试项目"
    path: "C:/Users/YourName/claude-projects/test-project"
    description: "用于测试的项目"
  
  - name: "个人网站"
    path: "C:/Users/YourName/claude-projects/my-website"
    description: "个人博客网站"
```

**同步线程**：

在 Telegram 发送：
```
/sync_threads
```

Bot 会为每个项目创建独立的话题，切换话题即切换项目。

### 9.2 Webhook 和自动化

接收 GitHub 等外部事件通知。

**启用配置** (`.env`)：

```bash
ENABLE_API_SERVER=true
API_SERVER_PORT=8080
GITHUB_WEBHOOK_SECRET=your_webhook_secret
WEBHOOK_API_SECRET=your_bearer_token
```

**GitHub Webhook 设置**：

1. 打开 GitHub 仓库 → Settings → Webhooks
2. 添加 Webhook：
   - URL: `http://你的IP:8080/webhooks/github`
   - Content type: `application/json`
   - Secret: 与 `GITHUB_WEBHOOK_SECRET` 相同
   - 选择事件：Push, Pull Request 等

**接收通知**：

当有 GitHub 事件时，Bot 会自动在 Telegram 发送通知。

### 9.3 定时任务

**启用配置** (`.env`)：

```bash
ENABLE_SCHEDULER=true
```

**使用场景**：
- 每日代码健康检查
- 定期生成报告
- 自动备份

具体配置需要修改 Bot 源码或使用外部调度工具。

### 9.4 工具白名单

限制 Claude 可使用的工具，提高安全性。

**配置** (`.env`)：

```bash
# 只允许读取和编辑，禁止执行命令
CLAUDE_ALLOWED_TOOLS=Read,Write,Edit,Glob,Grep

# 禁止特定工具（黑名单）
CLAUDE_DISALLOWED_TOOLS=Bash,Agent
```

**可用工具列表**：
- `Read`: 读取文件
- `Write`: 写入文件
- `Edit`: 编辑文件
- `Bash`: 执行命令
- `Glob`: 文件搜索
- `Grep`: 内容搜索
- `Agent`: 子代理
- 等等...

### 9.5 多用户管理

允许多个用户访问同一个 Bot。

**配置** (`.env`)：

```bash
# 逗号分隔多个 User ID
ALLOWED_USERS=123456789,987654321,456789123

# 每个用户独立的成本限制
CLAUDE_MAX_COST_PER_USER=5.0
```

**注意事项**：
- 每个用户有独立的会话历史
- 共享相同的项目目录访问权限
- 成本统计独立计算

---

## 十、安全最佳实践

### 10.1 保护 Bot Token

1. **不要分享 Token**

Bot Token 相当于密码，泄露后任何人都可以控制你的 Bot。

2. **Token 泄露后立即重置**

向 `@BotFather` 发送 `/mybots` → 选择 Bot → `API Token` → `Revoke current token`

3. **不要提交到代码仓库**

确保 `.env` 文件在 `.gitignore` 中：

```bash
echo .env >> .gitignore
```

### 10.2 限制访问范围

1. **严格设置 APPROVED_DIRECTORY**

只允许访问必要的目录：

```bash
# ✅ 正确：具体项目目录
APPROVED_DIRECTORY=C:\Users\YourName\work-projects

# ❌ 错误：系统根目录
APPROVED_DIRECTORY=C:\
```

2. **使用用户白名单**

只允许信任的用户访问：

```bash
ALLOWED_USERS=你的User_ID  # 只添加你自己
```

### 10.3 监控使用情况

1. **定期查看成本**

```
/status
```

2. **启用审计日志**

Bot 会自动记录所有操作，定期检查日志：

```bash
# 查看最近的操作
tail -f bot.log | grep "User action"
```

3. **设置成本告警**

配置合理的成本上限：

```bash
CLAUDE_MAX_COST_PER_USER=10.0  # $10 封顶
```

### 10.4 网络安全

1. **电脑防火墙**

确保只有必要的端口开放。

2. **定期更新**

保持 Bot 和依赖库最新：

```bash
pip install --upgrade git+https://github.com/RichardAtCT/claude-code-telegram@latest
```

3. **使用 HTTPS（Webhook 模式）**

如果启用 Webhook，建议使用反向代理（如 Nginx）配置 HTTPS。

---

## 十一、使用技巧

### 11.1 高效对话

**✅ 推荐**：

```
"在 src/api.py 中添加错误处理"
"重构 UserService 类，提取公共方法"
"运行所有测试并修复失败的用例"
```

**❌ 避免**：

```
"帮我看看代码"  # 太模糊
"有问题"        # 没有上下文
"改一下"        # 不清楚改什么
```

### 11.2 利用上下文

Bot 会记住会话历史：

```
你：读取 user.py
Bot：[显示文件内容]

你：把密码字段改成加密存储
Bot：[知道你在说 user.py，直接修改]

你：测试一下
Bot：[知道上下文，运行相关测试]
```

### 11.3 批量操作

```
你：列出所有 Python 文件中的 TODO 注释

你：将所有 .txt 文件转换为 .md 格式

你：检查项目中所有的安全漏洞
```

### 11.4 Git 操作

```
你：查看最近 5 次提交

你：创建新分支 feature/user-auth

你：查看当前分支的变更

你：提交所有变更，消息为 "Add user authentication"
```

### 11.5 调整详细程度

根据需求调整输出：

- **工作时**：`/verbose 0` 只看结果
- **学习时**：`/verbose 2` 看完整过程
- **调试时**：`/verbose 2` 了解 Claude 的思考过程

---

## 十二、常见问题 FAQ

### Q1: Bot 会上传我的代码到云端吗？

**答**：不会。Bot 运行在你的电脑上，Claude Code 在本地处理文件，只有 API 请求会发送到 Anthropic 服务器，代码本身不会上传。

### Q2: 关机后 Bot 还能用吗？

**答**：不能。Bot 必须在电脑开机且程序运行时才能工作。可以使用云服务器部署实现 7x24 小时运行。

### Q3: 可以多个人同时使用吗？

**答**：可以。在 `ALLOWED_USERS` 中添加多个 User ID 即可，每个用户有独立的会话。

### Q4: 如何备份会话历史？

**答**：会话数据存储在 `bot.db` 文件中，定期备份该文件即可：

```bash
cp bot.db bot.db.backup
```

### Q5: 支持哪些编程语言？

**答**：Claude Code 支持所有主流编程语言，包括但不限于：Python、JavaScript、Java、C++、Go、Rust 等。

### Q6: 如何卸载？

**答**：

```bash
# 卸载程序
pip uninstall claude-code-telegram

# 删除配置
rm -rf ~/claude-telegram-bot

# 删除 Bot（向 @BotFather 发送）
/deletebot
```

---

## 十三、参考资源

### 官方文档

- **Claude Code**: https://claude.ai/code
- **Anthropic API**: https://docs.anthropic.com/
- **Telegram Bot API**: https://core.telegram.org/bots/api
- **项目 GitHub**: https://github.com/RichardAtCT/claude-code-telegram

### 相关工具

- **Python 官网**: https://www.python.org/
- **Git 官网**: https://git-scm.com/
- **VSCode**: https://code.visualstudio.com/

### 社区支持

- **GitHub Issues**: https://github.com/RichardAtCT/claude-code-telegram/issues
- **Telegram 开发者社区**: https://t.me/BotDevelopment

---

## 附录A：完整配置示例

```bash
# ================================
# Telegram 配置
# ================================
TELEGRAM_BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
TELEGRAM_BOT_USERNAME=my_claude_code_bot

# ================================
# 安全配置
# ================================
ALLOWED_USERS=123456789
APPROVED_DIRECTORY=C:\Users\YourName\claude-projects

# ================================
# Claude 配置
# ================================
# ANTHROPIC_API_KEY=sk-ant-api03-xxxxx  # 可选
CLAUDE_MAX_COST_PER_USER=10.0
CLAUDE_TIMEOUT_SECONDS=300

# ================================
# 模式配置
# ================================
AGENTIC_MODE=true
VERBOSE_LEVEL=1

# ================================
# 限流配置
# ================================
RATE_LIMIT_REQUESTS=10
RATE_LIMIT_WINDOW=60

# ================================
# 功能开关
# ================================
ENABLE_GIT_INTEGRATION=true
ENABLE_FILE_UPLOADS=true
ENABLE_QUICK_ACTIONS=true

# ================================
# 高级功能（可选）
# ================================
# ENABLE_PROJECT_THREADS=false
# ENABLE_API_SERVER=false
# ENABLE_SCHEDULER=false

# ================================
# 工具控制（可选）
# ================================
# CLAUDE_ALLOWED_TOOLS=Read,Write,Edit,Bash,Glob,Grep
```

---

## 附录B：快速部署脚本

**Windows (setup.bat)**:

```batch
@echo off
echo === Claude Code Telegram Bot 快速部署 ===

echo 检查 Python...
python --version || (echo 请先安装 Python 3.11+ && exit /b 1)

echo 检查 Claude Code...
claude --version || (echo 请先安装 Claude Code CLI && exit /b 1)

echo 创建目录...
mkdir C:\claude-telegram-bot
mkdir C:\Users\%USERNAME%\claude-projects
cd C:\claude-telegram-bot

echo 安装 Bot...
pip install git+https://github.com/RichardAtCT/claude-code-telegram@v1.3.0

echo 下载配置模板...
curl -o .env https://raw.githubusercontent.com/RichardAtCT/claude-code-telegram/main/.env.example

echo.
echo === 部署完成！===
echo 请编辑 C:\claude-telegram-bot\.env 填入配置
echo 然后运行: cd C:\claude-telegram-bot ^&^& python -m claude_code_telegram
pause
```

**macOS/Linux (setup.sh)**:

```bash
#!/bin/bash
set -e

echo "=== Claude Code Telegram Bot 快速部署 ==="

echo "检查 Python..."
python3 --version || { echo "请先安装 Python 3.11+"; exit 1; }

echo "检查 Claude Code..."
claude --version || { echo "请先安装 Claude Code CLI"; exit 1; }

echo "创建目录..."
mkdir -p ~/claude-telegram-bot
mkdir -p ~/claude-projects
cd ~/claude-telegram-bot

echo "安装 Bot..."
pip3 install git+https://github.com/RichardAtCT/claude-code-telegram@v1.3.0

echo "下载配置模板..."
curl -o .env https://raw.githubusercontent.com/RichardAtCT/claude-code-telegram/main/.env.example

echo ""
echo "=== 部署完成！==="
echo "请编辑 ~/.claude-telegram-bot/.env 填入配置"
echo "然后运行: cd ~/claude-telegram-bot && python3 -m claude_code_telegram"
```

使用方式：

**Windows**:
```cmd
setup.bat
```

**macOS/Linux**:
```bash
chmod +x setup.sh
./setup.sh
```

---

## 文档更新记录

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2026-06-29 | 初始版本，完整部署手册 |

---

**文档结束**


