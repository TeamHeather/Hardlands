package org.heather.hardlands.core.data.json;

import com.google.gson.JsonElement;

public interface JsonConvertible {
    void fromJson(JsonElement json);

    JsonElement toJson();
}
