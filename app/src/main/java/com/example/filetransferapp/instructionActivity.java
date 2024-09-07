package com.example.filetransferapp;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class instructionActivity extends AppCompatActivity {
    TextView txtInstruction;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.instruction);

        txtInstruction = findViewById(R.id.txtInstruction);
        String instruction_user="Web Server :- It will enable file transfer on web browser\n" +
                "       URL - http://server_ip_displayed_on_main_screen:8089\n\n" +
                "FTP Server :- It will enable file transfer on FTP client like Filezilla, FileExplore +\n" +
                "       IP - server_ip_displayed_on_main_screen\n" +
                "       Port - 2121\n" +
                "       Username - admin\n" +
                "       Password - admin123\n" +
                "File Location for both files - open \"File Explorer\"  search android/media/FTPApplication/Files\n" +
                "For any file transfer server mobile will be used for storing files\n" +
                "Any files uploaded in this location will be visible to client\n" +
                "\n" +
                "Manage Files :- it is used for uploading files in server mobile device and deleting those files.";
        txtInstruction.setText(instruction_user);
    }
}
