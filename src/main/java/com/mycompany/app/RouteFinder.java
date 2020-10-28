package com.mycompany.app;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.tasks.networkanalysis.*;

import java.util.Arrays;

public class RouteFinder {
    final String routetTaskSandiego =
            "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route";
    // create route task from San Diego service
    RouteTask routeTask = new RouteTask(routetTaskSandiego);

    RouteParameters routeParameters = new RouteParameters();

    private void getRoute() {
        // load route task
        routeTask.loadAsync();
        routeTask.addDoneLoadingListener(() -> {
            if (routeTask.getLoadError() == null && routeTask.getLoadStatus() == LoadStatus.LOADED) {
                try {
                    SpatialReference ESPG_3857 = SpatialReference.create(102100);
                    Point stop1Loc = new Point(-1.3018598562659847E7, 3863191.8817135547, ESPG_3857);
                    Point stop2Loc = new Point(-1.3036911787723785E7, 3839935.706521739, ESPG_3857);

// add route stops
                    routeParameters.setStops(Arrays.asList(new Stop(stop1Loc), new Stop(stop2Loc)));

// create barriers
                    PointBarrier pointBarrier = new PointBarrier(new Point(-1.302759917994629E7, 3853256.753745117, ESPG_3857));
// add barriers to routeParameters
                    routeParameters.setPointBarriers(Arrays.asList(pointBarrier));
                    // get default route parameters
                    routeParameters = routeTask.createDefaultParametersAsync().get();
                    // set flags to return stops and directions
                    routeParameters.setReturnStops(true);
                    routeParameters.setReturnDirections(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public RouteResult execute() {
        getRoute();
        RouteResult result = null;
        try {
            result = routeTask.solveRouteAsync(routeParameters).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

}
