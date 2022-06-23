package io.github.duzhaokun123.hcbetter.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.view.Menu
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import io.github.duzhaokun123.hcbetter.R
import io.github.duzhaokun123.hcbetter.XposedInit.Companion.moduleRes
import io.github.duzhaokun123.hcbetter.utils.*
import io.github.duzhaokun123.hcbetter.view.SimpleItem
import kotlinx.coroutines.delay
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import java.io.File
import java.io.FileOutputStream

class InstallCAHook(mClassLoader: ClassLoader) : BaseHook(mClassLoader) {
    companion object {
        const val CODE_EXPORT_PEM = 114_1
        const val CODE_EXPORT_0 = 114_2
        const val CODE_EXPORT_MODULE = 114_3
    }
    override fun startHook() {
        "com.guoshi.httpcanary.ui.foreplay.ForeplayActivity".hookAfterMethod(
            mClassLoader,
            "onCreateOptionsMenu",
            Menu::class.java
        ) {
            val self = it.thisObject as Activity
            val configButton = self.findViewByClass(Button::class.java, true)[0]
            if (configButton.tag != "hooked1") configButton.tag =
                "hooked1" else return@hookAfterMethod
            configButton.addOnClickAfter {
                runMain {
                    delay(100)
                    val installButton= self.findViewByClass(Button::class.java, true)[0]
                    if (installButton.tag != "hooked2") configButton.tag =
                        "hooked2" else return@runMain
                    val directInstall = installButton.getOnClick()
                    installButton.setOnClickListener {
                        AlertDialog.Builder(self).setItems(
                            arrayOf("direct", ".pem", ".0", "Magisk module")
                        ) { _, i ->
                            when(i) {
                                0 -> directInstall.onClick(installButton)
                                1 -> {
                                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                                    intent.type = "application/x-pem-file"
                                    intent.putExtra(Intent.EXTRA_TITLE, "HttpCanary.pem")
                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                    self.startActivityForResult(intent, CODE_EXPORT_PEM)
                                }
                                2 -> {
                                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                                    intent.type = "application/x-0"
                                    intent.putExtra(Intent.EXTRA_TITLE, "87bc3517.0")
                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                    self.startActivityForResult(intent, CODE_EXPORT_0)
                                }
                                3 -> {
                                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                                    intent.type = "application/zip"
                                    intent.putExtra(Intent.EXTRA_TITLE, "HttpCanary_System_CA.zip")
                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                    self.startActivityForResult(intent, CODE_EXPORT_MODULE)
                                }
                            }
                        }.show()
                    }
                }
            }
        }

        "com.guoshi.httpcanary.ui.certificate.CertificateSettingsActivity".hookAfterMethod(mClassLoader, "onResume") {
            val self = it.thisObject as Activity
            val sl = self.findViewByClass(ScrollView::class.java, true)[0]
            if (sl.tag != "hooked1") sl.tag =
                "hooked1" else return@hookAfterMethod
            ((sl as ViewGroup).getChildAt(0) as ViewGroup).apply {
                addView(SimpleItem(self).apply {
                    title = "Mark as installed"
                    desc = "Create HttpCanary.jks file at ${self.cacheDir}"
                    setOnClickListener {
                        File(self.cacheDir, "HttpCanary.jks").createNewFile()
                        self.recreate()
                    }
                })
                addView(SimpleItem(self).apply {
                    title = "Export magisk module"
                    desc = "Install system CA via magisk"
                    setOnClickListener {
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                        intent.type = "application/zip"
                        intent.putExtra(Intent.EXTRA_TITLE, "HttpCanary_System_CA.zip")
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        self.startActivityForResult(intent, CODE_EXPORT_MODULE)
                    }
                })
            }
        }

        Activity::class.java.hookAfterMethod("onActivityResult", Int::class.java, Int::class.java, Intent::class.java) {
            val self = it.thisObject as Activity
            val requestCode = it.args[0] as Int
            val resultCode = it.args[1]
            val intent = it.args[2] as Intent?
            if (requestCode !in listOf(CODE_EXPORT_PEM, CODE_EXPORT_0, CODE_EXPORT_MODULE))
            if (resultCode != Activity.RESULT_OK) return@hookAfterMethod
            val data = intent?.data ?: return@hookAfterMethod
            getJks(mClassLoader)
            val pemFile = File(self.cacheDir, "HttpCanary.pem")
            val `in` = when(requestCode) {
                CODE_EXPORT_PEM,CODE_EXPORT_0  -> pemFile.inputStream()
                CODE_EXPORT_MODULE -> {
                    val tf = File.createTempFile("root_ca", null)
                    moduleRes.openRawResource(R.raw.module_temp).use { ai ->
                        FileOutputStream(tf).use { fo ->
                            ai.copyTo(fo)
                        }
                    }
                    ZipFile(tf).addFile(File(currentContext.cacheDir, "HttpCanary.pem"), ZipParameters().apply {
                        fileNameInZip = "/system/etc/security/cacerts/87bc3517.0"
                    })
                    tf.inputStream()
                }
                else -> throw RuntimeException("unknown requestCode: $requestCode")
            }
            val out = self.contentResolver.openOutputStream(data)!!
            `in`.copyTo(out)
            `in`.close()
            out.close()
            Log.toast("exported")
        }
    }
}