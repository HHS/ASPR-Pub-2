package gcm.core.epi.test.manual.vaccine;

import gcm.core.epi.plugin.vaccine.resourcebased.*;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.ImmutableAgeGroup;
import gcm.core.epi.propertytypes.AgeWeights;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSimpleEfficacyFunction {

    @Test
    public void test() {
        Map<ExternalEfficacyType, AgeWeights> afterPeakHalfLives = new EnumMap<>(ExternalEfficacyType.class);
        afterPeakHalfLives.put(ExternalEfficacyType.VE_S, AgeWeights.from(180.0));
        SimpleEfficacyFunction efficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(AgeWeights.from(24.0))
                .peakTime(AgeWeights.from(31.0))
                .peakDuration(AgeWeights.from(30.0))
                .afterPeakHalfLife(afterPeakHalfLives)
                .build();
        AgeGroup ageGroup = ImmutableAgeGroup.builder().name("A").build();

        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, ageGroup, 0.0), 0.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, ageGroup, 14.0), 0.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, ageGroup, 24.0), 0.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, ageGroup, (24.0 + 31.0)/2), 0.5);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, ageGroup, 31.0), 1.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, ageGroup, 61.0), 1.0);
        assertTrue(efficacyFunction.getValue(ExternalEfficacyType.VE_S, ageGroup, 62.0) < 1.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, ageGroup, 61.0 + 180.0), 0.5);
    }

}
