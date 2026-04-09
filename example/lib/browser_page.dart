import 'package:flutter/material.dart';
import 'package:youzan_sdk_flutter/youzan_sdk_flutter.dart';
import 'package:youzan_sdk_flutter/youzan_browser_view.dart';

class BrowserPage extends StatefulWidget {
  final String url;
  const BrowserPage({Key? key, required this.url}) : super(key: key);

  @override
  State<BrowserPage> createState() => _BrowserPageState();
}

class _BrowserPageState extends State<BrowserPage> {
  YouzanBrowserController? _browserController;
  final List<String> _logs = [];
  String _pageTitle = '加载页面展示';

  @override
  void dispose() {
    _browserController?.dispose(); // 兜底：页面消亡时强制通知 Android 销毁清理内存
    super.dispose();
  }

  void _addLog(String msg) {
    if (mounted) {
      setState(() {
        _logs.insert(0, msg);
      });
      print(msg); // 同步输出到控制台方便查看
    }
  }

  @override
  Widget build(BuildContext context) {
    return WillPopScope(
      onWillPop: () async {
        final res = await _browserController?.goBack();
        final canGoBack = res != null && res['success'] == true;
        if (canGoBack) {
          _addLog('🔙 [返回键] WebView 回退成功，Flutter 页面保留');
        } else {
          _addLog('🔙 [返回键] WebView 已到顶，关闭 Flutter 页面');
        }
        return !canGoBack;
      },
      child: Scaffold(
      appBar: AppBar(
        title: Text(_pageTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () async {
              final res = await _browserController?.goBack();
              // 如果原生容器反馈已经到顶了（无法再回退页面）则把这个 Flutter 路由页 pop 掉
              if (res == null || res['success'] == false) {
                if (mounted) {
                  Navigator.of(context).pop();
                }
              }
            },
            tooltip: "内层返回并支持退栈",
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => _browserController?.reload(),
            tooltip: "刷新页面",
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            flex: 5,
            child: Container(
              decoration:
                  BoxDecoration(border: Border.all(color: Colors.blueAccent)),
              child: YouzanBrowserView(
                initialUrl: widget.url,
                hideLoading: false,
                onBrowserCreated: (controller) {
                  _browserController = controller;

                  // 完善所有事件监听和打印
                  controller.onEvent.listen((event) {
                    final String eventName = event['eventName'] ?? "未知事件";
                    final dynamic params = event['params'];

                    if (eventName == 'jsBridgeEvent') {
                      // 这是通过 browser.subscribe(AbsXXEvent) 或 URL 白名单回传的协议
                      final String bridgeType =
                          params?['type'] ?? "UnknownType";
                      final dynamic bridgeData = params?['data'];
                      // 具体细节对应原生 `initRegisterEvent()` 所有事件流
                      _addLog(
                          "🌉 [JSBridge通用派发]: '$bridgeType' | Data: $bridgeData");

                      // 处理同步 URL 拦截（规则来自于 registerInterceptRules）
                      if (bridgeType == 'UrlIntercepted') {
                        _addLog("🛑 主动拦截命中: 阻止了URL跳转 -> $bridgeData");
                        YouzanSdkFlutter.toast("拦截触发！即将打开新页面...");

                        Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (context) =>
                                BrowserPage(url: bridgeData.toString()),
                          ),
                        );
                      }

                      // 对齐 UniApp 处理逻辑：当 WebView 要求登录授权时自动走登录并发起重载
                      if (bridgeType == 'AuthEvent' ||
                          bridgeType == 'getUserInfo') {
                        _addLog("🔔 触发了登录拦截，正在静默获取凭证...");
                        YouzanSdkFlutter.login(
                          userId: '13777836524',
                          nickName: 'Flutter User',
                        ).then((res) {
                          if (res['success'] == true) {
                            _addLog("✅ 登录成功，正在重载并放行页面！");
                            controller.reload();
                          } else {
                            _addLog("❌ 登录受阻: ${res['message']}");
                          }
                        });
                      }
                    } else if (eventName == 'receivedTitle') {
                      final title = params?['title'];
                      _addLog("📑 [页面生命周期]: 网页标题获取为 '$title'");
                      // 将标题栏也同步修改
                      if (mounted && title != null && title.isNotEmpty) {
                        setState(() {
                          _pageTitle = title;
                        });
                      }
                    } else if (eventName == 'webViewStartLoad') {
                      _addLog("🚀 [页面生命周期]: 网页开始加载...");
                    } else if (eventName == 'webViewFinishLoad') {
                      _addLog("✅ [页面生命周期]: 网页加载完成！");
                    } else if (eventName == 'webViewLoadError') {
                      _addLog(
                          "❌ [页面网络报错]: 错误码: ${params?['code']}, 信息: ${params?['message']}");
                    } else {
                      _addLog("⚠️ [未分类事件]: $eventName | Props: $params");
                    }
                  });
                },
              ),
            ),
          ),
          // 底部日志栏保留，以便您观测事件详细拆解
          Expanded(
            flex: 2,
            child: Container(
              width: double.infinity,
              color: Colors.grey[200],
              child: ListView.builder(
                itemCount: _logs.length,
                itemBuilder: (context, index) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 8.0, vertical: 2.0),
                    child: Text(
                      _logs[index],
                      style:
                          const TextStyle(fontSize: 11, color: Colors.blueGrey),
                    ),
                  );
                },
              ),
            ),
          ),
        ],
      ),
    ));
  }
}
