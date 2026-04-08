# Project Context

## Purpose
该项目 `YouzanSDKFlutter` 是一个 Flutter 插件，用于将有赞原生应用引擎和组件体系（如 x5sdk 等）的能力透传和桥接到 Flutter 环境中。从而让使用 Flutter 开发的工程能够轻松集成有赞业务界面和功能组件。

## Tech Stack
- Frontend (Bridge): Flutter 2.x, Dart 2.16+
- Android: Kotlin, Gradle, X5WebView, `com.youzanyun.open.mobile:x5sdk:7.17.1`
- iOS: Swift, Cocoapods

## Project Conventions

### Code Style
- 遵循极简原则 (KISS)，不要过度工程化或防御性构建。
- **严禁**擅自对已经存在的存量代码进行格式化。只修改必要业务变更必须改动的行。
- 使用直观准确的命名，如能用一层级搞定绝不抽象出多层次封装。

### Architecture Patterns
- Flutter Plugin 跨端通道：采用单一/专用的 `MethodChannel` 模式，所有平台特性的封装（包括组件化 JS桥接、WebView回调代理等）优先在各自 Native 代码内部闭环后统一通过标准的 JSON 字典暴露。

### Testing Strategy
- 使用 `/example` 目录作为集成容器。任何基础方法的增加（如 `login`、`setupSDK`、`loadUrl`）必须在 example 代码内提供最简调用的代码。
- 原则上去除模拟数据带来的副作用，测试环境应该通过真实输入连跑。

### Git Workflow
- 基于 OpenSpec 的严格控制流：`proposal` -> `tasks` -> `review` -> `dev` -> `archive`
- 单个提案 (Feature Proposal) 需要关联单独的 Commit 或 PR，归档完成后主分支代表全局当前能力真理。

## Domain Context
- 有赞体系中，端内分为 JSBridge 层（解决内里 H5 网页回调/触发）和 Native SDK 层（配置初始化参数 `client_id`、登录凭证、Cookie 等）。在设计 Flutter API 时，我们要把这些事件和接口平稳对接到 Dart `Stream` 与 `Method` 调用上。
- Native Log Event 是一条通用通道，需要在 Flutter 获取以帮助类似 Uniapp 等上层调用方自己处理日志记录。

## Important Constraints
- 回复内容以及撰写实施任务相关的思维活动 **必须严格使用中文 (Chinese)**。
- 初始化 FVM 后在沙盒内极易存在由于全盘权限机制导致后台构建报错（如 FVM 全局缓存 `PathAccessException`）。因此当遇到终端命令权限问题，不要死磕命令执行，应指导业务方在具有读写全盘权限的外部主终端执行。

## External Dependencies
- 有赞 Android X5 SDK：`com.youzanyun.open.mobile:x5sdk:7.17.1`
- 业务方传递和初始化必备的：`client_id`, `app_key`, `user_id` 等开放平台核心凭证。
