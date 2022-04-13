package com.example.karla;

import android.provider.DocumentsContract;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CommunicationManager {

    private String LOG_TAG = "COMMUNICATION";
    private String wavFile;

    public void start(String wavFile) {
        this.wavFile = wavFile;
    }

    public void sendWavFile() {
        try {
            File file = new File(wavFile);
            int size = (int) file.length();
            byte[] bytes = new byte[size];

            sendPost("/recognition/fromWav", bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendPost(String surl, byte[] bytes) throws IOException {
        URL url = new URL("http://192.168.0.110:8004" + surl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        String data = "";

        try {
            Log.e(LOG_TAG, "Sending POST: " + url.getPath());
            urlConnection.setRequestMethod("POST");

            urlConnection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.write(bytes);
            wr.flush();
            wr.close();

            InputStream in = urlConnection.getInputStream();
            Log.e(LOG_TAG, readStream(new InputStreamReader(in)));
        }
        catch (Exception e) {
            InputStream in = urlConnection.getErrorStream();
            Log.e(LOG_TAG, Integer.toString(urlConnection.getResponseCode()));
            Log.e(LOG_TAG, readStream(new InputStreamReader(in)));
        } finally {
            urlConnection.disconnect();
        }
    }

    public void sendGet(String surl) throws IOException {
        URL url = new URL("http://192.168.0.110:8004" + surl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        String data = "";

        try {
            Log.e(LOG_TAG, "Sending GET: " + url.getPath());
            urlConnection.setRequestMethod("GET");

            InputStream in = urlConnection.getInputStream();
            Log.e(LOG_TAG, readStream(new InputStreamReader(in)));
        }
        catch (Exception e) {
            InputStream in = urlConnection.getErrorStream();
            Log.e(LOG_TAG, Integer.toString(urlConnection.getResponseCode()));
            Log.e(LOG_TAG, readStream(new InputStreamReader(in)));
        } finally {
            urlConnection.disconnect();
        }
    }

    private String readStream(InputStreamReader inp) throws IOException {
        String data = "";
        int inputStreamData = inp.read();
        while (inputStreamData != -1) {
            char current = (char) inputStreamData;
            inputStreamData = inp.read();
            data += current;
        }
        return data;
    }
}
