## 1. 基础环境搭建与依赖集成
- [x] 1.1 在 `android/build.gradle` 引入有赞 `x5sdk:7.17.1` (根据上下文视需引入相关依赖)。
- [x] 1.2 在 Dart 端搭建统一通信层，结合 `MethodChannel` 与 `EventChannel`。

## 2. API 接口底层桥接与能力导出 (Flutter -> Android)
- [x] 2.1 模块 API (YZModule级)：实现 `setupSDK(client_id, app_key, debug)`、`login(user_id, avatar, extra, nick_name, gender)` 及其异步回调映射，实现 `logout`。
- [x] 2.2 辅助杂项 API：实现辅助调试的 `log`, `nativeLog`, `toast` 方法。
- [x] 2.3 WebView 控制API：封装 `loadUrl`、`goBack`、`canGoBack`、`canGoForward`、`goForward`、`gobackWithStep` (参数 step)、`backListCount`、`reload`、`evaluateJavaScript` (参数 script)、`removeMessageHandler` (参数 name)、`currentUrl`、`sharePage`。并实现它们的回调/返回值格式(`{success: Boolean, message/response: ...}`)。

## 3. Webview Lifecycle / Action Events 监听暴露 (Android -> Flutter)
- [x] 3.1 监听原生 `WebChromeClient` 和 `WebViewClient` 的回调机制：向 Flutter 派发页面生命周期事件：`receivedTitle`, `webViewStartLoad`, `webViewFinishLoad`, `webViewLoadError`。
- [x] 3.2 封装基于 YouzanBrowser `subscribe()` 派发的集成 JS 通用事件库 `jsBridgeEvent` 给 Flutter：
  - **核心身份认证**：`AbsAuthEvent`, `AbsAccountCancelSuccessEvent`, `AbsAccountCancelFailEvent`, `AbsAuthorizationSuccessEvent`, `AbsAuthorizationErrorEvent`(携带 code, msg)。
  - **行为交互类**：`AbsShareEvent`(携带 title, desc, link, imgUrl), `AbsAddToCartEvent`(携带 goodsId, skuId, title), `AbsBuyNowEvent`(携带 goodsId, skuId, title), `AbsAddUpEvent`(携带 goodsList, totalCount), `AbsGoCashierEvent`(携带 orderNo, orderType)。
  - **支付与合规**：`AbsWxPayEvent`(携带 appId), `AbsPaymentFinishedEvent`(携带 payType), `PrivacyDisagreeProtocolEvent`。
  - **扩展能力**：接入 `AbsCustomEvent` 并暴露其 `action` 与 `data`。
  - **系统集成**：接入照片选取 `AbsChooserEvent` 与定位请求的验证逻辑。
- [x] 3.3 补齐 `example` Demo 中的页面测试代码以验证各项桥接流。
