package org.hombro.utils;

import com.sun.istack.internal.logging.Logger;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private final static Logger log = Logger.getLogger(Controller.class);
    private final static String path = "E:\\code\\bin";
    private final static String ffmpeg = path + "\\ffmpeg.exe";
    private final static String youtubeDl = path + "\\youtube-dl.exe";

    @FXML
    public WebView webView;
    @FXML
    public Button btnVideo, btnMp3;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.webView.getEngine().load("https://www.youtube.com");
        this.webView.getEngine().getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    System.out.println("newState = " + newState);
                    if (newState == Worker.State.SUCCEEDED) {
                        System.out.println(webView.getEngine().getLocation());
                    }
                });
    }

    private String retrieveUrl() {
        ObservableList<WebHistory.Entry> entries = webView.getEngine().getHistory().getEntries();
        return entries.get(entries.size() - 1).getUrl();
    }

    private String createVideoTitle(String url) {
        log.info("createVideoTitle " + url);
        String line;
        try {
            ProcessBuilder p = (new ProcessBuilder()).command(Arrays.asList(youtubeDl, "--skip-download", "--get-title", url));
            log.info("issuing command: " + String.join(" ", p.command()));
            Process process = p.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            line = in.readLine();
            process.waitFor();
            in.close();
        } catch (InterruptedException | IOException e) {
            line = "";
        }
        return line;
    }

    private void downloadVideo(String url) throws IOException, InterruptedException {
        log.info("downloadVideo " + url);
        ProcessWrappers.voidProc(Arrays.asList(youtubeDl, url), path);
    }

    private void downloadMp3(String url) throws IOException, InterruptedException {
        log.info("downloadMp3 " + url);
        downloadVideo(url);
        File directory = new File(path);
        String title = createVideoTitle(url).replace("/", "_");
        log.info("video title: " + title);
        File[] temp = directory.listFiles(f -> f.getName().contains(title));
        if (temp.length != 1) {
            log.info("got " + temp.length + " " + temp);
            return;
        }

        log.info("going to extract audio from " + temp[0].getName());
        ProcessWrappers.voidProc(Arrays.asList(
                ffmpeg,
                "-i",
                String.format("\"%s\"", temp[0].getAbsolutePath()),
                "-vn",
                "-ac",
                "2",
                "-ar",
                "44100",
                "-ab",
                "320k",
                "-f",
                "mp3",
                String.format("\"%s.mp3\"", title)
        ), path);
        log.info("deleting " + temp[0].getName());
        if(!temp[0].delete())
            log.info("failed to delete " + temp[0].getName());
    }

    @FXML
    public void clickVideo(Event event) throws IOException, InterruptedException {
        log.info("clickVideo " + event.toString());
        downloadVideo(retrieveUrl());
    }

    @FXML
    public void clickMusic(Event event) throws IOException, InterruptedException {
        log.info("clickMusic " + event.toString());
        downloadMp3(retrieveUrl());
    }
}
