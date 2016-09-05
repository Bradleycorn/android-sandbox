/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bradball.android.sandbox.util;

import android.util.Log;

import net.bradball.android.sandbox.BuildConfig;
import net.bradball.android.sandbox.provider.RecordingsProvider;
import net.bradball.android.sandbox.service.MusicHandlerThread;
import net.bradball.android.sandbox.service.MusicService;
import net.bradball.android.sandbox.sync.ArchiveOrgSyncAdapter;
import net.bradball.android.sandbox.ui.fragments.MediaBrowserFragment;

import java.util.HashMap;
import java.util.Map;

// Set logging for a TAG with:
// adb shell setprop log.tag.<the tag>  <log level>

public class LogHelper {

    private static final String LOG_PREFIX = "SB_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;


    /*
     * Android's isLoggable appears to not always work correctly.
     * Often times there are log messages that SHOULD be logged,
     * but isLoggable returns false and thus they aren't logged.
     * So we're going to implement our own logging check.
     *
     * Add Tags, and the debug level you want for the tag to the
     * TAG_LIST hash below. The isLoggable() method below will
     * check Android Log.isLoggable() first, and if it's false
     * It will check this the TAG_LIST hash map and return
     * true or false accordingly. The log() method below
     * makes use of our isLoggable method to make sure the logs
     * work like we intend them to for development.
     */
    private static final Map<String, Integer> TAG_LIST = new HashMap<>();
    static {
        //TAG_LIST.put(makeLogTag(ArchiveOrgSyncAdapter.class), Log.DEBUG);
        TAG_LIST.put(makeLogTag(MusicService.class), Log.DEBUG);
        //TAG_LIST.put(makeLogTag(MediaBrowserFragment.class), Log.DEBUG);
        //TAG_LIST.put(makeLogTag(MusicHandlerThread.class), Log.DEBUG);
        //TAG_LIST.put(makeLogTag(RecordingsProvider.class), Log.DEBUG);
    }


    public static String makeLogTag(String str) {
        String tag = LOG_PREFIX + str;

        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            tag = LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH);
        }

        return tag;
    }

    /**
     * Don't use this when obfuscating class names!
     */
    public static String makeLogTag(Class cls) {
        return makeLogTag(cls.getSimpleName());
    }


    public static void v(String tag, Object... messages) {
        // Only log VERBOSE if build type is DEBUG
        if (BuildConfig.DEBUG) {
            log(tag, Log.VERBOSE, null, messages);
        }
    }

    public static void d(String tag, Object... messages) {
        // Only log DEBUG if build type is DEBUG
        if (BuildConfig.DEBUG) {
            log(tag, Log.DEBUG, null, messages);
        }
    }

    public static void i(String tag, Object... messages) {
        log(tag, Log.INFO, null, messages);
    }

    public static void w(String tag, Object... messages) {
        log(tag, Log.WARN, null, messages);
    }

    public static void w(String tag, Throwable t, Object... messages) {
        log(tag, Log.WARN, t, messages);
    }

    public static void e(String tag, Object... messages) {
        log(tag, Log.ERROR, null, messages);
    }

    public static void e(String tag, Throwable t, Object... messages) {
        log(tag, Log.ERROR, t, messages);
    }

    public static void log(String tag, int level, Throwable t, Object... messages) {
        if (isLoggable(tag, level)) {
            String message;
            if (t == null && messages != null && messages.length == 1) {
                // handle this common case without the extra cost of creating a stringbuffer:
                message = messages[0].toString();
            } else {
                StringBuilder sb = new StringBuilder();
                if (messages != null) for (Object m : messages) {
                    sb.append(m);
                }
                if (t != null) {
                    sb.append("\n").append(Log.getStackTraceString(t));
                }
                message = sb.toString();
            }
            Log.println(level, tag, message);
        }
    }

    public static boolean isLoggable(String tag, int level) {
        if (Log.isLoggable(tag, level)) {
            return true;
        }

        return (TAG_LIST.containsKey(tag) && level >= TAG_LIST.get(tag));
    }
}
