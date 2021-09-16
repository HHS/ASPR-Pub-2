package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.propertytypes.AgeWeights;
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
    Map<ExternalEfficacyType, AgeWeights> firstDoseRelativeEfficacy() {
        return new EnumMap<>(ExternalEfficacyType.class);
    }

    // Only used for TWO_DOSE type
    @Value.Default
    double secondDoseDelay() {
        return 0.0;
    }

    public abstract Map<ExternalEfficacyType, AgeWeights> efficacy();

    public abstract Map<VariantId, Map<ExternalEfficacyType, AgeWeights>> variantRelativeEfficacy();

    // Only used for TWO_DOSE type - this is in addition to the overall variant relative efficacy
    public abstract Map<VariantId, Map<ExternalEfficacyType, AgeWeights>> variantFirstDoseRelativeEfficacy();

    private Map<ExternalEfficacyType, AgeWeights> getEfficacyMapWithDefaults(Map<ExternalEfficacyType, AgeWeights> efficacyMap) {
        Map<ExternalEfficacyType, AgeWeights> efficacyMapWithDefaults = new EnumMap<>(ExternalEfficacyType.class);
        efficacyMapWithDefaults.putAll(efficacyMap);
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_S, AgeWeights.from(0.0));
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_I, AgeWeights.from(0.0));
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_SP,
                // Assume VE_P = 0 implicitly if not specified
                efficacyMapWithDefaults.get(ExternalEfficacyType.VE_S));
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_SPH,
                // Assume VE_H = 0 implicitly if not specified
                efficacyMapWithDefaults.get(ExternalEfficacyType.VE_SP));
        efficacyMapWithDefaults.putIfAbsent(ExternalEfficacyType.VE_SPD,
                // Assume VE_D = 0 implicitly if not specified
                efficacyMapWithDefaults.get(ExternalEfficacyType.VE_SPH));
        return efficacyMapWithDefaults;
    }

    /*
        Fill in unspecified efficacy dimensions and check that they are consistent
     */
    @Value.Check
    VaccineDefinition insertEfficacyDefaults() {
        if (this.efficacy().keySet().size() < ExternalEfficacyType.values().length) {
            Map<ExternalEfficacyType, AgeWeights> efficacyMapWithDefaults = getEfficacyMapWithDefaults(this.efficacy());
            return ImmutableVaccineDefinition.builder()
                    .from(this)
                    .efficacy(efficacyMapWithDefaults)
                    .build();
        } else {
            if (efficacy().values().stream().anyMatch(efficacy -> !efficacy.greaterThanOrEqualTo(AgeWeights.from(0)) |
                    !efficacy.lessThanOrEqualTo(AgeWeights.from(1)))) {
                throw new RuntimeException("Efficacy must always be between 0 and 1.");
            }
            if (!efficacy().get(ExternalEfficacyType.VE_SP).greaterThanOrEqualTo(
                    efficacy().get(ExternalEfficacyType.VE_S))) {
                throw new RuntimeException("VE_P implied by VE_SP and VE_S is impossible.");
            }
            if (!efficacy().get(ExternalEfficacyType.VE_SPH).greaterThanOrEqualTo(
                    efficacy().get(ExternalEfficacyType.VE_SP))) {
                throw new RuntimeException("VE_H implied by VE_SPH and VE_SP is impossible.");
            }
            if (!efficacy().get(ExternalEfficacyType.VE_SPD).greaterThanOrEqualTo(
                    efficacy().get(ExternalEfficacyType.VE_SPH))) {
                throw new RuntimeException("VE_D implied by VE_SPD and VE_SPH is impossible.");
            }
            return this;
        }
    }

    /*
        Derived efficacy of first dose by taking the base efficacy and multiplying by the appropriate relative efficacy
     */
    @Value.Derived
    Map<ExternalEfficacyType, AgeWeights> firstDoseEfficacy() {
        return efficacy().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue()
                                .multiply(firstDoseRelativeEfficacy().getOrDefault(entry.getKey(), AgeWeights.from(1.0)))
                ));
    }

    /*
        Derived efficacy of final dose by taking the base efficacy and adjusting for the appropriate variant multiplier
     */
    @Value.Derived
    Map<VariantId, Map<ExternalEfficacyType, AgeWeights>> variantEfficacy() {
        Map<VariantId, Map<ExternalEfficacyType, AgeWeights>> variantEfficacy = new HashMap<>();
        Set<VariantId> variantIds = new HashSet<>(variantRelativeEfficacy().keySet());
        variantIds.addAll(variantFirstDoseRelativeEfficacy().keySet());
        for (VariantId variantId : variantIds) {
            Map<ExternalEfficacyType, AgeWeights> relativeEfficacy = variantRelativeEfficacy().getOrDefault(variantId,
                    new EnumMap<>(ExternalEfficacyType.class));
            variantEfficacy.put(variantId,
                    efficacy().entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> relativeEfficacy.getOrDefault(entry.getKey(), AgeWeights.from(1.0))
                                            .multiply(entry.getValue())
                            )));
        }
        return variantEfficacy;
    }

    /*
        Derived efficacy of first dose by taking the base variant efficacy and adjusting for the first dose multiplier
     */
    @Value.Derived
    Map<VariantId, Map<ExternalEfficacyType, AgeWeights>> variantFirstDoseEfficacy() {
        Map<VariantId, Map<ExternalEfficacyType, AgeWeights>> variantFirstDoseEfficacy = new HashMap<>();
        for (VariantId variantId : variantEfficacy().keySet()) {
            Map<ExternalEfficacyType, AgeWeights> firstDoseRelativeEfficacy = variantFirstDoseRelativeEfficacy()
                    .getOrDefault(variantId, new EnumMap<>(ExternalEfficacyType.class));
            variantFirstDoseEfficacy.put(variantId,
                    variantEfficacy().get(variantId).entrySet().stream()
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    entry -> entry.getValue()
                                            .multiply(firstDoseRelativeEfficacy.getOrDefault(entry.getKey(), AgeWeights.from(1.0)))
                                            .multiply(firstDoseRelativeEfficacy().getOrDefault(entry.getKey(), AgeWeights.from(1.0)))
                            )));
        }
        return variantFirstDoseEfficacy;
    }

    public double getVaccineEfficacy(EfficacyType efficacyType, double timeSinceLastDose, VariantId variantId,
                                     AgeGroup ageGroup, long doses) {
        if (doses == 0) {
            return 0;
        }

        switch (efficacyType) {
            case VE_S:
                return getVaccineEfficacy(ExternalEfficacyType.VE_S, timeSinceLastDose, variantId, ageGroup, doses);
            case VE_I:
                return getVaccineEfficacy(ExternalEfficacyType.VE_I, timeSinceLastDose, variantId, ageGroup, doses);
            case VE_P:
                double VE_SP = getVaccineEfficacy(ExternalEfficacyType.VE_SP, timeSinceLastDose, variantId, ageGroup, doses);
                if (VE_SP < 1) {
                    return 1.0 - ((1.0 - VE_SP) / (1.0 - getVaccineEfficacy(ExternalEfficacyType.VE_S,
                            timeSinceLastDose, variantId, ageGroup, doses)));
                } else {
                    return 1.0;
                }
            case VE_H:
                double VE_SPH = getVaccineEfficacy(ExternalEfficacyType.VE_SPH, timeSinceLastDose, variantId, ageGroup, doses);
                if (VE_SPH < 1) {
                    return 1.0 - ((1.0 - VE_SPH) / (1.0 - getVaccineEfficacy(ExternalEfficacyType.VE_SP,
                            timeSinceLastDose, variantId, ageGroup, doses)));
                } else {
                    return 1.0;
                }
            case VE_D:
                double VE_SPD = getVaccineEfficacy(ExternalEfficacyType.VE_SPD, timeSinceLastDose, variantId, ageGroup, doses);
                if (VE_SPD < 1) {
                    return 1.0 - ((1.0 - VE_SPD) / (1.0 - getVaccineEfficacy(ExternalEfficacyType.VE_SPH,
                            timeSinceLastDose, variantId, ageGroup, doses)));
                } else {
                    return 1.0;
                }
            default:
                throw new RuntimeException("Unhandled vaccine efficacy type");
        }
    }

    public double getVaccineEfficacy(ExternalEfficacyType externalEfficacyType, double timeSinceLastDose, VariantId variantId,
                                     AgeGroup ageGroup, long doses) {

        if (doses == 0) {
            return 0;
        }

        switch (type()) {
            case ONE_DOSE:
                double maxEfficacy;
                if (variantEfficacy().containsKey(variantId)) {
                    maxEfficacy = variantEfficacy().get(variantId).get(externalEfficacyType).getWeight(ageGroup);
                } else {
                    maxEfficacy = efficacy().get(externalEfficacyType).getWeight(ageGroup);
                }
                if (doses == 1) {
                    return maxEfficacy * firstDoseEfficacyFunction().getValue(externalEfficacyType, ageGroup, timeSinceLastDose);
                }
                throw new RuntimeException("Unhandled number of doses");
            case TWO_DOSE:
                double maxFirstDoseEfficacy;
                if (variantFirstDoseEfficacy().containsKey(variantId)) {
                    maxFirstDoseEfficacy = variantFirstDoseEfficacy().get(variantId).get(externalEfficacyType).getWeight(ageGroup);
                } else {
                    maxFirstDoseEfficacy = firstDoseEfficacy().get(externalEfficacyType).getWeight(ageGroup);
                }
                if (doses == 1) {
                    return maxFirstDoseEfficacy * firstDoseEfficacyFunction().getValue(externalEfficacyType, ageGroup, timeSinceLastDose);
                }
                if (doses == 2) {
                    double firstDoseEfficacyAtTimeOfSecondDose = maxFirstDoseEfficacy *
                            firstDoseEfficacyFunction().getValue(externalEfficacyType, ageGroup, secondDoseDelay());
                    double maxSecondDoseEfficacy;
                    if (variantEfficacy().containsKey(variantId)) {
                        maxSecondDoseEfficacy = variantEfficacy().get(variantId).get(externalEfficacyType).getWeight(ageGroup);
                    } else {
                        maxSecondDoseEfficacy = efficacy().get(externalEfficacyType).getWeight(ageGroup);
                    }
                    if (maxSecondDoseEfficacy > 0) {
                        double initialEfficacyFunctionValue = firstDoseEfficacyAtTimeOfSecondDose / maxSecondDoseEfficacy;
                        return maxSecondDoseEfficacy * secondDoseEfficacyFunction().getValue(externalEfficacyType, ageGroup, timeSinceLastDose,
                                initialEfficacyFunctionValue);
                    } else {
                        // Transition to 0 from the first dose efficacy value
                        if (timeSinceLastDose < secondDoseEfficacyFunction().initialDelay().getWeight(ageGroup)) {
                            return firstDoseEfficacyAtTimeOfSecondDose;
                        } else if (timeSinceLastDose < secondDoseEfficacyFunction().peakTime().getWeight(ageGroup)) {
                            return firstDoseEfficacyAtTimeOfSecondDose *
                                    (secondDoseEfficacyFunction().peakTime().getWeight(ageGroup) -
                                            secondDoseEfficacyFunction().initialDelay().getWeight(ageGroup) - timeSinceLastDose) /
                                    (secondDoseEfficacyFunction().peakTime().getWeight(ageGroup) -
                                            secondDoseEfficacyFunction().initialDelay().getWeight(ageGroup));
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
