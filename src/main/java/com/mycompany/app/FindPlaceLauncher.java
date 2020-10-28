package com.mycompany.app;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;

public class FindPlaceLauncher {

    public static void main(String[] args) {

        ArcGISRuntimeEnvironment.setInstallDirectory("/home/leonid/arcgis-runtime-sdk-java-100.8.0");

        FindPlaceSample.main(args);
    }
}
