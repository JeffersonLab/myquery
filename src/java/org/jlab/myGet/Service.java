package org.jlab.myGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ryans
 */
public class Service {

    private final static Logger LOGGER = Logger.getLogger(Service.class.getName());

    private static final long MAX_EXECUTE_MILLIS = 30000;

    public List<Record> getRecordList(String c, String b, String e,
            String l, String p, String m, String M, String d, String f,
            String s) throws
            Exception {

        List<Record> recordList = null;

        String execPath = System.getenv("MYGET_PATH");

        if (execPath == null || execPath.isEmpty()) {
            throw new Exception("Please set environment variable MYGET_PATH");
        }
        List<String> command = new ArrayList<>();
        command.add(execPath);
        if (c == null) {
            throw new Exception("Channel argument (c) is required");
        } else {
            command.add("-c");
            command.add(c);
        }
        if (b != null && !b.isEmpty()) {
            command.add("-b");
            command.add(b);
        }
        if (e != null && !e.isEmpty()) {
            command.add("-e");
            command.add(e);
        }
        if (l != null && !l.isEmpty()) {
            command.add("-l");
            command.add(l);
        }
        if (m != null && !m.isEmpty()) {
            command.add("-m");
            command.add(m);
        }
        if (M != null && !M.isEmpty()) {
            command.add("-M");
            command.add(M);
        }
        if (d != null && !d.isEmpty()) {
            command.add("-d");
            command.add(d);
        }
        if (f != null && !f.isEmpty()) {
            command.add("-f");
            command.add(f);
        }
        
        // Boolean flags
        if (s != null) {
            command.add("-s");
        }
        if (p != null) {
            command.add("-p");
        }

        LOGGER.log(Level.FINEST, "command = {0}", command.toString());
        ProcessBuilder builder = new ProcessBuilder(command);

        builder.redirectErrorStream(true);

        Timer timer = new Timer();
        timer.schedule(new InterruptTimerTask(Thread.currentThread()), MAX_EXECUTE_MILLIS);
        Process proc = builder.start();
        StreamGobbler gobbler = new StreamGobbler(proc.getInputStream());
        Thread t = new Thread(gobbler);
        t.start();

        try {
            int status = proc.waitFor();

            if (status != 0) {
                throw new Exception("Unexpected status from process: " + status + "; output: " + gobbler.toString());
            }
            recordList = parseOutput(gobbler.toString());
        } catch (InterruptedException ex) {
            proc.destroy();
            throw new Exception("Interrupted while waiting for process", ex);
        } finally {
            // If task completes without interruption we must cancel the 
            // interrupt task to prevent interrupt later on!
            timer.cancel();

            // Clear interrupted flag for two cases:
            // (1) task completed but timer task sets interrupt flag before
            // we can cancel it
            // (2) task isn't completed and is interrupted by timer task; note 
            // that most things in Java clear the interrupt flag before throwing
            // and exception, but Process.waitFor does not;
            // see http://bugs.sun.com/view_bug.do?bug_id=6420270
            Thread.interrupted();
        }

        return recordList;
    }

    private List<Record> parseOutput(String output) throws Exception {
        //LOGGER.log(Level.FINEST, "Output: {0}", output);

        List<Record> recordList = new ArrayList<>();

        try (Scanner scanner = new Scanner(output)) {
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //LOGGER.log(Level.INFO, "Line: {0}", line);
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 3 ) {
                    String date = tokens[0] + " " + tokens[1];
                    String channelValue = tokens[2];
                    for(int i=3; i < tokens.length; i++) {
                        channelValue = channelValue + " " + tokens[i];
                    }
                    recordList.add(new Record(date, channelValue));
                } else {
                    throw new Exception("Unexpected number of tokens");
                }
            }
        }

        return recordList;
    }

    private class StreamGobbler implements Runnable {

        private final InputStream in;
        private final StringBuilder builder = new StringBuilder();

        StreamGobbler(InputStream in) {
            this.in = in;
        }

        @Override
        public String toString() {
            return builder.toString();
        }

        @Override
        public void run() {
            try {
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader buffer = new BufferedReader(reader);
                String line = null;
                while ((line = buffer.readLine()) != null) {
                    builder.append(line);
                    builder.append("\n");
                    //LOGGER.log(Level.INFO, line);
                }
            } catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, "Unable to gobble stream", ioe);
            }
        }
    }

    private class InterruptTimerTask extends TimerTask {

        private final Thread thread;

        public InterruptTimerTask(Thread t) {
            this.thread = t;
        }

        @Override
        public void run() {
            thread.interrupt();
        }
    }
}
