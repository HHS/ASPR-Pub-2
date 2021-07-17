package gcm.core.epi.test.manual.vaccine;

import gcm.core.epi.plugin.vaccine.resourcebased.EfficacyFunction;
import gcm.core.epi.plugin.vaccine.resourcebased.ImmutableEfficacyFunction;
import gcm.core.epi.plugin.vaccine.resourcebased.ImmutableSimpleEfficacyFunction;
import gcm.core.epi.plugin.vaccine.resourcebased.SimpleEfficacyFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSimpleEfficacyFunction {

    @Test
    public void test() {
        SimpleEfficacyFunction efficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(24.0)
                .peakTime(31.0)
                .peakDuration(30.0)
                .afterPeakHalfLife(180)
                .build();

        assertEquals(efficacyFunction.getValue(0.0), 0.0);
        assertEquals(efficacyFunction.getValue(14.0), 0.0);
        assertEquals(efficacyFunction.getValue(24.0), 0.0);
        assertEquals(efficacyFunction.getValue((24.0 + 31.0)/2), 0.5);
        assertEquals(efficacyFunction.getValue(31.0), 1.0);
        assertEquals(efficacyFunction.getValue(61.0), 1.0);
        assertTrue(efficacyFunction.getValue(62.0) < 1.0);

    }

}
