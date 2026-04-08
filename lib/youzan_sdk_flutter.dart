import 'dart:async';
import 'package:flutter/services.dart';

class YouzanSdkFlutter {
  static const MethodChannel _channel = MethodChannel('youzan_sdk_flutter');

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// 初始化有赞 SDK (代替旧版 initYouzan)
  static Future<Map<String, dynamic>> setupSDK({
    required String clientId,
    required String appKey,
    bool debug = false,
  }) async {
    final Map<dynamic, dynamic>? result =
        await _channel.invokeMethod('setupSDK', {
      'client_id': clientId,
      'app_key': appKey,
      'debug': debug,
    });
    return Map<String, dynamic>.from(result ?? {});
  }

  /// 有赞体系登录
  static Future<Map<String, dynamic>> login({
    required String userId,
    String avatar = "",
    String extra = "",
    String nickName = "",
    String gender = "0",
  }) async {
    final Map<dynamic, dynamic>? result = await _channel.invokeMethod('login', {
      'user_id': userId,
      'avatar': avatar,
      'extra': extra,
      'nick_name': nickName,
      'gender': gender,
    });
    return Map<String, dynamic>.from(result ?? {});
  }

  /// 统一注入全局静态路由拦截验证池，脱离异步劫障实现真正的原生纯判定
  static Future<void> registerInterceptRules({
    List<String> exactUrls = const [],
    List<String> hostAndPathUrls = const [],
  }) async {
    await _channel.invokeMethod('registerInterceptRules', {
      'exactUrls': exactUrls,
      'hostAndPathUrls': hostAndPathUrls,
    });
  }

  /// 有赞体系登出
  static Future<bool> logout() async {
    final Map<dynamic, dynamic>? result = await _channel.invokeMethod('logout');
    return result?['success'] == true;
  }

  /// 辅助方法：输出有赞系统底层日志
  static Future<void> nativeLog(String message) async {
    await _channel.invokeMethod('nativeLog', {'message': message});
  }

  /// 辅助方法：原生 Toast 提示
  static Future<void> toast(String message) async {
    await _channel.invokeMethod('toast', {'message': message});
  }
}
