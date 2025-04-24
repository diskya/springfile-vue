# SpringFile Vue 项目 (中文版)

## 项目概述

这是一个全栈文件管理应用程序，具备文件分类和语义搜索功能。用户可以上传文件（PDF、DOCX、PNG 等），对文件进行分类，并根据文件内容进行智能搜索。

## 主要功能

*   **文件上传:** 支持上传多种文件类型 (PDF, DOCX, PNG)。
*   **文件分类:** 将文件组织到不同的分类和子分类中。
*   **语义搜索:** 基于文件内容的含义进行搜索，而不仅仅是关键词匹配。
*   **文件管理:** 查看、浏览和管理已上传的文件。

## 系统架构

项目采用面向微服务的架构：

1.  **`springfile-backend` (Spring Boot):**
    *   处理核心 API 逻辑（文件的增删改查、分类管理）。
    *   管理文件存储（本地或云存储）。
    *   与数据库交互（本地 H2 或 Cloud SQL）。
    *   与 FastAPI 服务通信，用于文件内容索引和搜索。
2.  **`springfile-fastapi` (FastAPI):**
    *   提供 AI 驱动的功能。
    *   从上传的文档（PDF, DOCX）中提取文本内容。
    *   使用 Sentence Transformers 生成文本嵌入 (Embeddings)。
    *   将嵌入存储在 ChromaDB 中，以实现高效的语义搜索。
    *   提供用于索引和搜索的 API 端点。
3.  **`springfile-vue-frontend` (Vue.js):**
    *   提供用户界面。
    *   使用 Vue 3, Vite, 和 Vue Router 构建。
    *   与 Spring Boot 后端 API 进行交互。

## Google Cloud 部署架构 (可选)

本项目可以部署到 Google Cloud Platform，利用以下服务：

*   **Cloud Run:**
    *   部署 `springfile-backend` Spring Boot 应用。
    *   部署 `springfile-fastapi` FastAPI 应用。
    *   提供可扩展、无服务器的容器化应用托管。
*   **Cloud SQL for PostgreSQL:**
    *   替代本地 H2 数据库，提供生产级的关系型数据库服务。
*   **Cloud Storage:**
    *   用于存储用户上传的文件，提供高可用性和可扩展性的对象存储。

*   *截图: Google Cloud Run 服务*
    ![Cloud Run 服务](img/Screenshot%202025-04-24%20160442.png)

## 应用截图

*   *截图 1: 文件管理界面*
    ![文件管理](img/Screenshot%202025-04-24%20155832.png)
*   *截图 2: 文件上传界面*
    ![文件上传](img/Screenshot%202025-04-24%20155905.png)
*   *截图 3: 搜索功能*
    ![搜索](img/Screenshot%202025-04-24%20160020.png)
*   *截图 4: 文件分类侧边栏*
    ![分类](img/Screenshot%202025-04-24%20160305.png)


## 本地安装与运行

**环境要求:**
*   Java 17+
*   Maven
*   Node.js & npm
*   Python 3.x & pip

**1. 后端 (`springfile-backend`):**
   ```bash
   # 进入后端目录
   cd springfile-backend
   # 构建项目 (下载依赖)
   ./mvnw install
   ```

**2. FastAPI 服务 (`springfile-fastapi`):**
   ```bash
   # 进入 FastAPI 目录
   cd springfile-fastapi
   # 创建虚拟环境 (推荐)
   python -m venv venv
   source venv/bin/activate # Windows 使用 `venv\Scripts\activate`
   # 安装依赖
   pip install -r requirements.txt
   ```

**3. 前端 (`springfile-vue-frontend`):**
   ```bash
   # 进入前端目录
   cd springfile-vue-frontend
   # 安装依赖
   npm install
   ```

## 启动应用 (本地)

1.  **启动 FastAPI 服务:**
    ```bash
    cd springfile-fastapi
    # 如果创建了虚拟环境，请确保已激活
    uvicorn main:app --reload --port 8001 # 或其他端口
    ```
2.  **启动 Spring Boot 后端:**
    ```bash
    cd springfile-backend
    ./mvnw spring-boot:run
    ```
    *(后端通常运行在 8080 端口)*
3.  **启动 Vue 前端:**
    ```bash
    cd springfile-vue-frontend
    npm run dev
    ```
    *(前端开发服务器通常运行在 5173 或类似端口)*

在浏览器中访问 Vite 开发服务器提供的 URL (例如 `http://localhost:5173`) 来使用应用。
