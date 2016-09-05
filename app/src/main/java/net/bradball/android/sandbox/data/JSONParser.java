package net.bradball.android.sandbox.data;

import android.content.ContentProviderOperation;
import android.util.Log;

import net.bradball.android.sandbox.network.ArchiveAPI;
import net.bradball.android.sandbox.util.LogHelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;

public abstract class JSONParser {
    private static final String TAG = LogHelper.makeLogTag(JSONParser.class);
    protected JsonObject mJsonObject;
    protected JsonDeserializationContext mJsonDeserializationContext;

    public abstract void processJson(JsonElement element);
    public abstract void getContentProviderInserts(ArrayList<ContentProviderOperation> list);

    public JSONParser() { }

    protected void setDeserializationContext(JsonDeserializationContext context, JsonObject jsonObject) {
        mJsonDeserializationContext = context;
        mJsonObject = jsonObject;
    }

    protected <T>T getValue(String field, T defaultValue) {
        try {
            if (mJsonObject.has(field)) {
                return mJsonDeserializationContext.deserialize(mJsonObject.get(field), defaultValue.getClass());
            } else {
                return defaultValue;
            }
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    protected int getStringInt(String field, int defaultValue) {
        try {
            if (mJsonObject.has(field)) {
                String value = mJsonDeserializationContext.deserialize(mJsonObject.get(field), String.class);
                return Integer.parseInt(value);
            } else {
                return defaultValue;
            }
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    protected float getStringFloat(String field, float defaultValue) {
        try {
            if (mJsonObject.has(field)) {
                String value = mJsonDeserializationContext.deserialize(mJsonObject.get(field), String.class);
                return Float.parseFloat(value);
            } else {
                return defaultValue;
            }
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    protected long getStringLong(String field, long defaultValue) {
        try {
            if (mJsonObject.has(field)) {
                String value = mJsonDeserializationContext.deserialize(mJsonObject.get(field), String.class);
                return Long.parseLong(value);
            } else {
                return defaultValue;
            }
        } catch (Exception ex) {
            return defaultValue;
        }
    }


    protected LocalDate getDate(String dateField) {
        LocalDate dateValue = null;
        if (mJsonObject.has(dateField)) {
            String dateStr = mJsonDeserializationContext.deserialize(mJsonObject.get(dateField), String.class);

            try {
                DateTimeFormatter fmt = DateTimeFormat.forPattern(ArchiveAPI.DATE_FORMAT);
                dateValue = fmt.parseLocalDate(dateStr);
            } catch (IllegalArgumentException ex) {
                LogHelper.e(TAG, "Could not parse string (" + dateStr + ") into valid LocalDate object");
                return null;
            }
        }

        return dateValue;
    }

    protected ArrayList<String> getStringArray(String field) {
        ArrayList<String> values = new ArrayList<String>();
        if (mJsonObject.has(field)) {
            JsonElement element = mJsonObject.get(field);
            if (element.isJsonArray()) {
                JsonArray jsonArray = element.getAsJsonArray();
                for (int i = 0; i < jsonArray.size(); i++) {
                    values.add(jsonArray.get(i).getAsString());
                }
            } else {
                values.add(element.getAsString());
            }
        }

        return values;
    }
}