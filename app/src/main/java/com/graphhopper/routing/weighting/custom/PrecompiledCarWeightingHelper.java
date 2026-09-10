package com.graphhopper.routing.weighting.custom;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.routing.ev.*;
import java.util.Map;
import com.graphhopper.util.CustomModel;
import com.graphhopper.storage.BaseGraph;

/**
 * Hand-checked, ahead-of-time copy of the class GraphHopper's Janino-based CustomModelParser
 * would normally generate at runtime for our "car" custom model (see routing-build/config.yml,
 * distance_influence=90 / priority "!car_access -> *0" / speed "limit_to car_average_speed").
 * Janino compiles JVM bytecode on the fly, which does not work on Android (ART cannot load
 * classes generated that way), so this equivalent is compiled ahead of time by the normal
 * Android build instead. It was produced by running the same import with
 * -Dorg.codehaus.janino.source_debugging.enable=true (see .github/workflows/build-routing-data.yml)
 * and copying the generated source verbatim, only renaming the class.
 */
public class PrecompiledCarWeightingHelper extends CustomWeightingHelper {

    protected DecimalEncodedValue car_average_speed_enc;
    protected BooleanEncodedValue car_access_enc;

    @Override
    public void init(CustomModel customModel, EncodedValueLookup lookup, Map<String, com.graphhopper.util.JsonFeature> areas) {
        this.lookup = lookup;
        this.customModel = customModel;
        this.car_average_speed_enc = (DecimalEncodedValue) lookup.getEncodedValue("car_average_speed", EncodedValue.class);
        this.car_access_enc = (BooleanEncodedValue) lookup.getEncodedValue("car_access", EncodedValue.class);
    }

    @Override
    public double getPriority(EdgeIteratorState edge, boolean reverse) {
        double value = 1.0;
        boolean car_access = reverse ? edge.getReverse(car_access_enc) : edge.get(car_access_enc);
        if (!car_access) {
            value *= 0;
        }
        return value;
    }

    @Override
    public double getSpeed(EdgeIteratorState edge, boolean reverse) {
        double value = 999.0;
        double car_average_speed = reverse ? edge.getReverse(car_average_speed_enc) : edge.get(car_average_speed_enc);
        value = Math.min(value, car_average_speed);
        return value;
    }

    @Override
    public double getTurnPenalty(BaseGraph graph, EdgeIntAccess edgeIntAccess, int inEdge, int viaNode, int outEdge) {
        return 0;
    }
}
