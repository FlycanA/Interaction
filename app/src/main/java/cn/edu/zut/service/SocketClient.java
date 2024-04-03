package cn.edu.zut.service;

import android.content.ClipboardManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.edu.zut.config.Storage;

public class SocketClient extends Thread {
    private static final String TAG = "d8g";
    private String ip;
    private int port;
    private Socket socket;
    private ClipMonitor monitor;
    private BufferedWriter writer;
    private BufferedReader reader;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SocketClient() throws IOException {
        this.ip = Storage.ip;
        this.port = Storage.port;
        this.socket = new Socket(ip, port);
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // 将接收到的内容复制到粘贴板
                monitor.setLastContent(line);
                this.monitor.setClipboardText(decodeData(line));
            }
            // 关闭流和套接字
            reader.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMonitor(ClipMonitor monitor) {
        this.monitor = monitor;
    }

    public void sendDataToServer(String data) {
        executorService.submit(() -> {
            try {
                Log.i(TAG, "sendDataToServer: " + encodeData(data + "\n\n"));
                writer.write(encodeData(data + "\n\n"));
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // 拓展，此处不用策略者模式了，比较简单
    public static String decodeData(Object data) {
        return (String) data;
//        String encodedData = (String) data;
//        StringBuilder decodedData = new StringBuilder();
//        for (int i = 0; i < encodedData.length(); i++) {
//            char decodedChar = (char) ((int) encodedData.charAt(i) ^ 1);
//            decodedData.append(decodedChar);
//        }
//        return decodedData.toString();
    }


    public static String encodeData(Object data) {
        return (String) data;
//        String processed = "";
//        for (int i = 0; i < ((String) data).length(); i++) {
//            processed += (char)((int) ((String) data).charAt(i) ^ 1);
//        }
//        return (String) processed;
    }
}