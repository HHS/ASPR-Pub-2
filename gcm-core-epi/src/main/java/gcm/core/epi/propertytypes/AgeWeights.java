package gcm.core.epi.propertytypes;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.population.AgeGroup;
import org.immutables.value.Value;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@Value.Immutable
@JsonDeserialize(as = ImmutableAgeWeights.class)
public abstract class AgeWeights extends AbstractWeights<AgeGroup> {
    public static AgeWeights from(double value) {
        return ImmutableAgeWeights.builder().defaultValue(value).build();
    }

    public AgeWeights transform(Function<Double, Double> transformer) {
        ImmutableAgeWeights.Builder builder = ImmutableAgeWeights.builder()
                .defaultValue(transformer.apply(defaultValue()));
        values().forEach(
                ((ageGroup, weight) -> builder.putValues(ageGroup, transformer.apply(weight)))
        );
        return builder.build();
    }

    public AgeWeights multiply(AgeWeights ageWeights) {
        ImmutableAgeWeights.Builder builder = ImmutableAgeWeights.builder()
                .defaultValue(defaultValue() * ageWeights.defaultValue());
        // Add all values with weights
        Set<AgeGroup> ageGroupsWithValues = new HashSet<>();
        ageGroupsWithValues.addAll(values().keySet());
        ageGroupsWithValues.addAll(ageWeights.values().keySet());
        for (AgeGroup ageGroup : ageGroupsWithValues) {
            builder.putValues(ageGroup, getWeight(ageGroup) * ageWeights.getWeight(ageGroup));
        }
        return builder.build();
    }

    public boolean lessThanOrEqualTo(AgeWeights ageWeights) {
        if (defaultValue() > ageWeights.defaultValue()) {
            return false;
        }
        Set<AgeGroup> ageGroupsWithValues = new HashSet<>();
        ageGroupsWithValues.addAll(values().keySet());
        ageGroupsWithValues.addAll(ageWeights.values().keySet());
        for (AgeGroup ageGroup : ageGroupsWithValues) {
            if (getWeight(ageGroup) > ageWeights.getWeight(ageGroup)) {
                return false;
            }
        }
        return true;
    }

    public boolean greaterThanOrEqualTo(AgeWeights ageWeights) {
        if (defaultValue() < ageWeights.defaultValue()) {
            return false;
        }
        Set<AgeGroup> ageGroupsWithValues = new HashSet<>();
        ageGroupsWithValues.addAll(values().keySet());
        ageGroupsWithValues.addAll(ageWeights.values().keySet());
        for (AgeGroup ageGroup : ageGroupsWithValues) {
            if (getWeight(ageGroup) < ageWeights.getWeight(ageGroup)) {
                return false;
            }
        }
        return true;
    }

}
