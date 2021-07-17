package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

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

    @Value.Default
    double afterPeakHalfLife() {
        return Double.POSITIVE_INFINITY;
    }

    @Value.Derived
    double decayRate() { return Math.log(2) / afterPeakHalfLife();}

    public double getValue(double time) {
        return getValue(time, 0.0);
    }

    /*
        Starts at initialValue and remains there for the initial delay
        Grows linearly until the peak of 1.0 at the specified time
        Remains at peak and then decays linearly
     */
    public double getValue(double time, double initialValue) {
        if (time < initialDelay()) {
            return initialValue;
        } else if (time < peakTime()) {
            return initialValue + (1.0 - initialValue) * (time - initialDelay()) / (peakTime() - initialDelay());
        } else if (time < peakTime() + peakDuration()) {
            return 1.0;
        } else {
            return Math.exp(-decayRate() * (time - peakTime() - peakDuration()));
        }
    }

}
