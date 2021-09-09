package gcm.core.epi.plugin.vaccine.resourcebased;

public enum EfficacyType {
    // Against infection
    VE_S,
    // Against onward transmission
    VE_I,
    // Against symptoms conditional on infection
    VE_P,
    // Against hospitalization conditional on symptomatic infection
    VE_H,
    // Against death conditional on symptomatic infection and requiring hospitalization
    VE_D
}
