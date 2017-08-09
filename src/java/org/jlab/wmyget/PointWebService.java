package org.jlab.wmyget;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
public class PointWebService {

    private final static Logger LOGGER = Logger.getLogger(PointWebService.class.getName());

    private static final long MAX_EXECUTE_MILLIS = 300000;

    public Record getRecord(String c, String t, String m, String M, String d, String f,
            String w, String s) throws
            Exception {

        Record record = null;

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
        if (t == null || t.trim().isEmpty()) {
            throw new Exception("Time of interest argument (t) is required");
        }
        command.add("-t");
        command.add(t);
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
        if (w != null) {
            command.add("-w+");
        }        
        if (s != null) {
            command.add("-s");
        }

        LOGGER.log(Level.FINEST, "command = {0}", command.toString());
        ProcessBuilder builder = new ProcessBuilder(command);

        builder.redirectErrorStream(true);

        Timer timer = new Timer();
        timer.schedule(new InterruptTimerTask(Thread.currentThread()), MAX_EXECUTE_MILLIS);
        Process proc = builder.start();
        // Note: we don't gobble stream in current thread because InputStream.read may block and is 
        // not interruptable - so it is tricky to implement a timeout.   Putting the potentially 
        // blocked stream in a separate thread allows us to wait with a timeout and then "destroy"
        // the process that is making the gobbler thread block
        StreamGobbler gobbler = new StreamGobbler(proc.getInputStream());
        Thread thread = new Thread(gobbler);
        thread.start();

        try {
            int status = proc.waitFor(); // This may result in InterruptedException

            // I've observed it is possible for waitFor (process thread) to return yet gobbler 
            // thread is still running.
            // Though unlikely, worst case execution time could be close to MAX_EXECUTE_MILLIS X 2
            synchronized (gobbler) {
                if (!gobbler.isDone()) {
                    //LOGGER.log(Level.INFO, "Gobbler is still gobbling");
                    //t.join(MAX_EXECUTE_MILLIS); // This might be more concise than using wait on gobbler, but both do essentially same thing
                    gobbler.wait(MAX_EXECUTE_MILLIS); // Can't interrupt gobbler so we just abandon it after timeout
                    if (!gobbler.isDone()) {
                        proc.destroy(); // This should kill the gobbler thread too indirectly, I hope
                        throw new Exception(
                                "Process I/O Gobbler thread is still not done, but I'm tired of waiting");
                    }
                }
            }

            if (status != 0) {
                throw new Exception("Unexpected status from process: " + status + "; output: "
                        + gobbler.toString());
            }

            record = parseOutput(gobbler.toString());
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

        return record;
    }

    private Record parseOutput(String output) throws Exception {
        //LOGGER.log(Level.FINEST, "Output: {0}", output);

        Record record = null;

        try (Scanner scanner = new Scanner(output)) {

            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //LOGGER.log(Level.INFO, "Line: {0}", line);
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 3) {
                    String date = tokens[0] + "T" + tokens[1];
                    String channelValue = tokens[2];
                    for (int i = 3; i < tokens.length; i++) {
                        channelValue = channelValue + " " + tokens[i];
                    }
                    record = new Record(date, channelValue);
                } else {
                    throw new Exception("Unexpected number of tokens");
                }
            }
        }

        return record;
    }

    private class StreamGobbler implements Runnable {

        private final InputStream in;
        private String output = null;
        private boolean done = false;

        StreamGobbler(InputStream in) {
            this.in = in;
        }

        @Override
        public String toString() {
            return output;
        }

        public synchronized boolean isDone() {
            return done;
        }

        @Override
        public void run() {
            try {
                //long start = System.currentTimeMillis();
                doItFast();
                //long end = System.currentTimeMillis();
                //LOGGER.log(Level.INFO, "Gobble Seconds: {0}", (end - start) / 1000.0f);
            } catch (IOException ioe) {
                LOGGER.log(Level.SEVERE, "Unable to gobble stream", ioe);
            }

            synchronized (this) {
                done = true;
                notifyAll();
            }
        }

        private void doItFast() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(in, out);
            output = out.toString("UTF-8");
        }

        private long copy(InputStream in, OutputStream out)
                throws IOException {
            byte[] buffer = new byte[4096];
            long count = 0;
            int n = 0;

            while (-1 != (n = in.read(buffer))) {
                out.write(buffer, 0, n);
                count += n;
            }

            return count;
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
