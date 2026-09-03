package team.heather.hardlands.module.scenario;

import java.util.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.json.JsonConvertible;

public final class ScenarioManager implements JsonConvertible {

    public static final List<Scenario> REGISTERED_SCENARIOS = List.of(Scenario.values());

    private final Set<Scenario> enabledScenarios = new LinkedHashSet<>();

    public ScenarioManager(Hardlands plugin) {
        REGISTERED_SCENARIOS.forEach(scenario -> scenario.initialize(plugin));
    }

    public boolean enableScenario(Scenario scenario) {
        if (this.isScenarioEnabled(scenario) || !scenario.getProcessor().canEnable()) {
            return false;
        }

        scenario.enable();
        this.enabledScenarios.add(scenario);
        return true;
    }

    public boolean disableScenario(Scenario scenario) {
        if (!this.isScenarioEnabled(scenario)) {
            return false;
        }

        scenario.disable();
        this.enabledScenarios.remove(scenario);
        return true;
    }

    public boolean toggleScenario(Scenario scenario) {
        return this.isScenarioEnabled(scenario)
                ? this.disableScenario(scenario)
                : this.enableScenario(scenario);
    }

    public boolean enableScenario(String identifier) {
        return this.findScenario(identifier)
                .map(this::enableScenario)
                .orElse(false);
    }

    public boolean disableScenario(String identifier) {
        return this.findScenario(identifier)
                .map(this::disableScenario)
                .orElse(false);
    }

    public boolean toggleScenario(String identifier) {
        return this.findScenario(identifier)
                .map(this::toggleScenario)
                .orElse(false);
    }

    public boolean isScenarioEnabled(Scenario scenario) {
        return this.enabledScenarios.contains(scenario);
    }

    public boolean isScenarioEnabled(String identifier) {
        return this.findScenario(identifier)
                .map(this::isScenarioEnabled)
                .orElse(false);
    }

    public Optional<Scenario> findScenario(String identifier) {
        return Scenario.findByKey(identifier);
    }

    public Optional<Scenario> findEnabledScenario(String identifier) {
        return this.findScenario(identifier).filter(this.enabledScenarios::contains);
    }

    public List<Scenario> getEnabledScenarios() {
        return List.copyOf(this.enabledScenarios);
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();

        this.enabledScenarios.forEach(scenario ->
                json.add(scenario.getNamespacedKey().getKey(), scenario.getProcessor().toJson()));

        return json;
    }

    @Override
    public void fromJson(JsonElement json) {
        List.copyOf(this.enabledScenarios).forEach(this::disableScenario);

        json.getAsJsonObject().entrySet().forEach(entry ->
                this.findScenario(entry.getKey()).ifPresent(scenario -> {
                    scenario.getProcessor().fromJson(entry.getValue());
                    this.enableScenario(scenario);
                }));
    }
}