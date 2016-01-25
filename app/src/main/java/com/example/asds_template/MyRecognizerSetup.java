package com.example.asds_template;

import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import java.io.File;
import java.io.IOException;

/**
 * Created by harry on 11/5/15.
 */
public class MyRecognizerSetup {
    private final Config config;

    public static MyRecognizerSetup defaultSetup() {
        return new MyRecognizerSetup(Decoder.defaultConfig());
    }

    public static MyRecognizerSetup setupFromFile(File configFile) {
        return new MyRecognizerSetup(Decoder.fileConfig(configFile.getPath()));
    }

    private MyRecognizerSetup(Config config) {
        this.config = config;
    }

    public MyRecognizer getRecognizer() throws IOException {
        return new MyRecognizer(this.config);
    }

    public MyRecognizerSetup setAcousticModel(File model) {
        return this.setString("-hmm", model.getPath());
    }

    public MyRecognizerSetup setDictionary(File dictionary) {
        return this.setString("-dict", dictionary.getPath());
    }

    public MyRecognizerSetup setSampleRate(int rate) {
        return this.setFloat("-samprate", (double)rate);
    }

    public MyRecognizerSetup setRawLogDir(File dir) {
        return this.setString("-rawlogdir", dir.getPath());
    }

    public MyRecognizerSetup setKeywordThreshold(float threshold) {
        return this.setFloat("-kws_threshold", (double)threshold);
    }

    public MyRecognizerSetup setBoolean(String key, boolean value) {
        this.config.setBoolean(key, value);
        return this;
    }

    public MyRecognizerSetup setInteger(String key, int value) {
        this.config.setInt(key, value);
        return this;
    }

    public MyRecognizerSetup setFloat(String key, double value) {
        this.config.setFloat(key, value);
        return this;
    }

    public MyRecognizerSetup setString(String key, String value) {
        this.config.setString(key, value);
        return this;
    }

    static {
        System.loadLibrary("pocketsphinx_jni");
    }
}
