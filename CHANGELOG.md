## 0.0.1

* 初版向 pub.dev 发布。
* 实现全局原生安卓端 X5 SDK 环境搭建控制系统（含 `setupSDK` / `login` / `logout` 的调用映射）。
* 完成 `YouzanBrowserPlatformView` 底层建设且 100% 对齐原先基于 UniApp 开发所包含的所有原生节点能力。
* 实装了全面的业务链路拦截支持 (`AuthEvent`, `ShareEvent`, `WxPayEvent` 等重要 `JSBridge`），并通过事件通道实时对齐给 Flutter。
* 原生实现 Android 层文件图库调用 (`ActivityAware` OnActivityResult 相册照片选取的代理打通)。
* 将 App 视图全域的声明周期彻底静默融合原生系统状态监听。
