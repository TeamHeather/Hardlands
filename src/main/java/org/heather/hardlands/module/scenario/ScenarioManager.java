package org.heather.hardlands.module.scenario;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.util.data.json.JsonConvertible;

public final class ScenarioManager implements JsonConvertible {

    private final Map<String, Scenario> registeredScenarios =
            new LinkedHashMap<>();
    private final Set<String> enabledScenarios =
            new LinkedHashSet<>();

    private final Hardlands plugin;

    public ScenarioManager(Hardlands plugin) {
        this.plugin = plugin;
        this.registerScenarios();
    }

    private void registerScenarios() {
        for (ScenarioDefinition definition : ScenarioDefinition.values()) {
            String identifier = definition.identifier();
            Scenario scenario = definition.createScenario();

            scenario.initializeScenario(this.plugin, identifier);

            if (this.registeredScenarios.putIfAbsent(
                    identifier,
                    scenario) != null) {
                throw new IllegalStateException(
                        "Scenario is already registered: " + identifier);
            }
        }
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();

        this.enabledScenarios.forEach(identifier -> json.add(
                identifier,
                this.registeredScenarios.get(identifier).toJson()));

        return json;
    }

    @Override
    public void fromJson(JsonElement json) {
        List.copyOf(this.enabledScenarios)
                .forEach(this::disableScenario);

        json.getAsJsonObject().entrySet().forEach(entry -> {
            Scenario scenario =
                    this.registeredScenarios.get(entry.getKey());

            if (scenario == null) {
                return;
            }

            scenario.fromJson(entry.getValue());
            this.enableScenario(entry.getKey());
        });
    }

    public boolean enableScenario(String identifier) {
        Scenario scenario = this.registeredScenarios.get(identifier);

        if (scenario == null
                || this.enabledScenarios.contains(identifier)
                || !scenario.isConfigurationValid()) {
            return false;
        }

        scenario.enableScenario();
        this.enabledScenarios.add(identifier);

        return true;
    }

    public boolean disableScenario(String identifier) {
        Scenario scenario = this.registeredScenarios.get(identifier);

        if (scenario == null
                || !this.enabledScenarios.contains(identifier)) {
            return false;
        }

        scenario.disableScenario();
        this.enabledScenarios.remove(identifier);

        return true;
    }

    public boolean toggleScenario(String identifier) {
        return this.enabledScenarios.contains(identifier)
                ? this.disableScenario(identifier)
                : this.enableScenario(identifier);
    }

    public boolean isScenarioEnabled(String identifier) {
        return this.enabledScenarios.contains(identifier);
    }

    public Optional<Scenario> findRegisteredScenario(String identifier) {
        return Optional.ofNullable(
                this.registeredScenarios.get(identifier));
    }

    public Optional<Scenario> findEnabledScenario(String identifier) {
        if (!this.enabledScenarios.contains(identifier)) {
            return Optional.empty();
        }

        return this.findRegisteredScenario(identifier);
    }

    public List<Scenario> getRegisteredScenarios() {
        return List.copyOf(this.registeredScenarios.values());
    }
}