package com.netnotes;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

public class NoteWatcher {

    private final WatchService m_watcher;
    private final NoteListener m_noteListener;
    private final ExecutorService m_executor;

    public NoteWatcher(File watchFile, NoteListener listener) throws IOException {

        m_watcher = FileSystems.getDefault().newWatchService();
        m_noteListener = listener;
        m_executor = Executors.newSingleThreadExecutor();

        Path path = watchFile.toPath();
        String pathString = path.toString();

        path.register(m_watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

        m_executor.execute(new Runnable() {
            private final ArrayList<String> eventCondenser = new ArrayList<>();

            private final ExecutorService executor = Executors.newCachedThreadPool(
                    new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });

            private Runnable notifyListener = () -> {
                for (int i = 0; i < eventCondenser.size(); i++) {
                    m_noteListener.onNoteChange(eventCondenser.get(i));
                }
                eventCondenser.clear();
            };

            private Future<?> lastExecution = null;

            @Override
            public void run() {
                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    WatchKey key;

                    try {
                        key = m_watcher.take();
                    } catch (InterruptedException e) {
                        break;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {

                        Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        Object contextObject = event.context();
                        if (contextObject != null && contextObject instanceof Path) {
                            Path eventContextPath = (Path) contextObject;
                            String filePathString = pathString + "/" + eventContextPath.toString();
                            if (!eventCondenser.contains(filePathString)) {
                                eventCondenser.add(filePathString);
                            }
                        } else {
                            continue;
                        }
                    }

                    int condenserSize = eventCondenser.size();
                    if (condenserSize > 0) {

                        if ((lastExecution != null && !lastExecution.isDone())) {

                        } else {
                            lastExecution = executor.submit(notifyListener);
                        }
                    }

                    key.reset();
                }
            }
        }
        );

    }

    public void shutdown() {
        m_executor.shutdownNow();
    }
}
