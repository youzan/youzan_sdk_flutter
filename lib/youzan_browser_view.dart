import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';

typedef YouzanBrowserCreatedCallback = void Function(YouzanBrowserController controller);

/// 用于内嵌的 Youzan X5 WebView 组件
class YouzanBrowserView extends StatefulWidget {
  final YouzanBrowserCreatedCallback? onBrowserCreated;
  final String? initialUrl;
  
  /// 是否隐藏有赞内核自带的白屏 Loading 过渡提示（默认为 false，即不隐藏）
  final bool hideLoading;

  const YouzanBrowserView({
    Key? key,
    this.onBrowserCreated,
    this.initialUrl,
    this.hideLoading = false,
  }) : super(key: key);

  @override
  State<YouzanBrowserView> createState() => _YouzanBrowserViewState();
}

class _YouzanBrowserViewState extends State<YouzanBrowserView> {
  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      // 开启 Hybrid Composition (混合图层渲染) 代替旧版的 Virtual Display
      // 这能彻底解决 Android WebView / X5内核的软键盘无法弹起以及光标不显示的问题
      return PlatformViewLink(
        viewType: 'youzan_browser',
        surfaceFactory: (BuildContext context, PlatformViewController controller) {
          return AndroidViewSurface(
            controller: controller as AndroidViewController,
            gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
            hitTestBehavior: PlatformViewHitTestBehavior.opaque,
          );
        },
        onCreatePlatformView: (PlatformViewCreationParams params) {
          return PlatformViewsService.initSurfaceAndroidView(
            id: params.id,
            viewType: 'youzan_browser',
            layoutDirection: TextDirection.ltr,
            creationParams: <String, dynamic>{
              if (widget.initialUrl != null) 'url': widget.initialUrl,
              'hideLoading': widget.hideLoading,
            },
            creationParamsCodec: const StandardMessageCodec(),
            onFocus: () {
              params.onFocusChanged(true);
            },
          )
            ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
            ..addOnPlatformViewCreatedListener(_onPlatformViewCreated)
            ..create();
        },
      );
    }
    // 暂不支持 iOS 端内嵌协议
    return const Center(child: Text('Only Android X5 SDK is supported in this example.'));
  }

  void _onPlatformViewCreated(int id) {
    if (widget.onBrowserCreated != null) {
      widget.onBrowserCreated!(YouzanBrowserController(id));
    }
  }
}

/// YouzanBrowser 交互通道抽象控制器
class YouzanBrowserController {
  final MethodChannel _channel;
  final EventChannel _eventChannel;

  YouzanBrowserController(int id)
      : _channel = MethodChannel('youzan_browser_$id'),
        _eventChannel = EventChannel('youzan_browser_events_$id');

  /// 订阅异步抛出的生命周期以及 JSBridge 事件
  Stream<Map<String, dynamic>> get onEvent =>
      _eventChannel.receiveBroadcastStream().map((dynamic event) => Map<String, dynamic>.from(event));

  Future<Map<String, dynamic>> loadUrl(String url) async {
    final result = await _channel.invokeMethod('loadUrl', {'url': url});
    return Map<String, dynamic>.from(result ?? {});
  }

  Future<Map<String, dynamic>> goBack() async {
    final result = await _channel.invokeMethod('goBack');
    return Map<String, dynamic>.from(result ?? {});
  }

  Future<bool> canGoBack() async {
    final result = await _channel.invokeMethod('canGoBack');
    return result == true;
  }

  Future<bool> canGoForward() async {
    final result = await _channel.invokeMethod('canGoForward');
    return result == true;
  }

  Future<Map<String, dynamic>> goForward() async {
    final result = await _channel.invokeMethod('goForward');
    return Map<String, dynamic>.from(result ?? {});
  }

  Future<Map<String, dynamic>> gobackWithStep(int step) async {
    final result = await _channel.invokeMethod('gobackWithStep', {'step': step});
    return Map<String, dynamic>.from(result ?? {});
  }

  Future<Map<String, dynamic>> backListCount() async {
    final result = await _channel.invokeMethod('backListCount');
    return Map<String, dynamic>.from(result ?? {});
  }

  Future<Map<String, dynamic>> reload() async {
    final result = await _channel.invokeMethod('reload');
    return Map<String, dynamic>.from(result ?? {});
  }

  /// 动态改变且向有赞内核层下发隐藏/开启自带的 WebLoading 加载态配置
  Future<void> setHideLoading(bool hideLoading) async {
    await _channel.invokeMethod('setHideLoading', {'hideLoading': hideLoading});
  }

  Future<Map<String, dynamic>> evaluateJavaScript(String script) async {
    final result = await _channel.invokeMethod('evaluateJavaScript', {'script': script});
    return Map<String, dynamic>.from(result ?? {});
  }

  Future<String> currentUrl() async {
    final result = await _channel.invokeMethod('currentUrl');
    return result?.toString() ?? "";
  }

  Future<Map<String, dynamic>> sharePage() async {
    final result = await _channel.invokeMethod('sharePage');
    return Map<String, dynamic>.from(result ?? {});
  }

  /// 通知原生端立即销毁并释放此 WebView 资源
  Future<void> dispose() async {
    await _channel.invokeMethod('dispose');
  }
}
