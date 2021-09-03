package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.variants.VariantId;
import org.immutables.value.Value;

import java.util.*;
import java.util.stream.Collectors;

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

    /*
        Fill in unspecified efficacy dimensions and check that they are consistent
     */
    @Value.Check
    VaccineDefinition insertEfficacyDefaults() {
        if (this.efficacy().keySet().size() < ExternalEfficacyType.values().length) {
            Map<ExternalEfficacyType, Double> efficacyMapWithDefaults = getEfficacyMapWithDefaults(this.efficacy());
            return ImmutableVaccineDefinition.builder()
                    .from(this)
                    .efficacy(efficacyMapWithDefaults)
                    .build();
        } else {
            if (efficacy().values().stream().anyMatch(efficacy -> efficacy < 0 | efficacy > 1)) {
                throw new RuntimeException("Efficacy must always be between 0 and 1.");
            }
            if (efficacy().get(ExternalEfficacyType.VE_SP) <
                    efficacy().get(ExternalEfficacyType.VE_S)) {
                throw new RuntimeException("VE_P implied by VE_SP and VE_S is impossible.");
            }
            if (efficacy().get(ExternalEfficacyType.VE_SPD) <
                    efficacy().get(ExternalEfficacyType.VE_SP)) {
                throw new RuntimeException("VE_D implied by VE_SPD and VE_SP is impossible.");
            }
            return this;
        }
    }

    /*
        Derived efficacy of first dose by taking the base efficacy and multiplying by the appropriate relative efficacy
     */
    @Value.Derived Map<ExternalEfficacyType, Double> firstDoseEfficacy() {
        return efficacy().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> entry.getValue() * relativeEfficacyOfFirstDose().getOrDefault(entry.getKey(), 1.0)
                ));
    }

    /*
        Derived efficacy of final dose by taking the base efficacy and adjusting for the appropriate variant multiplier
     */
    @Value.Derived
    Map<VariantId, Map<ExternalEfficacyType, Double>> variantEfficacy() {
        Map<VariantId, Map<ExternalEfficacyType, Double>> variantEfficacy = new HashMap<>();
        Set<VariantId> variantIds = new HashSet<>(variantRelativeEfficacy().keySet());
        variantIds.addAll(variantFirstDoseRelativeEfficacy().keySet());
        for (VariantId variantId : variantIds) {
            Map<ExternalEfficacyType, Double> relativeEfficacy = variantRelativeEfficacy().getOrDefault(variantId,
                    new EnumMap<>(ExternalEfficacyType.class));
            variantEfficacy.put(variantId,
                    efficacy().entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey(),
                            entry -> relativeEfficacy.getOrDefault(entry.getKey(), 1.0) * entry.getValue()
                                )));
        }
        return variantEfficacy;
    }

    /*
        Derived efficacy of first dose by taking the base variant efficacy and adjusting for the first dose multiplier
     */
    @Value.Derived
    Map<VariantId, Map<ExternalEfficacyType, Double>> variantFirstDoseEfficacy() {
        Map<VariantId, Map<ExternalEfficacyType, Double>> variantFirstDoseEfficacy = new HashMap<>();
        for (VariantId variantId : variantEfficacy().keySet()) {
            Map<ExternalEfficacyType, Double> firstDoseRelativeEfficacy = variantFirstDoseRelativeEfficacy()
                    .getOrDefault(variantId, new EnumMap<>(ExternalEfficacyType.class));
            variantFirstDoseEfficacy.put(variantId,
                    variantEfficacy().get(variantId).entrySet().stream()
                            .collect(Collectors.toMap(
                                    entry -> entry.getKey(),
                                    entry -> entry.getValue() *
                                            firstDoseRelativeEfficacy.getOrDefault(entry.getKey(), 1.0) *
                                            relativeEfficacyOfFirstDose().getOrDefault(entry.getKey(), 1.0)
                            )));
        }
        return variantFirstDoseEfficacy;
    }

    public double getVaccineEfficacy(EfficacyType efficacyType, double timeSinceLastDose, VariantId variantId, long doses) {
        if (doses == 0) {
            return 0;
        }

        switch (efficacyType) {
            case VE_S:
                return getVaccineEfficacy(ExternalEfficacyType.VE_S, timeSinceLastDose, variantId, doses);
            case VE_I:
                return getVaccineEfficacy(ExternalEfficacyType.VE_I, timeSinceLastDose, variantId, doses);
            case VE_P:
                double VE_SP = getVaccineEfficacy(ExternalEfficacyType.VE_SP, timeSinceLastDose, variantId, doses);
                if (VE_SP < 1) {
                    return 1.0 - ((1.0 - VE_SP) / (1.0 - getVaccineEfficacy(ExternalEfficacyType.VE_S,
                            timeSinceLastDose, variantId, doses)));
                } else {
                    return 1.0;
                }
            case VE_D:
                double VE_SPD = getVaccineEfficacy(ExternalEfficacyType.VE_SPD, timeSinceLastDose, variantId, doses);
                if (VE_SPD < 1) {
                    return 1.0 - ((1.0 - VE_SPD) / (1.0 - getVaccineEfficacy(ExternalEfficacyType.VE_SP,
                            timeSinceLastDose, variantId, doses)));
                } else {
                    return 1.0;
                }
            default:
                throw new RuntimeException("Unhandled vaccine efficacy type");
        }
    }

    public double getVaccineEfficacy(ExternalEfficacyType externalEfficacyType, double timeSinceLastDose, VariantId variantId, long doses) {

        if (doses == 0) {
            return 0;
        }

        switch (type()) {
            case ONE_DOSE:
                double maxEfficacy;
                if (variantEfficacy().containsKey(variantId)) {
                    maxEfficacy = variantEfficacy().get(variantId).get(externalEfficacyType);
                } else {
                    maxEfficacy = efficacy().get(externalEfficacyType);
                }
                if (doses == 1) {
                    return maxEfficacy * firstDoseEfficacyFunction().getValue(externalEfficacyType, timeSinceLastDose);
                }
                throw new RuntimeException("Unhandled number of doses");
            case TWO_DOSE:
                double maxFirstDoseEfficacy;
                if (variantFirstDoseEfficacy().containsKey(variantId)) {
                    maxFirstDoseEfficacy = variantFirstDoseEfficacy().get(variantId).get(externalEfficacyType);
                } else {
                    maxFirstDoseEfficacy = firstDoseEfficacy().get(externalEfficacyType);
                }
                if (doses == 1) {
                    return maxFirstDoseEfficacy * firstDoseEfficacyFunction().getValue(externalEfficacyType, timeSinceLastDose);
                }
                if (doses == 2) {
                    double firstDoseEfficacyAtTimeOfSecondDose = maxFirstDoseEfficacy *
                            firstDoseEfficacyFunction().getValue(externalEfficacyType, secondDoseDelay());
                    double maxSecondDoseEfficacy;
                    if (variantEfficacy().containsKey(variantId)) {
                        maxSecondDoseEfficacy = variantEfficacy().get(variantId).get(externalEfficacyType);
                    } else {
                        maxSecondDoseEfficacy = efficacy().get(externalEfficacyType);
                    }
                    if (maxSecondDoseEfficacy > 0) {
                        double initialEfficacyFunctionValue = firstDoseEfficacyAtTimeOfSecondDose / maxSecondDoseEfficacy;
                        return maxSecondDoseEfficacy * secondDoseEfficacyFunction().getValue(externalEfficacyType, timeSinceLastDose,
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

}
