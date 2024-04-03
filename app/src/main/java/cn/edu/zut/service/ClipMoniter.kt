package cn.edu.zut.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ClipboardManager.OnPrimaryClipChangedListener
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import cn.edu.zut.config.Storage
import cn.edu.zut.interaction.MainActivity

class ClipMonitor : Service() {
    private var clipboardManager: ClipboardManager? = null
    private lateinit var serverThread: SocketClient
    private var normalStatus = false // 表示是否正常启动了
    private val handler = Handler(Looper.getMainLooper())
    private var lastContent: String? = null
    public fun setLastContent(last: String) {
        this.lastContent = last
    }
    private fun  getLastContent(): String? {
        return this.lastContent
    }


    // 读代码时候小心这段，因为涉及线程，所以并不是简单的先后关系
    override fun onCreate() {
        super.onCreate()
        //ip port 数据是否为空，空则退出
        if (Storage.ip.isNullOrEmpty()) {
            Toast.makeText(applicationContext, "连接信息异常！", Toast.LENGTH_SHORT).show()
            Storage.mainActivity.switchButton(MainActivity.SwitchStatus.OFF)
            stopSelf()
        }
        // 是否可以连接服务器，在子线程尝试
        val tmpThread = Thread {
            try {
                this.serverThread = SocketClient()
                // 如果这里不抛出异常就说明连接成功了！
                normalStatus = true
            } catch (e: Exception) {
                handler.post {
                    Toast.makeText(
                        applicationContext,
                        "连接失败，请检查地址是否正确！",
                        Toast.LENGTH_SHORT
                    ).show()
                    Storage.mainActivity?.switchButton(MainActivity.SwitchStatus.OFF)
                }
                Log.e(TAG, "tmpThread exception occur: ${e.message}")
                stopSelf()
            }
        }
        // 阻塞等待申请..
        tmpThread.start()
        while (tmpThread.isAlive) {
        }
    }


    override fun onDestroy() {
        // 如果（线程）正常启动了提醒它下班
        if (normalStatus && serverThread.isAlive) {
            serverThread.interrupt()
            Toast.makeText(this, "停止成功", Toast.LENGTH_SHORT).show()
        }
        super.onDestroy()
        // 优雅的结束服务
        if (clipboardManager != null && clipChangedListener != null) {
            clipboardManager?.removePrimaryClipChangedListener(clipChangedListener)
        }
        // 停止前台服务
        stopForeground(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 正常连接后的逻辑
        if (normalStatus) {
            Toast.makeText(applicationContext, "连接成功！", Toast.LENGTH_SHORT).show()
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            this.serverThread.setMonitor(this)
            serverThread.start()
            // 注册剪贴板监听器
            clipboardManager?.addPrimaryClipChangedListener(clipChangedListener)
            // 启动前台服务,常驻服务
            startForeground(NOTIFICATION_ID, createNotification())
            Storage.mainActivity.backLauncher()
        }
        return START_REDELIVER_INTENT
    }

    // 设计粘贴板变化后的逻辑
    private var clipChangedListener = OnPrimaryClipChangedListener {
        // 获取粘贴板内容
        val clipData = clipboardManager?.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val clipText = clipData.getItemAt(0).text.toString()
        }
        val clipText = clipData?.getItemAt(0)?.text.toString()
        // 若不是服务器引起的粘贴板改变，发送数据到服务器
        if (lastContent == null || !clipText.equals(lastContent)) {
            serverThread.sendDataToServer(SocketClient.encodeData(clipText))
        }
        Log.i(TAG, "message change!${clipText}")
    }

    public fun setClipboardText(text: String) {
        Log.i(TAG, "receive Remote(${Storage.ip}): ${text}")
        this.clipboardManager?.setPrimaryClip(ClipData.newPlainText("interact", text))
    }


    /**
     * 下面的代码是用于创建通知的，可以忽略
     */
    companion object {
        private val TAG = "d8g"
        private const val NOTIFICATION_ID = 12345
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // 创建通知
    private fun createNotification(): Notification {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                ""
            }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Clip Monitor Service")
            .setContentText("Service is running...")
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setContentIntent(pendingIntent)
            .build()
    }

    // 创建通知渠道（仅适用于Android O及更高版本）
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "clip_monitor_channel"
        val channelName = "Clip Monitor Service"
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        return channelId
    }
}
