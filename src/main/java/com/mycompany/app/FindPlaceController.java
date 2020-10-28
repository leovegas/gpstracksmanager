package com.mycompany.app;

/*
 * Copyright 2017 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.*;

import com.esri.arcgisruntime.internal.jni.CoreSimpleMarkerSymbolStyle;
import com.esri.arcgisruntime.internal.mapping.view.VertexSketchTool;
import com.esri.arcgisruntime.internal.util.JsonUtil;
import com.esri.arcgisruntime.layers.BingMapsLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.*;
import com.esri.arcgisruntime.tasks.geocode.*;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.swing.event.ChangeEvent;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.esri.arcgisruntime.internal.jni.CoreSimpleMarkerSymbolStyle.*;

public class FindPlaceController {

    private static final String GEOLOGY_FEATURE_SERVICE =
            "https://sampleserver6.arcgisonline.com/arcgis/rest/services/Energy/Geology/FeatureServer/9";
    @FXML
    public Button goToButton;
    @FXML
    public ChoiceBox cb;
    @FXML
    public TextField sliderValueText;
    @FXML
    public TextField slider2ValueText;
    @FXML
    public Image imageCreatePolygon;
    @FXML
    public ToggleButton cutOffButton;
    @FXML
    private Slider slider;
    @FXML
    private Slider slider2;
    @FXML
    private ListProperty<String> choiceBoxItems = new SimpleListProperty(FXCollections.observableArrayList());
    @FXML
    private ObservableList<String> obsStrings;
    @FXML
    private TextField textArea;
    @FXML
    private TextField longLat;
    @FXML
    private TextField resultHeading;
    @FXML
    private TextField textTrack;
    @FXML
    private TextField textTrack2;
    @FXML
    private TextField textDistance;
    @FXML
    private ComboBox<String> locationBox;
    @FXML
    private ComboBox<String> namesBox;
    @FXML
    private MapView mapView;
    @FXML
    private Button redoButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button testButton;
    @FXML
    private ToggleButton makePolygonButton;
    @FXML
    private ToggleButton addTracksButton;

    private Preferences preferences;
    private Callout callout;
    private GraphicsOverlay graphicsOverlay;
    private LocatorTask locatorTask;
    private PictureMarkerSymbol pinSymbol;

    private int hexRed = 0xFFFF0000;
    private int hexGreen = 0xFF00FF00;
    private int hexBlue = 0x330000FF;
    private int hexBlue10 = 0x100000FF;
    private int hexBlueCover = 0x336babde;
    private int hexYellow= 0x33ffffcc;
    private int hexYellowDark= 0x33f5cf0d;

    private double maxlines;
    private double maxlines2;
    private double pointX;
    private double pointY;
    private double distance;
    private double sumArea;
    private double area;
    private double km = 0;


    private ArrayList<String> messages = new ArrayList<>();
    private ArrayList<Geometry> geometries = new ArrayList<>();
    private ArrayList<Geometry> geometriesCovers = new ArrayList<>();

    private ArrayList<Polygon> polygons = new ArrayList<>();
    private ArrayList<Point> pointsForAzimuth = new ArrayList<>();
    private ArrayList<Point> firstPoint = new ArrayList<>();

    private ArrayList<Point> polylinePointsTemp = new ArrayList<>();
    private ArrayList<Polyline> trackLines = new ArrayList<>();
    private Map<String, Polygon> polygonsDB = new LinkedHashMap<>();
    private ArrayList<String> names = new ArrayList<>();
    double azimuth;
    String chooseFromList;
    double delt;

    Map<String, String> JsonListDB;
    private SketchEditor mSketchEditor;

    SpatialReference spatialReference1 = SpatialReferences.getWgs84();
    SpatialReference spatialReference4 = SpatialReferences.getWgs84();
    SpatialReference spatialReference5 = SpatialReferences.getWgs84();

    PointCollection polylinePoints = new PointCollection(spatialReference1);
    PointCollection polygonPoints = new PointCollection(spatialReference4);
    PolylineBuilder polylineBuilder = new PolylineBuilder(spatialReference5);
    PointCollection tempCollection = new PointCollection(spatialReference4);

    private boolean flag;

    public static Cursor cursorM;

    public ListProperty<String> choiceBoxItemsProperty() {
        return choiceBoxItems ;
    }

    public ObservableList<String> getChoiceBoxItems() {
        return choiceBoxItemsProperty().get();
    }

    public void setComboBoxItems(ObservableList<String> choiceBoxItems) {
        choiceBoxItemsProperty().set(choiceBoxItems) ;
    }

    private void addPointGraphic(Point point) {
        if (graphicsOverlay != null) {
            SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CROSS, hexRed, 12.0f);
            pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, hexBlue, 1.0f));
            Graphic pointGraphic = new Graphic(point, pointSymbol);
            graphicsOverlay.getGraphics().add(pointGraphic);
        }
    }

    private void distanceCounter(Geometry geometry) {
        double dist = GeometryEngine.lengthGeodetic(geometry, new LinearUnit(LinearUnitId.METERS), GeodeticCurveType.GEODESIC);
        textDistance.setText(Math.round(dist) + " m");
    }


    private void addPolylineTemp(Point point) {
        if (graphicsOverlay != null) {

            String latLonToDouble = CoordinateFormatter.toLatitudeLongitude(point, CoordinateFormatter
                    .LatitudeLongitudeFormat.DECIMAL_DEGREES, 8);

            double pX = Double.parseDouble(latLonToDouble.substring(13, 25));
            double pY = Double.parseDouble(latLonToDouble.substring(0, 11));

            if (latLonToDouble.substring(11, 12).equals("S")) {
                pY = pY * (-1);
            }
            if (latLonToDouble.substring(25, 26).equals("W")) {
                pX = pX * (-1);
            }

            addPointGraphic(point);
            polylinePointsTemp.add(new Point(pX, pY));
            pointsForAzimuth.add(new Point(pX, pY));

            if (polylinePointsTemp.size() == 2) {

                LineSegment lineSegment =
                        new LineSegment(polylinePointsTemp.get(0), polylinePointsTemp.get(1));

                Part firstPart = new Part(SpatialReferences.getWgs84());
                firstPart.add(lineSegment);
                polylineBuilder.addPart(firstPart);

                SimpleLineSymbol polygonSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, hexRed, 2.0f);

                Graphic graphic = new Graphic(polylineBuilder.toGeometry(), polygonSymbol);
                trackLines.clear();
                trackLines.add(polylineBuilder.toGeometry());

                distanceCounter(polylineBuilder.toGeometry());

                if (pointsForAzimuth.size() == 2) {
                    azimuth = (GeometryEngine.distanceGeodetic(pointsForAzimuth.get(0), pointsForAzimuth.get(1), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth1() > 0) ?
                               GeometryEngine.distanceGeodetic(pointsForAzimuth.get(0), pointsForAzimuth.get(1), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth1() :
                               GeometryEngine.distanceGeodetic(pointsForAzimuth.get(0), pointsForAzimuth.get(1), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth2();

                    textDistance.setText(Math.round(azimuth * 10000.0) / 10000.0 + " degrees");

                    pointsForAzimuth.remove(0);
                }

                graphicsOverlay.getGraphics().add(graphic);
                polylinePointsTemp.clear();
            }
        }
    }

    private void addPolylineGraphic(Polyline polyline, int color, PointCollection pointCollection) {
        if (graphicsOverlay != null) {
            SimpleLineSymbol polylineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, color, 3f);
            if (pointCollection!=null) {
                Polyline temp=new Polyline(pointCollection);
                Graphic polylineGraphic = new Graphic(temp, polylineSymbol);
                graphicsOverlay.getGraphics().add(polylineGraphic);
            }
            else {
                Graphic polylineGraphic = new Graphic(polyline, polylineSymbol);
                graphicsOverlay.getGraphics().add(polylineGraphic);
            }
        }
    }


    @FXML
    private void addPolygonGraphic2() {
        if (graphicsOverlay != null) {
            if (makePolygonButton.isSelected()) {
                CursorStage();
                createModePolygon();
                makePolygonButton.setStyle("-fx-background-color: red");
                System.out.println("selected");
                makePolygonButton.setSelected(true);

            } else {
                    System.out.println("unselected");
                    if (mSketchEditor.isSketchValid()) {
                        createPolygonMethod((Polyline) mSketchEditor.getGeometry());
                    }
                    mSketchEditor.stop();
                    makePolygonButton.setSelected(false);
                makePolygonButton.setStyle("-fx-background-color: white");

            }
        }
    }

    @FXML
    private void cutOffToggle() {
        if (graphicsOverlay != null) {
            if (cutOffButton.isSelected()) {
                createModePolygon();
                cutOffButton.setStyle("-fx-background-color: red");
                cutOffButton.setSelected(true);

            } else {
                if (mSketchEditor.isSketchValid()) {
                    cutOff();
                }
                cutOffButton.setSelected(false);
                cutOffButton.setStyle("-fx-background-color: white");

            }
        }
    }


    @FXML
    private void addTracks() throws InterruptedException {
        if (graphicsOverlay != null) {
            if (addTracksButton.isSelected()) {
                setOnClickHandlerPolyline();
                addTracksButton.setStyle("-fx-background-color: red");
                addTracksButton.setSelected(true);
            } else {
                //Give you set of Threads
                Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
                //Iterate over set to find yours
                for(Thread thread : setOfThread){
                    if(thread.getName().equals("trackline")){
                        thread.interrupt();
                    }
                }
                trackLines.clear();
                addTracksButton.setSelected(false);
                addTracksButton.setStyle("-fx-background-color: white");

            }
        }
    }
    private void addPolygonGraphic(Point point) {
        if (graphicsOverlay != null) {

            String latLonToDouble = CoordinateFormatter.toLatitudeLongitude(point, CoordinateFormatter
                    .LatitudeLongitudeFormat.DECIMAL_DEGREES, 8);

            System.out.println(latLonToDouble);

            double pX = Double.parseDouble(latLonToDouble.substring(13, 25));
            double pY = Double.parseDouble(latLonToDouble.substring(0, 11));

            if (latLonToDouble.substring(11, 12).equals("S")) {
                pY = pY * (-1);
            }
            if (latLonToDouble.substring(25, 26).equals("W")) {
                pX = pX * (-1);
            }
            System.out.println(point.getX() + " " + point.getY());

            polygonPoints.add(new Point(pX, pY));

            if (polygonPoints.size() >= 3) {

                makePolygonButton.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.PRIMARY) {
                        makePolygonButton.setStyle("-fx-background-color: grey");
                        pointsForAzimuth.clear();
                        createPolygonMethod(null);
                    }
                });
            }

            textArea.setOnMouseClicked(this::handleMouseClicked);
        }
    }

    public void createPolygonMethod(Polyline pol) {
       // polygonPoints.add(polygonPoints.get(0));
        Polygon polygon=null;
        if (pol==null) {
            polygon = new Polygon(polygonPoints);
        }
        else {
            polygonPoints.clear();

            pol.getParts().getPartsAsPoints().forEach(e -> {
                String latLonToDouble = CoordinateFormatter.toLatitudeLongitude(e, CoordinateFormatter
                        .LatitudeLongitudeFormat.DECIMAL_DEGREES, 8);

                double pX = Double.parseDouble(latLonToDouble.substring(13, 25));
                double pY = Double.parseDouble(latLonToDouble.substring(0, 11));

                if (latLonToDouble.substring(11, 12).equals("S")) {
                    pY = pY * (-1);
                }
                if (latLonToDouble.substring(25, 26).equals("W")) {
                    pX = pX * (-1);
                }
                System.out.println(e.getX() + " " + e.getY());

                polygonPoints.add(new Point(pX, pY));
            });
            polygonPoints.add(polygonPoints.get(0));

        }
        polygon = new Polygon(polygonPoints);
        Point centerPoint = polygon.getExtent().getCenter();

        SimpleFillSymbol polygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, hexBlue10,
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, hexGreen, 2.0f));
        Graphic polygonGraphic = new Graphic(polygon, polygonSymbol);

        graphicsOverlay.getGraphics().add(polygonGraphic);

        polylineBuilder.getParts().clear();
        mSketchEditor.stop();
        graphicsOverlay.getGraphics().removeIf(e2 -> e2.getGeometry().getGeometryType().name().equals("POLYLINE"));
        graphicsOverlay.getGraphics().removeIf(e3 -> e3.getGeometry().getGeometryType().name().equals("POINT"));
        polylinePointsTemp.clear();
        polygonPoints.clear();

        geometries.add(polygon);

        distanceCounter(polygon);

        area = GeometryEngine.areaGeodetic(polygon, new AreaUnit(AreaUnitId.HECTARES), GeodeticCurveType.GEODESIC);
        sumArea = sumArea + area;
        System.out.println(area + " ha. Total " + sumArea);
        textArea.setText(String.valueOf(Math.round(area * 100.0) / 100.0 + " ha. Total " + Math.round(sumArea * 100.0) / 100.0 + " ha"));
    }

    public void createCoverPolygonMethod(PointCollection points, Geometry geometry) {
        Polygon polygon = new Polygon(points);
        Polygon resultPolygon = (Polygon) GeometryEngine.intersection(polygon,geometry);

        SimpleFillSymbol polygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, hexBlueCover,
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, hexRed, 2.0f));
        Graphic polygonGraphic = new Graphic(resultPolygon, polygonSymbol);
        graphicsOverlay.getGraphics().add(polygonGraphic);
        //graphicsOverlay.getGraphics().removeIf(e3 -> e3.getGeometry().getGeometryType().name().equals("POINT"));
        geometriesCovers.add(polygon);
    }

    public void handleMouseClicked(MouseEvent e) {
        // Get the source and type of the Event
        String type = e.getEventType().getName();

        if (type.equals("MOUSE_CLICKED")) {
            if (textArea.getText().contains("ha")) {
                textArea.setText(String.valueOf(Math.round((area * 10000) * 100.0) / 100.0 + " m2. Total " + Math.round((sumArea * 10000) * 100.0) / 100.0 + " m2"));
            } else {
                if (textArea.getText().contains("m2")) {
                    textArea.setText(String.valueOf(Math.round((area) * 100.0) / 100.0 + " ha. Total " + Math.round((sumArea) * 100.0) / 100.0 + " ha"));
                }
            }
        }
    }

    private Polygon getGeometryFromSketch(SketchEditor sketchEditor) {
        tempCollection = new PointCollection(spatialReference4);
        Polygon polygont = (Polygon) sketchEditor.getGeometry();
        Polyline pol = polygont.toPolyline();
        pol.getParts().getPartsAsPoints().forEach(e -> {
            String latLonToDouble = CoordinateFormatter.toLatitudeLongitude(e, CoordinateFormatter
                    .LatitudeLongitudeFormat.DECIMAL_DEGREES, 8);

            double pX = Double.parseDouble(latLonToDouble.substring(13, 25));
            double pY = Double.parseDouble(latLonToDouble.substring(0, 11));

            if (latLonToDouble.substring(11, 12).equals("S")) {
                pY = pY * (-1);
            }
            if (latLonToDouble.substring(25, 26).equals("W")) {
                pX = pX * (-1);
            }

            tempCollection.add(new Point(pX, pY));
        });
        Polygon polygon = new Polygon(tempCollection);
        return polygon;
    }

    private void showCalloutWithFieldInfo(Point location, String message1, String message2, Geometry geom) {
        Callout callout = mapView.getCallout();

        callout.setTitle("Location:");
//        String latLonDecimalDegrees = CoordinateFormatter.toLatitudeLongitude(location, CoordinateFormatter
//                .LatitudeLongitudeFormat.DECIMAL_DEGREES, 4);
//        String latLonDegMinSec = CoordinateFormatter.toLatitudeLongitude(location, CoordinateFormatter
//                .LatitudeLongitudeFormat.DEGREES_MINUTES_SECONDS, 1);
//        String utm = CoordinateFormatter.toUtm(location, CoordinateFormatter.UtmConversionMode.LATITUDE_BAND_INDICATORS,
//                true);
//        String usng = CoordinateFormatter.toUsng(location, 4, true);
        callout.setDetail(
                " Name: " + message1 + "\n" +
                " Area: " + message2 + "\n" +
                " Milage ("+sliderValueText.getText()+" m): " + Math.round((km) * 10.0) / 10.0 + " km" + "\n");
        messages.add(message1);

        Point2D screenPoint = mapView.locationToScreen(location);

        ContextMenu contextMenu = new ContextMenu();

        MenuItem menuItemEdit = new MenuItem("Edit",null);
        MenuItem menuItemSave = new MenuItem("Save",null);
        MenuItem menuItemRemove = new MenuItem("Remove",null);
        MenuItem menuItemInfo = new MenuItem("Info",null);
        MenuItem menuItemCalculate = new MenuItem("Calculate Tracks",null);
        MenuItem menuItemAddTracks = new MenuItem("Add Tracks",null);

        contextMenu.getItems().add(menuItemEdit);
        contextMenu.getItems().add(menuItemSave);
        contextMenu.getItems().add(menuItemRemove);
        contextMenu.getItems().add(menuItemInfo);
        contextMenu.getItems().add(menuItemCalculate);
        contextMenu.getItems().add(menuItemAddTracks);

        contextMenu.show(mapView,screenPoint.getX()-100,screenPoint.getY()-100);

        menuItemRemove.setOnAction(e -> {
            // Get the source and type of the Event
            String type = e.getEventType().getName();
            MenuItem type2 = (MenuItem) e.getSource();
            if (type.equals("ACTION"))
                if (type2.getText().equals("Remove")) {
                    removeFromDB(message1.substring(1,message1.length()-1));
                    messages.clear();
                    contextMenu.hide();
                }
        });

        menuItemEdit.setOnAction(e -> {
            // Get the source and type of the Event
            String type = e.getEventType().getName();
            MenuItem type2 = (MenuItem) e.getSource();
            if (type.equals("ACTION"))
                if (type2.getText().equals("Edit")) {
                mSketchEditor.start(geom);
                }
        });
        menuItemSave.setOnAction(e -> {
            // Get the source and type of the Event
            String type = e.getEventType().getName();
            MenuItem type2 = (MenuItem) e.getSource();
            if (type.equals("ACTION"))
            if (type2.getText().equals("Save")) {
                contextMenu.hide();
                removeFromDB(messages.get(0).substring(1,messages.get(0).length()-1));
                if (mSketchEditor.isSketchValid()) addToDB(messages.get(0).substring(1,messages.get(0).length()-1), getGeometryFromSketch(mSketchEditor));
                messages.clear();
                mSketchEditor.stop();
            }
        });
        menuItemInfo.setOnAction(e -> {
            // Get the source and type of the Event
            String type = e.getEventType().getName();
            MenuItem type2 = (MenuItem) e.getSource();
            if (type.equals("ACTION"))
                if (type2.getText().equals("Info")) {
                    contextMenu.hide();
                    callout.showCalloutAt(location, new Duration(500));
                }
        });

        menuItemCalculate.setOnAction(e -> {
            // Get the source and type of the Event
            String type = e.getEventType().getName();
            MenuItem type2 = (MenuItem) e.getSource();
            if (type.equals("ACTION"))
                if (type2.getText().equals("Calculate Tracks")) {
                    contextMenu.hide();
                    calculateTracks(geom);                //****************************//
                }
        });

        menuItemAddTracks.setOnAction(e -> {
            // Get the source and type of the Event
            String type = e.getEventType().getName();
            MenuItem type2 = (MenuItem) e.getSource();
            if (type.equals("ACTION"))
                if (type2.getText().equals("Add Tracks")) {
                    contextMenu.hide();
                    CoordinatsStage(geom);
                }
        });

        callout.setOnMouseClicked(
                evt -> {
                    // check that the primary mouse button was clicked and the user is not panning
                    if (evt.isStillSincePress() && evt.getButton() == MouseButton.PRIMARY) {
                        callout.dismiss();
                    }
                });
        //callout.showCalloutAt(location, new Duration(500));
    }

    private void showCalloutWithLocationCoordinates(Point location) {
        Callout callout = mapView.getCallout();
        callout.setTitle("Location:");
        String latLonDecimalDegrees = CoordinateFormatter.toLatitudeLongitude(location, CoordinateFormatter
                .LatitudeLongitudeFormat.DECIMAL_DEGREES, 4);
        String latLonDegMinSec = CoordinateFormatter.toLatitudeLongitude(location, CoordinateFormatter
                .LatitudeLongitudeFormat.DEGREES_MINUTES_SECONDS, 1);
        String utm = CoordinateFormatter.toUtm(location, CoordinateFormatter.UtmConversionMode.LATITUDE_BAND_INDICATORS,
                true);
        String usng = CoordinateFormatter.toUsng(location, 4, true);
        callout.setDetail(
                "Decimal Degrees: " + latLonDecimalDegrees + "\n" +
                        "Degrees, Minutes, Seconds: " + latLonDegMinSec + "\n" +
                        "UTM: " + utm + "\n" +
                        "USNG: " + usng + "\n"
        );
        mapView.getCallout().showCalloutAt(location, new Duration(500));
    }

    private void loadShapeFile(String path) {
        // instantiate shapefile feature table with the path to the .shp file
        ShapefileFeatureTable shapefileTable = new ShapefileFeatureTable(path);

        shapefileTable.loadAsync();
        shapefileTable.addDoneLoadingListener(() -> {
            if (shapefileTable.getLoadStatus() == LoadStatus.LOADED) {

                //create a feature layer for the shapefile feature table
                FeatureLayer shapefileLayer = new FeatureLayer(shapefileTable);

                //add the layer to the map.
                mapView.getMap().getOperationalLayers().add(shapefileLayer);
            }
        });
    }

    private void createPointTable(FeatureCollection featureCollection) {
        List<Feature> features = new ArrayList<>();
        List<Field> pointFields = new ArrayList<>();
        List<Field> polygonFields = new ArrayList<>();

        pointFields.add(Field.createString("Place", "Place Name", 50));
        polygonFields.add(Field.createString("Place2", "Place Name2", 50));

        FeatureCollectionTable pointsTable = new FeatureCollectionTable(pointFields, GeometryType.POINT, SpatialReferences.getWgs84());
        FeatureCollectionTable polygonsTable = new FeatureCollectionTable(polygonFields, GeometryType.POLYGON, SpatialReferences.getWgs84());

//        SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, 0xFF0000FF, 18);
//        SimpleRenderer renderer = new SimpleRenderer(simpleMarkerSymbol);

//        pointsTable.setRenderer(renderer);
//        featureCollection.getTables().add(pointsTable);

//        SimpleMarkerSymbol simpleMarkerSymbol2 = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, 0xFF0000FF, 18);
//        SimpleRenderer renderer2 = new SimpleRenderer(simpleMarkerSymbol2);
//        polygonsTable.setRenderer(renderer2);
        featureCollection.getTables().add(polygonsTable);

        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put(polygonFields.get(0).getName(), "Dodger Stadium");
//
//        PointCollection coloradoCorners = new PointCollection(SpatialReferences.getWgs84());
//        coloradoCorners.add(-109.048, 40.998);
//        coloradoCorners.add(-102.047, 40.998);
//        coloradoCorners.add(-102.037, 36.989);
//        coloradoCorners.add(-109.048, 36.998);
//        Polygon polygon = new Polygon(coloradoCorners);

        // features.add(polygonsTable.createFeature(attributes1, polygon));
        polygons.forEach(e -> features.add(polygonsTable.createFeature(attributes1, e)));

        // Dodger Stadium
//        Map<String, Object> attributes1 = new HashMap<>();
//        attributes1.put(pointFields.get(0).getName(), "Dodger Stadium");
//        Point point1 = new Point(-118.2406294, 34.0736221, SpatialReferences.getWgs84());
//        features.add(pointsTable.createFeature(attributes1, point1));
//
//        // Los Angeles Memorial Coliseum
//        Map<String, Object> attributes2 = new HashMap<>();
//        attributes2.put(pointFields.get(0).getName(), "LA Coliseum");
//        Point point2 = new Point(-118.287767, 34.013999, SpatialReferences.getWgs84());
//        features.add(pointsTable.createFeature(attributes2, point2));
//
//        // Staples Center
//        Map<String, Object> attributes3 = new HashMap<>();
//        attributes3.put(pointFields.get(0).getName(), "Staples Center");
//        Point point3 = new Point(-118.267028, 34.043145, SpatialReferences.getWgs84());
//        features.add(pointsTable.createFeature(attributes3, point3));

        polygonsTable.addFeaturesAsync(features);
    }

    @FXML
    private void createModeFreehandLine() {
        mSketchEditor.start(SketchCreationMode.FREEHAND_LINE);
    }

    @FXML
    private void createModePolygon() {
        SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, hexGreen, 2.0f);
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, hexRed, 10.0f);
        SimpleMarkerSymbol pointSymbol2 = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, hexRed, 3.0f);
        SimpleMarkerSymbol midPointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.X, hexRed, 3.0f);


        SketchStyle polylineSketchStyle = new SketchStyle();
        polylineSketchStyle.setLineSymbol(lineSymbol);
        polylineSketchStyle.setVertexSymbol(pointSymbol);
        polylineSketchStyle.setSelectedVertexSymbol(pointSymbol2);
        polylineSketchStyle.setMidVertexSymbol(midPointSymbol);

        mSketchEditor.setSketchStyle(polylineSketchStyle);
        mSketchEditor.start(SketchCreationMode.POLYLINE);

        mSketchEditor.addGeometryChangedListener(e -> {
            distanceCounter(e.getGeometry());
        });

    }

    @FXML
    public void initialize() throws FileNotFoundException {
        preferences = Preferences.userRoot();

        for (int i = 0; i < preferences.getInt("namesSize", 0); i++) {
            names.add(preferences.get(String.valueOf(i), null));
            choiceBoxItems.add(preferences.get(String.valueOf(i), null));
        }

        BingMapsLayer bingMapsLayer = new BingMapsLayer(BingMapsLayer.Style.AERIAL, "ArOX-EqbED6ssoRJWzcVYuo5m99PliBmI79Gj1M8xrR9nBzehg1pr0qffuXmg5_o");

        bingMapsLayer.addDoneLoadingListener(new Runnable() {
            public void run() {
                if (bingMapsLayer.getLoadStatus() == LoadStatus.LOADED) {
                    // work with bingMapsLayer here
                }
            }
        });

       // ArcGISMap map = new ArcGISMap(Basemap.Type.IMAGERY, 59.162777, 16.770897, 10);
       ArcGISMap map = new ArcGISMap();

        map.setInitialViewpoint(new Viewpoint(59.162777, 16.770897, 500000));
//        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(GEOLOGY_FEATURE_SERVICE);

        // create the feature layer using the service feature table
//        FeatureLayer featureLayer = new FeatureLayer(serviceFeatureTable);

        // add the layer to the ArcGISMap
        map.getOperationalLayers().add(bingMapsLayer);

        mapView.setMap(map);
        mapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

//        FeatureCollection featureCollection = new FeatureCollection();
//        FeatureCollectionLayer featureCollectionLayer = new FeatureCollectionLayer(featureCollection);
//        map.getOperationalLayers().add(featureCollectionLayer);
//
//
//        featureCollectionLayer.addDoneLoadingListener(new Runnable() {
//            @Override
//            public void run() {
//                if (featureCollectionLayer.getLoadStatus() == LoadStatus.LOADED) {
//                    System.out.println(featureCollection.toJson());
//                }
//            }
//        });
//        createPointTable(featureCollection);

     //   slider.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::changeLabelHandler);
     //   slider2.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::changeLabelHandler);


        // add a graphics overlay to the map view
        graphicsOverlay = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(graphicsOverlay);

        // create a new sketch editor and add it to the map view
        mSketchEditor = new SketchEditor();
        mapView.setSketchEditor(mSketchEditor);

        mapView.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                // Respond to primary (left) button only
                if (event.getButton() == MouseButton.SECONDARY) {
                    //make a screen coordinate from the clicked location
                    Point2D clickedPoint = new Point2D(event
                            .getX(), event.getY());
                    Point mapPoint = mapView.screenToLocation(clickedPoint);

                    geometries.forEach(e -> {
                        // identify graphics on the graphics overlay
                        final ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics = mapView
                                .identifyGraphicsOverlayAsync(
                                        graphicsOverlay,
                                        clickedPoint, 1, false, 2);

                        identifyGraphics
                                .addDoneListener(new Runnable() {

                                    @Override
                                    public void run() {
                                        //wait to do this on the UI thread
                                        Platform.runLater(new Runnable() {

                                            @Override
                                            public void run() {
                                                //when the layer is loaded refresh the layer list
                                                seaBirdDialog(identifyGraphics, mapPoint);
                                                try {
                                                    System.out.println(identifyGraphics.get().getGraphics().get(0).getAttributes());
                                                } catch (InterruptedException interruptedException) {
                                                    interruptedException.printStackTrace();
                                                } catch (ExecutionException executionException) {
                                                    executionException.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                });
                    });
                }
            }
        });

        // set the callout's default style
        callout = mapView.getCallout();
        callout.setLeaderPosition(Callout.LeaderPosition.BOTTOM);

        // create a locatorTask task
        locatorTask = new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");

        // create a pin graphic
        Image img = new Image(getClass().getResourceAsStream("/find_place/pin.png"), 0, 80, true, true);
        pinSymbol = new PictureMarkerSymbol(img);
        pinSymbol.loadAsync();

        // event to get auto-complete suggestions when the user types a place query
//        placeBox.getEditor().setOnKeyTyped((KeyEvent evt) -> {
//
//            // get the search box text for auto-complete suggestions
//            String typed = placeBox.getEditor().getText();
//
//            if (!"".equals(typed)) {
//
//                // suggest places only
//                SuggestParameters geocodeParameters = new SuggestParameters();
//                geocodeParameters.getCategories().add("POI");
//
//                // get suggestions from the locatorTask
//                ListenableFuture<List<SuggestResult>> suggestions = locatorTask.suggestAsync(typed, geocodeParameters);
//
//                // add a listener to update suggestions list when loaded
//                suggestions.addDoneListener(new SuggestionsLoadedListener(suggestions, placeBox));
//            }
//        });

        // event to get auto-complete suggestions for location when the user types a search location
        locationBox.getEditor().setOnKeyTyped((KeyEvent evt) -> {

            // get the search box text for auto-complete suggestions
            String typed = locationBox.getEditor().getText();

            if (!typed.equals("")) {

                // get suggestions from the locatorTask
                ListenableFuture<List<SuggestResult>> suggestions = locatorTask.suggestAsync(typed);

                // add a listener to update suggestions list when loaded
                suggestions.addDoneListener(new SuggestionsLoadedListener(suggestions, locationBox));
            }
        });

        textArea.setEditable(false);
        textDistance.setEditable(false);

        //  textArea.setDisable(true);
        sumArea = 0;

      //  setOnClickHandlerMapView();

        mapView.setOnMouseClicked(evt -> {
            // check that the primary mouse button was clicked and the user is not panning
            if (evt.isStillSincePress() && evt.getButton() == MouseButton.PRIMARY) {
                // get the map point where the user clicked
                pointX = evt.getX();
                pointY = evt.getY();
                // show the callout at the point with the different coordinate format strings

                // create a point from where the user clicked
                Point2D point = new Point2D(evt.getX(), evt.getY());
                System.out.println(mapView.screenToLocation(point).getX()+" "+mapView.screenToLocation(point).getY());

                firstPoint.add(mapView.screenToLocation(point));

            }
        });

    }

    private void addPolylineManual(double pX, double pY, double head) {
        // event to display a callout for a selected result
            // check that the primary mouse button was clicked and the user is not panning
                // get the map point where the user clicked
                pointX = pX;
                pointY = pY;
                // show the callout at the point with the different coordinate format strings
                // create a point from where the user clicked

                Point2D point = new Point2D(pX, pY);
                Point mapPoint = mapView.screenToLocation(point);
                Point projectedPoint = (Point) GeometryEngine.project(mapPoint, SpatialReference.create(4236));

                Point secondPoint = GeometryEngine.moveGeodetic(projectedPoint, 100, new LinearUnit(LinearUnitId.METERS), head, new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC);
                addPolylineTemp(projectedPoint);
                addPolylineTemp(secondPoint);


    }

    private void setOnClickHandlerPolyline() {
        // event to display a callout for a selected result
        mapView.setOnMouseClicked(evt -> {
            // check that the primary mouse button was clicked and the user is not panning
            if (evt.isStillSincePress() && evt.getButton() == MouseButton.PRIMARY) {

                // get the map point where the user clicked
                pointX = evt.getX();
                pointY = evt.getY();

                // show the callout at the point with the different coordinate format strings

                // create a point from where the user clicked
                Point2D point = new Point2D(evt.getX(), evt.getY());
                Point mapPoint = mapView.screenToLocation(point);

                System.out.println("X "+evt.getX());
                System.out.println("Y "+evt.getY());

                addPolylineTemp(mapPoint);

            }
        });
    }

        private void setOnClickHandlerMapView() {
        // event to display a callout for a selected result
        mapView.setOnMouseClicked(evt -> {
            // check that the primary mouse button was clicked and the user is not panning
            if (evt.isStillSincePress() && evt.getButton() == MouseButton.PRIMARY) {

                // get the map point where the user clicked
                pointX = evt.getX();
                pointY = evt.getY();
                // show the callout at the point with the different coordinate format strings

                // create a point from where the user clicked
                Point2D point = new Point2D(evt.getX(), evt.getY());
                Point mapPoint = mapView.screenToLocation(point);

                Runnable runnable =
                        () -> {
                            addPointGraphic(mapPoint);

                            addPolylineTemp(mapPoint);

                            addPolygonGraphic(mapPoint);
                        };
                Thread tr = new Thread(runnable);
                tr.start();

                // get layers with elements near the clicked location
                ListenableFuture<IdentifyGraphicsOverlayResult> identifyResults = mapView.identifyGraphicsOverlayAsync(graphicsOverlay, point,
                        10, false);
                identifyResults.addDoneListener(() -> {
                    try {
                        List<Graphic> graphics = identifyResults.get().getGraphics();
                        if (graphics.size() > 0) {
                            Graphic marker = graphics.get(0);
                            // update the callout
                            Platform.runLater(() -> {
                                callout.setTitle(marker.getAttributes().get("title").toString());
                                callout.setDetail(marker.getAttributes().get("detail").toString());
                                callout.showCalloutAt((Point) marker.getGeometry(), new Point2D(0, -24), Duration.ZERO);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    /**
     * Searches for places near the chosen location when the "search" button is clicked.
     */
    @FXML
    private void search() {
        String placeQuery = "1";
//        String placeQuery = placeBox.getEditor().getText();
        String locationQuery = locationBox.getEditor().getText();
        if (placeQuery != null && locationQuery != null && !"".equals(placeQuery) && !"".equals(locationQuery)) {
            GeocodeParameters geocodeParameters = new GeocodeParameters();
            geocodeParameters.getResultAttributeNames().add("*"); // return all attributes
            geocodeParameters.setOutputSpatialReference(mapView.getSpatialReference());

            // run the locatorTask geocode task
            ListenableFuture<List<GeocodeResult>> results = locatorTask.geocodeAsync(locationQuery, geocodeParameters);
            results.addDoneListener(() -> {
                try {
                    List<GeocodeResult> points = results.get();
                    if (points.size() > 0) {
                        // create a search area envelope around the location
                        Point p = points.get(0).getDisplayLocation();
                        Envelope preferredSearchArea = new Envelope(p.getX() - 10000, p.getY() - 10000, p.getX() + 10000, p.getY
                                () + 10000, p.getSpatialReference());
                        // set the geocode parameters search area to the envelope
                        geocodeParameters.setSearchArea(preferredSearchArea);
                        // zoom to the envelope
                        mapView.setViewpointAsync(new Viewpoint(preferredSearchArea));
                        // perform the geocode operation
                        ListenableFuture<List<GeocodeResult>> geocodeTask = locatorTask.geocodeAsync(placeQuery,
                                geocodeParameters);

                        // add a listener to display the results when loaded
                        geocodeTask.addDoneListener(new ResultsLoadedListener(geocodeTask));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void seaBirdDialog(ListenableFuture<IdentifyGraphicsOverlayResult> identifyGraphics, Point mp) {
        StringBuilder seaBirds = new StringBuilder();
        String area = null;
        String name = null;
        Point p= null;
        polylinePointsTemp.clear();

        try {
            // get the list of graphics returned by identify
            IdentifyGraphicsOverlayResult graphics = identifyGraphics.get();
            Geometry geom = null;

            //loop through the graphics
            for (Graphic grItem : graphics.getGraphics()) {
                area = getGeometryArea(grItem.getGeometry());
                name = String.valueOf(grItem.getAttributes().values());
                p = grItem.getGeometry().getExtent().getCenter();
                geom=grItem.getGeometry();
                seaBirds.append(name);
                seaBirds.append(area);

                if (seaBirds.length() > 0) {
                    showCalloutWithFieldInfo(p, name, area, geom);
                }


                polylineBuilder = new PolylineBuilder(SpatialReferences.getWgs84());
                textArea.clear();
            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Searches for places within the current map extent when the "redo search in this area" button is clicked.
     */

    private void placePictureMarkerSymbol(PictureMarkerSymbol markerSymbol, Point graphicPoint) {

        // set size of the image
        markerSymbol.setHeight(40);
        markerSymbol.setWidth(40);

        // load symbol asynchronously
        markerSymbol.loadAsync();

        // add to the graphic overlay once done loading
        markerSymbol.addDoneLoadingListener(() -> {
            if (markerSymbol.getLoadStatus() == LoadStatus.LOADED) {
                Graphic symbolGraphic = new Graphic(graphicPoint, markerSymbol);
                graphicsOverlay.getGraphics().add(symbolGraphic);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Picture Marker Symbol Failed to Load!");
                alert.show();
            }
        });
    }

    private void calculateTracks(Geometry geometry) {
        
        double delta = 0.02;

        Point point1 = new Point((geometry.getExtent().getXMin() - delta), (geometry.getExtent().getYMax() + delta), SpatialReferences.getWgs84());
        Point point2 = new Point((geometry.getExtent().getXMax() + delta), (geometry.getExtent().getYMax() + delta), SpatialReferences.getWgs84());
        Point point3 = new Point((geometry.getExtent().getXMax() + delta), (geometry.getExtent().getYMin() - delta), SpatialReferences.getWgs84());
        Point point4 = new Point((geometry.getExtent().getXMin() - delta), (geometry.getExtent().getYMin() - delta), SpatialReferences.getWgs84());

        PointCollection pointsCollection = new PointCollection(SpatialReferences.getWgs84());
        pointsCollection.add(point1);
        pointsCollection.add(point2);
        pointsCollection.add(point3);
        pointsCollection.add(point4);
        PointCollection linesTopPoints = new PointCollection(SpatialReferences.getWgs84());
        PointCollection linesBottomPoints = new PointCollection(SpatialReferences.getWgs84());
        PointCollection linesTopPoints28 = new PointCollection(SpatialReferences.getWgs84());
        PointCollection linesBottomPoints28 = new PointCollection(SpatialReferences.getWgs84());

        createCoverPolygonMethod(pointsCollection, geometry);

        PointCollection pointssTop = new PointCollection(SpatialReferences.getWgs84());
        pointssTop.add(pointsCollection.get(0));
        pointssTop.add(pointsCollection.get(1));
        Polyline polylineTop = new Polyline(pointssTop);

        PointCollection pointsBottom = new PointCollection(SpatialReferences.getWgs84());
        pointsBottom.add(pointsCollection.get(3));
        pointsBottom.add(pointsCollection.get(2));
        Polyline polylineBottom = new Polyline(pointsBottom);

        PointCollection pointsLeft = new PointCollection(SpatialReferences.getWgs84());
        pointsLeft.add(pointsCollection.get(0));
        pointsLeft.add(pointsCollection.get(3));
        Polyline polylineLeft = new Polyline(pointsLeft);

        PointCollection pointRight = new PointCollection(SpatialReferences.getWgs84());
        pointRight.add(pointsCollection.get(1));
        pointRight.add(pointsCollection.get(2));
        Polyline polylineRight = new Polyline(pointRight);

        try {
            addTrackLines(polylineTop);
            addTrackLines(polylineBottom);
        } catch (Exception e) {
        }
        try {
            addTrackLines(polylineLeft);
            addTrackLines(polylineRight);
        } catch (Exception e) {
        }
        try {
            addTrackLines(polylineTop);
            addTrackLines(polylineLeft);
        } catch (Exception e) {

        }
        try {
            addTrackLines(polylineBottom);
            addTrackLines(polylineRight);
        } catch (Exception e) {
        }

        try {
            addTrackLines(polylineBottom);
            addTrackLines(polylineLeft);
        } catch (Exception e) {
        }
        try {
            addTrackLines(polylineTop);
            addTrackLines(polylineRight);
        } catch (Exception e) {
        }

        pointsCollection.forEach(e -> {
            Point projectedPoint = (Point) GeometryEngine.project(e, SpatialReference.create(4236));
            //addPointGraphic(projectedPoint);  //cover polygon points
        });

        double coverLength = GeometryEngine.lengthGeodetic(polylineTop, new LinearUnit(LinearUnitId.METERS), GeodeticCurveType.GEODESIC);

        Polyline enterLine = trackLines.get(0);

//        if (flag) {  //TODO
//            delt += 0.0005;
//            PointCollection temp = new PointCollection(SpatialReferences.getWgs84());
//            Point a = enterLine.getParts().get(0).getStartPoint();
//            Point b = enterLine.getParts().get(0).getEndPoint();
//            Point aNew = new Point(a.getX()+delt,a.getY()+delt);
//            Point bNew = new Point(b.getX()+delt,b.getY()+delt);
//            temp.add(aNew);
//            temp.add(bNew);
//            enterLine = new Polyline(temp);
//        }

        double angle1 = (GeometryEngine.distanceGeodetic(enterLine.getParts().get(0).getStartPoint(), enterLine.getParts().get(0).getEndPoint(), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth1() > 0) ?
                GeometryEngine.distanceGeodetic(enterLine.getParts().get(0).getStartPoint(), enterLine.getParts().get(0).getEndPoint(), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth1() :
                GeometryEngine.distanceGeodetic(enterLine.getParts().get(0).getStartPoint(), enterLine.getParts().get(0).getEndPoint(), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth2();
        double angle2 = angle1 - 90;

        Point newPoint1 = GeometryEngine.moveGeodetic(enterLine.getParts().get(0).getStartPoint(), coverLength / 2, new LinearUnit(LinearUnitId.METERS), angle2, new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC);
        Point newPoint2 = GeometryEngine.moveGeodetic(enterLine.getParts().get(0).getStartPoint(), coverLength / 2, new LinearUnit(LinearUnitId.METERS), angle2 + 180, new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC);
        Point newPoint3 = GeometryEngine.moveGeodetic(enterLine.getParts().get(0).getEndPoint(), coverLength / 2, new LinearUnit(LinearUnitId.METERS), angle2, new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC);
        Point newPoint4 = GeometryEngine.moveGeodetic(enterLine.getParts().get(0).getEndPoint(), coverLength / 2, new LinearUnit(LinearUnitId.METERS), angle2 + 180, new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC);

        PointCollection orthogonalPolylinePoints1 = new PointCollection(SpatialReferences.getWgs84());
        orthogonalPolylinePoints1.add(newPoint1);
        orthogonalPolylinePoints1.add(newPoint2);
        Polyline orthogonalPolylineTop = new Polyline(orthogonalPolylinePoints1);
        PointCollection orthogonalPolylinePoints2 = new PointCollection(SpatialReferences.getWgs84());
        orthogonalPolylinePoints2.add(newPoint3);
        orthogonalPolylinePoints2.add(newPoint4);
        Polyline orthogonalPolylineBottom = new Polyline(orthogonalPolylinePoints2);

//       addPolylineGraphic(orthogonalPolylineTop);
//       addPolylineGraphic(orthogonalPolylineBottom);
        //  graphicsOverlay.getGraphics().removeIf(e2 -> e2.getGeometry().getGeometryType().name().equals("POLYLINE"));  //clean before draw tracks

        double orthogonalPolylineLength = GeometryEngine.lengthGeodetic(orthogonalPolylineTop, new LinearUnit(LinearUnitId.METERS), GeodeticCurveType.GEODESIC);

        Point enterLineStart = new Point(enterLine.getParts().get(0).getStartPoint().getX(),enterLine.getParts().get(0).getStartPoint().getY());
        Point enterLineEnd = new Point(enterLine.getParts().get(0).getEndPoint().getX(),enterLine.getParts().get(0).getEndPoint().getY());

        PointCollection orthogonalPolylinePart1 = new PointCollection(SpatialReferences.getWgs84());
        orthogonalPolylinePart1.add(enterLineStart);
        orthogonalPolylinePart1.add(newPoint1);
        Polyline orthogonalPolylineTopPart1 = new Polyline(orthogonalPolylinePart1);

        PointCollection orthogonalPolylinePart2 = new PointCollection(SpatialReferences.getWgs84());
        orthogonalPolylinePart2.add(enterLineStart);
        orthogonalPolylinePart2.add(newPoint2);
        Polyline orthogonalPolylineTopPart2 = new Polyline(orthogonalPolylinePart2);

        double orthogonalPolylinePart1Length1 = GeometryEngine.lengthGeodetic(orthogonalPolylineTopPart1, new LinearUnit(LinearUnitId.METERS), GeodeticCurveType.GEODESIC);
        double orthogonalPolylinePart1Length2 = GeometryEngine.lengthGeodetic(orthogonalPolylineTopPart2, new LinearUnit(LinearUnitId.METERS), GeodeticCurveType.GEODESIC);

        double sliderValue = Double.parseDouble(sliderValueText.getText());
        double slider2Value = Double.parseDouble(slider2ValueText.getText());

        double gpsPosLine = (slider2Value*(24/slider2Value))+slider2Value/2;

        double lines28m = orthogonalPolylinePart1Length1/gpsPosLine;
        
            maxlines = orthogonalPolylinePart1Length1 / sliderValue;
            maxlines2 = orthogonalPolylinePart1Length2 / sliderValue;

        double start28x = enterLineStart.getX();
        double end28x = newPoint2.getX();
        double interval28X = (end28x - start28x) / lines28m;
        double start28y = enterLineStart.getY();
        double end28y = newPoint2.getY();
        double interval28Y = (end28y - start28y) / lines28m;

        double start28x2 = enterLineEnd.getX();
        double end28x2 = newPoint2.getX();
        double interval28X2 = (end28x2 - start28x2) / lines28m;
        double start28y2 = enterLineEnd.getY();
        double end28y2 = newPoint2.getY();
        double interval28Y2 = (end28y2 - start28y2) / lines28m;

        double startx = enterLineStart.getX();
        double endx = newPoint2.getX();
        double intervalX = (endx - startx) / maxlines;
        double starty = enterLineStart.getY();
        double endy = newPoint2.getY();
        double intervalY = (endy - starty) / maxlines;

        double startx2 = enterLineEnd.getX();
        double endx2 = newPoint4.getX();
        double intervalX2 = (endx2 - startx2) / maxlines;
        double starty2 = enterLineEnd.getY();
        double endy2 = newPoint4.getY();
        double intervalY2 = (endy2 - starty2) / maxlines;

        double startx3 = enterLineStart.getX();
        double endx3 = newPoint1.getX();
        double intervalX3 = (endx3 - startx3) / maxlines2;
        double starty3 = enterLineStart.getY();
        double endy3 = newPoint1.getY();
        double intervalY3 = (endy3 - starty3) / maxlines2;

        double startx4 = enterLineEnd.getX();
        double endx4 = newPoint3.getX();
        double intervalX4 = (endx4 - startx4) / maxlines2;
        double starty4 = enterLineEnd.getY();
        double endy4 = newPoint3.getY();
        double intervalY4 = (endy4 - starty4) / maxlines2;

        linesTopPoints.add(new Point(startx3, starty3));
        linesBottomPoints.add(new Point(startx4, starty4));
        linesTopPoints.add(new Point(startx, starty));
        linesBottomPoints.add(new Point(startx2, starty2));

        for (int i = 0; i < maxlines; i++) {
            startx3 = startx3 + intervalX3;
            starty3 = starty3 + intervalY3;
            startx4 = startx4 + intervalX4;
            starty4 = starty4 + intervalY4;
            startx = startx + intervalX;
            starty = starty + intervalY;
            startx2 = startx2 + intervalX2;
            starty2 = starty2 + intervalY2;
            linesTopPoints.add(new Point(startx3, starty3));
            linesBottomPoints.add(new Point(startx4, starty4));
            linesTopPoints.add(new Point(startx, starty));
            linesBottomPoints.add(new Point(startx2, starty2));
        }

        km = 0;
        double kmMinus = GeometryEngine.lengthGeodetic(geometry, new LinearUnit(LinearUnitId.KILOMETERS), GeodeticCurveType.GEODESIC);

        PointCollection out = new PointCollection(SpatialReferences.getWgs84());
        out.add(linesTopPoints.get(0));
        out.add(linesBottomPoints.get(0));
        Polyline outPolyline=null;
        Polyline temp=new Polyline(out);


        for (int i = 1; i < linesTopPoints.size(); i++) {
            out = new PointCollection(SpatialReferences.getWgs84());
            out.add(linesTopPoints.get(i));
            out.add(linesBottomPoints.get(i));
            outPolyline = new Polyline(out);
            temp = (Polyline) GeometryEngine.union(outPolyline,temp);

        }
        // addPolylineGraphic(temp);

//                Polygon p = null;
//                p = (Polygon) GeometryEngine.offset(geometry,-0.00024, GeometryOffsetType.ROUNDED, 1,100);  //add around tracks
//                addPolygonToMap(p,null,null,null);

        Polyline result = (Polyline) GeometryEngine.intersection(geometry,temp);
        addPolylineGraphic(result, hexGreen, null);
        km = GeometryEngine.lengthGeodetic(result, new LinearUnit(LinearUnitId.KILOMETERS), GeodeticCurveType.GEODESIC);

        //   Point startGPS = (Point) GeometryEngine.project(, SpatialReference.create(4236));

        Point projectedPoint = (Point) GeometryEngine.project(geometry.getExtent().getCenter(), SpatialReference.create(4236));
        addPointGraphic(projectedPoint);  //cover polygon points
        projectedPoint = (Point) GeometryEngine.project(enterLine.getExtent().getCenter(), SpatialReference.create(4236));
        addPointGraphic(projectedPoint);  //cover polygon points

        PointCollection mainTrackPoints = new PointCollection(SpatialReferences.getWgs84());
        mainTrackPoints.add(new Point(enterLineStart.getX()-interval28X,enterLineStart.getY()-interval28Y));
        mainTrackPoints.add(new Point(enterLineEnd.getX()-interval28X2,enterLineEnd.getY()-interval28Y2));
        Polyline mainTrackPolyline = new Polyline(mainTrackPoints);


        if (GeometryEngine.distanceBetween(enterLine,geometry.getExtent().getCenter())>GeometryEngine.distanceBetween(mainTrackPolyline,geometry.getExtent().getCenter())) {
           // addPolylineGraphic(mainTrackPolyline, hexRed, null);                                               //add enter line for styring tracks
        }else {
            mainTrackPoints.clear();
            mainTrackPoints.add(new Point(enterLineStart.getX()+interval28X,enterLineStart.getY()+interval28Y));
            mainTrackPoints.add(new Point(enterLineEnd.getX()+interval28X2,enterLineEnd.getY()+interval28Y2));
            mainTrackPolyline = new Polyline(mainTrackPoints);
          //  addPolylineGraphic(mainTrackPolyline, hexRed, null);                                               //add enter line for styring tracks
        }

        Polyline RESULTPOLYLINE = (Polyline) GeometryEngine.intersection(geometry,mainTrackPolyline);
        addPolylineGraphic(RESULTPOLYLINE, hexRed, null);                                               //add enter line for styring tracks


        //   mainTrackPoints.clear();
        PointCollection mainTrackPoints2 = new PointCollection(SpatialReferences.getWgs84());
        RESULTPOLYLINE.getParts().getPartsAsPoints().forEach(e -> {
            addPointGraphic(e);
            mainTrackPoints2.add(e);
        });
        longLat.setText(String.valueOf(Math.round(mainTrackPoints2.get(0).getY() * 100000000.0) / 100000000.0)+", "+Math.round(mainTrackPoints2.get(0).getX() * 100000000.0) / 100000000.0);

        double azimuthResult = (GeometryEngine.distanceGeodetic(mainTrackPoints2.get(0), mainTrackPoints2.get(1), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth1() > 0) ?
                GeometryEngine.distanceGeodetic(mainTrackPoints2.get(0), mainTrackPoints2.get(1), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth1() :
                GeometryEngine.distanceGeodetic(mainTrackPoints2.get(0), mainTrackPoints2.get(1), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getAzimuth2();

        resultHeading.setText(Math.round(azimuthResult * 10000.0) / 10000.0 + " degrees");

    }

    @FXML
    private void clearAll() {
        makePolygonButton.setStyle("-fx-background-color: white");
        mapView.getCallout().dismiss();
        while (mSketchEditor.canUndo()) mSketchEditor.undo();
        mSketchEditor.stop();
        graphicsOverlay.getGraphics().removeIf(Objects::nonNull);
        polylineBuilder.getParts().clear();
        polylinePointsTemp.clear();
        polygonPoints.clear();
        clearButton.setFocusTraversable(false);
        redoButton.setFocusTraversable(false);
        sumArea = 0;
        textArea.clear();
        geometries.clear();
        pointsForAzimuth.clear();

//        loadShapeFile("/home/leonid/java-gradle-starter-project-master/src/main/resources/find_place/shapes/world-cities.shp");
    }


    private void addPolygonToMap(Polygon polygon, ArrayList<Geometry> list, String name, ArrayList<String> names) {
        SimpleFillSymbol polygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, hexBlue,
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, hexGreen, 2.0f));

        Map<String, Object> attributes1 = new HashMap<>();
        attributes1.put("name", name);
        if (list == null) {
            Graphic polygonGraphic = new Graphic(polygon, attributes1, polygonSymbol);
            graphicsOverlay.getGraphics().add(polygonGraphic);
        } else {
            int i = 0;
            for (Geometry g :
                    list) {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("name", names.get(i));
                System.out.println(names.get(i));
                i++;
                graphicsOverlay.getGraphics().add(new Graphic(g, attributes, polygonSymbol));
            }
        }
    }

    @FXML
    private void searchByCurrentViewpoint() {
//        String placeQuery = placeBox.getEditor().getText();
        String placeQuery = "1";

        GeocodeParameters geocodeParameters = new GeocodeParameters();
        geocodeParameters.getResultAttributeNames().add("*"); // return all attributes
        geocodeParameters.setOutputSpatialReference(mapView.getSpatialReference());
        geocodeParameters.setSearchArea(mapView.getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY).getTargetGeometry());

        //perform the geocode operation
        ListenableFuture<List<GeocodeResult>> geocodeTask = locatorTask.geocodeAsync(placeQuery, geocodeParameters);

        // add a listener to display the results when loaded
        geocodeTask.addDoneListener(new ResultsLoadedListener(geocodeTask));
    }

    @FXML
    private void goToField() {
        goToButton.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                getFromDB(cb.getValue().toString());
                addPolygonToMap((Polygon) geometries.get(0), null, cb.getValue().toString(), null);
                Point p = geometries.get(0).getExtent().getCenter();
                Viewpoint viewpoint = new Viewpoint(p, 10000);
                // zoom to the envelope
                mapView.setViewpointAsync(viewpoint, 3);
            }
        });
    }

    @FXML
    private void CursorStage() {

    }

    @FXML
    private void CoordinatsStage(Geometry geomLocal) {
        Stage newStage = new Stage();
        VBox comp = new VBox();
        TextField lat = new TextField("lat");
        TextField lon = new TextField("lon");
        TextField heading = new TextField("heading");

        Button ok = new Button("Get");
        Button cancel = new Button("Cancel");

        ok.setAlignment(Pos.CENTER);
        comp.getChildren().add(lat);
        comp.getChildren().add(lon);
        comp.getChildren().add(heading);
        comp.getChildren().add(ok);
        comp.getChildren().add(cancel);

        Scene stageScene = new Scene(comp, 300, 160);
        newStage.setScene(stageScene);
        newStage.show();

        cancel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                newStage.close();
            }
        });

        ok.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                addPolylineManual(Double.parseDouble(lat.getText()),Double.parseDouble(lon.getText()),Double.parseDouble(heading.getText()));
                calculateTracks(geomLocal);
                newStage.close();
            }
        });
    }

    @FXML
    private void showStage() {
        Stage newStage = new Stage();
        VBox comp = new VBox();
        TextField nameField = new TextField("Name");
        Button ok = new Button("Load");
        Button cancel = new Button("Cancel");
        Button add = new Button("Add to DB");
        Button getAll = new Button("Get all");

        ok.setAlignment(Pos.CENTER);
        comp.getChildren().add(nameField);
        comp.getChildren().add(ok);
        comp.getChildren().add(add);
        comp.getChildren().add(cancel);
        comp.getChildren().add(getAll);

        Scene stageScene = new Scene(comp, 300, 160);
        newStage.setScene(stageScene);
        newStage.show();

        cancel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                newStage.close();
            }
        });

        add.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                addToDB(nameField.getText(),null);
                newStage.close();
            }
        });
        ok.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                getFromDB(nameField.getText());
                addPolygonToMap((Polygon) geometries.get(0), null, nameField.getText(), null);
                Point p = geometries.get(0).getExtent().getCenter();
                Viewpoint viewpoint = new Viewpoint(p, 10000);
                // zoom to the envelope
                mapView.setViewpointAsync(viewpoint, 3);
                newStage.close();
            }
        });
        getAll.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                getAllFromDB();
                addPolygonToMap(null, geometries, null, names);
                newStage.close();
            }
        });
    }

    private void serializeHashMap(Map<String, String> hashMap) {
        try {
            FileOutputStream fos =
                    new FileOutputStream("/media/leonid/d1c9906b-7e4d-4be5-a677-60293515247f/leonid/java-gradle-starter-project-master/src/main/resources/Data/hashmap.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hashMap);
            oos.close();
            fos.close();
            System.out.printf("Serialized HashMap data is saved in hashmap.ser");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private HashMap deserializeHashMap() {

        HashMap map = null;
        try {
            FileInputStream fis = new FileInputStream("/media/leonid/d1c9906b-7e4d-4be5-a677-60293515247f/leonid/java-gradle-starter-project-master/src/main/resources/Data/hashmap.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (HashMap) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return map;
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
            return map;
        }
        System.out.println("Deserialized HashMap");
        // Display content using Iterator
        Set set = map.entrySet();
        for (Object o : set) {
            Map.Entry mentry = (Map.Entry) o;
            System.out.print("key: " + mentry.getKey() + " & Value: ");
            System.out.println(mentry.getValue());
        }
        return map;
    }

    private void addToDB(String name, Polygon polygon) {
        names.add(name);
        choiceBoxItems.add(name);
        for (int i = 0; i < names.size(); i++) {
            preferences.put(String.valueOf(i), names.get(i));
        }
        preferences.putInt("namesSize", names.size());

        try {
            if (polygon==null) {
                polygonsDB.put(name, (Polygon) geometries.get(geometries.size() - 1));
            }else {
                polygonsDB.put(name, (Polygon) polygon);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("problem");
        }
        JsonListDB = new LinkedHashMap<>();
        polygonsDB.keySet().forEach(e -> JsonListDB.put(e, polygonsDB.get(e).toJson()));
        System.out.println(JsonListDB.toString());
        serializeHashMap(JsonListDB);

        for (String x :
                JsonListDB.keySet()) {
            preferences.put(x, JsonListDB.get(x));
        }
    }

    public void getFromDB(String name) {
        geometries.clear();
        JsonListDB = new LinkedHashMap<>();

        JsonListDB.put(name, preferences.get(name, "null"));
        JsonListDB.keySet().forEach(e -> {
            geometries.add(Geometry.fromJson(JsonListDB.get(e)));
        });
    }

    public void removeFromDB(String name) {
        names.removeIf(e->e.equals(name));
        choiceBoxItems.removeIf(e->e.equals(name));
        for (int i = 0; i < names.size(); i++) {
            preferences.put(String.valueOf(i), names.get(i));
        }
        preferences.putInt("namesSize", names.size());
    }


    public void getAllFromDB() {
        geometries.clear();
        JsonListDB = new LinkedHashMap<>();

        //JsonListDB = (LinkedHashMap) deserializeHashMap();
        names.forEach(e -> {
            JsonListDB.put(e, preferences.get(e,"null"));
        });
        JsonListDB.keySet().forEach(e -> {
            geometries.add(Geometry.fromJson(JsonListDB.get(e)));
        });
    }


    @FXML
    private void testMethod() {

////        System.out.println(GeometryEngine.simplify(geometries.get(0)).toJson());
//        Geometry newGeometry = GeometryEngine.difference(geometries.get(0),geometries.get(1));
////        Geometry newGeometry = GeometryEngine.densify(geometries.get(0),0.5);
//        clearAll();
//        addPolygonToMap((Polygon) newGeometry);
      flag = true;
    }

    private String getGeometryArea(Geometry newGeometry) {
        double area = GeometryEngine.areaGeodetic(newGeometry, new AreaUnit(AreaUnitId.HECTARES), GeodeticCurveType.GEODESIC);
        return Math.round(area * 100.0) / 100.0 + " ha.";
    }

    public void cutOff() {
        SimpleFillSymbol polygonSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, hexBlue,
                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, hexGreen, 2.0f));
        createPolygonMethod((Polyline) mSketchEditor.getGeometry());
        Geometry newGeometry = GeometryEngine.difference(geometries.get(geometries.size() - 2), geometries.get(geometries.size() - 1));
        graphicsOverlay.getGraphics().removeIf(e2 -> e2.getGeometry().getGeometryType().name().equals("POLYGON"));
        area = GeometryEngine.areaGeodetic(newGeometry, new AreaUnit(AreaUnitId.HECTARES), GeodeticCurveType.GEODESIC);
        sumArea = sumArea + area;
        textArea.setText(String.valueOf(Math.round(area * 100.0) / 100.0 + " ha."));
        geometries.remove(geometries.get(geometries.size() - 1));
        geometries.remove(geometries.get(geometries.size() - 1));

        geometries.add(newGeometry);

        ArrayList<Graphic> graphics = new ArrayList<>();
        for (Geometry g :
                geometries) {
            graphics.add(new Graphic(g, polygonSymbol));
        }
        graphicsOverlay.getGraphics().addAll(graphics);
    }

    public void undoMethod() { //TODO
        mSketchEditor.undo();

        if (polylinePointsTemp.size() > 0) {
            polygonPoints.clear();
            trackLines.clear();

            polylinePointsTemp.remove(polylinePointsTemp.size() - 1);
        }
        System.out.println(graphicsOverlay.getGraphics().size());
        if (graphicsOverlay.getGraphics().size() > 1) {
            graphicsOverlay.getGraphics().remove(graphicsOverlay.getGraphics().size() - 1);
            graphicsOverlay.getGraphics().remove(graphicsOverlay.getGraphics().size() - 1);
        }
        System.out.println(graphicsOverlay.getGraphics().size());
    }

    public void testMethod2() throws BackingStoreException {
        preferences.clear();
        names.clear();
    }

    public Polyline addTrackLines(Polyline topPolyline) {
        Polyline p = trackLines.get(0);
        Polyline g = GeometryEngine.extend(p, topPolyline, ExtendOptions.DEFAULT);

        if (!g.isEmpty()) {
//            Alert a = new Alert(Alert.AlertType.NONE);
//            a.setAlertType(Alert.AlertType.CONFIRMATION);
//            a.show();
         //   addPolylineGraphic(g,hexYellow,null);
            trackLines.remove(0);
            trackLines.add(g);
            return g;
        }
        return null;
    }


    /**
     * A listener to update a {@link ComboBox} when suggestions from a call to
     * {@link LocatorTask#suggestAsync(String, SuggestParameters)} are loaded.
     */
    private class SuggestionsLoadedListener implements Runnable {

        private final ListenableFuture<List<SuggestResult>> results;
        private final ComboBox<String> comboBox;

        /**
         * Constructs a listener to update an auto-complete list for geocode
         * suggestions.
         *
         * @param results suggestion results from a {@link LocatorTask}
         * @param box     the {@link ComboBox} to update with the suggestions
         */
        SuggestionsLoadedListener(ListenableFuture<List<SuggestResult>> results, ComboBox<String> box) {
            this.results = results;
            this.comboBox = box;
        }

        @Override
        public void run() {

            try {
                List<SuggestResult> suggestResult = results.get();
                List<String> suggestions = suggestResult.stream().map(SuggestResult::getLabel).collect(Collectors.toList());

                // update the combo box with suggestions
                Platform.runLater(() -> {
                    comboBox.getItems().clear();
                    comboBox.getItems().addAll(suggestions);
                    comboBox.show();
                });

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runnable listener to update marker and callout when new results are loaded.
     */
    private class ResultsLoadedListener implements Runnable {

        private final ListenableFuture<List<GeocodeResult>> results;

        /**
         * Constructs a runnable listener for the geocode results.
         *
         * @param results results from a {@link LocatorTask#geocodeAsync} task
         */
        ResultsLoadedListener(ListenableFuture<List<GeocodeResult>> results) {
            this.results = results;
        }

        @Override
        public void run() {

            // hide callout if showing
            mapView.getCallout().dismiss();


            List<Graphic> markers = new ArrayList<>();
            try {
                List<GeocodeResult> geocodes = results.get();
                for (GeocodeResult geocode : geocodes) {

                    // get attributes from the result for the callout
                    String addrType = geocode.getAttributes().get("Addr_type").toString();
                    String placeName = geocode.getAttributes().get("PlaceName").toString();
                    String placeAddr = geocode.getAttributes().get("Place_addr").toString();
                    String matchAddr = geocode.getAttributes().get("Match_addr").toString();
                    String locType = geocode.getAttributes().get("Type").toString();

                    // format callout details
                    String title;
                    String detail;
                    switch (addrType) {
                        case "POI":
                            title = placeName.equals("") ? "" : placeName;
                            if (!placeAddr.equals("")) {
                                detail = placeAddr;
                            } else if (!matchAddr.equals("") && !locType.equals("")) {
                                detail = !matchAddr.contains(",") ? locType : matchAddr.substring(matchAddr.indexOf(", ") + 2);
                            } else {
                                detail = "";
                            }
                            break;
                        case "StreetName":
                        case "PointAddress":
                        case "Postal":
                            if (matchAddr.contains(",")) {
                                title = matchAddr.equals("") ? "" : matchAddr.split(",")[0];
                                detail = matchAddr.equals("") ? "" : matchAddr.substring(matchAddr.indexOf(", ") + 2);
                                break;
                            }
                        default:
                            title = "";
                            detail = matchAddr.equals("") ? "" : matchAddr;
                            break;
                    }

                    HashMap<String, Object> attributes = new HashMap<>();
                    attributes.put("title", title);
                    attributes.put("detail", detail);

                    // create the marker
                    Graphic marker = new Graphic(geocode.getDisplayLocation(), attributes, pinSymbol);
                    markers.add(marker);
                }

                // update the markers
                if (markers.size() > 0) {
                    Platform.runLater(() -> {
                        // clear out previous results
                        graphicsOverlay.getGraphics().clear();
                        //placeBox.hide();

                        // add the markers to the graphics overlay
                        graphicsOverlay.getGraphics().addAll(markers);

                        //reset redo search button
                        redoButton.setDisable(true);

                        // listener to enable the redo-search button the first time the user moves away from the initial search area
                        ViewpointChangedListener changedListener = new ViewpointChangedListener() {

                            @Override
                            public void viewpointChanged(ViewpointChangedEvent arg0) {

                                redoButton.setDisable(false);
                                mapView.removeViewpointChangedListener(this);
                            }
                        };

                        mapView.addViewpointChangedListener(changedListener);
                    });
                }


            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the animation and disposes of application resources.
     */
    void terminate() {

        if (mapView != null) {
            mapView.dispose();
        }
    }

}
