package com.vogue.socket;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vogue.socket.util.SocketTransceiver;
import com.vogue.socket.util.TcpClient;






public class MainActivity extends Activity implements View.OnClickListener {

    private Button bnConnect;
    private TextView txReceive;
    private EditText edIP, edPort, edData;

    private Handler handler = new Handler(Looper.getMainLooper());

    private TcpClient client = new TcpClient() {

        @Override
        public void onConnect(SocketTransceiver transceiver) {
            refreshUI(true);
        }

        @Override
        public void onDisconnect(SocketTransceiver transceiver) {
            refreshUI(false);
        }

        @Override
        public void onConnectFailed() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onReceive(SocketTransceiver transceiver, final String s) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    txReceive.append(unicodeToString(s));
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //come here
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        this.findViewById(R.id.bn_send).setOnClickListener(this);
        bnConnect = (Button) this.findViewById(R.id.bn_connect);
        bnConnect.setOnClickListener(this);

        edIP = (EditText) this.findViewById(R.id.ed_ip);
        edPort = (EditText) this.findViewById(R.id.ed_port);
        edData = (EditText) this.findViewById(R.id.ed_dat);
        txReceive = (TextView) this.findViewById(R.id.tx_receive);
        txReceive.setOnClickListener(this);

        refreshUI(false);
    }

    @Override
    public void onStop() {
        client.disconnect();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bn_connect:
                connect();
                break;
            case R.id.bn_send:
                sendStr();
                break;
            case R.id.tx_receive:
                clear();
                break;
        }
    }

    /**
     * 刷新界面显示
     *
     * @param isConnected
     */
    private void refreshUI(final boolean isConnected) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                edPort.setEnabled(!isConnected);
                edIP.setEnabled(!isConnected);
                bnConnect.setText(isConnected ? "断开" : "连接");
            }
        });
    }

    /**
     * 设置IP和端口地址,连接或断开
     */
    private void connect() {
        if (client.isConnected()) {
            // 断开连接
            client.disconnect();
        } else {
            try {
                String hostIP = edIP.getText().toString();
                int port = Integer.parseInt(edPort.getText().toString());
                client.connect(hostIP, port);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "端口错误", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送数据
     */
    private void sendStr() {
        try {
            String data = edData.getText().toString();
            client.getTransceiver().send(stringToUnicode(data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清空接收框
     */
    private void clear() {
        new AlertDialog.Builder(this).setTitle("确认清除?")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        txReceive.setText("");
                    }
                }).show();
    }
    //字符串转换unicode
    public static String stringToUnicode(String string) {
        StringBuffer unicode = new StringBuffer();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);  // 取出每一个字符
            unicode.append("\\u" +Integer.toHexString(c));// 转换为unicode
        }
        return unicode.toString();
    }

    //unicode 转字符串
    public static String unicodeToString(String unicode) {
        StringBuffer string = new StringBuffer();
        String[] hex = unicode.split("\\\\u");
        for (int i = 1; i < hex.length; i++) {
            int data = Integer.parseInt(hex[i], 16);// 转换出每一个代码点
            string.append((char) data);// 追加成string
        }
        return string.toString();
    }
}