package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.propertytypes.AgeWeights;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = ImmutableSimpleEfficacyFunction.class)
public abstract class SimpleEfficacyFunction {

    @Value.Default
    AgeWeights initialDelay() {
        return AgeWeights.from(0.0);
    }

    @Value.Default
    AgeWeights peakTime() {
        return AgeWeights.from(0.0);
    }

    @Value.Default
    AgeWeights peakDuration() {
        return AgeWeights.from(Double.POSITIVE_INFINITY);
    }

    abstract Map<ExternalEfficacyType, AgeWeights> afterPeakHalfLife();

    @Value.Derived
    Map<ExternalEfficacyType, AgeWeights> decayRate() {
        return Arrays.stream(ExternalEfficacyType.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        x -> afterPeakHalfLife().getOrDefault(x, AgeWeights.from(Double.POSITIVE_INFINITY))
                                .transform(y -> Math.log(2) / y)
                ));
    }

    public double getValue(ExternalEfficacyType externalEfficacyType, AgeGroup ageGroup, double time) {
        return getValue(externalEfficacyType, ageGroup, time, 0.0);
    }

    /*
        Starts at initialValue and remains there for the initial delay
        Grows linearly until the peak of 1.0 at the specified time
        Remains at peak and then decays exponentially
     */
    public double getValue(ExternalEfficacyType externalEfficacyType, AgeGroup ageGroup, double time, double initialValue) {
        if (time < initialDelay().getWeight(ageGroup)) {
            return initialValue;
        } else if (time < peakTime().getWeight(ageGroup)) {
            return initialValue + (1.0 - initialValue) * (time - initialDelay().getWeight(ageGroup)) /
                    (peakTime().getWeight(ageGroup) - initialDelay().getWeight(ageGroup));
        } else if (time < peakTime().getWeight(ageGroup) + peakDuration().getWeight(ageGroup)) {
            return 1.0;
        } else {
            return Math.exp(-decayRate().get(externalEfficacyType).getWeight(ageGroup) *
                    (time - peakTime().getWeight(ageGroup) - peakDuration().getWeight(ageGroup)));
        }
    }

}
