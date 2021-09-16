package gcm.core.epi.plugin.vaccine.resourcebased;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gcm.core.epi.propertytypes.AgeWeights;
import gcm.core.epi.propertytypes.FipsCodeDouble;
import gcm.core.epi.propertytypes.ImmutableAgeWeights;
import gcm.core.epi.propertytypes.ImmutableFipsCodeDouble;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableVaccineAdministratorDefinition.class)
public abstract class VaccineAdministratorDefinition {

    public abstract VaccineAdministratorId id();

    @Value.Default
    public FipsCodeDouble vaccinationRatePerDay() {
        return ImmutableFipsCodeDouble.builder().build();
    }

    @Value.Default
    public AgeWeights vaccineUptakeWeights() {
        return AgeWeights.from(1.0);
    }

    @Value.Default
    public UptakeNormalization vaccineUptakeNormalization() {
        return UptakeNormalization.POPULATION;
    }

    @Value.Default
    public AgeWeights vaccineHighRiskUptakeWeights() {
        return AgeWeights.from(1.0);
    }

    @Value.Default
    public AgeWeights vaccineInfectedUptakeWeights() {
        return AgeWeights.from(1.0);
    }

    @Value.Default
    public Boolean reserveSecondDoses() {
        return true;
    }

    @Value.Default
    public Double fractionReturnForSecondDose() {
        return 1.0;
    }

    @Value.Default
    public Boolean forceSecondDoseFraction() {
        return false;
    }

    @Value.Default
    public Double secondDoseFraction() {
        return fractionReturnForSecondDose() / (1.0 + fractionReturnForSecondDose());
    }

    public enum UptakeNormalization {
        POPULATION,
        DOSES
    }

}
