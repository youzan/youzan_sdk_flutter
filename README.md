# youzan_sdk_flutter

这是有赞移动端 SDK（X5 Android / iOS）的 Flutter 插件包装器。它在有赞独立 App SDK 上提供了强大、可用于生产环境的 `PlatformView` 封装，赋予开发者在原生 Flutter Android 应用中无缝内嵌具备完整商业闭环能力的有赞微商城代码的能力。

## 核心特性

- **全局 SDK 调度**: 依托 `setupSDK` 初始化环境支持，并通过 `login` / `logout` 保护您的私域用户登录会话状态。
- **混合图层容器渲染**: 将深度集成的有赞 X5 内核 (`YouzanBrowserView`) 作为原生 Flutter 组件渲染，底层开启 Hybrid Composition (混合层渲染)，彻底解决诸如原生输入法键盘无法弹起或渲染丢帧错乱问题。
- **全链路 JSBridge 事件代理**: 开箱即用的原生交互体系映射。随时订阅 `AuthEvent`, `ShareEvent`, `WxPayEvent` 等内部协议栈，让你拥有完全自主在 Flutter 修改底层购物拦截的能力。
- **智能生命周期绑定**: 后台通过注册应用级别的生命周期回调来替补 Flutter 通讯落差，精准实现休眠暂停调度。
- **硬件设备接口代理**: 自带完整的 Activity `receiveFile` 重写，选取系统相册和图片零代码介入全自动放行。

## 安装指南

在工程中的 `pubspec.yaml` 补充下方引入项:

```yaml
dependencies:
  youzan_sdk_flutter: ^0.0.1
```

## 配置 & 初始化

在进入 UI 页面或者触发购买前，请必须先对有赞引擎发起预加载：

```dart
import 'package:youzan_sdk_flutter/youzan_sdk_flutter.dart';

void main() async {
  await YouzanSdkFlutter.setupSDK(
    clientId: 'YOUR_CLIENT_ID', // 您的开放平台授权标记
    appKey: 'YOUR_APP_KEY',     // 您的应用密钥
    debug: true,                // 是否开启有赞框架深度运行时日志和 Debug
  );
  runApp(MyApp());
}
```

对访客账号体系进行单点打通：

```dart
final result = await YouzanSdkFlutter.login(
  userId: '13777836524',
  nickName: 'Flutter User',
);
```

## 构建渲染 YouzanBrowserView

您可以轻而易举地把全套成品的企业级商城视图融合到任意 Flutter 页面路由或者片段中去：

```dart
import 'package:youzan_sdk_flutter/youzan_browser_view.dart';

YouzanBrowserView(
  initialUrl: "https://shopxxxxxx.m.youzan.com/wscshop/showcase/homepage",
  onBrowserCreated: (YouzanBrowserController controller) {
    // 拦截所有的底层硬件申请或者是内部预埋好的购物事件路由：
    controller.onEvent.listen((event) {
      if (event['eventName'] == 'jsBridgeEvent') {
         final type = event['params']?['type'];
         if (type == 'AuthEvent' || type == 'getUserInfo') {
             // 此处您可以自动调用上方的登录系统打入内部凭据，然后再放行...
             controller.reload(); // 令牌写入完成后更新 WebView 继续购买流程
         }
      }
    });
  },
)
```
