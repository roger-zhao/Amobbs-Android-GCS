package com.tlog.helper;

import android.os.Handler;
import android.util.Log;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Parser;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Parse TLog file into Events
 */
public class TLogParser {
    private static final String LOG_TAG = TLogParser.class.getSimpleName();

    private static final Parser parser = new Parser();

    private static final TLogParserFilter DEFAULT_FILTER = new TLogParserFilter() {
        @Override
        public boolean includeEvent(Event e) {
            return true;
        }

        @Override
        public boolean shouldIterate() {
            return true;
        }
    };

    // Private constructor to prevent instantiation.
    private TLogParser() {
    }

    /**
     * * Returns a list of all events in specified TLog file
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static List<Event> getAllEvents(final File file) throws Exception {
        return getAllEvents(file, DEFAULT_FILTER);
    }

    /**
     * Returns a list of all events in specified TLog file using the specified filter
     *
     * @param file   {@link File}
     * @param filter {@link TLogParserFilter}
     * @return
     * @throws Exception
     */
    public static List<Event> getAllEvents(final File file, final TLogParserFilter filter) throws Exception {
        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            ArrayList<Event> eventList = new ArrayList<>();
            Event event = next(in);
            while (event != null && filter.shouldIterate()) {
                if (filter.includeEvent(event)) {
                    eventList.add(event);
                }
                event = next(in);
            }
            return eventList;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to close file " + file.getName(), e);
                }
            }
        }
    }

    /**
     * Returns a list of all events in specified TLog file
     *
     * @param handler  {@link Handler} Handler to specify what thread to callback on. This cannot be null.
     * @param file     {@link File}
     * @param callback {@link TLogParserCallback}
     */
    public static void getAllEventsAsync(final Handler handler, final File file, final TLogParserCallback callback) {
        getAllEventsAsync(handler, file, DEFAULT_FILTER, callback);
    }

    /**
     * Returns a list of all events in specified TLog file using the specified filter
     *
     * @param handler  {@link Handler} Handler to specify what thread to callback on. This cannot be null.
     * @param file     {@link File}
     * @param filter   {@link TLogParserFilter}
     * @param callback {@link TLogParserCallback}
     */
    public static void getAllEventsAsync(final Handler handler, final File file, final TLogParserFilter filter, final TLogParserCallback callback) {
        getInstance().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<Event> eventList = getAllEvents(file, filter);

                    if (eventList.isEmpty()) {
                        sendFailed(handler, callback, new NoSuchElementException());
                    } else {
                        sendResult(handler, callback, eventList);
                    }
                } catch (Exception e) {
                    sendFailed(handler, callback, e);
                }
            }
        });
    }

    private static void sendResult(Handler handler, final TLogParserCallback callback, final List<Event> events) {
        if (callback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(events);
                }
            });
        }
    }

    private static void sendFailed(Handler handler, final TLogParserCallback callback, final Exception e) {
        if (callback != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onFailed(e);
                }
            });
        }
    }

    private static Event next(DataInputStream in) throws IOException {
        try {
            // 此方法返回8个字节的输入流,解释为long值,实质上为时间戳
            long timestamp = in.readLong() / 1000;// 1s
            MAVLinkPacket packet;
            while ((packet = parser.mavlink_parse_char(in.readUnsignedByte())) == null) ;
            MAVLinkMessage message = packet.unpack();
            if (message == null) {
                return null;
            }
            return new Event(timestamp, message);
        } catch (EOFException e) {
            // File may not be complete so return null
            return null;
        }
    }

    private static ExecutorService getInstance() {
        return InitializeExecutorService.executorService;
    }

    /**
     * Iterator class to iterate and parse the Tlog file.
     */
    public static class TLogIterator {
        private static final TLogIteratorFilter DEFAULT_FILTER = new TLogIteratorFilter() {
            @Override
            public boolean acceptEvent(Event event) {
                return true;
            }
        };
        private final Handler handler;
        private File file;
        private DataInputStream in = null;

        /**
         * Constructor to create a TLogIterator with new Handler.
         *
         * @param file Location of the TLog files
         */
        public TLogIterator(File file) {
            this(file, new Handler());
        }

        /**
         * Constructor to create a TLogIterator with a specified Handler.
         *
         * @param file    Location of the TLog files
         * @param handler Handler to post results to
         */
        public TLogIterator(File file, Handler handler) {
            this.handler = handler;
            this.file = file;
        }

        /**
         * Opens TLog file to begin iterating.
         *
         * @throws FileNotFoundException
         */
        public void start() throws FileNotFoundException {
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        }

        /**
         * Closes TLog file
         *
         * @throws IOException
         */
        public void finish() throws IOException {
            in.close();
        }

        /**
         * Retrieve next message from TLog file asynchronously.
         * {@link #start()} must be called before this method.
         *
         * @param callback {@link TLogIteratorCallback}
         */
        public void nextAsync(final TLogIteratorCallback callback) {
            nextAsync(DEFAULT_FILTER, callback);
        }

        /**
         * Retrieve next message with specified message filter from TLog file asynchronously.
         * {@link #start()} must be called before this method.
         *
         * @param filter   {@link TLogIteratorFilter}
         * @param callback {@link TLogIteratorCallback}
         */
        public void nextAsync(final TLogIteratorFilter filter, final TLogIteratorCallback callback) {
            getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Event event = next(in);
                        while (event != null) {
                            if (filter.acceptEvent(event)) {
                                sendResult(callback, event);
                                return;
                            }
                            event = next(in);
                        }

                        sendFailed(callback, new NoSuchElementException());
                    } catch (IOException e) {
                        sendFailed(callback, e);
                    }
                }
            });
        }

        private void sendResult(final TLogIteratorCallback callback, final Event event) {
            if (callback != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(event);
                    }
                });
            }
        }

        private void sendFailed(final TLogIteratorCallback callback, final Exception e) {
            if (callback != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailed(e);
                    }
                });
            }
        }
    }

    private static class InitializeExecutorService {
        private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Mavlink message event.
     * Mavlink的事件类
     */
    public static class Event {
        private long timestamp;
        private MAVLinkMessage mavLinkMessage;

        private Event(long timestamp, MAVLinkMessage mavLinkMessage) {
            this.timestamp = timestamp;
            this.mavLinkMessage = mavLinkMessage;
        }

        /**
         * Returns time of mavlink message in ms
         *
         * @return
         */
        public long getTimestamp() {
            return timestamp;
        }

        /**
         * Returns content of mavlink message.
         *
         * @return {@link MAVLinkMessage}
         */
        public MAVLinkMessage getMavLinkMessage() {
            return mavLinkMessage;
        }
    }
}
