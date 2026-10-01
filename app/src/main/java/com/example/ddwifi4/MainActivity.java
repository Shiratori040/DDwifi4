package com.example.ddwifi4;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.Observer;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    Button btnOnOff, btnDiscover, btnStopDiscover;
    ListView listView;
    TextView connectionStatus;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    TextView textLocation;
    Button btnStart;
    Button btnEnd;
    LocationSensor locationSensor;

    Button btnOpenDatabase;

    private static final int AUTH_CODE = 12345; // 認証コード

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
        requestLocationPermission();

        textLocation = findViewById(R.id.textLocation);
        btnStart = findViewById(R.id.btnStart);
        btnEnd = findViewById(R.id.btnEnd);
        locationSensor = new LocationSensor(this);

        btnEnd.setEnabled(false);

        btnOpenDatabase = findViewById(R.id.btnOpenDatabase);
        btnOpenDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Database.class);
                startActivity(intent);
            }
        });

        locationSensor.location.observe(this, new Observer<Location>() {
            @Override
            public void onChanged(Location location) {
                if (location != null) {
                    textLocation.setText("緯度: " + location.getLatitude() + "\n経度: " + location.getLongitude());
                } else {
                    textLocation.setText("位置情報が利用できません");
                }
            }
        });

        btnStart.setOnClickListener(v -> {
            if (!locationSensor.run) {
                locationSensor.start();
                btnStart.setEnabled(false);
                btnEnd.setEnabled(true);
                textLocation.setText("位置情報を探索中");
            }
        });

        btnEnd.setOnClickListener(v -> {
            if (locationSensor.run) {
                locationSensor.stop();
                btnStart.setEnabled(true);
                btnEnd.setEnabled(false);
                textLocation.setText("位置情報の更新を停止しました");
            }
        });
    }

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void exqListener() {
        btnOnOff.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent panelIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(panelIntent);
            } else {
                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                } else {
                    wifiManager.setWifiEnabled(true);
                }
                updateWifiButton();
            }
        });

        btnDiscover.setOnClickListener(view -> {
            // まず、許可があるかを確認する
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // 許可がない場合はリクエストを行う
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            } else {
                // 許可がある場合のみdiscoverPeersを実行
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery Failed");
                    }
                });
            }
        });

        btnStopDiscover.setOnClickListener(view -> {
            mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    connectionStatus.setText("Discovery Stopped");
                    Toast.makeText(MainActivity.this, "探索が停止しました", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    connectionStatus.setText("Failed to Stop Discovery");
                    Toast.makeText(MainActivity.this, "探索の停止に失敗しました: " + reason, Toast.LENGTH_SHORT).show();
                }
            });
        });




        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            final WifiP2pDevice device = deviceArray[i];
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Reconnecting to " + device.deviceName, Toast.LENGTH_SHORT).show();
                    connectionStatus.setText("Reconnecting to " + device.deviceName);
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(getApplicationContext(), "Reconnection failed", Toast.LENGTH_SHORT).show();
                    connectionStatus.setText("Reconnection failed");
                }
            });
        });
    }

    private void initialWork() {
        btnOnOff = findViewById(R.id.onOff);
        btnDiscover = findViewById(R.id.discover);
        btnStopDiscover = findViewById(R.id.stopdiscover);
        listView = findViewById(R.id.peerListView);
        connectionStatus = findViewById(R.id.connectionStatus);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        updateWifiButton();
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void disconnect() {
        new Thread(() -> {
            try {
                if (sendReceive != null) {
                    sendReceive.write("DISCONNECT".getBytes());
                    sendReceive.closeConnection();
                    sendReceive = null;
                }
                if (clientClass != null) {
                    clientClass.socket.close();
                    clientClass = null;
                }
                if (serverClass != null) {
                    serverClass.serverSocket.close();
                    serverClass = null;
                }

                mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            connectionStatus.setText("Disconnected");
                            Toast.makeText(MainActivity.this, "Disconnected successfully", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onFailure(int reason) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "Failed to remove group: " + reason, Toast.LENGTH_SHORT).show()
                        );
                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error disconnecting: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                connectionStatus.setText("Host");
                serverClass = new ServerClass();
                serverClass.start();
            } else if (wifiP2pInfo.groupFormed) {
                connectionStatus.setText("Client");
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendDatabase();  // ネットワーク操作をバックグラウンドで実行
                    }
                }).start();
            }

        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        updateWifiButton();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public class ServerClass extends Thread {
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {

                // 既存のサーバーソケットが開いている場合は閉じる
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }

                serverSocket = new ServerSocket(3184);
                socket = serverSocket.accept();

                Log.d("ServerClass", "Client connected");

                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes = inputStream.read(buffer);
                String receivedAuthCode = new String(buffer, 0, bytes).trim();

                if (Integer.parseInt(receivedAuthCode) != AUTH_CODE) {
                    disconnect();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Unauthorized connection. Disconnected.", Toast.LENGTH_SHORT).show());
                    return;
                }

                sendReceive = new SendReceive(socket);
                sendReceive.start();

                sendDatabase();

            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "サーバーエラー: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                // サーバーソケットを閉じてリソースを解放する
                if (serverSocket != null && !serverSocket.isClosed()) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e("ServerClass", "Error closing serverSocket: " + e.getMessage());
                    }
                }
            }
        }
    }

    private class SendReceive extends Thread {
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private boolean isDoneSent = false;

        public SendReceive(Socket skt) {
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Error initializing streams: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (socket != null && !socket.isClosed()) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String receivedMessage = new String(buffer, 0, bytes).trim();

                        if (receivedMessage.equals("DONE")) {
                            if (isDoneSent) {
                                // 送信済みかつ相手からも「DONE」を受信した場合に接続を切断
                                disconnect();
                                break;
                            }
                        } else {
                            // 通常のデータ受信処理
                            handleReceivedData(receivedMessage);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection lost: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    break;
                }
            }
        }

        private void handleReceivedData(String data) {
            // JSON データとしてデータを処理
            if (data.startsWith("[")) {
                DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
                try {
                    JSONArray jsonArray = new JSONArray(data);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        dbHelper.addPost(
                                jsonObject.getString("content"),
                                jsonObject.getLong("expiryTimestamp"),
                                jsonObject.getDouble("latitude"),
                                jsonObject.getDouble("longitude")
                        );
                    }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Database updated with received data", Toast.LENGTH_SHORT).show());

                    sendDone();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }

        public void sendDone() {
            try {
                outputStream.write("DONE".getBytes());
                outputStream.flush();
                isDoneSent = true; // DONE送信済みにする
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error sending DONE signal: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }

        public void closeConnection() {
            synchronized (this) {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                        inputStream = null;
                    }
                    if (outputStream != null) {
                        outputStream.close();
                        outputStream = null;
                    }
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                        socket = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public class ClientClass extends Thread {
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress) {
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 3184), 500);

                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(String.valueOf(AUTH_CODE).getBytes());
                outputStream.flush();

                // サーバーからの応答を待機（例: 5秒のタイムアウト設定）
                socket.setSoTimeout(5000); // 違うアプリに接続した場合のケア

                sendReceive = new SendReceive(socket);
                sendReceive.start();
                sendDatabase();
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "クライアントエラー: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void sendDatabase() {
        DatabaseHelper dbHelper = new DatabaseHelper(MainActivity.this);
        List<Post> posts = dbHelper.getAllPosts();
        JSONArray jsonArray = new JSONArray();

        try {
            for (Post post : posts) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("content", post.getContent());
                jsonObject.put("expiryTimestamp", post.getExpiryTimestamp());
                jsonObject.put("latitude", post.getLatitude());
                jsonObject.put("longitude", post.getLongitude());
                jsonArray.put(jsonObject);
            }
            // デバッグ用ログ出力
            Log.d("sendDatabase", "Sending JSON data: " + jsonArray.toString());

            if (sendReceive != null) {
                sendReceive.write(jsonArray.toString().getBytes());
                Log.d("sendDatabase", "Data sent to client.");
            } else {
                Log.e("sendDatabase", "sendReceive is null, cannot send data.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }



    private void updateWifiButton() {
        if (wifiManager.isWifiEnabled()) {
            btnOnOff.setText("Wi-Fi_OFF");
        } else {
            btnOnOff.setText("Wi-Fi_ON");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
