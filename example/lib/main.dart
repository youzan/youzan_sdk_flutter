import 'package:flutter/material.dart';

import 'package:youzan_sdk_flutter/youzan_sdk_flutter.dart';
import 'package:youzan_sdk_flutter/youzan_browser_view.dart';
import 'browser_page.dart'; // 导入分离出来的浏览器挂载界面

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({Key? key}) : super(key: key);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  // 刚才您填入的预设 URL
  final TextEditingController _urlController = TextEditingController(
      text:
          "https://shop42618405.m.youzan.com/wscshop/showcase/homepage?kdt_id=42426237");
  final TextEditingController _clientIdController =
      TextEditingController(text: "1c3691de6f9aebc0d4");
  String _statusMessage = "未初始化1";

  void _setStatus(String msg) {
    setState(() {
      _statusMessage = msg;
    });
  }

  void _setupSDK() async {
    _setStatus("Calling setupSDK...");
    final result = await YouzanSdkFlutter.setupSDK(
      clientId: _clientIdController.text.trim(),
      appKey: 'YOUR_APP_KEY',
      debug: true, // 如果是正常版本的话 false， 填写正确的YOUR_CLIENT_ID 和 YOUR_APP_KEY
    );

    // 配置全局的 URL 跳转同步拦截匹配规则 (仅在客户端做静默匹配处理，不走 Flutter 通道规避死锁)
    await YouzanSdkFlutter.registerInterceptRules(
      hostAndPathUrls: [
        "https://shop42618405.youzan.com/wscuser/membercenter",
      ],
    );

    _setStatus('SDK Init Result: $result； ${_clientIdController.text.trim()}');
  }

  void _login() async {
    _setStatus("Calling login...");
    final result = await YouzanSdkFlutter.login(
      userId: '13777836524',
      nickName: 'Flutter User',
    );
    _setStatus('Login Result: $result');
  }

  void _logout() async {
    _setStatus("Calling logout...");
    final result = await YouzanSdkFlutter.logout();
    _setStatus('Logout Result: $result');
  }

  void _goToBrowser() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => BrowserPage(url: _urlController.text),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Youzan X5SDK Example'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text("📌 步骤 1: 全局能力测试",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 8),
            TextField(
              controller: _clientIdController,
              decoration: const InputDecoration(
                labelText: '指定 Client ID (初始化参数)',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 8),
            ElevatedButton(
                onPressed: _setupSDK, child: const Text('初始化SDK (必须第一步执行)')),
            const SizedBox(height: 8),
            ElevatedButton(
                onPressed: _login, child: const Text('唤起登录交互(或者更新登录态)')),
            const SizedBox(height: 8),
            ElevatedButton(
                onPressed: _logout,
                child: const Text('登出当前用户',
                    style: TextStyle(color: Colors.redAccent)),
                style: ElevatedButton.styleFrom(primary: Colors.white)),
            const SizedBox(height: 8),
            ElevatedButton(
              onPressed: () {
                YouzanSdkFlutter.toast('Toast From Flutter!');
                YouzanSdkFlutter.nativeLog('Hello Native Logger!');
              },
              child: const Text('系统层通讯测试'),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(8),
              color: Colors.blue.withOpacity(0.1),
              child: Text('执行状态: \n$_statusMessage',
                  style: const TextStyle(color: Colors.blue)),
            ),
            const Divider(height: 48),
            const Text("📌 步骤 2: 渲染 WebView",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 8),
            TextField(
              controller: _urlController,
              decoration: const InputDecoration(
                labelText: '业务链接 (支持动态传参)',
                border: OutlineInputBorder(),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                  primary: Colors.blue, // 修补特定 Flutter 老版本的 API 参数变更报错
                  onPrimary: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16)),
              onPressed: _goToBrowser,
              child: const Text('跳转至 WebView 页面并加载',
                  style: TextStyle(fontSize: 16)),
            ),
          ],
        ),
      ),
    );
  }
}
