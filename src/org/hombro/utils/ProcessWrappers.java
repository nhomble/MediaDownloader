package org.hombro.utils;


import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by nicolas on 11/13/2016.
 */
public class ProcessWrappers {
    private final static java.util.logging.Logger log = java.util.logging.Logger.getAnonymousLogger();
    public static void trackProcess(Process process) throws InterruptedException, IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuffer out = new StringBuffer(), error = new StringBuffer();
        String temp;
        while ((temp = in.readLine()) != null) {
            out.append(temp);
        }
        log.info(out.toString());
        while ((temp = err.readLine()) != null) {
            error.append(temp);
        }
        log.info(error.toString());
        process.waitFor();
        log.info("process is executing normally");

        in.close();
    }

    public static Task voidProc(List<String> args, String path) throws IOException, InterruptedException {
        return new Task(){

            @Override
            protected Object call() throws Exception {
                ProcessBuilder builder = (new ProcessBuilder()).directory(new File(path)).command(args);
                Process p = builder.start();
                log.info("issuing command: " + String.join(" ", builder.command()));
                trackProcess(p);
                return null;
            }
        };
    }
}
