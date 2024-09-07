package com.example.filetransferapp;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import fi.iki.elonen.NanoHTTPD;

public class MyNanoHTTPD extends NanoHTTPD {
    private static final long INACTIVITY_LIMIT = 10 * 60 * 1000; // 10 minutes in milliseconds
    private long lastActivityTime;
    private Timer inactivityTimer;
    private static final int PORT = 8089;
    private static final String BASE_DIR = "/storage/emulated/0/Android/media/FTPApplication/Files";
    private static final String BASE_DIR_CHAT = "/storage/emulated/0/Android/media/FTPApplication/Chats";
    private final Context context;

    public MyNanoHTTPD(Context context) {
        super(PORT);
        this.context = context;
        //this.lastActivityTime = System.currentTimeMillis(); // Initialize the last activity time
        //startInactivityTimer();
    }

    @Override
    public void stop() {
        super.stop();
        //System.out.println("Server stopped successfully.");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String completepath = BASE_DIR+uri;
        //Log.d("MyNanoHTTPD", "Requested URI: " + uri);
        //updateLastActivityTime();

        try {
            if(uri.startsWith("/showFiles/")){
                //Log.d("MyNanoHTTPD", "Requested URI got hit : " + uri);
                String fileName = uri.substring("/showFiles/".length());
                String fullPath = BASE_DIR + "/"+fileName;

                File file = new File(fullPath);
                if (file.exists() && file.isFile()) {
                    try {
                            return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", new FileInputStream(file), file.length());
                    } catch (IOException e) {
                        e.printStackTrace();
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error");
                    }
                } else {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File Not Found");
                }
            }
            else{
            switch (uri) {
                case "/":
                    return servePage("index.html",uri);
                case "/showFiles":
                    return serveFileListPage(uri);
                case "/saveChat":
                    return (Method.POST.equals(session.getMethod())) ? saveChatMessage(session) : notFoundResponse();
                case "/deleteChat":
                    return (Method.POST.equals(session.getMethod())) ? deleteChatMessages(session) : notFoundResponse();
                case "/chat.txt":
                    return serveChatFile();
                case "/upload-file":
                    return (Method.POST.equals(session.getMethod())) ? handleFileUpload(session) : notFoundResponse();
                default:
                    return notFoundResponse();

            }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return internalErrorResponse();
        }
    }

    private void updateLastActivityTime() {
        lastActivityTime = System.currentTimeMillis();
    }

    private void startInactivityTimer() {
        inactivityTimer = new Timer(true);
        inactivityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkInactivity();
            }
        }, 60 * 1000, 60 * 1000); // Check every minute
    }

    private void checkInactivity() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastActivityTime > INACTIVITY_LIMIT) {
            //System.out.println("Stopping server due to inactivity...");
            stopServer();
        }
    }

    private void stopServer() {
        try {
            stop(); // Stop the NanoHTTPD server
            inactivityTimer.cancel(); // Cancel the inactivity timer
            Toast.makeText(context, "Server stopped on port : 8089 due to inactivity", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Response serveFileListPage(String uri) {
        String htmlContent = loadHtmlFromAssets("fileList.html");
        StringBuilder fileListHtml = new StringBuilder();
        File[] files = new File(BASE_DIR).listFiles();
        Log.d("MyNanoHTTPD", "All files1 : " + files.length);

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String fileUrl = uri.equals("/") ? "/" + fileName : uri + "/" + fileName;

                fileListHtml.append("<li><a href=\"").append(fileUrl).append("\">").append(fileName).append("</a>")
                        .append(" - <button onclick=\"previewFile('").append(fileUrl).append("')\">Preview</button>")
                        .append("</li>");
            }
        }

        return newFixedLengthResponse(Response.Status.OK, "text/html",
                htmlContent.replace("<ul id=\"fileList\"></ul>", "<ul id=\"fileList\">" + fileListHtml + "</ul>"));
    }



    private Response servePage(String pageName,String uri) {
        File[] files = new File(BASE_DIR).listFiles();
        //Log.d("MyNanoHTTPD", "All files : " + files.length);
        String htmlContent = loadHtmlFromAssets(pageName);
        StringBuilder fileListHtml = new StringBuilder();
        fileListHtml.append("<h1>Directory listing for ").append(uri).append("</h1>").append("<ul>");

        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String fileUrl = uri.equals("/") ? "/showFiles/" + fileName : uri + "/" + fileName;
                fileListHtml.append("<li><a href=\"").append(fileUrl).append("\">").append(fileName).append("</a>").append("</li>");
            }
        }
        fileListHtml.append("</ul>");
            htmlContent = htmlContent.replace("<div id=\"main-right\"></div>", "<div id=\"main-right\">" + fileListHtml + "</div>");


        return newFixedLengthResponse(Response.Status.OK, "text/html", htmlContent);
    }

    private String loadHtmlFromAssets(String htmlPage) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(htmlPage)))) {
            StringBuilder htmlContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                htmlContent.append(line).append("\n");
            }
            return htmlContent.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private Response handleFileUpload(IHTTPSession session) throws IOException, ResponseException {
        Map<String, String> files = new HashMap<>();
        session.parseBody(files);
        String tempFilePath = files.get("file");
        String filename = session.getParameters().get("filename").get(0).replace("[", "").replace("]", "");

        File baseDir = new File(BASE_DIR);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            return internalErrorResponse();
        }

        try (FileInputStream fis = new FileInputStream(tempFilePath);
             FileOutputStream fos = new FileOutputStream(new File(BASE_DIR, filename))) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            //loadHtmlFromAssets("index.html");
            return newFixedLengthResponse(Response.Status.OK, "text/html", "File uploaded successfully: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
            return internalErrorResponse();
        }
    }

    private Response saveChatMessage(IHTTPSession session) throws IOException, ResponseException {
        session.parseBody(new HashMap<>());
        String message = session.getParameters().get("message").toString();
        File chatFile = new File(BASE_DIR_CHAT, "chat.txt");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(chatFile, true))) {
            bw.write(message);
            bw.newLine();
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Message saved successfully");
        } catch (IOException e) {
            e.printStackTrace();
            return internalErrorResponse();
        }
    }

    private Response deleteChatMessages(IHTTPSession session) throws IOException, ResponseException {
        session.parseBody(new HashMap<>());
        List<String> selectedMessages = session.getParameters().get("selectedMessages");
        File chatFile = new File(BASE_DIR_CHAT, "chat.txt");

        if (selectedMessages == null || selectedMessages.isEmpty()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No messages selected for deletion");
        }

        try (BufferedReader br = new BufferedReader(new FileReader(chatFile))) {
            StringBuilder remainingMessages = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (!selectedMessages.contains(line)) {
                    remainingMessages.append(line).append("\n");
                }
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(chatFile))) {
                bw.write(remainingMessages.toString());
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Selected messages deleted successfully");
            } catch (IOException e) {
                e.printStackTrace();
                return internalErrorResponse();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return internalErrorResponse();
        }
    }


    private Response serveChatFile() {
        File chatFile = new File(BASE_DIR_CHAT, "chat.txt");
        if (!chatFile.exists()) {
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "");
        }

        try (FileInputStream fis = new FileInputStream(chatFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return newFixedLengthResponse(Response.Status.OK, "text/plain", baos.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return internalErrorResponse();
        }
    }

    private Response internalErrorResponse() {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server Error");
    }

    private Response notFoundResponse() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
    }
}
