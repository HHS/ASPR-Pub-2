package gcm.core.epi.test.manual.vaccine;

import gcm.core.epi.plugin.vaccine.resourcebased.*;
import gcm.core.epi.population.AgeGroup;
import gcm.core.epi.population.ImmutableAgeGroup;
import gcm.core.epi.propertytypes.AgeWeights;
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
        double secondDoseDelay = 21.0;

        double halflifeVES = 60.0;
        double halflifeVESP = 120.0;
        double VES = 0.9;
        double VESP = 0.95;
        double VESPH = 0.98;
        double firstDoseRelativeEfficacy = 0.5;

        AgeGroup ageGroup = ImmutableAgeGroup.builder().name("A").build();

        SimpleEfficacyFunction firstDoseEfficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(AgeWeights.from(initialDelay))
                .peakTime(AgeWeights.from(peakTime))
                .peakDuration(AgeWeights.from(0.0))
                .build();
        SimpleEfficacyFunction secondDoseEfficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(AgeWeights.from(initialDelay))
                .peakTime(AgeWeights.from(peakTime))
                .peakDuration(AgeWeights.from(0.0))
                .putAfterPeakHalfLife(ExternalEfficacyType.VE_S, AgeWeights.from(halflifeVES))
                .putAfterPeakHalfLife(ExternalEfficacyType.VE_SP, AgeWeights.from(halflifeVESP))
                // Implicitly no waning on VE_SPD
                .build();

        VariantId referenceVariant = VariantId.REFERENCE_ID;
        VariantId otherVariant = VariantId.of("Other");
        double variantRelativeEfficacy = 0.75;
        Map<ExternalEfficacyType, AgeWeights> variantEfficacy =
                new EnumMap<>(ExternalEfficacyType.class);
        variantEfficacy.put(ExternalEfficacyType.VE_S, AgeWeights.from(variantRelativeEfficacy));
        variantEfficacy.put(ExternalEfficacyType.VE_SP, AgeWeights.from(variantRelativeEfficacy));
        // Implicitly no effect on VE_SPD

        VaccineDefinition vaccineDefinition = ImmutableVaccineDefinition.builder()
                .id(VaccineId.of("Vaccine One"))
                .type(VaccineDefinition.DoseType.TWO_DOSE)
                .secondDoseDelay(secondDoseDelay)
                .firstDoseEfficacyFunction(firstDoseEfficacyFunction)
                .secondDoseEfficacyFunction(secondDoseEfficacyFunction)
                .putEfficacy(ExternalEfficacyType.VE_S, AgeWeights.from(VES))
                .putEfficacy(ExternalEfficacyType.VE_SP, AgeWeights.from(VESP))
                .putEfficacy(ExternalEfficacyType.VE_SPH, AgeWeights.from(VESPH))
                .putFirstDoseRelativeEfficacy(ExternalEfficacyType.VE_S, AgeWeights.from(firstDoseRelativeEfficacy))
                .putVariantRelativeEfficacy(otherVariant, variantEfficacy)
                .putVariantFirstDoseRelativeEfficacy(otherVariant, variantEfficacy)
                .build();

        // One dose reference
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, referenceVariant, ageGroup,
                1), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, initialDelay, referenceVariant, ageGroup,
                1), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, (initialDelay + peakTime) * 0.5, referenceVariant, ageGroup,
                1), VES * firstDoseRelativeEfficacy * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime, referenceVariant, ageGroup,
                1), VES * firstDoseRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 28.0, referenceVariant, ageGroup,
                1), VES * firstDoseRelativeEfficacy);
        // Two dose reference
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, referenceVariant, ageGroup,
                2), VES * firstDoseRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, initialDelay, referenceVariant, ageGroup,
                2), VES * firstDoseRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, (initialDelay + peakTime) * 0.5, referenceVariant, ageGroup,
                2), (VES * firstDoseRelativeEfficacy + VES) * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime, referenceVariant, ageGroup,
                2), VES);
        double wanedVES = vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime + halflifeVES, referenceVariant, ageGroup, 2);
        assertEquals(wanedVES, VES * 0.5);
        wanedVES = vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime + halflifeVESP, referenceVariant, ageGroup, 2);
        double wanedVEP = vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_P, peakTime + halflifeVESP, referenceVariant, ageGroup, 2);
        double wanedVESP = 1.0 - (1.0 - wanedVES) * (1.0 - wanedVEP);
        assertEquals(wanedVESP, VESP * 0.5);
        double wanedVED = vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_H, peakTime + halflifeVESP, referenceVariant, ageGroup, 2);
        assertEquals(1.0 - (1.0 - wanedVED) * (1.0 - wanedVESP), VESPH);

        // One dose variant
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, ageGroup,
                1), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, initialDelay, otherVariant, ageGroup,
                1), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, (initialDelay + peakTime) * 0.5, otherVariant, ageGroup,
                1), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime, otherVariant, ageGroup,
                1), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime + 14.0, otherVariant, ageGroup,
                1), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy);

        // Two dose variant
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, ageGroup,
                2), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, initialDelay, otherVariant, ageGroup,
                2), VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, (initialDelay + peakTime) * 0.5, otherVariant, ageGroup,
                2), (VES * firstDoseRelativeEfficacy * variantRelativeEfficacy * variantRelativeEfficacy + VES * variantRelativeEfficacy) * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime, otherVariant, ageGroup,
                2), VES * variantRelativeEfficacy);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, peakTime + halflifeVES, otherVariant, ageGroup,
                2), VES * variantRelativeEfficacy * 0.5);

    }

    @Test
    public void testEfficacy() {

        VariantId referenceVariant = VariantId.REFERENCE_ID;
        VariantId otherVariant = VariantId.of("Other");
        Map<ExternalEfficacyType, AgeWeights> variantRelativeEfficacy =
                new EnumMap<>(ExternalEfficacyType.class);
        variantRelativeEfficacy.put(ExternalEfficacyType.VE_S, AgeWeights.from(0.8));
        variantRelativeEfficacy.put(ExternalEfficacyType.VE_SP, AgeWeights.from(0.9));
        variantRelativeEfficacy.put(ExternalEfficacyType.VE_SPH, AgeWeights.from(1.0));

        AgeGroup ageGroup = ImmutableAgeGroup.builder().name("A").build();

        VaccineDefinition vaccineDefinition = ImmutableVaccineDefinition.builder()
                .id(VaccineId.of("Vaccine One"))
                .type(VaccineDefinition.DoseType.TWO_DOSE)
                .secondDoseDelay(21.0)
                .putEfficacy(ExternalEfficacyType.VE_S, AgeWeights.from(0.9))
                .putEfficacy(ExternalEfficacyType.VE_SP, AgeWeights.from(0.95))
                .putEfficacy(ExternalEfficacyType.VE_SPH, AgeWeights.from(0.98))
                .putEfficacy(ExternalEfficacyType.VE_I, AgeWeights.from(0.2))
                .putFirstDoseRelativeEfficacy(ExternalEfficacyType.VE_S, AgeWeights.from(0.5))
                .putVariantRelativeEfficacy(otherVariant, variantRelativeEfficacy)
                .build();

        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, referenceVariant, ageGroup,
                2), 0.9);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_I, 0.0, referenceVariant, ageGroup,
                2), 0.2);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_P, 0.0, referenceVariant, ageGroup,
                2), 0.5, 1e-12);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_H, 0.0, referenceVariant, ageGroup,
                2), 0.6);

        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, ageGroup,
                2), 0.9 * 0.8, 1e-12);
        assertEquals(vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_I, 0.0, otherVariant, ageGroup,
                2), 0.2);
        assertEquals(1.0 - (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, ageGroup,
                2)) * (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_P, 0.0, otherVariant, ageGroup,
                2)), 0.95 * 0.9);
        assertEquals(1.0 - (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_S, 0.0, otherVariant, ageGroup,
                2)) * (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_P, 0.0, otherVariant, ageGroup,
                2)) * (1.0 - vaccineDefinition.getVaccineEfficacy(EfficacyType.VE_H, 0.0, otherVariant, ageGroup,
                2)), 0.98);
    }
}
