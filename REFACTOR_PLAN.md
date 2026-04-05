# 弦电子书改造计划 - 离线阅读器

## 目标概述

将弦电子书从一个支持手环设备同步的阅读应用，改造为一个功能完善的离线阅读器，保留核心阅读功能，移除所有手环设备通讯功能。

## 保留功能
1. 书籍管理模块（多格式导入、书籍删除、书籍分类、章节编辑）
2. 阅读体验模块（阅读界面、进度记录、阅读时间统计、书签功能、深色模式）
3. 数据备份模块（json导出、json导入、跨设备迁移）

## 删除功能
1. 设备通讯模块（手环连接、数据推送、进度同步、时间同步、书签同步）
2. 通知管理模块（传输进度通知、前台服务）

---

## 改造步骤

### 步骤 1: 删除手环设备通讯相关文件

#### 删除文件列表
1. `app/src/main/java/com/bandbbs/ebook/logic/` - 整个目录
   - `InterHandshake.kt`
   - `InterconnetFile.kt`
   - `Interconn.kt`
2. `app/src/main/java/com/bandbbs/ebook/notifications/` - 整个目录
   - `ForegroundTransferService.kt`
   - `LiveNotificationManager.kt`
3. `app/libs/xms-wearable-lib_1.4_release.aar` - 手环SDK库
4. 相关UI组件
   - `app/src/main/java/com/bandbbs/ebook/ui/components/ConnectionErrorBottomSheet.kt`
   - `app/src/main/java/com/bandbbs/ebook/ui/components/FirstSyncConfirmBottomSheet.kt`
   - `app/src/main/java/com/bandbbs/ebook/ui/components/SyncReadingDataBottomSheet.kt`
   - `app/src/main/java/com/bandbbs/ebook/ui/components/SyncReadingDataConfirmDialog.kt`
   - `app/src/main/java/com/bandbbs/ebook/ui/components/VersionIncompatibleDialog.kt`
   - `app/src/main/java/com/bandbbs/ebook/ui/components/UpdateCheckBottomSheet.kt`
   - `app/src/main/java/com/bandbbs/ebook/ui/components/IpCollectionPermissionDialog.kt`
5. 相关屏幕
   - `app/src/main/java/com/bandbbs/ebook/ui/screens/BandSettingsScreen.kt`
   - `app/src/main/java/com/bandbbs/ebook/ui/screens/SyncOptionsScreen.kt`
   - `app/src/main/java/com/bandbbs/ebook/ui/screens/PushScreen.kt`
6. ViewModel相关处理器
   - `app/src/main/java/com/bandbbs/ebook/ui/viewmodel/handlers/` 中与连接和推送相关的文件

#### 操作命令
```bash
rm -rf app/src/main/java/com/bandbbs/ebook/logic
rm -rf app/src/main/java/com/bandbbs/ebook/notifications
rm -f app/libs/xms-wearable-lib_1.4_release.aar
rm -f app/src/main/java/com/bandbbs/ebook/ui/components/ConnectionErrorBottomSheet.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/components/FirstSyncConfirmBottomSheet.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/components/SyncReadingDataBottomSheet.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/components/SyncReadingDataConfirmDialog.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/components/VersionIncompatibleDialog.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/components/UpdateCheckBottomSheet.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/components/IpCollectionPermissionDialog.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/screens/BandSettingsScreen.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/screens/SyncOptionsScreen.kt
rm -f app/src/main/java/com/bandbbs/ebook/ui/screens/PushScreen.kt
```

---

### 步骤 2: 修改 AndroidManifest.xml

#### 修改内容
- 移除手环相关权限
- 移除 ForegroundTransferService 声明
- 保留必要的存储和通知权限

#### 修改后的 AndroidManifest.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".App"
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.App"
        tools:targetApi="31">
        <activity
            android:name=".ui.activity.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.App">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />

                <data android:mimeType="text/plain" />
            </intent-filter>
        </activity>
        <activity
            android:name=".ui.activity.BookStatisticsActivity"
            android:exported="false"
            android:theme="@style/Theme.App" />
    </application>
</manifest>
```

---

### 步骤 3: 修改 App.kt

#### 修改内容
- 移除手环SDK初始化
- 移除通知管理器初始化（LiveNotificationManager）
- 保留PDFBox初始化

#### 修改后的 App.kt
```kotlin
package com.bandbbs.ebook

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
    }
}
```

---

### 步骤 4: 修改 build.gradle.kts

#### 修改内容
- 移除 xms-wearable-lib 依赖
- 保留其他必要依赖

#### 修改位置
删除或注释掉以下行：
```kotlin
implementation(files("./libs/xms-wearable-lib_1.4_release.aar"))?.let { implementation(it) }
```

---

### 步骤 5: 修改 MainActivity.kt

#### 需要修改的部分
1. 移除手环连接相关代码
2. 移除 PushScreen、SyncOptionsScreen、BandSettingsScreen 相关代码
3. 移除通知和前台服务相关代码
4. 移除文件传输相关逻辑
5. 简化屏幕导航

---

### 步骤 6: 修改 MainViewModel.kt

#### 需要修改的部分
1. 移除 ConnectionHandler、PushHandler
2. 移除连接状态、推送状态、同步数据状态
3. 移除手环设置相关状态
4. 移除更新检查和IP收集相关代码
5. 移除同步阅读数据功能
6. 简化删除书籍逻辑（只保留手机端删除）

---

### 步骤 7: 修改 SettingsScreen.kt

#### 需要修改的部分
1. 移除"设备"部分（手环端设置）
2. 移除"同步与连接"部分
3. 移除"更新与隐私"部分的联网相关设置
4. 保留"显示与交互"、"外观"、"高级"部分

---

### 步骤 8: 修改 MainScreen.kt

#### 需要修改的部分
1. 移除连接状态卡片
2. 移除存储信息卡片
3. 移除"更多选项"菜单中的同步和重连选项
4. 简化删除书籍对话框（只保留删除手机选项）
5. 移除同步阅读数据相关代码
6. 移除版本不兼容对话框
7. 移除连接错误提示

---

### 步骤 9: 清理 ViewModel 中的 Handlers

如果 ViewModel 使用了 handlers 目录下的处理器，需要：
1. 删除或简化 ConnectionHandler
2. 删除或简化 PushHandler
3. 保留 ImportHandler、LibraryHandler、CategoryHandler

---

### 步骤 10: 验证和测试

#### 测试清单
1. 应用能正常启动
2. 书籍导入功能正常
3. 书籍管理（删除、分类）功能正常
4. 章节编辑功能正常
5. 阅读功能正常
6. 书签功能正常
7. 阅读时间统计正常
8. 深色模式正常
9. 数据备份和恢复功能正常
10. 应用无崩溃

---

## 文件修改总结

### 删除的文件
- 3个逻辑层文件
- 2个通知相关文件
- 1个AAR库
- 6个UI组件
- 3个屏幕

### 修改的文件
1. `AndroidManifest.xml` - 权限和服务声明
2. `App.kt` - 应用初始化
3. `build.gradle.kts` - 依赖配置
4. `MainActivity.kt` - 主活动
5. `MainViewModel.kt` - 主视图模型
6. `SettingsScreen.kt` - 设置界面
7. `MainScreen.kt` - 主界面

---

## 注意事项

1. 确保在删除文件前先备份
2. 按步骤逐步修改，每步完成后编译检查
3. 测试每个功能模块确保正常工作
4. 保留的功能模块不要意外修改
5. 注意移除所有未使用的导入语句

---

## 预期结果

改造完成后，应用将成为一个功能完善的离线电子书阅读器，具有：
- ✅ 多格式电子书支持
- ✅ 书籍管理功能
- ✅ 章节编辑功能
- ✅ 流畅的阅读体验
- ✅ 阅读进度和时间统计
- ✅ 书签功能
- ✅ 深色/浅色主题
- ✅ 数据备份和恢复
- ❌ 无手环设备通讯功能
- ❌ 无前台传输服务
- ❌ 无联网更新检查
