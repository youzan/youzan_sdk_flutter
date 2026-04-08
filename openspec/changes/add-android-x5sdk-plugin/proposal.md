# Change: 集成有赞 Android X5SDK 及网页交互体系能力

## Why
目前 YouzanSDKFlutter 跨端底座尚未接入有赞官方体系内的 `x5sdk` 底层容器环境。业务方提出需集成包含 JSBridge 拦截、Web 生命周期控制、API 操作能力的核心业务体系引擎，以此来向 Flutter 端和上层容器应用暴露出例如初始化认证、商城下单流程回调等复杂能力支持。

## What Changes
- [新增] Android `build.gradle` 环境引入 `com.youzanyun.open.mobile:x5sdk:7.17.1`
- [新增] Flutter 侧对应 MethodChannel/EventChannel，包括 `setupSDK`, `login`, `loadUrl` 接口。
- [新增] JavaScript Bridge 消息机制及相关业务事件监听（如 `goBack`, `wxPay`, `addToCart`）等路由向 Flutter 层转发处理。
- [新增] 全局安全与缓存管理支持（`clearCookies`, `logout`）。

## Impact
- Affected specs: `x5-webview`
- Affected code: 
  - `android/build.gradle`
  - `android/src/main/kotlin/.../YouzanSdkFlutterPlugin.kt`
  - `lib/youzan_sdk_flutter.dart`
  - 示例工程 `example` 联调相关的代码
