package com.example.youzan_sdk_flutter

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull

import com.youzan.androidsdk.InitConfig
import com.youzan.androidsdk.LogCallback
import com.youzan.androidsdk.YouzanLog
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdk.YouzanToken
import com.youzan.androidsdk.YzLoginCallback
import com.youzan.androidsdkx5.YouZanSDKX5Adapter

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** YouzanSdkFlutterPlugin */
class YouzanSdkFlutterPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener, Application.ActivityLifecycleCallbacks {
  private lateinit var channel : MethodChannel
  private var applicationContext: android.content.Context? = null
  
  var activityBinding: ActivityPluginBinding? = null
  var factory: YouzanBrowserFactory? = null

  var exactInterceptUrls = listOf<String>()
  var hostPathInterceptUrls = listOf<String>()

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    applicationContext = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "youzan_sdk_flutter")
    channel.setMethodCallHandler(this)
    
    // Register Platform View
    factory = YouzanBrowserFactory(flutterPluginBinding.binaryMessenger, this)
    flutterPluginBinding.platformViewRegistry.registerViewFactory("youzan_browser", factory!!)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activityBinding = binding
    binding.addActivityResultListener(this)
    binding.activity.application.registerActivityLifecycleCallbacks(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activityBinding?.activity?.application?.unregisterActivityLifecycleCallbacks(this)
    activityBinding?.removeActivityResultListener(this)
    activityBinding = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activityBinding = binding
    binding.addActivityResultListener(this)
    binding.activity.application.registerActivityLifecycleCallbacks(this)
  }

  override fun onDetachedFromActivity() {
    activityBinding?.activity?.application?.unregisterActivityLifecycleCallbacks(this)
    activityBinding?.removeActivityResultListener(this)
    activityBinding = null
  }

  // --- ActivityLifecycleCallbacks 监听，让原生直接感知生命周期 ---
  override fun onActivityResumed(activity: Activity) {
      if (activity === activityBinding?.activity) {
          factory?.activeViews?.forEach { it.browser.onResume() }
      }
  }

  override fun onActivityPaused(activity: Activity) {
      if (activity === activityBinding?.activity) {
          factory?.activeViews?.forEach { it.browser.onPause() }
      }
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
  override fun onActivityStarted(activity: Activity) {}
  override fun onActivityStopped(activity: Activity) {}
  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
  override fun onActivityDestroyed(activity: Activity) {}

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
    var handled = false
    factory?.activeViews?.forEach { view ->
        if (!handled && view.browser.receiveFile(requestCode, data)) {
            handled = true
        }
    }
    return handled
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
        "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")
        
        "setupSDK" -> {
            val clientId = call.argument<String>("client_id")
            val appKey = call.argument<String>("app_key")
            val debug = call.argument<Boolean>("debug") ?: false

            if (debug) {
                YouzanSDK.isDebug(true)
            }

            val config = InitConfig.builder()
                .clientId(clientId)
                .appkey(appKey)
                .adapter(YouZanSDKX5Adapter())
                .initCallBack { ready, message ->
                    result.success(mapOf("success" to ready, "message" to message, "code" to "success"))
                }
                .logCallback(object : LogCallback {
                    override fun onLog(eventType: String, message: String) {
                        Log.d("有赞 SDK", "log:$eventType:$message")
                    }
                })
                .build()

            applicationContext?.let { YouzanSDK.init(it, config) }
        }
        
        "login" -> {
            val userId = call.argument<String>("user_id") ?: ""
            val avatar = call.argument<String>("avatar") ?: ""
            val extra = call.argument<String>("extra") ?: ""
            val nickName = call.argument<String>("nick_name") ?: ""
            val gender = call.argument<String>("gender") ?: "0"

            YouzanSDK.yzlogin(
                userId, avatar, extra, nickName, gender,
                object : YzLoginCallback {
                    override fun onSuccess(youzanToken: YouzanToken) {
                        applicationContext?.let { YouzanSDK.sync(it, youzanToken) }
                        result.success(mapOf("success" to true, "yz_open_id" to youzanToken.yzOpenId, "message" to "登录成功"))
                    }

                    override fun onFail(message: String?, code: Int) {
                        result.success(mapOf("success" to false, "message" to (message ?: "登录失败"), "code" to code))
                    }
                }
            )
        }

        "logout" -> {
            applicationContext?.let { YouzanSDK.userLogout(it) }
            result.success(mapOf("success" to true))
        }

        "nativeLog" -> {
            val msg = call.argument<String>("message")
            YouzanLog.addLog("from Flutter ", "log:$msg")
            result.success(true)
        }

        "toast" -> {
            val msg = call.argument<String>("message") ?: "空"
            applicationContext?.let { Toast.makeText(it, msg, Toast.LENGTH_SHORT).show() }
            result.success(true)
        }
        
        "registerInterceptRules" -> {
            val exactUrls = call.argument<List<String>>("exactUrls") ?: emptyList()
            val hostPathUrls = call.argument<List<String>>("hostAndPathUrls") ?: emptyList()
            exactInterceptUrls = exactUrls
            hostPathInterceptUrls = hostPathUrls
            result.success(true)
        }

        else -> result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }
}
