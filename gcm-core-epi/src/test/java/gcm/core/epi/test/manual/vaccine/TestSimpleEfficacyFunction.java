package gcm.core.epi.test.manual.vaccine;

import gcm.core.epi.plugin.vaccine.resourcebased.*;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSimpleEfficacyFunction {

    @Test
    public void test() {
        Map<ExternalEfficacyType, Double> afterPeakHalfLives = new EnumMap<>(ExternalEfficacyType.class);
        afterPeakHalfLives.put(ExternalEfficacyType.VE_S, 180.0);
        SimpleEfficacyFunction efficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(24.0)
                .peakTime(31.0)
                .peakDuration(30.0)
                .afterPeakHalfLife(afterPeakHalfLives)
                .build();

        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, 0.0), 0.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, 14.0), 0.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, 24.0), 0.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, (24.0 + 31.0)/2), 0.5);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, 31.0), 1.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, 61.0), 1.0);
        assertTrue(efficacyFunction.getValue(ExternalEfficacyType.VE_S, 62.0) < 1.0);
        assertEquals(efficacyFunction.getValue(ExternalEfficacyType.VE_S, 61.0 + 180.0), 0.5);
    }

}
