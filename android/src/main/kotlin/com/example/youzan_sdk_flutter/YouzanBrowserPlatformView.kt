package com.example.youzan_sdk_flutter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.youzan.androidsdk.event.*
import com.youzan.androidsdk.model.cashier.GoCashierModel
import com.youzan.androidsdk.model.goods.GoodsOfCartModel
import com.youzan.androidsdk.model.goods.GoodsOfSettleModel
import com.youzan.androidsdk.model.goods.GoodsShareModel
import com.youzan.androidsdk.model.goods.WxPayModel
import com.youzan.androidsdk.model.trade.TradePayFinishedModel
import com.youzan.androidsdkx5.YouzanBrowser
import com.youzan.androidsdkx5.compat.CompatWebChromeClient
import com.youzan.androidsdkx5.compat.VideoCallback
import com.youzan.androidsdkx5.compat.WebChromeClientConfig
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class YouzanBrowserPlatformView(
    val context: Context,
    val id: Int,
    creationParams: Map<String?, Any?>?,
    messenger: BinaryMessenger,
    val plugin: YouzanSdkFlutterPlugin
) : PlatformView, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

    // ==== 核心架构级修复 ====
    // 1. Flutter 框架 (JNI层) 在热重启回收时，严苛要求 getView() 返回的顶层 View 必须是它下发的包裹 Context。
    // 2. 有赞的 X5 内核又极其娇贵，如果不用真实的 Activity Context，它就会拒绝渲染纹理，导致彻底的白屏。
    // 方案：造一个符合 Flutter 规矩的 FrameLayout 容器，再把注入了真正 Activity 的 WebView 包进去！
    private val container = android.widget.FrameLayout(context)
    val browser: YouzanBrowser = YouzanBrowser(plugin.activityBinding?.activity ?: context)
    
    private val methodChannel: MethodChannel
    private val eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null

    // 修复：记录主动要求加载的免死金牌 URL，防止新打开的页面立刻被自身的同步拦截器截杀导致白屏（因为它的初始链接就是刚才被拦截的目标 URL！）
    private var explicitLoadUrl: String? = null

    init {
        setupYouzanBrowser()
        methodChannel = MethodChannel(messenger, "youzan_browser_$id")
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(messenger, "youzan_browser_events_$id")
        eventChannel.setStreamHandler(this)

        val url = creationParams?.get("url") as? String
        Log.d("YouzanBrowserView", "📌 [初始化] 分配 ID=$id, 即将加载首屏 URL=$url")
        if (!url.isNullOrEmpty()) {
            explicitLoadUrl = url
            // 不要在此处过早加载，抛给主线程等挂载完毕后再加载，防止白屏死锁
            container.post {
                browser.loadUrl(url)
            }
        }

        // 动态读取来自 Flutter 组件控制层的属性设定
        val hideLoading = creationParams?.get("hideLoading") as? Boolean ?: false
        browser.needLoading(!hideLoading) // 取反传入机制


        // 装填入兼容层容器
        container.addView(browser, android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    override fun getView(): View = container

    override fun dispose() {
        try {
            container.removeAllViews()
            browser.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        plugin.factory?.activeViews?.remove(this)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        this.eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        this.eventSink = null
    }

    private fun fireEvent(eventName: String, params: Map<String, Any?>? = null) {
        val payload = mutableMapOf<String, Any>("eventName" to eventName)
        params?.let { payload["params"] = it }
        eventSink?.success(payload)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "loadUrl" -> {
                val url = call.argument<String>("url")
                Log.d("YouzanBrowserView", "📌 [Dart 主动调用加载]: URL=$url")
                if (!url.isNullOrEmpty()) {
                    explicitLoadUrl = url
                    browser.loadUrl(url)
                }
                result.success(mapOf("success" to true))
            }
            "goBack" -> {
                val can = browser.canGoBack()
                if (can) browser.goBack()
                result.success(mapOf("success" to can))
            }
            "canGoBack" -> result.success(browser.canGoBack())
            "canGoForward" -> result.success(browser.canGoForward())
            "goForward" -> {
                val can = browser.canGoForward()
                if (can) browser.goForward()
                result.success(mapOf("success" to can))
            }
            "gobackWithStep" -> {
                val step = call.argument<Int>("step") ?: 1
                val copyList = browser.copyBackForwardList()
                val targetIndex = copyList.currentIndex - step
                if (targetIndex >= 0 && targetIndex < copyList.size) {
                    browser.goBackOrForward(-step)
                    result.success(mapOf("success" to true))
                } else {
                    result.success(mapOf("success" to false))
                }
            }
            "reload" -> {
                browser.reload()
                result.success(mapOf("success" to true))
            }
            "setHideLoading" -> {
                val hideLoading = call.argument<Boolean>("hideLoading") ?: true
                browser.needLoading(!hideLoading)
                result.success(mapOf("success" to true))
            }
            "currentUrl" -> result.success(browser.url ?: "")
            "sharePage" -> {
                browser.sharePage()
                result.success(mapOf("success" to true))
            }
            "dispose" -> {
                this.dispose()
                result.success(true)
            }
            else -> result.notImplemented()
        }
    }

    private fun setupYouzanBrowser() {
        if (browser.getX5WebViewExtension() != null) {
            val data = Bundle()
            data.putBoolean("standardFullScreen", true)
            data.putBoolean("supportLiteWnd", true)
            data.putInt("DefaultVideoScreen", 2)
            browser.getX5WebViewExtension().invokeMiscMethod("setVideoParams", data)
        }


        browser.setWebChromeClient(
            object : CompatWebChromeClient(
                WebChromeClientConfig(true, object : VideoCallback {
                    override fun onVideoCallback(b: Boolean) {}
                })
            ) {
                override fun onReceivedTitle(p0: WebView?, p1: String?) {
                    super.onReceivedTitle(p0, p1)
                    fireEvent("receivedTitle", mapOf("title" to p1))
                }
                override fun onGeolocationPermissionsShowPrompt(origin: String?, geolocationCallback: GeolocationPermissionsCallback?) {}
                override fun onGeolocationPermissionsHidePrompt() {}
            }
        )

        browser.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(webView: WebView?, url: String?) {
                super.onPageFinished(webView, url)
                fireEvent("webViewFinishLoad")
            }

            override fun onPageStarted(webView: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(webView, url, favicon)
                fireEvent("webViewStartLoad")
            }

            override fun onReceivedError(webView: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                super.onReceivedError(webView, errorCode, description, failingUrl)
                fireEvent("webViewLoadError", mapOf("code" to errorCode, "message" to description))
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false
                Log.d("YouzanBrowserView", "🔍 [底网跳转嗅探] 发起 URL: $url")

                // ======== 核心修复 ========
                // 如果这是由 Flutter 刚刚指令 (初始化加载 / loadUrl) 明确要求加载的页面，
                // 那么无视一切规则绝对放行一次，同时清空免死金牌，以防止出现【新页面打开发白屏】（因为如果目标 URL 在拦截规则内，会被无限拦截无法出始化框架！）
                if (explicitLoadUrl != null && url == explicitLoadUrl) {
                    Log.d("YouzanBrowserView", "🟢 [主动放行命中] 绕过自杀拦截进入正常流: $url")
                    explicitLoadUrl = null
                    return false
                }

                // 方案更新：通过匹配 Plugin 级全局下发的白名单规则做本地纯原生死锁豁免级的直接同步拦截
                var shouldIntercept = false
                
                // 1. 完整拼写匹配
                if (plugin.exactInterceptUrls.contains(url)) {
                    shouldIntercept = true
                } else {
                    // 2. 忽略 queryString 的 host + path 匹配
                    try {
                        val uri = android.net.Uri.parse(url)
                        val hostPath = "${uri.scheme}://${uri.host}${uri.path}"
                        if (plugin.hostPathInterceptUrls.contains(hostPath)) {
                            shouldIntercept = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (shouldIntercept) {
                    // 若同步判定被拦截，将触发通知派发回给 Flutter
                    Log.d("YouzanBrowserView", "🛑 [原生态阻断抛出] 这条链路命中商户要求，已经截断并将发给 Dart -> $url")
                    fireEvent("jsBridgeEvent", mapOf("type" to "UrlIntercepted", "data" to url))
                    return true 
                }
                
                Log.d("YouzanBrowserView", "➡️ [正常放行] URL: $url")
                return false
            }
        })

        browser.setOnlyWebRegionLoadingShow(true)
        initRegisterEvent()
    }

    private fun initRegisterEvent() {
        browser.subscribe(object : AbsAuthEvent() {
            override fun call(context: Context, needLogin: Boolean) {
                fireEvent("jsBridgeEvent", mapOf("type" to "AuthEvent"))
            }
        })
        
        browser.subscribe(object : AbsChooserEvent() {
            override fun call(context: Context, intent: Intent, requestCode: Int) {
                plugin.activityBinding?.activity?.startActivityForResult(intent, requestCode)
            }
        })

        browser.subscribe(object : AbsShareEvent() {
            override fun call(context: Context, data: GoodsShareModel) {
                val params = mapOf(
                    "title" to data.title,
                    "desc" to data.desc,
                    "link" to data.link,
                    "imgUrl" to data.imgUrl
                )
                fireEvent("jsBridgeEvent", mapOf("type" to "ShareEvent", "data" to params))
            }
        })

        browser.subscribe(object : AbsAddToCartEvent() {
            override fun call(p0: Context?, data: GoodsOfCartModel?) {
                val params = data?.let { mapOf("goodsId" to it.itemId, "skuId" to it.skuId, "title" to it.title) }
                fireEvent("jsBridgeEvent", mapOf("type" to "AddToCartEvent", "data" to params))
            }
        })

        browser.subscribe(object : AbsBuyNowEvent() {
            override fun call(p0: Context?, data: GoodsOfCartModel?) {
                val params = data?.let { mapOf("goodsId" to it.itemId, "skuId" to it.skuId, "title" to it.title) }
                fireEvent("jsBridgeEvent", mapOf("type" to "BuyNowEvent", "data" to params))
            }
        })

        browser.subscribe(object : AbsWxPayEvent() {
            override fun call(p0: Context?, data: WxPayModel?) {
                val params = data?.let { mapOf("appId" to it.appId) }
                fireEvent("jsBridgeEvent", mapOf("type" to "WxPayEvent", "data" to params))
            }
        })

        browser.subscribe(object : AbsAddUpEvent() {
            override fun call(p0: Context?, data: MutableList<GoodsOfSettleModel>?) {
                val goodsList = data?.map { mapOf("goodsId" to it.itemId, "skuId" to it.skuId, "title" to it.title) } ?: emptyList()
                fireEvent("jsBridgeEvent", mapOf("type" to "AddUpEvent", "data" to mapOf("goodsList" to goodsList, "totalCount" to (data?.size ?: 0))))
            }
        })

        browser.subscribe(object : AbsAccountCancelSuccessEvent(null) {
            override fun call(p0: Context?) = fireEvent("jsBridgeEvent", mapOf("type" to "AccountCancelSuccess"))
        })

        browser.subscribe(object : AbsAccountCancelFailEvent(null) {
            override fun call(p0: Context?) = fireEvent("jsBridgeEvent", mapOf("type" to "AccountCancelFail"))
        })

        browser.subscribe(object : AbsAuthorizationSuccessEvent() {
            override fun call(p0: Context?) = fireEvent("jsBridgeEvent", mapOf("type" to "AuthorizationSuccess"))
        })

        browser.subscribe(object : AbsAuthorizationErrorEvent() {
            override fun call(p0: Context?, p1: Int, p2: String?) = fireEvent("jsBridgeEvent", mapOf("type" to "AuthorizationError", "data" to mapOf("code" to p1, "msg" to p2)))
        })

        browser.subscribe(object : PrivacyDisagreeProtocolEvent(null) {
            override fun call(p0: Context?): Boolean {
                fireEvent("jsBridgeEvent", mapOf("type" to "PrivacyDisagreeProtocol"))
                return false
            }
        })

        browser.subscribe(object : AbsGoCashierEvent() {
            override fun call(data: GoCashierModel?) {
                val params = data?.let { mapOf("orderNo" to it.orderNo, "orderType" to it.orderType) }
                fireEvent("jsBridgeEvent", mapOf("type" to "GoCashierEvent", "data" to params))
            }
        })

        browser.subscribe(object : AbsPaymentFinishedEvent() {
            override fun call(context: Context, tradePayFinishedModel: TradePayFinishedModel) {
                fireEvent("jsBridgeEvent", mapOf("type" to "PaymentFinishedEvent", "data" to mapOf("payType" to tradePayFinishedModel.payType)))
            }
        })

        browser.subscribe(object : AbsCustomEvent() {
            override fun callAction(context: Context, action: String, data: String) {
                fireEvent("jsBridgeEvent", mapOf("type" to "CustomEvent_$action", "data" to data))
            }
        })
    }
}
