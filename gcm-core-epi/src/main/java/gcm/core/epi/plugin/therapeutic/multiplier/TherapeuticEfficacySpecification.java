package gcm.core.epi.plugin.therapeutic.multiplier;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.StepFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.immutables.value.Value;

import java.util.*;

@Value.Immutable
@JsonDeserialize(as = ImmutableTherapeuticEfficacySpecification.class)
public abstract class TherapeuticEfficacySpecification {

    abstract Map<Double, Map<ExternalEfficacyType, Double>> efficacies();

    @Value.Default
    boolean interpolate() {
        return false;
    }

    @Value.Check
    TherapeuticEfficacySpecification checkAndFillDefaults() {
        boolean needsFilling = false;
        Map<Double, Map<ExternalEfficacyType, Double>> efficaciesWithDefaults = new HashMap<>();
        Set<Double> times = new HashSet<>();
        times.addAll(efficacies().keySet());
        // Add VE 0 entries at times -1 and 0 if they do not exist already to make step and interpolating functions work
        times.add(-1.0);
        times.add(0.0);
        for (Double time : times) {
            Map<ExternalEfficacyType, Double> currentEfficacy = efficacies().getOrDefault(time,
                    new EnumMap<>(ExternalEfficacyType.class));
            if (currentEfficacy.keySet().size() != ExternalEfficacyType.values().length) {
                needsFilling = true;
                Map<ExternalEfficacyType, Double> efficacy = new EnumMap<>(ExternalEfficacyType.class);
                efficacy.putAll(currentEfficacy);
                efficacy.putIfAbsent(ExternalEfficacyType.TE_S, 0.0);
                efficacy.putIfAbsent(ExternalEfficacyType.TE_I, 0.0);
                efficacy.putIfAbsent(ExternalEfficacyType.TE_SP, efficacy.get(ExternalEfficacyType.TE_S));
                efficacy.putIfAbsent(ExternalEfficacyType.TE_SPH, efficacy.get(ExternalEfficacyType.TE_SP));
                efficacy.putIfAbsent(ExternalEfficacyType.TE_SPD, efficacy.get(ExternalEfficacyType.TE_SPH));
                efficaciesWithDefaults.put(time, efficacy);
            }
        }
        if (needsFilling) {
            return ImmutableTherapeuticEfficacySpecification.builder()
                    .from(this)
                    // Add in defaults
                    .putAllEfficacies(efficaciesWithDefaults)
                    .build();
        } else {
            return this;
        }
    }

    @Value.Derived
    boolean efficacyIsZero() {
        // Only if every efficacy value is zero
        return efficacies().values().stream().allMatch(
                        entry -> entry.values().stream().allMatch(ve -> ve == 0));
    }

    @Value.Derived
    Map<ExternalEfficacyType, UnivariateFunction> efficaciesOverTime() {
        Map<ExternalEfficacyType, UnivariateFunction> efficaciesOverTime = new EnumMap<>(ExternalEfficacyType.class);
        double[] times = efficacies().keySet().stream().mapToDouble(Double::doubleValue).sorted().toArray();
        if (times.length >= 2) {
            for (ExternalEfficacyType externalEfficacyType : ExternalEfficacyType.values()) {
                double[] efficacyValues = Arrays.stream(times)
                        .map(time -> efficacies().get(time).get(externalEfficacyType).doubleValue())
                        .toArray();
                if (interpolate()) {
                    efficaciesOverTime.put(externalEfficacyType, new LinearInterpolator().interpolate(times, efficacyValues));
                } else {
                    // Step function
                    efficaciesOverTime.put(externalEfficacyType, new StepFunction(times, efficacyValues));
                }

            }
        }
        return efficaciesOverTime;
    }

    double getEfficacy(double time, EfficacyType efficacyType) {
        if (efficacyIsZero()) {
            return 0.0;
        }
        switch (efficacyType) {
            case TE_S:
                return efficaciesOverTime().get(ExternalEfficacyType.TE_S).value(time);
            case TE_I:
                return efficaciesOverTime().get(ExternalEfficacyType.TE_I).value(time);
            case TE_P:
                double TESP = efficaciesOverTime().get(ExternalEfficacyType.TE_SP).value(time);
                if (TESP < 1.0) {
                    return 1.0 - (1.0 - efficaciesOverTime().get(ExternalEfficacyType.TE_S).value(time)) /
                            (1.0 - TESP);
                }
                else {
                    return 1.0;
                }
            case TE_H:
                double TESPH = efficaciesOverTime().get(ExternalEfficacyType.TE_SPH).value(time);
                if (TESPH < 1.0) {
                    return 1.0 - (1.0 - efficaciesOverTime().get(ExternalEfficacyType.TE_SP).value(time)) /
                            (1.0 - TESPH);
                }
                else {
                    return 1.0;
                }
            case TE_D:
                double TESPD = efficaciesOverTime().get(ExternalEfficacyType.TE_SPD).value(time);
                if (TESPD < 1.0) {
                    return 1.0 - (1.0 - efficaciesOverTime().get(ExternalEfficacyType.TE_SPH).value(time)) /
                            (1.0 - TESPD);
                }
                else {
                    return 1.0;
                }
            default:
                throw new RuntimeException("Unhandled therapeutic efficacy type.");
        }
    }

}
