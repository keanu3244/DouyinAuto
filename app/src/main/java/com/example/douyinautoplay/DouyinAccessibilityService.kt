package com.example.douyinautoplay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class DouyinAccessibilityService : AccessibilityService() {
    companion object {
        var isRunning = AtomicBoolean(false)
        var phoneNumber = ""
        var verificationCode: String? = null
    }

    private val linksFile = "/sdcard/links.txt"
    private lateinit var links: List<String>
    private var currentLinkIndex = 0
    private var isWaitingForCode = false
    private var isWaitingForConfirmation = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: return
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_VIEW_CLICKED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100
        this.serviceInfo = info
        Toast.makeText(this, "无障碍服务已启动", Toast.LENGTH_SHORT).show()

        if (!File(linksFile).exists()) {
            Toast.makeText(this, "链接文件不存在：$linksFile", Toast.LENGTH_LONG).show()
            return
        }
        links = File(linksFile).readLines().filter { it.trim().isNotEmpty() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isRunning.get()) return
        val rootNode = rootInActiveWindow ?: return
        if (event?.packageName != "com.ss.android.ugc.aweme") return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (isLoggedIn(rootNode)) {
                    if (!isWaitingForConfirmation) {
                        showConfirmationDialog(rootNode)
                    }
                } else {
                    performLogin(rootNode)
                }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (event.text?.contains("开始") == true) {
                    isWaitingForConfirmation = false
                    playNextVideo(rootNode)
                }
            }
        }
    }

    private fun isLoggedIn(rootNode: AccessibilityNodeInfo): Boolean {
        return rootNode.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/title").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/user_profile").isNotEmpty()
    }

    private fun showConfirmationDialog(rootNode: AccessibilityNodeInfo) {
        isWaitingForConfirmation = true
        Toast.makeText(this, "已检测到登录，点击屏幕上的'开始'按钮播放视频", Toast.LENGTH_LONG).show()
    }

    private fun performLogin(rootNode: AccessibilityNodeInfo) {
        val loginButton = rootNode.findAccessibilityNodeInfosByText("登录")
        if (loginButton.isNotEmpty()) {
            loginButton[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Thread.sleep(2000)
        }

        val phoneInput = rootNode.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/phone_input")
        if (phoneInput.isNotEmpty()) {
            val bundle = Bundle()
            bundle.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, phoneNumber)
            phoneInput[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
            Thread.sleep(1000)

            val getCodeButton = rootNode.findAccessibilityNodeInfosByText("获取验证码")
            if (getCodeButton.isNotEmpty()) {
                getCodeButton[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Thread.sleep(2000)
                isWaitingForCode = true
                waitForCode(rootNode)
            }
        }
    }

    private fun waitForCode(rootNode: AccessibilityNodeInfo) {
        var attempts = 0
        while (isWaitingForCode && attempts < 30) {
            if (verificationCode != null) {
                val codeInput = rootNode.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/code_input")
                if (codeInput.isNotEmpty()) {
                    val bundle = Bundle()
                    bundle.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, verificationCode)
                    codeInput[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                    Thread.sleep(1000)

                    val confirmButton = rootNode.findAccessibilityNodeInfosByText("确认")
                    if (confirmButton.isNotEmpty()) {
                        confirmButton[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Thread.sleep(2000)
                        isWaitingForCode = false
                        verificationCode = null
                    }
                }
                break
            }
            Thread.sleep(1000)
            attempts++
        }

        if (isWaitingForCode) {
            Toast.makeText(this, "未收到验证码，请手动输入", Toast.LENGTH_LONG).show()
        }
    }

    private fun playNextVideo(rootNode: AccessibilityNodeInfo) {
        if (currentLinkIndex >= links.size) {
            Toast.makeText(this, "所有视频播放完成", Toast.LENGTH_LONG).show()
            currentLinkIndex = 0
            return
        }

        val link = links[currentLinkIndex]
        Toast.makeText(this, "打开链接: $link", Toast.LENGTH_SHORT).show()

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("link", link)
        clipboard.setPrimaryClip(clip)

        val searchIcon = rootNode.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/search_icon")
        if (searchIcon.isNotEmpty()) {
            searchIcon[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Thread.sleep(2000)

            val searchInput = rootNode.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/search_input")
            if (searchInput.isNotEmpty()) {
                searchInput[0].performAction(AccessibilityNodeInfo.ACTION_PASTE)
                Thread.sleep(1000)

                val searchButton = rootNode.findAccessibilityNodeInfosByViewId("com.ss.android.ugc.aweme:id/search_button")
                if (searchButton.isNotEmpty()) {
                    searchButton[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Thread.sleep(5000)
                }
            }

            Thread.sleep(10000)
            performGlobalAction(GLOBAL_ACTION_BACK)
            Thread.sleep(2000)
            performGlobalAction(GLOBAL_ACTION_BACK)
            Thread.sleep(2000)

            currentLinkIndex++
            playNextVideo(rootNode)
        } else {
            Toast.makeText(this, "未找到搜索图标，跳过...", Toast.LENGTH_SHORT).show()
            currentLinkIndex++
            playNextVideo(rootNode)
        }
    }

    override fun onInterrupt() {
        isRunning.set(false)
    }
}