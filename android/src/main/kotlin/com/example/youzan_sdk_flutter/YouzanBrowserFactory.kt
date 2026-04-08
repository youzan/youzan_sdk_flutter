package com.example.youzan_sdk_flutter

import android.content.Context
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class YouzanBrowserFactory(
    private val messenger: BinaryMessenger,
    private val plugin: YouzanSdkFlutterPlugin
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    
    val activeViews = mutableListOf<YouzanBrowserPlatformView>()

    override fun create(context: Context, id: Int, args: Any?): PlatformView {
        val creationParams = args as? Map<String?, Any?>
        android.util.Log.d("YouzanBrowserFactory", "🚀 [Flutter要求创建视图] id=$id, params=$creationParams")
        
        val view = YouzanBrowserPlatformView(context, id, creationParams, messenger, plugin)
        activeViews.add(view)
        
        android.util.Log.d("YouzanBrowserFactory", "✅ [视图已挂载] 当前内存中存活的 WebView 数量: ${activeViews.size}")
        return view
    }
}
