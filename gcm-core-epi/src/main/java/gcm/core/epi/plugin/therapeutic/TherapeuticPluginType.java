package gcm.core.epi.plugin.therapeutic;

import gcm.core.epi.plugin.therapeutic.multiplier.MultiplierTherapeuticPlugin;

public enum TherapeuticPluginType {

    MULTIPLIER(MultiplierTherapeuticPlugin.class);

    private final Class<? extends TherapeuticPlugin> therapeuticPluginClass;

    TherapeuticPluginType(Class<? extends TherapeuticPlugin> therapeuticPluginClass) {
        this.therapeuticPluginClass = therapeuticPluginClass;
    }

    public Class<? extends TherapeuticPlugin> getPluginClass() {
        return therapeuticPluginClass;
    }
}
