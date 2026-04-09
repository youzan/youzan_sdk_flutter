# youzan_sdk_flutter

有赞移动端 SDK（X5 Android 内核）的 Flutter 插件封装。内置 Hybrid Composition 混合图层渲染层，原生支持 JSBridge 事件全链路拦截，以及智能 URL 路由拦截机制。

> 📌 当前版本仅支持 **Android**，iOS 端后续迭代跟进。

---

## 核心特性

| 能力 | 描述 |
|------|------|
| **SDK 全局调度** | `setupSDK` 初始化引擎，`login` / `logout` 管理用户登录态 |
| **Hybrid Composition 渲染** | X5 内核以原生混合图层方式内嵌，软键盘弹起无障碍，光标渲染正常 |
| **JSBridge 事件全代理** | `AuthEvent`、`ShareEvent`、`WxPayEvent`、`GoCashierEvent` 等开箱即用 |
| **URL 路由拦截** | 基于原生同步规则引擎，无死锁拦截指定链接并路由至 Flutter |
| **控制器 API 完整** | loadUrl、goBack、reload、evaluateJavaScript 等全覆盖 |
| **动态标题同步** | `receivedTitle` 事件自动同步 AppBar 标题 |

---

## 安装

在你的 `pubspec.yaml` 中添加依赖：

```yaml
dependencies:
  youzan_sdk_flutter: ^0.0.2
```

---

## 快速接入流程

### 第一步：初始化 SDK

在 `main()` 或首屏加载前调用，**必须在展示 WebView 之前完成**：

```dart
import 'package:youzan_sdk_flutter/youzan_sdk_flutter.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await YouzanSdkFlutter.setupSDK(
    clientId: 'YOUR_CLIENT_ID', // 有赞开放平台颁发的授权标识
    appKey:   'YOUR_APP_KEY',   // 应用密钥
    debug:    false,            // 正式环境请设为 false
  );

  runApp(const MyApp());
}
```

### 第二步：注册 URL 拦截规则（可选）

在 `setupSDK` 之后调用，用于在 WebView 内部跳转时拦截指定链接并路由至 Flutter 处理：

```dart
await YouzanSdkFlutter.registerInterceptRules(
  // 精确完整 URL 匹配
  exactUrls: [
    'https://example.com/some/exact/page',
  ],
  // host + path 匹配（忽略 queryString）
  hostAndPathUrls: [
    'https://shop123.youzan.com/wscuser/membercenter',
  ],
);
```

### 第三步：登录打通用户体系

```dart
final result = await YouzanSdkFlutter.login(
  userId:   '用户唯一标识',   // 必填
  nickName: '昵称',          // 可选
  avatar:   '头像 URL',      // 可选
  gender:   '1',             // 可选，'0'=未知 '1'=男 '2'=女
);

if (result['success'] == true) {
  print('登录成功');
}
```

### 第四步：渲染 WebView 页面

在任意页面的 Widget 树中嵌入 `YouzanBrowserView`：

```dart
import 'package:youzan_sdk_flutter/youzan_browser_view.dart';

class BrowserPage extends StatefulWidget {
  final String url;
  const BrowserPage({Key? key, required this.url}) : super(key: key);

  @override
  State<BrowserPage> createState() => _BrowserPageState();
}

class _BrowserPageState extends State<BrowserPage> {
  YouzanBrowserController? _controller;

  @override
  void dispose() {
    _controller?.dispose(); // 页面销毁时释放原生 WebView 资源
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('有赞商城'),
        actions: [
          IconButton(
            icon: const Icon(Icons.arrow_back_ios),
            onPressed: () async {
              final res = await _controller?.goBack();
              // goBack 返回 false 表示无历史记录，退出 Flutter 路由页
              if (res == null || res['success'] == false) {
                Navigator.of(context).pop();
              }
            },
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => _controller?.reload(),
          ),
        ],
      ),
      body: YouzanBrowserView(
        initialUrl: widget.url,
        hideLoading: false, // false=显示内核默认 loading，true=隐藏
        onBrowserCreated: (YouzanBrowserController controller) {
          _controller = controller;
          _listenEvents(controller);
        },
      ),
    );
  }

  void _listenEvents(YouzanBrowserController controller) {
    controller.onEvent.listen((event) {
      final String eventName = event['eventName'] ?? '';
      final dynamic params = event['params'];

      switch (eventName) {
        case 'jsBridgeEvent':
          final String type = params?['type'] ?? '';
          final dynamic data = params?['data'];

          if (type == 'UrlIntercepted') {
            // URL 命中拦截规则，在 Flutter 中打开新页面
            Navigator.of(context).push(MaterialPageRoute(
              builder: (_) => BrowserPage(url: data.toString()),
            ));
          }

          if (type == 'AuthEvent' || type == 'getUserInfo') {
            // 需要登录，自动注入凭证后重载页面
            YouzanSdkFlutter.login(userId: 'YOUR_USER_ID').then((res) {
              if (res['success'] == true) controller.reload();
            });
          }
          break;

        case 'receivedTitle':
          // 页面标题变化，可同步更新 AppBar
          print('标题: ${params?['title']}');
          break;

        case 'webViewStartLoad':
          print('开始加载');
          break;

        case 'webViewFinishLoad':
          print('加载完成');
          break;

        case 'webViewLoadError':
          print('加载失败: ${params?['code']} ${params?['message']}');
          break;
      }
    });
  }
}
```

---

## 完整 API 文档

### `YouzanSdkFlutter`（全局能力）

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `setupSDK` | `clientId`, `appKey`, `debug` | `Map<String, dynamic>` | 初始化有赞 SDK，全局调用一次 |
| `login` | `userId`, `nickName`, `avatar`, `gender`, `extra` | `Map<String, dynamic>` | 注入用户信息，打通登录态 |
| `logout` | — | `bool` | 清除当前用户凭证，退出登录 |
| `registerInterceptRules` | `exactUrls`, `hostAndPathUrls` | `void` | 注册原生 URL 同步拦截规则 |
| `toast` | `message` | `void` | 调用原生 Toast 提示 |
| `nativeLog` | `message` | `void` | 向有赞 SDK 日志系统写入自定义信息 |

---

### `YouzanBrowserView`（WebView 组件）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `initialUrl` | `String?` | `null` | WebView 启动时加载的首屏链接 |
| `hideLoading` | `bool` | `false` | 是否隐藏 X5 内核自带的 Loading 过渡动画 |
| `onBrowserCreated` | `Function(YouzanBrowserController)` | — | WebView 创建完成时回调，用于获取控制器 |

---

### `YouzanBrowserController`（WebView 控制器）

| 方法 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `onEvent` | — | `Stream<Map<String, dynamic>>` | 订阅 WebView 生命周期和 JSBridge 事件流 |
| `loadUrl` | `url: String` | `Map` | 主动加载指定 URL |
| `reload` | — | `Map` | 重新加载当前页面 |
| `goBack` | — | `Map` | 后退，返回 `success: false` 表示无历史可回退 |
| `goForward` | — | `Map` | 前进 |
| `canGoBack` | — | `bool` | 检测是否有返回历史 |
| `canGoForward` | — | `bool` | 检测是否有前进历史 |
| `gobackWithStep` | `step: int` | `Map` | 向后跳转指定步数 |
| `currentUrl` | — | `String` | 获取当前页面 URL |
| `evaluateJavaScript` | `script: String` | `Map` | 在当前页面执行 JS 脚本 |
| `setHideLoading` | `hideLoading: bool` | `void` | 运行时动态切换内核 Loading 显示状态 |
| `sharePage` | — | `Map` | 触发有赞分享事件 |
| `dispose` | — | `void` | 销毁并释放当前 WebView 实例资源 |

---

### JSBridge 事件类型一览

通过 `controller.onEvent.listen(...)` 订阅，`event['eventName']` 为事件名称：

| `eventName` | 说明 |
|-------------|------|
| `jsBridgeEvent` | 所有有赞业务事件的统一出口，通过 `params['type']` 区分 |
| `receivedTitle` | 页面标题更新，`params['title']` 为新标题 |
| `webViewStartLoad` | 网页开始加载 |
| `webViewFinishLoad` | 网页加载完成 |
| `webViewLoadError` | 网页加载失败，含 `code` 和 `message` |

`jsBridgeEvent` 下的 `type` 子类型：

| `type` | 说明 |
|--------|------|
| `AuthEvent` / `getUserInfo` | 页面要求用户登录 |
| `UrlIntercepted` | URL 命中拦截规则，`data` 为被拦截的完整 URL |
| `ShareEvent` | 用户触发分享，`data` 含 `title`, `desc`, `link`, `imgUrl` |
| `WxPayEvent` | 微信支付回调，`data` 含 `appId` |
| `GoCashierEvent` | 进入收银台，`data` 含 `orderNo`, `orderType` |
| `AddToCartEvent` | 加入购物车，`data` 含 `goodsId`, `skuId`, `title` |
| `BuyNowEvent` | 立即购买，`data` 含 `goodsId`, `skuId`, `title` |
| `PaymentFinishedEvent` | 支付完成，`data` 含 `payType` |
| `AccountCancelSuccess` | 注销账号成功 |
| `PrivacyDisagreeProtocol` | 用户拒绝隐私协议 |

---

## 常见问题

**Q: WebView 加载出来是白屏？**  
检查 `setupSDK` 是否在 `runApp` 之前完成，且 `clientId` 配置无误。查看 Logcat 搜索 `YouzanBrowserView` 标签确认拦截情况。

**Q: 软键盘无法弹出？**  
确认使用的是 Hybrid Composition 渲染（插件默认已开启），不要在外层包裹 `AbsorbPointer` 或 `GestureDetector`。

**Q: URL 拦截后新页面白屏？**  
这是正常现象，插件内部已实现"免死金牌"机制：被拦截的 URL 作为新页面 `initialUrl` 传入时，首次加载会无视拦截规则直接放行。

**Q: 如何在本地开发时使用源码而非 pub 发布包？**  
在 `pubspec.yaml` 中添加 `dependency_overrides`：

```yaml
dependency_overrides:
  youzan_sdk_flutter:
    path: ../  # 指向本地插件目录
```

---

## License

MIT © Youzan
