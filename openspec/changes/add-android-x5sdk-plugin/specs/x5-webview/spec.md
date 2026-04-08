## ADDED Requirements

### Requirement: SDK Initialization and Authentication
The plugin MUST provide programmatic initialization and user authentication interfaces extending from `YZModule` behaviors.

#### Scenario: Developer initializing the SDK
- **WHEN** `setupSDK` is called with params (`client_id`, `app_key`, `debug`)
- **THEN** SDK configures the debug flag, sets up the `YouZanSDKX5Adapter` environment, and executes the callback with `{"success": true|false, "message": message, "code": "success"}`.

#### Scenario: User login processing
- **WHEN** `login` is called with user data (`user_id`, `avatar`, `extra`, `nick_name`, `gender`)
- **THEN** it synchronizes the session via `YouzanSDK.sync` and executes the callback returning `{"success": true, "yz_open_id": openId, "message": "登录成功"}` upon success, or `{"success": false, "message": err, "code": code}` upon failure.

#### Scenario: User logout
- **WHEN** `logout` is invoked
- **THEN** it must call `YouzanSDK.userLogout` and return success.

### Requirement: WebView Basic Navigation and Control
The system SHALL expose standardized APIs matching `YouzanBrowserComponent` capabilities to control the internal X5 WebView core.

#### Scenario: Load URL action
- **WHEN** `loadUrl` or `url` is requested
- **THEN** the WebView navigates to that destination.

#### Scenario: Check historic navigation boundaries
- **WHEN** `canGoBack` or `canGoForward` is requested
- **THEN** emit via callback a straightforward boolean result specifying whether internal historical tracking permits the motion.

#### Scenario: Back / Forward / Multi back
- **WHEN** `goBack`, `goForward` or `gobackWithStep` (with integer `step`) is triggered
- **THEN** safely iterate over `copyBackForwardList()` validation and traverse accordingly. A `{success, message}` response payload is fired indicating correctness.

### Requirement: Advanced Execution and Tools
Provide the necessary runtime tools bridging Native to JS.

#### Scenario: Script Injection
- **WHEN** `evaluateJavaScript` is performed
- **THEN** return a `response` value or an `error` description.

#### Scenario: App Level Tracing
- **WHEN** `nativeLog`, `log`, or `toast` routines are instantiated
- **THEN** Native Android layer intercepts the values triggering OS level toasts or the underlying default Logcat tracing systems.

### Requirement: YouzanBrowser Event Subscriptions (jsBridgeEvent)
The bridging component SHALL register multiple `AbsXxxEvent` subscribers, wrapping payloads explicitly matching internal schemas and routing them as `jsBridgeEvent` (with dynamic event details). 

#### Scenario: Product behaviors handling
- **WHEN** events like `AbsAddToCartEvent` or `AbsBuyNowEvent` occurs
- **THEN** bundle parameters `{ goodsId, skuId, title }` and dispatch upward to Flutter.

#### Scenario: Transaction finalization and payment
- **WHEN** payment is processed and calls `AbsPaymentFinishedEvent`, `AbsGoCashierEvent`, or `AbsWxPayEvent`
- **THEN** forward mapping fields such as `{ payType }` or `{ orderNo, orderType }` appropriately to allow Flutter application to synchronize UI state.

### Requirement: Android WebView Action Logs
The standard WebChromeClient/WebViewClient overrides shall map their lifecycle to Flutter scope.

#### Scenario: Navigation Load Tracking
- **WHEN** native views hook into `onPageStarted` / `onPageFinished`
- **THEN** broadcast `{ webViewStartLoad }` and `{ webViewFinishLoad }` empty markers respectively.

#### Scenario: Exception Tracking
- **WHEN** an `onReceivedError` strikes
- **THEN** formulate `{ code, message }` parameters payload nested into the `webViewLoadError` event dispatch channel.
