package gcm.core.epi.test.manual.vaccine;

import gcm.core.epi.plugin.vaccine.resourcebased.*;
import gcm.core.epi.variants.VariantId;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVariantEfficacy {

    @Test
    public void testTime() {
        double initialDelay = 7.0;
        double peakTime = 14.0;
        double secondDoseDelay = 20.1;

        double halflifeVES = 60.0;
        double halflifeVESP = 120.0;
        double VES = 0.9;
        double VESP = 0.95;
        double VESPD = 0.98;
        double firstDoseRelativeEfficacy = 0.5;

        SimpleEfficacyFunction firstDoseEfficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(initialDelay)
                .peakTime(peakTime)
                .peakDuration(0.0)
                .build();
        SimpleEfficacyFunction secondDoseEfficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(initialDelay)
                .peakTime(peakTime)
                .peakDuration(0.0)
                .putAfterPeakHalfLife(ExternalEfficacyType.VE_S, halflifeVES)
                .putAfterPeakHalfLife(ExternalEfficacyType.VE_SP, halflifeVESP)
                // Implicitly no waning on VE_SPD
                .build();

        VariantId referenceVariant = VariantId.REFERENCE_ID;
        VariantId otherVariant = VariantId.of("Other");
        double variantRelativeEfficacy = 0.75;
        Map<ExternalEfficacyType, Double> variantEfficacy =
                new EnumMap<>(ExternalEfficacyType.class);
        variantEfficacy.put(ExternalEfficacyType.VE_S, variantRelativeEfficacy);
        variantEfficacy.put(ExternalEfficacyType.VE_SP, variantRelativeEfficacy);
        // Implicitly no effect on VE_SPD

        VaccineDefinition vaccineDefinition = ImmutableVaccineDefinition.builder()
                .id(VaccineId.of("Vaccine One"))
                .type(VaccineDefinition.DoseType.TWO_DOSE)
                .secondDoseDelay(21.0)
                .firstDoseEfficacyFunction(firstDoseEfficacyFunction)
                .secondDoseEfficacyFunction(secondDoseEfficacyFunction)
                .putEfficacy(ExternalEfficacyType.VE_S, VES)
                .putEfficacy(ExternalEfficacyType.VE_SP, VESP)
                .putEfficacy(ExternalEfficacyType.VE_SPD, VESPD)
                .putFirstDoseRelativeEfficacy(ExternalEfficacyType.VE_S, firstDoseRelativeEfficacy)
                .putVariantRelativeEfficacy(otherVariant, variantEfficacy)
                .putVariantFirstDoseRelativeEfficacy(otherVariant, variantEfficacy)
                .build();

        // One dose reference
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, referenceVariant, 1
        ), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, initialDelay, referenceVariant, 1
        ), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, (initialDelay + peakTime) * 0.5, referenceVariant, 1
        ), VES * firstDoseRelativeEfficacy * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime, referenceVariant, 1
        ), VES * firstDoseRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 28.0, referenceVariant, 1
        ), VES * firstDoseRelativeEfficacy);
        // Two dose reference
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, referenceVariant, 2
        ), VES * firstDoseRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, initialDelay, referenceVariant, 2
        ), VES * firstDoseRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, (initialDelay + peakTime) * 0.5, referenceVariant, 2
        ), (VES * firstDoseRelativeEfficacy + VES) * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime, referenceVariant, 2
        ), VES);
        double wanedVES = vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime + halflifeVES, referenceVariant, 2);
        assertEquals(wanedVES, VES * 0.5);
        wanedVES = vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime + halflifeVESP, referenceVariant, 2);
        double wanedVEP = vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_P, peakTime + halflifeVESP, referenceVariant, 2);
        double wanedVESP = 1.0 - (1.0 - wanedVES) * (1.0 - wanedVEP);
        assertEquals(wanedVESP, VESP * 0.5);
        double wanedVED = vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_D, peakTime + halflifeVESP, referenceVariant, 2);
        assertEquals(1.0 - (1.0 - wanedVED) * (1.0 - wanedVESP), VESPD);

        // One dose variant
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, 1
        ), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, initialDelay, otherVariant, 1
        ), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, (initialDelay + peakTime) * 0.5, otherVariant, 1
        ), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime, otherVariant, 1
        ), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 28.0, otherVariant, 1
        ), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy);

        // Two dose variant
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, 2
        ), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, initialDelay, otherVariant, 2
        ), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, (initialDelay + peakTime) * 0.5, otherVariant, 2
        ), (VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy + VES * variantRelativeEfficacy) * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime, otherVariant, 2
        ), VES * variantRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime + halflifeVES, otherVariant, 2
        ), VES * variantRelativeEfficacy * 0.5);

    }

    @Test
    public void testEfficacy() {

        VariantId referenceVariant = VariantId.REFERENCE_ID;
        VariantId otherVariant = VariantId.of("Other");
        Map<ExternalEfficacyType, Double> variantRelativeEfficacy =
                new EnumMap<>(ExternalEfficacyType.class);
        variantRelativeEfficacy.put(ExternalEfficacyType.VE_S, 0.8);
        variantRelativeEfficacy.put(ExternalEfficacyType.VE_SP, 0.9);
        variantRelativeEfficacy.put(ExternalEfficacyType.VE_SPD, 1.0);

        VaccineDefinition vaccineDefinition = ImmutableVaccineDefinition.builder()
                .id(VaccineId.of("Vaccine One"))
                .type(VaccineDefinition.DoseType.TWO_DOSE)
                .secondDoseDelay(21.0)
                .putEfficacy(ExternalEfficacyType.VE_S, 0.9)
                .putEfficacy(ExternalEfficacyType.VE_SP, 0.95)
                .putEfficacy(ExternalEfficacyType.VE_SPD, 0.98)
                .putEfficacy(ExternalEfficacyType.VE_I, 0.2)
                .putFirstDoseRelativeEfficacy(ExternalEfficacyType.VE_S, 0.5)
                .putVariantRelativeEfficacy(otherVariant, variantRelativeEfficacy)
                .build();

        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, referenceVariant, 2
        ), 0.9);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_I, 0.0, referenceVariant, 2
        ), 0.2);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_P, 0.0, referenceVariant, 2
        ), 0.5, 1e-12);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_D, 0.0, referenceVariant, 2
        ), 0.6);

        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, 2
        ), 0.9 * 0.8, 1e-12);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_I, 0.0, otherVariant, 2
        ), 0.2);
        assertEquals(1.0 - (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, 2
        )) * (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_P, 0.0, otherVariant, 2
        )), 0.95 * 0.9);
        assertEquals(1.0 - (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, 2
        )) * (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_P, 0.0, otherVariant, 2
        )) * (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_D, 0.0, otherVariant, 2
        )), 0.98);
    }
}
