import Flutter
import UIKit

public class SwiftYouzanSdkFlutterPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "youzan_sdk_flutter", binaryMessenger: registrar.messenger())
    let instance = SwiftYouzanSdkFlutterPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if (call.method == "getPlatformVersion") {
      result("iOS " + UIDevice.current.systemVersion)
    } else if (call.method == "initYouzan") {
      let args = call.arguments as? [String: Any]
      let _ = args?["clientId"] as? String // clientId
      // 在这里调用 iOS 有赞 AppSDK 初始化逻辑
      result(true)
    } else {
      result(FlutterMethodNotImplemented)
    }
  }
}
