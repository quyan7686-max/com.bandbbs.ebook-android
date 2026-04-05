# 弦电子书 Code Wiki

## 1. 项目概述

### 1.1 项目简介
弦电子书（BandBBS_EBook）是一款专为小米手环等可穿戴设备设计的电子书管理和传输应用。该应用允许用户在手机端导入、管理多种格式的电子书，并将其传输到小米手环上进行阅读。

### 1.2 主要功能
- **多格式电子书导入**：支持TXT、EPUB、DOCX、PDF、MOBI、NVB等多种格式
- **智能分章**：提供多种分章方式，包括按标题识别、按字数分割等
- **手环通信**：通过小米可穿戴设备SDK与手环进行数据传输
- **阅读数据同步**：支持阅读进度、书签、阅读时长等数据的双向同步
- **书籍管理**：书籍分类、编辑、封面设置等功能
- **阅读统计**：记录阅读时长、进度等数据

### 1.3 项目信息
- **应用包名**：com.bandbbs.ebook.plus
- **版本**：V26.2.1 (versionCode: 126201)
- **最低SDK版本**：23 (Android 6.0)
- **目标SDK版本**：35
- **编译SDK版本**：36

---

## 2. 项目架构

### 2.1 整体架构图
```
┌─────────────────────────────────────────────────────────────────┐
│                           UI Layer (Compose)                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ Screens  │  │Components│  │  Bottom  │  │  Dialog  │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        ViewModel Layer                           │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                     MainViewModel                         │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │  │
│  │  │Import    │ │Library   │ │Push      │ │Connection│ │  │
│  │  │Handler   │ │Handler   │ │Handler   │ │Handler   │ │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Logic & Utils Layer                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │Interconn │  │ Parsers  │  │Chapter   │  │  Storage │    │
│  │  (Comm)  │  │(Epub/Txt)│  │Splitter  │  │  Utils   │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer (Room)                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                    │
│  │ BookDao  │  │ChapterDao│  │BookmarkDao│                    │
│  └──────────┘  └──────────┘  └──────────┘                    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块职责

#### 2.2.1 数据库模块 (`com.bandbbs.ebook.database`)
- **AppDatabase.kt**：Room数据库主类，管理书籍、章节、书签数据
- **BookEntity.kt**：书籍数据实体
- **BookDao.kt**：书籍数据访问对象
- **Chapter.kt**：章节数据实体
- **ChapterDao.kt**：章节数据访问对象
- **BookmarkEntity.kt**：书签数据实体
- **BookmarkDao.kt**：书签数据访问对象

#### 2.2.2 逻辑模块 (`com.bandbbs.ebook.logic`)
- **InterHandshake.kt**：实现与手环的握手协议和版本兼容检查
- **Interconn.kt**：基础通信连接类
- **InterconnetFile.kt**：文件传输相关功能

#### 2.2.3 UI模块 (`com.bandbbs.ebook.ui`)
- **activity/**：主Activity和统计Activity
- **components/**：各种底部对话框、对话框和自定义组件
- **screens/**：各个功能页面（主页、阅读器、设置等）
- **theme/**：应用主题和颜色定义
- **viewmodel/**：主ViewModel和状态管理
- **model/**：UI数据模型

#### 2.2.4 工具模块 (`com.bandbbs.ebook.utils`)
- **ChapterSplitter.kt**：章节分割工具，支持多种分章方式
- **BookInfoParser.kt**：书籍信息解析
- **EpubParser.kt**：EPUB格式解析
- **PdfParser.kt**：PDF格式解析
- **MobiParser.kt**：MOBI格式解析
- **DocxParser.kt**：DOCX格式解析
- **NvbParser.kt**：NVB格式解析
- **ChapterContentManager.kt**：章节内容管理
- **BookmarkManager.kt**：书签管理
- **DataBackupManager.kt**：数据备份和恢复
- **StorageUtils.kt**：存储相关工具
- **ReadingTimeStorage.kt**：阅读时长存储
- **VersionChecker.kt**：版本检查

#### 2.2.5 通知模块 (`com.bandbbs.ebook.notifications`)
- **ForegroundTransferService.kt**：前台传输服务
- **LiveNotificationManager.kt**：通知管理

---

## 3. 关键类与函数说明

### 3.1 核心类

#### 3.1.1 App.kt
应用入口类，继承自Application。
- **主要职责**：初始化PDFBox资源、通知管理器
- **关键属性**：
  - `conn: InterHandshake`：手环连接实例

#### 3.1.2 MainActivity.kt
主Activity，应用的核心界面容器。
- **主要职责**：
  - 管理应用导航
  - 处理文件选择和权限请求
  - 监听ViewModel状态变化
  - 管理前台服务
- **导航页面**：
  - `HomePager`：主页面（包含主页、统计、设置三个分页）
  - `BandSettings`：手环设置页面
  - `SyncOptions`：同步选项页面
  - `Push`：传输进度页面
  - `ChapterList`：章节列表页面
  - `Reader`：阅读器页面

#### 3.1.3 MainViewModel.kt
核心ViewModel，管理应用的所有业务逻辑和状态。
- **主要职责**：
  - 管理书籍列表和状态
  - 处理书籍导入、分章
  - 管理与手环的连接和数据传输
  - 处理章节编辑、阅读数据同步
- **关键状态**：
  - `books`：书籍列表
  - `connectionState`：连接状态
  - `pushState`：传输状态
  - `importState`：导入状态
- **关键Handler**：
  - `ConnectionHandler`：连接管理
  - `ImportHandler`：导入处理
  - `PushHandler`：推送处理
  - `LibraryHandler`：库管理
  - `CategoryHandler`：分类管理

#### 3.1.4 InterHandshake.kt
手环握手协议实现类。
- **主要职责**：
  - 实现与手环的握手协议
  - 版本兼容性检查
  - 连接状态管理
- **关键参数**：
  - `PHONE_VERSION_CODE = 126201`：手机端版本号
  - `MIN_BAND_VERSION_CODE = 260302`：最低手环版本要求
- **关键函数**：
  - `sendMessage(message: String)`：发送消息到手环
  - `init()`：初始化连接

#### 3.1.5 AppDatabase.kt
Room数据库类。
- **主要职责**：管理应用的本地数据存储
- **数据库版本**：4
- **实体**：
  - `BookEntity`：书籍
  - `Chapter`：章节
  - `BookmarkEntity`：书签
- **DAO访问**：
  - `bookDao()`：获取书籍DAO
  - `chapterDao()`：获取章节DAO
  - `bookmarkDao()`：获取书签DAO

### 3.2 关键工具类

#### 3.2.1 ChapterSplitter.kt
章节分割工具类，提供多种分章方式。
- **分章方法**：
  - `METHOD_DEFAULT`：默认（第X章/卷/节/部/篇/回/番外…）
  - `METHOD_DEFAULT_LOOSE`：默认-宽松
  - `METHOD_CHAPTER`：英文（Chapter X）
  - `METHOD_ZH_NUM_DOT`：中文数字（一、 二.）
  - `METHOD_DIGIT_DOT`：阿拉伯数字（1. 2、）
  - `METHOD_BY_WORD_COUNT`：按字数分章
  - `METHOD_CUSTOM`：自定义正则表达式
- **关键函数**：
  - `split()`：从URI读取并分章
  - `splitFromText()`：从文本内容分章
  - `readTextFromUri()`：读取URI文本内容，自动检测编码

#### 3.2.2 解析器类
- **EpubParser.kt**：解析EPUB格式电子书
- **PdfParser.kt**：解析PDF格式电子书
- **MobiParser.kt**：解析MOBI格式电子书
- **DocxParser.kt**：解析DOCX格式电子书
- **NvbParser.kt**：解析NVB格式电子书

---

## 4. 依赖关系

### 4.1 主要依赖库

| 依赖库 | 版本 | 用途 |
|--------|------|------|
| Kotlin | 2.2.0 | 主要编程语言 |
| Jetpack Compose | - | UI框架 |
| Room | 2.8.4 | 本地数据库 |
| Coroutines | 1.10.2 | 异步编程 |
| Kotlinx Serialization | 1.10.0 | JSON序列化 |
| Navigation3 | 1.0.1 | 导航库 |
| Miuix KMP | 0.8.5 | UI组件库 |
| Coil | 2.7.0 | 图片加载 |
| PDFBox Android | 2.0.27.0 | PDF解析 |
| Jsoup | 1.22.1 | HTML解析 |
| Juniversalchardet | 2.5.0 | 编码检测 |
| RE2J | 1.8 | 正则表达式 |
| xms-wearable-lib | 1.4 | 小米可穿戴SDK |

### 4.2 模块依赖关系

```
MainActivity
    ├── MainViewModel
    │       ├── ConnectionHandler
    │       │       └── InterHandshake
    │       │               └── Interconn
    │       ├── ImportHandler
    │       │       ├── ChapterSplitter
    │       │       ├── EpubParser
    │       │       ├── PdfParser
    │       │       ├── MobiParser
    │       │       ├── DocxParser
    │       │       └── NvbParser
    │       ├── PushHandler
    │       ├── LibraryHandler
    │       └── CategoryHandler
    └── AppDatabase
            ├── BookDao
            ├── ChapterDao
            └── BookmarkDao
```

---

## 5. 项目运行方式

### 5.1 环境要求
- Android Studio Hedgehog或更高版本
- JDK 11或更高版本
- Android SDK API 36
- Gradle 8.x

### 5.2 构建步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd BandBBS_EBook
   ```

2. **配置签名**
   在项目根目录创建 `keystore.properties` 文件，内容如下：
   ```properties
   storeFile=path/to/your/keystore.jks
   storePassword=your-store-password
   keyAlias=your-key-alias
   keyPassword=your-key-password
   ```

3. **同步Gradle**
   在Android Studio中打开项目，等待Gradle同步完成

4. **构建应用**
   ```bash
   # Debug构建
   ./gradlew assembleDebug
   
   # Release构建
   ./gradlew assembleRelease
   ```

5. **安装应用**
   ```bash
   # 安装Debug版本
   ./gradlew installDebug
   ```

### 5.3 运行要求
- Android 6.0 (API 23) 或更高版本的设备
- 小米手环8或更高版本设备（用于传输功能）
- 蓝牙权限
- 通知权限（Android 13+）

---

## 6. 核心功能流程

### 6.1 书籍导入流程

```
用户选择文件
    ↓
MainViewModel.startImport()
    ↓
ImportHandler 检测文件类型
    ↓
显示导入配置对话框（书名、分章方式等）
    ↓
用户确认
    ↓
ChapterSplitter.split() 分章
    ↓
保存到AppDatabase
    ↓
保存章节内容到文件系统
    ↓
完成导入
```

### 6.2 书籍传输流程

```
用户点击传输书籍
    ↓
MainViewModel.startPush()
    ↓
PushHandler 检查连接状态
    ↓
显示同步选项对话框（选择章节）
    ↓
用户确认
    ↓
建立与手环的文件传输连接
    ↓
传输书籍信息和封面
    ↓
逐章传输章节内容
    ↓
更新传输进度UI
    ↓
完成传输
```

### 6.3 阅读数据同步流程

```
用户点击同步数据
    ↓
MainViewModel.syncAllReadingData()
    ↓
选择同步模式（自动/手环单向/手机单向）
    ↓
逐本书处理：
    ├─ 获取手环端阅读数据
    ├─ 获取手机端阅读数据
    ├─ 根据同步模式合并数据
    └─ 保存更新
    ↓
显示同步结果
```

---

## 7. 数据库设计

### 7.1 书籍表 (books)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Int | 主键，自增 |
| name | String | 书名 |
| path | String | 文件路径 |
| size | Long | 文件大小 |
| format | String | 文件格式（默认txt） |
| coverImagePath | String? | 封面图片路径 |
| author | String? | 作者 |
| summary | String? | 简介 |
| bookStatus | String? | 书籍状态 |
| category | String? | 分类 |
| localCategory | String? | 本地分类 |

### 7.2 章节表 (chapters)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Int | 主键，自增 |
| bookId | Int | 所属书籍ID |
| index | Int | 章节索引 |
| name | String | 章节名称 |
| contentFilePath | String | 内容文件路径 |
| wordCount | Int | 字数 |

### 7.3 书签表 (bookmarks)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | Int | 主键，自增 |
| bookId | Int | 所属书籍ID |
| chapterId | Int | 所属章节ID |
| position | Int | 位置 |
| note | String? | 备注 |
| createTime | Long | 创建时间 |

---

## 8. 关键技术点

### 8.1 Jetpack Compose UI
- 完全使用Compose构建UI
- 使用Miuix KMP组件库
- 支持Material You动态配色

### 8.2 协程与Flow
- 使用Kotlin协程处理异步操作
- 使用StateFlow管理UI状态
- 使用Room的Flow支持响应式数据库查询

### 8.3 手环通信
- 使用小米可穿戴SDK (xms-wearable-lib)
- 实现自定义握手协议
- 支持文件传输和数据同步

### 8.4 电子书解析
- 支持多种电子书格式
- 使用PDFBox处理PDF
- 使用Jsoup解析HTML/EPUB
- 自动检测文本编码

---

## 9. 版本历史

| 版本 | 版本号 | 说明 |
|------|--------|------|
| V26.2.1 | 126201 | 当前版本 |

---

## 10. 参考资源

- [喵喵电子书多端设计稿](https://mastergo.com/goto/KWzbQtxB?file=165290124574010)
- [弦电子书Vela客户端](https://github.com/youshen2/com.bandbbs.ebook)
- [小米快应用官方文档](https://iot.mi.com/vela/quickapp)

---

*文档生成时间：2026-04-05*
