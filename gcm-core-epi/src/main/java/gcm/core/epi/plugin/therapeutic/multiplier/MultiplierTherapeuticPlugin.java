package gcm.core.epi.plugin.therapeutic.multiplier;

import gcm.core.epi.plugin.therapeutic.TherapeuticPlugin;
import gcm.core.epi.util.property.DefinedGlobalProperty;
import gcm.core.epi.util.property.TypedPropertyDefinition;
import gcm.core.epi.variants.VariantId;
import plugins.gcm.agents.Environment;
import plugins.people.support.PersonId;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class MultiplierTherapeuticPlugin implements TherapeuticPlugin {

    @Override
    public double getTES(Environment environment, PersonId personId, VariantId variantId) {
        return getEfficacy(environment, EfficacyType.TE_S);
    }

    @Override
    public double getTEI(Environment environment, PersonId personId, VariantId variantId) {
        return getEfficacy(environment, EfficacyType.TE_I);
    }

    @Override
    public double getTEP(Environment environment, PersonId personId, VariantId variantId) {
        return getEfficacy(environment, EfficacyType.TE_P);
    }

    @Override
    public double getTEH(Environment environment, PersonId personId, VariantId variantId) {
        return getEfficacy(environment, EfficacyType.TE_H);
    }

    @Override
    public double getTED(Environment environment, PersonId personId, VariantId variantId) {
        return getEfficacy(environment, EfficacyType.TE_D);
    }

    private double getEfficacy(Environment environment, EfficacyType efficacyType) {
        double time = environment.getTime();
        TherapeuticEfficacySpecification therapeuticEfficacySpecification = environment.getGlobalPropertyValue(
                TherapeuticGlobalProperty.THERAPEUTIC_EFFICACY_MULTIPLIER);
        return therapeuticEfficacySpecification.getEfficacy(time, efficacyType);
    }

    @Override
    public Set<DefinedGlobalProperty> getGlobalProperties() {
        return new HashSet(EnumSet.allOf(TherapeuticGlobalProperty.class));
    }

    enum TherapeuticGlobalProperty implements DefinedGlobalProperty {

        THERAPEUTIC_EFFICACY_MULTIPLIER(TypedPropertyDefinition.builder()
                .type(TherapeuticEfficacySpecification.class)
                .defaultValue(ImmutableTherapeuticEfficacySpecification.builder().build())
                .isMutable(false)
                .build());

        private final TypedPropertyDefinition propertyDefinition;

        TherapeuticGlobalProperty(TypedPropertyDefinition propertyDefinition) {
            this.propertyDefinition = propertyDefinition;
        }

        @Override
        public boolean isExternalProperty() {
            return true;
        }

        @Override
        public TypedPropertyDefinition getPropertyDefinition() {
            return propertyDefinition;
        }
    }
}
