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
        SimpleEfficacyFunction firstDoseEfficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(7.0)
                .peakTime(14.0)
                .build();
        SimpleEfficacyFunction secondDoseEfficacyFunction = ImmutableSimpleEfficacyFunction.builder()
                .initialDelay(7.0)
                .peakTime(14.0)
                .build();

        VariantId referenceVariant = VariantId.REFERENCE_ID;
        VariantId otherVariant = VariantId.of("Other");
        Map<VaccineDefinition.ExternalEfficacyType, Double> variantEfficacy =
                new EnumMap<>(VaccineDefinition.ExternalEfficacyType.class);
        variantEfficacy.put(VaccineDefinition.ExternalEfficacyType.VE_S, 0.5);

        VaccineDefinition vaccineDefinition = ImmutableVaccineDefinition.builder()
                .id(VaccineId.of("Vaccine One"))
                .type(VaccineDefinition.DoseType.TWO_DOSE)
                .secondDoseDelay(21.0)
                .firstDoseEfficacyFunction(firstDoseEfficacyFunction)
                .secondDoseEfficacyFunction(secondDoseEfficacyFunction)
                .putEfficacy(VaccineDefinition.ExternalEfficacyType.VE_S, 1.0)
                .putRelativeEfficacyOfFirstDose(VaccineDefinition.ExternalEfficacyType.VE_S, 0.5)
                .putVariantRelativeEfficacy(otherVariant, variantEfficacy)
                .putVariantFirstDoseRelativeEfficacy(otherVariant, variantEfficacy)
                .build();

        // One dose reference
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 0.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 7.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, (7.0 + 14.0) / 2.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.25);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 14.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 28.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5);
        // Two dose reference
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 7.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, (7.0 + 14.0) / 2.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.75);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 14.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 1.0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 28.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 1.0);

        // One dose variant
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 7.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, (7.0 + 14.0) / 2.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.25 * 0.5 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 14.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(1, 28.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5 * 0.5);

        // Two dose variant
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 7.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.5 * 0.5 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, (7.0 + 14.0) / 2.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), (0.5 * 0.5 + 1.0)/2 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 14.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 1.0 * 0.5);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 28.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 1.0 * 0.5);

    }

    @Test
    public void testEfficacy() {

        VariantId referenceVariant = VariantId.REFERENCE_ID;
        VariantId otherVariant = VariantId.of("Other");
        Map<VaccineDefinition.ExternalEfficacyType, Double> variantRelativeEfficacy =
                new EnumMap<>(VaccineDefinition.ExternalEfficacyType.class);
        variantRelativeEfficacy.put(VaccineDefinition.ExternalEfficacyType.VE_S, 0.8);
        variantRelativeEfficacy.put(VaccineDefinition.ExternalEfficacyType.VE_SP, 0.9);
        variantRelativeEfficacy.put(VaccineDefinition.ExternalEfficacyType.VE_SPD, 1.0);

        VaccineDefinition vaccineDefinition = ImmutableVaccineDefinition.builder()
                .id(VaccineId.of("Vaccine One"))
                .type(VaccineDefinition.DoseType.TWO_DOSE)
                .secondDoseDelay(21.0)
                .putEfficacy(VaccineDefinition.ExternalEfficacyType.VE_S, 0.9)
                .putEfficacy(VaccineDefinition.ExternalEfficacyType.VE_SP, 0.95)
                .putEfficacy(VaccineDefinition.ExternalEfficacyType.VE_SPD, 0.98)
                .putEfficacy(VaccineDefinition.ExternalEfficacyType.VE_I, 0.2)
                .putRelativeEfficacyOfFirstDose(VaccineDefinition.ExternalEfficacyType.VE_S, 0.5)
                .putVariantRelativeEfficacy(otherVariant, variantRelativeEfficacy)
                .build();

        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.9);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_I), 0.2);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_P), 0.5, 1e-12);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, referenceVariant,
                VaccineDefinition.EfficacyType.VE_D), 0.6);

        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S), 0.9 * 0.8, 1e-12);
        assertEquals(vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_I), 0.2);
        assertEquals(1.0 - (1.0 - vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S)) * (1.0 - vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_P)), 0.95 * 0.9);
        assertEquals(1.0 - (1.0 - vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_S)) * (1.0 - vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_P)) * (1.0 - vaccineDefinition.getVaccineEfficacy(2, 0.0, otherVariant,
                VaccineDefinition.EfficacyType.VE_D)), 0.98);
    }
}
