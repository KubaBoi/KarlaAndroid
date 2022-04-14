package com.example.karla;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommunicationManager {

    private String LOG_TAG = "COMMUNICATION";
    private String inputFile;
    private String outputFile;

    private BufferedOutputStream wr;
    private InputStream in;

    public void start(String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public boolean getFromSound() {
        byte[] bytesArray = new byte[0];
        try {
            Path file = Paths.get(inputFile);
            bytesArray = Files.readAllBytes(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return getMp3("/recognition/fromWav", bytesArray);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean getFromJson(JSONObject json) {
        try {
            return getMp3("/recognition/toMp3", json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean getMp3(String surl, byte[] bytes) throws IOException {
        URL url = new URL("http://192.168.0.110:8004" + surl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        boolean ret = false;

        try {
            Log.e(LOG_TAG, "Sending POST: " + url.getPath());
            urlConnection.setRequestMethod("POST");

            urlConnection.setDoOutput(true);

            wr = new BufferedOutputStream(urlConnection.getOutputStream());
            wr.write(bytes, 0, bytes.length);
            wr.flush();
            wr.close();

            InputStream in = urlConnection.getInputStream();
            byte[] data = readByteStream(in);

            File file = new File(outputFile);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(outputFile);
            stream.write(data);
            Log.e(LOG_TAG, "File was saved");
            ret = true;
        }
        catch (Exception e) {
            in = urlConnection.getErrorStream();
            Log.e(LOG_TAG, Integer.toString(urlConnection.getResponseCode()));
            Log.e(LOG_TAG, readStream(new InputStreamReader(in)));
        } finally {
            urlConnection.disconnect();
        }

        return ret;
    }

    private byte[] readByteStream(InputStream inp) throws IOException {
        final int bufLen = 1024;
        byte[] buf = new byte[bufLen];
        int readLen;
        IOException exception = null;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while ((readLen = inp.read(buf, 0, bufLen)) != -1)
                outputStream.write(buf, 0, readLen);

            return outputStream.toByteArray();
        } catch (IOException e) {
            exception = e;
            throw e;
        } finally {
            if (exception == null) inp.close();
            else try {
                inp.close();
            } catch (IOException e) {
                exception.addSuppressed(e);
            }
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
