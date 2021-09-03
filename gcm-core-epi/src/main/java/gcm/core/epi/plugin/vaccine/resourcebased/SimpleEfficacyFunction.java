package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = ImmutableSimpleEfficacyFunction.class)
public abstract class SimpleEfficacyFunction {

    @Value.Default
    double initialDelay() {
        return 0.0;
    }

    @Value.Default
    double peakTime() {
        return 0.0;
    }

    @Value.Default
    double peakDuration() {
        return Double.POSITIVE_INFINITY;
    }

    abstract Map<ExternalEfficacyType, Double> afterPeakHalfLife();

    @Value.Derived
    Map<ExternalEfficacyType, Double> decayRate() {
        return Arrays.stream(ExternalEfficacyType.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        x -> Math.log(2) / afterPeakHalfLife().getOrDefault(x, Double.POSITIVE_INFINITY)
                ));
    }

    public double getValue(ExternalEfficacyType externalEfficacyType, double time) {
        return getValue(externalEfficacyType, time, 0.0);
    }

    /*
        Starts at initialValue and remains there for the initial delay
        Grows linearly until the peak of 1.0 at the specified time
        Remains at peak and then decays exponentially
     */
    public double getValue(ExternalEfficacyType externalEfficacyType, double time, double initialValue) {
        if (time < initialDelay()) {
            return initialValue;
        } else if (time < peakTime()) {
            return initialValue + (1.0 - initialValue) * (time - initialDelay()) / (peakTime() - initialDelay());
        } else if (time < peakTime() + peakDuration()) {
            return 1.0;
        } else {
            return Math.exp(-decayRate().get(externalEfficacyType) * (time - peakTime() - peakDuration()));
        }
    }

}
