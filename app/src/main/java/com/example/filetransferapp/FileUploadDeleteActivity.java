package com.example.filetransferapp;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import android.os.Handler;


public class FileUploadDeleteActivity extends AppCompatActivity {
    private static final String BASE_DIR_FILES = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/media/FTPApplication/Files";
    private ListView fileListView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> fileList;

    // Progress dialog components
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressPercentage, timeRemaining;
    private Button cancelButton;
    private Handler handler = new Handler(Looper.getMainLooper());
    private int progress = 0;
    private boolean isUploading = false; // Flag to handle cancel upload

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_upload_delete);

        fileListView = findViewById(R.id.fileListView);
        fileList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileList);
        fileListView.setAdapter(adapter);

        Button uploadButton = findViewById(R.id.uploadFileButton);
        Button deleteButton = findViewById(R.id.deleteFileButton);

        // Handle file upload button
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFileForUpload();
            }
        });

        // Handle file delete button
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSelectedFile();
            }
        });

        // Load files in the directory
        loadFilesInDirectory();
    }

    // Launcher for picking a file
    private final ActivityResultLauncher<String> pickFileLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    showProgressPopup(); // Show progress popup before upload starts
                    uploadFile(uri);
                }
            });

    // Method to trigger file picker
    private void pickFileForUpload() {
        pickFileLauncher.launch("*/*");
    }

    // Method to show progress popup
    private void showProgressPopup() {
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popup_progress, null);

        progressBar = popupView.findViewById(R.id.progressBar);
        progressPercentage = popupView.findViewById(R.id.progressPercentage);
        timeRemaining = popupView.findViewById(R.id.timeRemaining);
        cancelButton = popupView.findViewById(R.id.cancelButton);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popupView);
        builder.setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();

        cancelButton.setOnClickListener(v -> {
            isUploading = false; // Cancel the upload
            progressDialog.dismiss();
            Toast.makeText(FileUploadDeleteActivity.this, "Upload Canceled", Toast.LENGTH_SHORT).show();
        });

        isUploading = true; // Reset the flag for upload
    }

    // Method to upload a file to app's specific directory with progress
    private void uploadFile(Uri uri) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                String fileName = getFileName(uri);
                File destFile = new File(BASE_DIR_FILES, fileName);

                if (!destFile.exists()) {
                    destFile.createNewFile();
                }

                FileOutputStream outputStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[1024];
                int length;
                long totalBytes = inputStream.available();
                long uploadedBytes = 0;
                long startTime = System.currentTimeMillis();

                while ((length = inputStream.read(buffer)) > 0 && isUploading) {
                    outputStream.write(buffer, 0, length);
                    uploadedBytes += length;

                    // Update progress
                    int currentProgress = (int) ((uploadedBytes * 100) / totalBytes);
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long remainingTime = (totalBytes - uploadedBytes) * elapsedTime / uploadedBytes;

                    handler.post(() -> updateProgress(currentProgress, remainingTime / 1000)); // Update progress in the UI thread
                }

                inputStream.close();
                outputStream.close();

                // If upload completes successfully
                if (isUploading) {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        fileList.add(fileName);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(FileUploadDeleteActivity.this, "File uploaded: " + fileName, Toast.LENGTH_LONG).show();
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(FileUploadDeleteActivity.this, "Error uploading file", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Method to update progress in the popup
    private void updateProgress(int currentProgress, long remainingTimeSeconds) {
        progressBar.setProgress(currentProgress);
        progressPercentage.setText(currentProgress + "%");
        timeRemaining.setText("Time Remaining: " + remainingTimeSeconds + " seconds");

        if (currentProgress == 100) {
            progressDialog.dismiss();
            Toast.makeText(this, "Upload Complete!", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to get file name from Uri
    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    // Method to load files in directory
    private void loadFilesInDirectory() {
        File directory = new File(BASE_DIR_FILES);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                fileList.add(file.getName());
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Method to delete the selected file from the list
    private void deleteSelectedFile() {
        if (!fileListView.isItemChecked(fileListView.getCheckedItemPosition())) {
            Toast.makeText(this, "Please select a file to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        String fileName = fileList.get(fileListView.getCheckedItemPosition());
        File fileToDelete = new File(BASE_DIR_FILES, fileName);

        if (fileToDelete.exists()) {
            if (fileToDelete.delete()) {
                fileList.remove(fileName);
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "File deleted: " + fileName, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
