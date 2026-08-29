package team.heather.hardlands.module.scenario;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.core.data.json.JsonConvertible;

public final class ScenarioManager implements JsonConvertible {

    private final Set<Scenario> enabledScenarios = new LinkedHashSet<>();

    public ScenarioManager(Hardlands plugin) {
        for (Scenario scenario : Scenario.values()) {
            scenario.initialize(plugin);
        }
    }

    public boolean enableScenario(Scenario scenario) {
        if (this.enabledScenarios.contains(scenario)
                || !scenario.getProcessor().canEnable()) {
            return false;
        }

        scenario.enable();
        this.enabledScenarios.add(scenario);

        return true;
    }

    public boolean disableScenario(Scenario scenario) {
        if (!this.enabledScenarios.contains(scenario)) {
            return false;
        }

        scenario.disable();
        this.enabledScenarios.remove(scenario);

        return true;
    }

    public boolean toggleScenario(Scenario scenario) {
        return this.enabledScenarios.contains(scenario)
                ? this.disableScenario(scenario)
                : this.enableScenario(scenario);
    }

    public boolean enableScenario(String identifier) {
        return Scenario.findByKey(identifier)
                .map(this::enableScenario)
                .orElse(false);
    }

    public boolean disableScenario(String identifier) {
        return Scenario.findByKey(identifier)
                .map(this::disableScenario)
                .orElse(false);
    }

    public boolean toggleScenario(String identifier) {
        return Scenario.findByKey(identifier)
                .map(this::toggleScenario)
                .orElse(false);
    }

    public boolean isScenarioEnabled(Scenario scenario) {
        return this.enabledScenarios.contains(scenario);
    }

    public boolean isScenarioEnabled(String identifier) {
        return Scenario.findByKey(identifier)
                .map(this::isScenarioEnabled)
                .orElse(false);
    }

    public Optional<Scenario> findScenario(String identifier) {
        return Scenario.findByKey(identifier);
    }

    public Optional<Scenario> findEnabledScenario(String identifier) {
        return Scenario.findByKey(identifier)
                .filter(this.enabledScenarios::contains);
    }

    public List<Scenario> getRegisteredScenarios() {
        return List.of(Scenario.values());
    }

    public List<Scenario> getEnabledScenarios() {
        return List.copyOf(this.enabledScenarios);
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        this.enabledScenarios.forEach(scenario ->
                json.add(
                        scenario.getNamespacedKey().getKey(),
                        scenario.getProcessor().toJson()
                ));
        return json;
    }

    @Override
    public void fromJson(JsonElement json) {
        enabledScenarios.forEach(this::disableScenario);
        json.getAsJsonObject().entrySet().forEach(entry ->
                Scenario.findByKey(entry.getKey()).ifPresent(scenario -> {
                    scenario.getProcessor().fromJson(entry.getValue());
                    enableScenario(scenario);
                }));
    }
}