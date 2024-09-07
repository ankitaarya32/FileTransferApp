package com.example.filetransferapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/media/FTPApplication/";
    private static final String BASE_DIR_FILES = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/media/FTPApplication/Files";
    private static final String BASE_DIR_CHAT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/media/FTPApplication/Chats";
    private static final int REQUEST_CODE = 1001;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private TextView tvServerIP;
    private Button btnStartServer,btnStopServer,btnFtpStartServer,btnFtpStopServer,btnInstruction;
    private final MyNanoHTTPD httpServer = new MyNanoHTTPD(this);
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //requestStoragePermissions();
        if (hasStoragePermissions()) {
            // If permissions are granted, proceed to the main activity
            createDirectoryOnStart();
            setContentView(R.layout.activity_main);
            initializeUI();
        } else {
            // If permissions are not granted, request them
            requestStoragePermissions();
        }
        tvServerIP = findViewById(R.id.tvServerIP);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        updateIPAddress(); // Initial IP display
        registerNetworkCallback(); // Monitor network changes
        initializeUI();
        createDirectoryOnStart();
        //startLocalServers();
    }
    private void createDirectoryOnStart() {
        File directory = new File(BASE_DIR);
        File directory_chat = new File(BASE_DIR_CHAT);
        File directory_files = new File(BASE_DIR_FILES);

        if (!directory.exists()) directory.mkdirs();
        if (!directory_chat.exists()) directory_chat.mkdirs();
        if (!directory_files.exists()) directory_files.mkdirs();
    }

    private boolean hasStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            activityResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                             createDirectoryOnStart();
                            // Permission granted, now open activity_main
                            setContentView(R.layout.activity_main);
                            initializeUI();
                        }
                    });
            activityResultLauncher.launch(intent);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now open activity_main
                updateIPAddress();
                setContentView(R.layout.activity_main);
                initializeUI();
            } else {
                Toast.makeText(this, "Storage permissions are required", Toast.LENGTH_SHORT).show();
                finish(); // Close the app if permissions are not granted
            }
        }
    }

    private void registerNetworkCallback() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                runOnUiThread(() -> updateIPAddress()); // Update IP when network is available
            }
            @Override
            public void onLost(Network network) {
                super.onLost(network);
                runOnUiThread(() -> updateIPAddress());
            }
        };

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }



    private void initializeUI() {
        tvServerIP = findViewById(R.id.tvServerIP);
        btnStartServer = findViewById(R.id.btnStartServer);
        btnStopServer = findViewById(R.id.btnStopServer);
        btnFtpStartServer = findViewById(R.id.btnFtpStartServer);
        btnFtpStopServer = findViewById(R.id.btnFtpStopServer);
        btnInstruction = findViewById(R.id.btnInstruction);
        updateIPAddress();

        btnStartServer.setOnClickListener(v -> {
            startLocalServers();
        });
        btnFtpStartServer.setOnClickListener(v -> {
            startLocalFtpServers();
        });
        btnFtpStopServer.setOnClickListener(v -> {
            stopLocalFtpServers();
        });
        btnStopServer.setOnClickListener(v -> {
            stopLocalServers();
        });
        btnInstruction.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, instructionActivity.class);
            startActivity(intent);
        });
        // Start File Management UI
        findViewById(R.id.manageFilesButton).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FileUploadDeleteActivity.class);
            startActivity(intent);
        });
    }

    void updateIPAddress() {
        String ipAddress = IPAddressUtil.getIPAddress(this);
        String LocalIpAddress = IPAddressUtil.getLocalIpAddress();
        tvServerIP.setText("Server IP: " + (ipAddress != null ? ipAddress+" or "+LocalIpAddress : LocalIpAddress));
    }

    private void startLocalServers() {
        // Start HTTP server
        try {
            httpServer.start();
            Toast.makeText(this, "Server Started on port : 8089", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            //e.printStackTrace();
            Toast.makeText(this, "Server already running on port : 8089", Toast.LENGTH_SHORT).show();
        }
    }
    private void startLocalFtpServers() {
        // Start FTP server
        try{
            startService(new Intent(this, FTPServerService.class));
            //Toast.makeText(this, "FTP Server Started on port : 2121", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            //Toast.makeText(this, "FTP Server  already Started on port : 2121", Toast.LENGTH_SHORT).show();
        }

    }



    private void stopLocalServers() {
        // stop HTTP server
        try{
        httpServer.stop();
        Toast.makeText(this, "Server stopped on port : 8089", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e ){
            //e.printStackTrace();
            Toast.makeText(this, "Server not running on port : 8089", Toast.LENGTH_SHORT).show();
        }
    }
    private void stopLocalFtpServers() {
        try{
            // Start FTP server
            stopService(new Intent(this, FTPServerService.class));
            Toast.makeText(this, "Server stopped on port : 2121", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e ){
            //e.printStackTrace();
            Toast.makeText(this, "Server already running on 2121", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }


}
