package org.hombro.utils;

import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class Controller implements Initializable {
    private final static Logger log = Logger.getAnonymousLogger();

    @FXML
    public WebView webView;
    @FXML
    public Button btnVideo, btnMp3;
    @FXML
    public TextField path = new TextField(), youtubeDl = new TextField(), ffmpeg = new TextField(), viewUrl = new TextField();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        path.setText("E:\\code\\bin");
        ffmpeg.setText(path.getText() + "\\ffmpeg.exe");
        youtubeDl.setText(path.getText() + "\\youtube-dl.exe");
        viewUrl.setText("https://www.youtube.com");
        this.webView.getEngine().load(viewUrl.getText());
        this.webView.getEngine().getLoadWorker().stateProperty().addListener(
                (ov, oldState, newState) -> {
                    log.info("newState = " + newState);
                    if (newState == Worker.State.SUCCEEDED) {
                        log.info(webView.getEngine().getLocation());
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
            ProcessBuilder p = (new ProcessBuilder()).command(Arrays.asList(youtubeDl.getText(), "--skip-download", "--get-title", "--verbose", url));
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
        ProcessWrappers.voidProc(Arrays.asList(youtubeDl.getText(), "--verbose", url), path.getText());
    }

    private void downloadMp3(String url) throws IOException, InterruptedException {
        log.info("downloadMp3 " + url);
        downloadVideo(url);
        File directory = new File(path.getText());
        String title = createVideoTitle(url).replace("/", "_");
        log.info("video title: " + title);
        File[] temp = directory.listFiles(f -> f.getName().contains(title));
        if (temp.length != 1) {
            log.info("got " + temp.length + " " + temp);
            return;
        }

        log.info("going to extract audio from " + temp[0].getName());
        ProcessWrappers.voidProc(Arrays.asList(
                ffmpeg.getText(),
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
        ), path.getText());
        log.info("deleting " + temp[0].getName());
        if(!temp[0].delete())
            log.info("failed to delete " + temp[0].getName());
    }

    @FXML
    public void commitText(KeyEvent event){
        if(event.getCode() == KeyCode.ENTER){
            if(event.getSource() == viewUrl){
                log.info("explicitly changing the web address! " + viewUrl.getText());
                webView.getEngine().load(viewUrl.getText());
            }
        }
    }

    @FXML
    public void click(Event event) throws IOException, InterruptedException {
        log.info("click " + event.toString());
        if(event.getSource() == btnMp3)
            downloadMp3(retrieveUrl());
        else if(event.getSource() == btnVideo)
            downloadVideo(retrieveUrl());
        else
            log.info("unknown click! " + event.toString());
    }
}
