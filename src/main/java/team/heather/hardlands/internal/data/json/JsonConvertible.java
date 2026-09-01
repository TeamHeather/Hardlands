package team.heather.hardlands.internal.data.json;

import com.google.gson.JsonElement;

public interface JsonConvertible {
    void fromJson(JsonElement json);

    JsonElement toJson();
}