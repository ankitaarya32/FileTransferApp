package com.example.filetransferapp;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FTPServerService extends Service {

    private FtpServer ftpServer;

    @Override
    public void onCreate() {
        super.onCreate();

        // Copy the users.properties file from assets to internal storage
        copyUserPropertiesFile();

        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(2121); // Set the port on which the server will listen

        serverFactory.addListener("default", factory.createListener());

        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setFile(new File(getFilesDir(), "users.properties"));

        // Use ClearTextPasswordEncryptor to allow plain text passwords
        userManagerFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor());
        serverFactory.setUserManager(userManagerFactory.createUserManager());

        ftpServer = serverFactory.createServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (ftpServer != null && !ftpServer.isStopped()) {
                Toast.makeText(this, "FTP server is already running on : 2121", Toast.LENGTH_SHORT).show();
                //Log.w("FTPServerService", "FTP server is already running.");
            } else {
                ftpServer.start();
                Toast.makeText(this, "FTP server started on : 2121", Toast.LENGTH_SHORT).show();
                //Log.i("FTPServerService", "FTP server started successfully.");
            }
        } catch (IllegalStateException e) {
            Toast.makeText(this, "FTP server is already running on : 2121", Toast.LENGTH_SHORT).show();
            //Log.e("FTPServerService", "FTP server is already running or in an illegal state.", e);
        } catch (FtpException e) {
            Toast.makeText(this, "Error starting FTP server.", Toast.LENGTH_SHORT).show();
            //Log.e("FTPServerService", "Error starting FTP server.", e);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ftpServer != null && !ftpServer.isStopped()) {
            ftpServer.stop();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void copyUserPropertiesFile() {
        AssetManager assetManager = getAssets();
        InputStream in = null;
        FileOutputStream out = null;

        try {
            in = assetManager.open("users.properties");
            File outFile = new File(getFilesDir(), "users.properties");
            out = new FileOutputStream(outFile);
            int bufferSize = 32 * 1024;// increase dec file speed
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("FTPServerService", "Error copying users.properties", e);
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
