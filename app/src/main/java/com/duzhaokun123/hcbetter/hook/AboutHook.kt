package com.duzhaokun123.hcbetter.hook

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import com.duzhaokun123.hcbetter.BuildConfig
import com.duzhaokun123.hcbetter.XposedInit
import com.duzhaokun123.hcbetter.utils.*

class AboutHook(mClassLoader: ClassLoader) : BaseHook(mClassLoader) {
    override fun startHook() {
        "com.guoshi.httpcanary.ui.settings.SettingsActivity".hookAfterMethod(mClassLoader, "onResume") {
            val self = it.thisObject as Activity
            val sl = self.findViewByClass(ScrollView::class.java, true)[0]
            if (sl.tag != "hooked1") sl.tag =
                "hooked1" else return@hookAfterMethod
            ((sl as ViewGroup).getChildAt(0) as ViewGroup).addView(Button(self).apply {
                text = "about HC Better"
                setOnClickListener {
                    AlertDialog.Builder(self)
                        .setTitle("about HC Better")
                        .setMessage("HC Better version: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE}) from ${XposedInit.modulePath}${if (isBuiltIn) "(BuiltIn)" else ""}\n" +
                                "HttpCanary version: ${getPackageVersion(self.packageName)} (${if (is64) "64" else "32"}bit)\n" +
                                "SDK: ${Build.VERSION.RELEASE}(${Build.VERSION.SDK_INT}); Phone: ${Build.BRAND} ${Build.MODEL}\n" +
                                "Config: ${sPrefs.all}")
                        .show()
                }
            })
        }
    }
}