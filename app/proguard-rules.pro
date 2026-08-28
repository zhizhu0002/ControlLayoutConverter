# JavaScript bridge methods are invoked reflectively by WebView.
-keepclassmembers class com.zhizhu.controlconverter.MainActivity$Bridge {
    <methods>;
}

# Native converter entry points are resolved through JNI.
-keep class com.tungsten.fcl.util.LayoutConverter { *; }
-keep class com.zhizhu.controlconverter.OfficialConverter { *; }

# Keep the Activity entry point and its lifecycle methods.
-keep public class com.zhizhu.controlconverter.MainActivity { *; }
