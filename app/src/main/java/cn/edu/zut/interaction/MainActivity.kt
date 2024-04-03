package cn.edu.zut.interaction

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.edu.zut.config.Storage
import cn.edu.zut.service.ClipMonitor
import java.net.Socket


class MainActivity : AppCompatActivity() {
    private lateinit var editIP: EditText
    private lateinit var editPort: EditText
    private lateinit var btn: Button
    private var running: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 把MainActivity注册到Storage中
        Storage.mainActivity = this
        // 初始化界面
        editIP = findViewById(R.id.edit_ip)
        editPort = findViewById(R.id.edit_port)
        btn = findViewById<View>(R.id.btn_copy) as Button
        btn.setOnClickListener {
            // 填充数据
            Storage.ip = editIP.text.toString()
            Storage.port = editPort.text.toString().toInt()
            if (running) {
                switchButton(SwitchStatus.OFF)
            } else {
                switchButton(SwitchStatus.ON)
                startService(Intent(this, ClipMonitor::class.java))
            }
        }
    }

    fun switchButton(status: SwitchStatus) {
        if (status == SwitchStatus.ON) {
            // 当按钮为 启动时
            this.running = false
            btn.text = "停止服务"
            editIP.isEnabled = false
            editPort.isEnabled = false
            editPort.isFocusable = false
            editPort.isFocusable = false
            running = true
        } else {
            // 当按钮为 停止时
            this.running = false
            stopService(Intent(this, ClipMonitor::class.java))
            editIP.isEnabled = true
            editPort.isEnabled = true
            editPort.isFocusable = true
            editPort.isFocusable = true
            btn.text = "启动服务"
        }
    }

    public fun backLauncher() {
/*        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)*/
    }

    public enum class SwitchStatus {
        ON, OFF
    }
}