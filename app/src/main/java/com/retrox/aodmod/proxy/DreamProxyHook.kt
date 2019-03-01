package com.retrox.aodmod.proxy

import android.app.AndroidAppHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.dreams.DreamService
import com.retrox.aodmod.MainHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import android.os.Process
import com.retrox.aodmod.pref.XPref

object DreamProxyHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_AOD) return
        val classLoader = lpparam.classLoader
        var dreamProxy: DreamProxy? = null
        val dozeServiceClass = XposedHelpers.findClass("com.oneplus.doze.DozeService", classLoader)

        val killReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Process.killProcess(Process.myPid())
            }
        }

        XposedHelpers.findAndHookConstructor(dozeServiceClass, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                AndroidAppHelper.currentApplication().applicationContext.registerReceiver(killReceiver, IntentFilter("com.retrox.aod.killmyself"))
            }
        })

        if (XPref.getDisplayMode() == "SYSTEM") {
            return
        }

        XposedHelpers.findAndHookConstructor(dozeServiceClass, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (dreamProxy == null) {
                    dreamProxy = DreamProxy(param.thisObject as DreamService)
                } else {
                    XposedHelpers.setObjectField(dreamProxy, "dreamService", param.thisObject)
                    // do the trick 避免重复初始化占内存 我真他妈是个聪明鬼
                }
            }
        })

        XposedHelpers.findAndHookMethod(dozeServiceClass, "onCreate", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                dreamProxy?.onCreate()
                param.result = null
            }
        })

        XposedHelpers.findAndHookMethod(dozeServiceClass, "onAttachedToWindow", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                dreamProxy?.onAttachedToWindow()
                param.result = null
            }
        })

        XposedHelpers.findAndHookMethod(dozeServiceClass, "onDreamingStarted", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                dreamProxy?.onDreamingStarted()
                param.result = null
            }
        })

        XposedHelpers.findAndHookMethod(dozeServiceClass, "onDreamingStopped", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                dreamProxy?.onDreamingStopped()
                param.result = null
            }
        })

        XposedHelpers.findAndHookMethod(dozeServiceClass, "onWakingUp", String::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                dreamProxy?.onWakingUp()
                param.result = null
            }
        })

        XposedHelpers.findAndHookMethod(dozeServiceClass, "onSingleTap", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                dreamProxy?.onSingleTap()
                param.result = null
            }
        })
    }
}