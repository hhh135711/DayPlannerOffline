# 每日计划 - 纯离线 Android APP

功能：多模板编辑、开始时间顺延生成每日计划、单日任务和备注编辑、完成打卡、按日期查看历史记录。数据保存在应用私有 SQLite 数据库，无联网权限。

## 编译
1. 安装 Android Studio（推荐近期稳定版本，JDK 17）。
2. File > Open，选择本项目文件夹，等待 Gradle Sync（首次同步需联网下载构建依赖，但 APP 运行不需要网络）。
3. Build > Build Bundle(s) / APK(s) > Build APK(s)。
4. APK 通常生成于 app/build/outputs/apk/debug/app-debug.apk。

注意：卸载 APP 或清除应用数据会删除本地计划。第一版不含自动备份、提醒、跨日连续排程和任务拖拽排序。生成当天计划时如当天已有任务，系统会先提示覆盖风险。

当前交付是源码工程，不是已经验证编译成功的 APK。
