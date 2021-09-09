package gcm.core.epi.plugin.therapeutic.multiplier;

public enum EfficacyType {
    // Against infection
    TE_S,
    // Against onward transmission
    TE_I,
    // Against symptoms conditional on infection
    TE_P,
    // Against hospitalization conditional on symptomatic infection
    TE_H,
    // Against death conditional on symptomatic infection and requiried hospitalization
    TE_D
}
