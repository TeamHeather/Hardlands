package org.heather.hardlands.util.data.json;

import com.google.gson.JsonElement;

public interface JsonConvertible {
    void fromJson(JsonElement json);

    JsonElement toJson();
}
