#import "YouzanSdkFlutterPlugin.h"
#if __has_include(<youzan_sdk_flutter/youzan_sdk_flutter-Swift.h>)
#import <youzan_sdk_flutter/youzan_sdk_flutter-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "youzan_sdk_flutter-Swift.h"
#endif

@implementation YouzanSdkFlutterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftYouzanSdkFlutterPlugin registerWithRegistrar:registrar];
}
@end
