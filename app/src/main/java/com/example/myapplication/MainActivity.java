package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Button;
public class MainActivity extends AppCompatActivity {
    private TextView textViewRed;
    private TextView textViewIr;
    private TextView textViewHr;
    private TextView textViewHrValid;
    private TextView textViewSpo2;
    private TextView textViewSpo2Valid;

    private TextView ToptextViewHr;
    private TextView ToptextViewSpo2;

    private Button buttonStartStop;

    private boolean isReadingData = false; // 用于跟踪读取状态
    private static final String TAG = "TCPResponse";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // 使用 findViewById 获取 TextView
        textViewRed = findViewById(R.id.textView16);
        textViewIr = findViewById(R.id.textView15);
        textViewHr = findViewById(R.id.textView10);
        textViewHrValid = findViewById(R.id.textView12);
        textViewSpo2 = findViewById(R.id.textView11);
        textViewSpo2Valid = findViewById(R.id.textView13);

        ToptextViewHr = findViewById(R.id.textView19);
        ToptextViewSpo2 = findViewById(R.id.textView18);

        //启动停止采样按钮
        buttonStartStop = findViewById(R.id.button);

        // 设置按钮点击事件
        buttonStartStop.setOnClickListener(v -> {
            if (isReadingData) {
                stopDataReading();
            } else {
                startDataReading();
            }
        });

        // 设置Bottom Navigation
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    // 执行TCP请求并返回结果
    private String performTcpRequest(String ipAddress, int port) {
        StringBuilder result = new StringBuilder();
        Socket socket = null;
        PrintWriter out = null;
        BufferedReader in = null;

        try {
            socket = new Socket(ipAddress, port); // 创建TCP连接
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 发送数据到服务器
            String requestData = "Your request data here"; // 根据实际需求填入请求数据
            out.println(requestData); // 发送数据

            // 读取服务器响应
            String responseLine;
            while ((responseLine = in.readLine()) != null) {
                processResponseData(responseLine); // 立即处理每行数据
                result.append(responseLine); // 如果仍需要存储结果，可以继续使用
                result.append("\n"); // 保持换行
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException occurred: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing resources: " + e.getMessage());
            }
        }
        return result.toString();
    }

    // 处理每行数据的方法
    private void processResponseData(String responseLine) {
        // 将返回的数据分割为键值对
        String[] pairs = responseLine.split(", ");
        int red = 0, ir = 0, hr = 0, hrValid = 0, spo2 = 0, spo2Valid = 0;

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim(); // 获取键
                String value = keyValue[1].trim(); // 获取值

                switch (key) {
                    case "red":
                        red = Integer.parseInt(value);
                        break;
                    case "ir":
                        ir = Integer.parseInt(value);
                        break;
                    case "HR":
                        hr = Integer.parseInt(value);
                        break;
                    case "HRvalid":
                        hrValid = Integer.parseInt(value);
                        break;
                    case "SpO2":
                        spo2 = Integer.parseInt(value);
                        break;
                    case "SPO2Valid":
                        spo2Valid = Integer.parseInt(value);
                        break;
                }
            }
        }

        // 更新UI
        updateUI(red, ir, hr, hrValid, spo2, spo2Valid);
    }


    // 处理UI更新
    private void updateUI(int red, int ir, int hr, int hrValid, int spo2, int spo2Valid) {
        // 确保在主线程中更新UI
        runOnUiThread(() -> {
            textViewRed.setText(red != 0 ? String.valueOf(red) : "");
            textViewIr.setText(ir != 0 ? String.valueOf(ir) : "");
            textViewHr.setText(hr != 0 ? String.valueOf(hr) : "");
            if (textViewHrValid != null) textViewHrValid.setText(String.valueOf(hrValid));
            textViewSpo2.setText(spo2 != 0 ? String.valueOf(spo2) : "");
            if (textViewSpo2Valid != null) textViewSpo2Valid.setText(String.valueOf(spo2Valid));

            // 同时设置给 ToptextViewHr 和 ToptextViewSpo2
            ToptextViewHr.setText(hr != 0 ? String.valueOf(hr) : "");
            ToptextViewSpo2.setText(spo2 != 0 ? String.valueOf(spo2) : "");
        });
    }
    private void startDataReading() {

        // 更新按钮文本
        buttonStartStop.setText("停止读取数据");
        isReadingData = true;

        // 执行TCP通信
        executorService.execute(() -> {
            String result = performTcpRequest("192.168.4.1", 80); // TCP服务器IP和端口
        });
    }

    private void stopDataReading() {
        // 更新按钮文本
        buttonStartStop.setText("开始读取数据");
        isReadingData = false;
    }

}
