package team.heather.hardlands.internal.json;

import com.google.gson.JsonElement;

public interface JsonConvertible {
    void fromJson(JsonElement json);

    JsonElement toJson();
}