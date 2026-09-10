package com.autokm.app.routing

import com.graphhopper.GraphHopper
import com.graphhopper.config.Profile
import com.graphhopper.json.Statement
import com.graphhopper.routing.DefaultWeightingFactory
import com.graphhopper.routing.WeightingFactory
import com.graphhopper.routing.weighting.TurnCostProvider
import com.graphhopper.routing.weighting.custom.CustomWeighting
import com.graphhopper.routing.weighting.custom.PrecompiledCarWeightingHelper
import com.graphhopper.util.CustomModel
import com.graphhopper.util.Parameters

/**
 * GraphHopper only ships one profile ("car") which is a CustomWeighting profile. Normally,
 * setting up such a profile compiles a small helper class at runtime via Janino - that does
 * not work on Android (see PrecompiledCarWeightingHelper for why), so this subclass swaps in
 * the ahead-of-time compiled equivalent instead of going through GraphHopper's own
 * CustomModelParser/Janino path.
 */
class AndroidGraphHopper : GraphHopper() {

    override fun createWeightingFactory(): WeightingFactory {
        val fallback = DefaultWeightingFactory(baseGraph, encodingManager)
        return WeightingFactory { profile, hints, disableTurnCosts ->
            if (profile.name == CAR_PROFILE) {
                createCarWeighting(profile)
            } else {
                fallback.createWeighting(profile, hints, disableTurnCosts)
            }
        }
    }

    private fun createCarWeighting(profile: Profile): CustomWeighting {
        val customModel = profile.customModel ?: buildCarCustomModel()
        val helper = PrecompiledCarWeightingHelper()
        helper.init(customModel, encodingManager, CustomModel.getAreasAsMap(customModel.areas))
        val parameters = CustomWeighting.Parameters(
            helper::getSpeed, helper::calcMaxSpeed,
            helper::getPriority, helper::calcMaxPriority,
            helper::getTurnPenalty,
            customModel.distanceInfluence ?: 0.0,
            customModel.headingPenalty ?: Parameters.Routing.DEFAULT_HEADING_PENALTY,
        )
        return CustomWeighting(TurnCostProvider.NO_TURN_COST_PROVIDER, parameters)
    }

    companion object {
        const val CAR_PROFILE = "car"

        /** Mirrors routing-build/config.yml's "car" profile / GraphHopper's built-in car.json. */
        fun buildCarCustomModel(): CustomModel {
            val model = CustomModel()
            model.setDistanceInfluence(90.0)
            model.addToPriority(Statement.If("!car_access", Statement.Op.MULTIPLY, "0"))
            model.addToSpeed(Statement.If("true", Statement.Op.LIMIT, "car_average_speed"))
            return model
        }
    }
}
