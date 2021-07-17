package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.variants.VariantId;
import org.immutables.value.Value;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Value.Immutable
@JsonDeserialize(as = ImmutableVaccineDefinition.class)
public abstract class VaccineDefinition {

    public abstract VaccineId id();

    @Value.Default
    public DoseType type() {
        return DoseType.ONE_DOSE;
    }

    @Value.Default
    SimpleEfficacyFunction firstDoseEfficacyFunction() {
        return ImmutableSimpleEfficacyFunction.builder().build();
    }

    // Only used for TWO_DOSE type
    @Value.Default
    SimpleEfficacyFunction secondDoseEfficacyFunction() {
        return ImmutableSimpleEfficacyFunction.builder().build();
    }

    // Only used for TWO_DOSE type
    @Value.Default
    Map<ExternalEfficacyType, Double> relativeEfficacyOfFirstDose() {
        return new EnumMap<>(ExternalEfficacyType.class);
    }

    // Only used for TWO_DOSE type
    @Value.Default
    double secondDoseDelay() {
        return 0.0;
    }

    public abstract Map<ExternalEfficacyType, Double> efficacy();

    public abstract Map<VariantId, Map<ExternalEfficacyType, Double>> variantRelativeEfficacy();

    // Only used for TWO_DOSE type - this is in addition to the overall variant relative efficacy
    public abstract Map<VariantId, Map<ExternalEfficacyType, Double>> variantFirstDoseRelativeEfficacy();

    private Map<ExternalEfficacyType, Double> getEfficacyMapWithDefaults(Map<ExternalEfficacyType, Double> efficacyMap) {
        Map<ExternalEfficacyType, Double> efficacyMapWithDefaults = new EnumMap<>(ExternalEfficacyType.class);
        efficacyMapWithDefaults.putAll(efficacyMap);
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_S, 0.0);
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_I, 0.0);
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_SP,
                // Assume VE_P = 0 implicitly if not specified
                efficacyMapWithDefaults.get(ExternalEfficacyType.VE_S));
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_SPD,
                // Assume VE_D = 0 implicitly if not specified
                efficacyMapWithDefaults.get(ExternalEfficacyType.VE_SP));
        return efficacyMapWithDefaults;
    }

    private Map<EfficacyType, Double> getInternalEfficacyMap(Map<ExternalEfficacyType, Double> externalEfficacyMap) {
        Map<ExternalEfficacyType, Double> efficacyMapWithDefaults = getEfficacyMapWithDefaults(externalEfficacyMap);
        double VES = efficacyMapWithDefaults.get(ExternalEfficacyType.VE_S);
        double VEI = efficacyMapWithDefaults.get(ExternalEfficacyType.VE_I);
        double VESP = efficacyMapWithDefaults.get(ExternalEfficacyType.VE_SP);
        double VESPD = efficacyMapWithDefaults.get(ExternalEfficacyType.VE_SPD);

        double VEP = VES < 1.0 ? 1.0 - (1.0 - VESP) / (1.0 - VES) : 0.0;
        if (VEP < 0.0 | VEP > 1.0) {
            throw new RuntimeException("VEP implied by VESP and VES is impossible");
        }
        double VED = VESP < 1.0 ? 1.0 - (1.0 - VESPD) / (1.0 - VESP) : 0.0;
        if (VED < 0.0 | VED > 1.0) {
            throw new RuntimeException("VED implied by VESPD and VESP is impossible");
        }

        Map<EfficacyType, Double> internalEfficacyMap = new EnumMap<>(EfficacyType.class);
        internalEfficacyMap.put(EfficacyType.VE_S, VES);
        internalEfficacyMap.put(EfficacyType.VE_I, VEI);
        internalEfficacyMap.put(EfficacyType.VE_P, VEP);
        internalEfficacyMap.put(EfficacyType.VE_D, VED);
        return internalEfficacyMap;
    }

    @Value.Derived
    Map<EfficacyType, Double> internalEfficacy() {
        return getInternalEfficacyMap(efficacy());
    }

    // Only used for TWO_DOSE type
    @Value.Derived
    Map<EfficacyType, Double> internalFirstDoseEfficacy() {
        Map<ExternalEfficacyType, Double> efficacyMapWithDefaults = getEfficacyMapWithDefaults(efficacy());
        Map<ExternalEfficacyType, Double> firstDoseEfficacyMap = new EnumMap<>(ExternalEfficacyType.class);
        for (ExternalEfficacyType externalEfficacyType : ExternalEfficacyType.values()) {
            firstDoseEfficacyMap.put(externalEfficacyType,
                    efficacyMapWithDefaults.get(externalEfficacyType) *
                    relativeEfficacyOfFirstDose().getOrDefault(externalEfficacyType, 1.0));
        }
        return getInternalEfficacyMap(firstDoseEfficacyMap);
    }

    @Value.Derived
    Map<VariantId, Map<EfficacyType, Double>> internalVariantEfficacy() {
        Map<VariantId, Map<EfficacyType, Double>> internalVariantEfficacy = new HashMap<>();
        for (VariantId variantId : variantRelativeEfficacy().keySet()) {
            // First compute the externally-parameterized efficacy
            Map<ExternalEfficacyType, Double> variantEfficacyMap = new EnumMap<>(ExternalEfficacyType.class);
            Map<ExternalEfficacyType, Double> efficacyMapWithDefaults = getEfficacyMapWithDefaults(efficacy());
            for (ExternalEfficacyType externalEfficacyType : efficacyMapWithDefaults.keySet()) {
                variantEfficacyMap.put(externalEfficacyType, efficacyMapWithDefaults.get(externalEfficacyType) *
                        variantRelativeEfficacy().get(variantId).getOrDefault(externalEfficacyType, 1.0));
            }
            // Store the internally-parameterized efficacy
            internalVariantEfficacy.put(variantId, getInternalEfficacyMap(variantEfficacyMap));
        }
        return internalVariantEfficacy;
    }

    // Only used for TWO_DOSE type
    @Value.Derived
    Map<VariantId, Map<EfficacyType, Double>> internalVariantFirstDoseEfficacy() {
        Map<VariantId, Map<EfficacyType, Double>> internalVariantEfficacy = new HashMap<>();
        for (VariantId variantId : variantFirstDoseRelativeEfficacy().keySet()) {
            // First compute the externally-parameterized efficacy
            Map<ExternalEfficacyType, Double> variantEfficacyMap = new EnumMap<>(ExternalEfficacyType.class);
            Map<ExternalEfficacyType, Double> efficacyMapWithDefaults = getEfficacyMapWithDefaults(efficacy());
            for (ExternalEfficacyType externalEfficacyType : efficacyMapWithDefaults.keySet()) {
                double finalRelativeEfficacy;
                if (variantRelativeEfficacy().containsKey(variantId)) {
                    finalRelativeEfficacy = variantRelativeEfficacy().get(variantId).getOrDefault(externalEfficacyType, 1.0);
                } else {
                    finalRelativeEfficacy = 1.0;
                }
                variantEfficacyMap.put(externalEfficacyType, efficacyMapWithDefaults.get(externalEfficacyType) *
                        relativeEfficacyOfFirstDose().getOrDefault(externalEfficacyType, 1.0) *
                        finalRelativeEfficacy *
                        variantFirstDoseRelativeEfficacy().get(variantId).getOrDefault(externalEfficacyType, 1.0));
            }
            // Store the internally-parameterized efficacy
            internalVariantEfficacy.put(variantId, getInternalEfficacyMap(variantEfficacyMap));
        }
        return internalVariantEfficacy;
    }

    public double getVaccineEfficacy(long doses, double timeSinceLastDose, VariantId variantId, EfficacyType efficacyType) {
        if (doses == 0) {
            return 0;
        }

        switch (type()) {
            case ONE_DOSE:
                double maxEfficacy;
                if (internalVariantEfficacy().containsKey(variantId)) {
                    maxEfficacy = internalVariantEfficacy().get(variantId).get(efficacyType);
                } else {
                    maxEfficacy = internalEfficacy().get(efficacyType);
                }
                if (doses == 1) {
                    return maxEfficacy * firstDoseEfficacyFunction().getValue(timeSinceLastDose);
                }
                throw new RuntimeException("Unhandled number of doses");
            case TWO_DOSE:
                double maxFirstDoseEfficacy;
                if (internalVariantFirstDoseEfficacy().containsKey(variantId)) {
                    maxFirstDoseEfficacy = internalVariantFirstDoseEfficacy().get(variantId).get(efficacyType);
                } else {
                    maxFirstDoseEfficacy = internalFirstDoseEfficacy().get(efficacyType);
                }
                if (doses == 1) {
                    return maxFirstDoseEfficacy * firstDoseEfficacyFunction().getValue(timeSinceLastDose);
                }
                if (doses == 2) {
                    double firstDoseEfficacyAtTimeOfSecondDose = maxFirstDoseEfficacy *
                            firstDoseEfficacyFunction().getValue(secondDoseDelay());
                    double maxSecondDoseEfficacy;
                    if (internalVariantEfficacy().containsKey(variantId)) {
                        maxSecondDoseEfficacy = internalVariantEfficacy().get(variantId).get(efficacyType);
                    } else {
                        maxSecondDoseEfficacy = internalEfficacy().get(efficacyType);
                    }
                    if (maxSecondDoseEfficacy > 0) {
                        double initialEfficacyFunctionValue = firstDoseEfficacyAtTimeOfSecondDose / maxSecondDoseEfficacy;
                        return maxSecondDoseEfficacy * secondDoseEfficacyFunction().getValue(timeSinceLastDose,
                                initialEfficacyFunctionValue);
                    } else {
                        // Transition to 0 from the first dose efficacy value
                        if (timeSinceLastDose < secondDoseEfficacyFunction().initialDelay()) {
                            return firstDoseEfficacyAtTimeOfSecondDose;
                        } else if (timeSinceLastDose < secondDoseEfficacyFunction().peakTime()) {
                            return firstDoseEfficacyAtTimeOfSecondDose *
                                    (secondDoseEfficacyFunction().peakTime() - secondDoseEfficacyFunction().initialDelay() - timeSinceLastDose) /
                                    (secondDoseEfficacyFunction().peakTime() - secondDoseEfficacyFunction().initialDelay());
                        } else {
                            return 0.0;
                        }
                    }
                }
                throw new RuntimeException("Unhandled number of doses");
            default:
                throw new RuntimeException("Unhandled dose type");
        }
    }

    public int dosesPerRegimen() {
        switch (type()) {
            case ONE_DOSE:
                return 1;
            case TWO_DOSE:
                return 2;
            default:
                throw new RuntimeException("Unhandled dose type");
        }
    }

    public enum DoseType {
        ONE_DOSE,
        TWO_DOSE
    }

    public enum EfficacyType {
        // Against infection
        VE_S,
        // Against onward transmission
        VE_I,
        VE_P,
        VE_D
    }

    /*

     */
    public enum ExternalEfficacyType {
        // Against infection
        VE_S,
        // Against onward transmission
        VE_I,
        // Overall against symptomatic infection
        VE_SP,
        // Overall against hospitalization and death
        VE_SPD
    }

}
