package gcm.core.epi.plugin.therapeutic.multiplier;

import gcm.core.epi.plugin.therapeutic.TherapeuticPlugin;
import gcm.core.epi.variants.VariantId;
import plugins.gcm.agents.Environment;
import plugins.people.support.PersonId;

public class MultiplierTherapeuticPlugin implements TherapeuticPlugin {

    @Override
    public double getTES(Environment environment, PersonId personId, VariantId variantId) {
        return 0;
    }

    @Override
    public double getTEI(Environment environment, PersonId personId, VariantId variantId) {
        return 0;
    }

    @Override
    public double getTEP(Environment environment, PersonId personId, VariantId variantId) {
        return 0;
    }

    @Override
    public double getTEH(Environment environment, PersonId personId, VariantId variantId) {
        return 0;
    }

    @Override
    public double getTED(Environment environment, PersonId personId, VariantId variantId) {
        return 0;
    }
    
}
