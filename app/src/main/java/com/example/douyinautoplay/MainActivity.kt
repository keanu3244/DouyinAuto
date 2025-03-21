package com.example.douyinautoplay

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.phone.SmsRetriever

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var smsReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 SMS 检索
        startSmsRetriever()

        // 注册广播接收器
        smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                Log.d(TAG, "BroadcastReceiver: onReceive called")
                if (intent?.action == SmsRetriever.SMS_RETRIEVED_ACTION) {
                    val extras = intent.extras
                    val status = extras?.get(SmsRetriever.EXTRA_STATUS)
                    Log.d(TAG, "BroadcastReceiver: Status = $status")
                    // 处理 SMS 检索结果
                }
            }
        }
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(smsReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(smsReceiver, intentFilter)
        }

        // 设置 UI
        val phoneNumberInput = findViewById<EditText>(R.id.phoneNumberInput)
        val enableServiceButton = findViewById<Button>(R.id.enableServiceButton)
        val startButton = findViewById<Button>(R.id.startButton)

        enableServiceButton.setOnClickListener {
            Log.d(TAG, "enableServiceButton: Clicked")
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        startButton.setOnClickListener {
            Log.d(TAG, "startButton: Clicked")
            val phoneNumber = phoneNumberInput.text.toString()
            if (phoneNumber.length == 11) {
                // 直接构造 Intent 启动抖音的 MainActivity
                val intent = Intent(Intent.ACTION_MAIN)
                intent.setClassName("com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.main.MainActivity")
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent.putExtra("phoneNumber", phoneNumber)
                try {
                    startActivity(intent)
                    Log.d(TAG, "startButton: Douyin started successfully")
                } catch (e: ActivityNotFoundException) {
                    android.widget.Toast.makeText(this, "请安装抖音或更新到最新版本", android.widget.Toast.LENGTH_LONG).show()
                    Log.w(TAG, "startButton: Douyin not installed or MainActivity not found", e)
                } catch (e: SecurityException) {
                    android.widget.Toast.makeText(this, "无权限启动抖音: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    Log.e(TAG, "startButton: SecurityException occurred", e)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "启动抖音失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    Log.e(TAG, "startButton: Error starting Douyin", e)
                }
            } else {
                phoneNumberInput.error = "请输入11位手机号"
                Log.d(TAG, "startButton: Invalid phone number length")
            }
        }
    }

    private fun startSmsRetriever() {
        try {
            val client = SmsRetriever.getClient(this)
            client.startSmsRetriever()
            Log.d(TAG, "startSmsRetriever: Started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "startSmsRetriever: Exception occurred", e)
            android.widget.Toast.makeText(this, "SMS Retriever Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(smsReceiver)
            Log.d(TAG, "onDestroy: unregisterReceiver completed")
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: Exception occurred", e)
        }
    }
}