package gcm.core.epi.plugin.therapeutic;

import gcm.core.epi.identifiers.GlobalProperty;
import gcm.core.epi.identifiers.PersonProperty;
import gcm.core.epi.plugin.Plugin;
import gcm.core.epi.variants.VariantId;
import gcm.core.epi.variants.VariantsDescription;
import plugins.gcm.agents.Environment;
import plugins.gcm.experiment.ExperimentBuilder;
import plugins.people.support.PersonId;

import java.util.Optional;

public interface TherapeuticPlugin extends Plugin {

    /*
        Get the reduction in probability that a treated susceptible person will be infected by an exposure
     */
    double getTES(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the reduction in probability that a treated infected person will transmit infection
     */
    double getTEI(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the reduction in probability that a treated infected person will have symptoms
     */
    double getTEP(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the reduction in probability of hospitalization for a treated symptomatic person
     */
    double getTEH(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the reduction in probability of death for a treated hospitalization-requiring person
     */
    double getTED(Environment environment, PersonId personId, VariantId variantId);

    /*
        Get the probability that treatment fails to prevent transmission, taking into account TEi and TEs of the
            source and target respectively
     */
    default double getProbabilityTherapeuticFailsToPreventTransmission(Environment environment,
                                                                       PersonId sourcePersonId,
                                                                       PersonId targetPersonId) {
        VariantsDescription variantsDescription = environment.getGlobalPropertyValue(GlobalProperty.VARIANTS_DESCRIPTION);
        int sourceStrainId = environment.getPersonPropertyValue(sourcePersonId, PersonProperty.PRIOR_INFECTION_STRAIN_INDEX_1);
        VariantId sourceVariant = variantsDescription.variantIdList().get(sourceStrainId);
        double sourceTEI = getTEI(environment, sourcePersonId, sourceVariant);
        double targetTES = getTES(environment, targetPersonId, sourceVariant);
        return (1.0 - sourceTEI) * (1.0 - targetTES);
    }

    @Override
    default void load(ExperimentBuilder experimentBuilder) {
        Plugin.super.load(experimentBuilder);
        experimentBuilder.addGlobalPropertyValue(GlobalProperty.THERAPEUTIC_PLUGIN, Optional.of(this));
    }

}
