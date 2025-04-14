[<img src="https://about.mappls.com/images/mappls-b-logo.svg" height="80"/> </p>](https://www.mapmyindia.com/api)

# [Mappls Navigation Module for Android](Mappls-Navigation-Module-for-Android)

# [eManual for Navigation Module](eManual-for-Navigation-Module)

## [Getting Started](Getting-Started)

This guide is a quick start to adding **Navigation Module** to an Android app already utilizing Mappls Vector Maps SDK for Android. [Android Studio](https://developer.android.com/studio/index.html) is the recommended development environment for building an app with the Mappls Vector Maps SDK for Android.

### [Download Android Studio](Download-Android-Studio)

Follow the [guides](https://developer.android.com/studio/index.html) to download and [install](https://developer.android.com/studio/install.html?pkg=studio) Android Studio.

### [Installing the Navigation Module of Mappls Vector Maps SDK](Installing-the-Navigation-Module-of-Mappls-Vector-Maps-SDK)

#### 1. [Add Mappls Vector Maps SDK](Add-Mappls-Vector-Maps-SDK)

Follow these steps to add the SDK to your project –

- Add Mappls repository in your project level `build.gradle`

```groovy
 allprojects {
    repositories {
        maven {
            url 'https://maven.mappls.com/repository/mappls/'
        }
        maven {
            url "https://maven.mappls.com/repository/mappls-private/"
            authentication {
                basic(BasicAuthentication)
            }
            credentials {
                username = "<MAVEN_USER_NAME>"
                password = "<MAVEN_PASSWORD>"
            }
        }
    }
}
```

- Add below dependency in your app-level `build.gradle`

```groovy
implementation 'com.mappls.sdk:mappls-android-sdk:8.2.2'
implementation 'com.mappls.sdk:navigation-sdk:0.14.1'
```


#### 2. [Run gradle sync](Run-gradle-sync)

## [Importing the Mappls Maps Navigation Sample project](Importing-the-Mappls-Maps-Navigation-Sample-project)

Follow these steps to create a new app project including a map activity:

1. Start Android Studio.
2. Clone / Download Sample Projects from the URL shared and open it in Android Studio via **Open an existing Android Studio Project**.

Android Studio starts Gradle and builds your project. This may take a few seconds. For more information about creating a project in Android Studio, see the [Android Studio documentation](https://developer.android.com/studio/projects/create-project.html).

The next section describes getting the API keys in more detail.

## [Getting Mappls Maps API keys](Getting-Mappls-Maps-API-keys)

Your application needs API keys to access the Mappls Maps servers. The key is free. You can use it with any of your applications that call the Mappls Navigation for Android.

Your Navigation module usage needs a set of license keys (get them [here](http://www.mappls.com/api/signup)) and is governed by the API [terms and conditions](http://www.mappls.com/api/terms-&-conditions). As part of the terms and conditions, **you cannot remove or hide the Mappls logo and copyright information** in your project.

The allowed SDK hits are described on the user dashboard ([http://www.mappls.com/api/dashboard](http://www.mappls.com/api/dashboard)) page. Note that your usage is shared between platforms if you use the same keys across apps of different platforms(e.g. the API hits you make from a web application, Android app or an iOS app all add up to your allowed daily limit; if they share the API keys).

You must have at least one API key associated with your project.

To get an API key:

1.  Go to the [Mappls API Dashboard](http://www.mappls.com/api/dashboard).
2.  Login using your Mappls login credentials.
3.  Once logged in, you will see the API keys associated with your account at the right of the page. These are useful for accessing our RESTful APIs.

## [Adding Mappls Maps API key to the sample project](Adding-Mappls-Maps-API-key-to-the-sample-project)

1.  Edit the project's `activity/applications` file.
2.  Edit and copy the following code snippet within the activity's java file at the point which lies before any calls to Mappls Navigation in Vector Maps SDK are made:

```java
MapplsAccountManager.getInstance().setRestAPIKey(getRestAPIKey());
MapplsAccountManager.getInstance().setMapSDKKey(getMapSDKKey());
MapplsAccountManager.getInstance().setAtlasClientId(getAtlasClientId());
MapplsAccountManager.getInstance().setAtlasClientSecret(getAtlasClientSecret());
Mappls.getInstance(getApplicationContext());
```

_You cannot use the Mappls Navigation in Vector Map SDK without this function call_.


## [Usage Steps for Navigation Module](Usage-Steps-for-Navigation-Module)

- Add below code in your Application class:
```java
    @Override
    public void onCreate() {
        super.onCreate();
        NavigationContext.init(this); //To initialise Navigation SDK
        MapplsNavigationHelper.getInstance().init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        NavigationContext.terminate();//To de-initialise Navigation SDK
    }   
```

- We are using `retrofit` call for our REST APIs, so developer can use it as follows:

```java
MapplsDirections.Builder builder = MapplsDirections.builder()
     .origin(origin)
     .destination(destination)
     .profile(DirectionsCriteria.PROFILE_DRIVING)
     .resource(DirectionsCriteria.RESOURCE_ROUTE_ETA)
     .steps(true)
     .alternatives(true)
     .annotations(DirectionsCriteria.ANNOTATION_CONGESTION, DirectionsCriteria.ANNOTATION_NODES, DirectionsCriteria.ANNOTATION_DURATION)
     .routeRefresh(true)
     .deviceId(Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID))
     .overview(DirectionsCriteria.OVERVIEW_FULL);

MapplsDirections mapplsDirections = builder.build();

MapplsDirectionManager.newInstance(mapplsDirections).call(new OnResponseCallback<DirectionsResponse>() {
    @Override
public void onSuccess(DirectionsResponse directionsResponse) {
        //Handle Response
 }

    @Override
public void onError(int i, String s) {
        //Handle Error
  }
});
```

- Route API will provide distance (total), time taken (total) and steps to reach destination.
- We have created a Plugin to draw route and waypoints on the map `DirectionPolylinePlugin`.
  One needs to initialize it in `onMapReady` callback:
  ```java
  DirectionPolylinePlugin directionPolylinePlugin = new DirectionPolylinePlugin(mapView, mapplsMap);
  ```
  To plot polyline and point one needs to call,
  ```java
  public void setTrips(List<LineString> trips, LatLng startLatLng, LatLng endLatLng, ArrayList<LatLng> wayPoints, List<DirectionsRoute> directionsRouteList)
  ```
  of DirectionPolylinePlugin class.
- If you are starting navigation from foreground/ui thread, use:

  ```java
  public void startNavigation(final DirectionsResponse directionsResponse, final int selectedIndex, final LatLng currentLocation, final WayPoint endLatLng, final List<WayPoint> wayPoints, @Nullable final String clusterId, @Nullable final OnAuthentication onAuthentication)
  ```

  of `MapplsNavigationHelper` class.

- If you are starting navigation from background thread, use:
  ```java
  public NavigationResponse startNavigation(DirectionsResponse trip, int selectedIndex, LatLng currentLocation, WayPoint destination, List<WayPoint> wayPoints, String clusterId)
  ```
  of `MapplsNavigationHelper` class.
  If `navigationResponse` or `navigationResponse.getError()` is null (i.e., `navigationResponse == null || navigationResponse.getError() == null`) then it means request is successfully authenticated and navigation has started so you can open Navigation UI.
  If navigationResponse and navigationResponse.getError() is not null (i.e., navigationResponse!= null && navigationResponse.getError() !=null) then it means you cannot start navigation

\***\*NOTE:\*\*** 1. If we want to restrict navigation to whitelisted devices than pass device Id in `clusterId` otherwise pass `null`. if we are passing device Id in `clusterId` and `navigationResponse.getError().errorCode == 409` than it means there is an active session on provided deviceId, so we need to call deleteSession() api for this device:

```java
MapplsNavigationHelper.getInstance().deleteSession(deviceId, new IStopSession() {
   @Override
public  void onSuccess() {
      Timber.d("session deleted");
   }

   @Override
public  void onFailure() {
      Timber.d("session deletion failed");
   }
});
```

2. If we want to set deviceAlias then we can set using below method:

```java
MapplsAccountManager.getInstance().setClusterId("INSERT_CLUSTER_ID", "INSERT_DEVICE_ALIAS");
```

**Glossary**:
`route` : route you got from directions API
`currentLocation`: your current location destination: destination location.

- Show user navigation screen and updated UI based on callbacks from navigation engine You can register to callbacks using:
  ```java
  MapplsNavigationHelper.getInstance().addNavigationListener(iNavigationListener);
  ```
- In order to stop listening you need to call:
  ```java
  MapplsNavigationHelper.getInstance().removeNavigationListener(iNavigationListener);
  ```
- To stop navigation, you can call
  ```java
  MapplsNavigationHelper.getInstance().stopNavigation();
  ```
- To get the information of GPS fixed and satellite information from SDK:
~~~java
  MapplsNavigationHelper.getInstance().addLocationChangeListener(locationChangedListener);
~~~
- To stop listening of GPS status callback:
~~~java
MapplsNavigationHelper.getInstance().removeLocationChangeListener(locationChangedListener);
~~~


### [Callback Methods Available](Callback-Methods-Available)

In `OnAuthentication` there are two callback methods :

1. **Has navigation successfully started**:
   `onSuccess()`: if device is successfully authenticated and navigation stated, you can show user navigation screen.
2. **Has navigation failed to start**:
   `onFailure()`: in case there is an error or user is not allowed for navigation.

In `INavigationListener`, following callback methods are available:

1. **When Navigation Started**:
   This willl be called when the navigation starts

   ```java
   void onNavigationStarted();
   ```

2. **On New Route Calculation**:
   This will be called whenever new route is calculated.
   `@param geometry` is the list of locations which forms route polyline
   ```java
   void onNewRoute(String geometry);
   ```
3. **On Cancellation of Navigation**:
   In case ongoing route is cancelled, such as by using`stopNavigation`
   ```java
   void onNavigationCancelled();
   ```
4. **When Navigation finishes**:
   This will be called when navigation finishes i.e., when the user reaches the destination.
   ```java
   void onNavigationFinished();
   ```
5. **On reaching way point**:
   This will be called when user reaches way-point.
   ```java
   void onWayPointReached(String waypointName);
   ```
6. **When Advice info is updated**:
   This will be called at each location change,
   It gives you all info you need to update your UI.
   `@param adviseInfo` where `adviseInfo` contains ETA, distance left, `isRouteBeingCalculated`... etc.
   ```java
   void onRouteProgress(AdviseInfo adviseInfo);
   ```

Content of AdviseInfo Object1

- `getText()`_[String]_: It gives the complete text for the next turn info with road name (if available)
- `getShortText()`_[String]_: It gives only next turn Information
- `getDistanceToNextAdvise()`_[Integer]_: It gives distance to next turn (in metre)
- `getLeftDistance()`_[Integer]_: It gives distance left to the destination (in. metre)
- `getEta()`_[String]_: It gives the Estimated Arrival time
- `isRouteBeingRecalculated()`_[Boolean]_: It gives the flag for rerouting request
- `isOnRoute()`_[Boolean]_: It gives the flag that currently user is on route or not
- `getLeftTime()`_[Integer]_: It gives the time left to the destination (in seconds)
- `getLocation()`_[Location]_: User current location

In `LocationChangedListener`, following callback methods are available:
1. **On Every location Change**
This callback will be called for locatioon change
```java
   void onLocationChanged(Location location)
```

2. **On GPS connection Change**
This callback will be called on GPS lost
```java
    void onGPSConnectionChanged(boolean gpsRestored);
```

3. **On Satellite Info Change**
This callback will be called when there is change in Satellite information
```java
    void onSatelliteInfoChanged(GPSInfo gpsInfo);
```

Content of GPSInfo Object
- `foundSatellites` _[Integer]_: It gives the number of satellite found
- `usedSatellites` _[Integer]_: It gives the number of satellite that is used
- `fixed` _[Boolean]_: Check if GPs is fixed or not

### [Support for summarized Turn-By-Turn info.](Support-for-summarized-Turn-By-Turn-info)

1.  **To get the short text instruction for manoeuvres**:
    This needs to be called for getting short instructions from the SDK.

```java
adviseInfo.getShortText();
```

2.  **To get the numerical ID for manoeuvres**:
    This needs to be called for getting numerical ID for manoeuvres from the SDK.

```java
adviseInfo.getManeuverID();
```

### [Navigation Summary](navigation-summary)

To get the Summary for Navigation:

```java
NavigationSummary navigationSummary = MapplsNavigationHelper.getInstance().getNavigationSummary();
navigationSummary.getTotalDistance(); //Get Total Travelled distance in metre
navigationSummary.getAverageSpeed(); //Get Average Speed in m/s
navigationSummary.getTotalTimeTaken(); //Get Total time taken in seconds
```

### [Traffic/Safety events in Mappls Navigation SDK](traffic-safety-events-in-mappls-navigation-sdk)

#### [1. Option to enable/disable](option-to-enable-disable)

```java
MapplsNavigationHelper.getInstance().setNavigationEventEnabled(true);
```

Its default value is set to false.

#### [2. Get list of navigation events when navigation is active](get-list-of-navigation-events-when-navigation-is-active)

```java
List<NavEvent> list = MapplsNavigationHelper.getInstance().getEvents();
```

There is also a callback which inform the user if events on the route changed during navigation.

```java
MapplsNavigationHelper.getInstance().setNavigationEventLoadedListener(new NavigationEventLoadedListener() {
   @Override
public void onNavigationEventsLoaded(List<ReportDetails> list) {

   }
 });
```

#### [3. Callback method to inform a user about upcoming navigation event.](callback-method-to-inform-a-user-about-upcoming-navigation-event)

```java
 MapplsNavigationHelper.getInstance().setNavigationEventListener(new NavigationEventListener() {
    @Override
 public void onNavigationEvent(NavEvent navEvent) {
       if (navEvent != null) {
         //show navigation event related UI
 } else {
         //Hide if you are showing any navigation event related UI
 }
    }
});
```

#### [4. Option to configure distance. Before how many meters a user will start getting a navigation event in callback. default will be 200 and a user can not set it less than 50.](option-to-configure-visual-prompt-distance)

```java
MapplsNavigationHelper.getInstance().setEventVisualPromptBefore(200);
```

#### [5. Option to configure distance. Before how many meters a user will get navigation event audio prompt. default will be 200 and a user can not set it less than 50.](option-to-configure-audio-prompt-distance)

```java
MapplsNavigationHelper.getInstance().setEventAudioPromptBefore(200);
```

**NavEvent** class has following data members

1.  `getName()`[String]: name of the event e.g. speed breaker latitude: latitude of the event
2.  `getLongitude()`[Double]: longitude of the event
3.  `getLatitude()`[Double]: latitude of the event
4.  `getDistanceLeft()`[Double]: distance left(use this distance when receive event in onNavigationEvent callback method)
5.  `getReportDetails()`[ReportDetails]: Provides the current Events

**ReportDetails** class has following data members:

1.  `getId()`[String]: Id of the event
2.  `getReportIcon(String pixel)`[String]: It provides the icon url for provided pixel. Possible values are:

- ReportCriteria.ICON_24_PX
- ReportCriteria.ICON_36_PX
- ReportCriteria.ICON_48_PX
- ReportCriteria.ICON_56_PX

3.  `getLongitude()`[Double]: longitude of the event
4.  `getLatitude()`[Double]: latitude of the event

#### [6. Option to show events on map](option--show-events)

We have created `MapEventsPlugin`to show event marker on map

- To Initialise plugin:
  ```java
  mapEventsPlugin = new MapEventsPlugin(mapView, mapplsMap);
  ```
- To show events on Map:
  ```java
  mapEventsPlugin.setNavigationEvents(MapplsNavigationHelper.getInstance().getEvents());
  ```

#### [7. Option to change Events Voice Settings](events-voice-settings)

- To enable/disable the Navigation Events Voice
  ```java
  MapplsNavigationHelper.getInstance().setNavigationEventAudioPromptEnabled(true);//True means enable and false means disable
  ```
- To enable/disable the Navigation Road Conditions Events Voice
  ```java
  MapplsNavigationHelper.getInstance().speakRoadConditionsEvents(true);//True means enable and false means disable
  ```
- To enable/disable the Navigation Safety Events Voice
  ```java
  MapplsNavigationHelper.getInstance().speakSafetyEvents(true);//True means enable and false means disable
  ```
- To enable/disable the Navigation Traffic Events Voice
  ```java
  MapplsNavigationHelper.getInstance().speakTrafficEvents(true);//True means enable and false means disable
  ```

#### [8. Options to change Navigation Events Settings](navigation-events-settings)

- To enable/disable the Navigation Road Conditions Events
  ```java
  MapplsNavigationHelper.getInstance().showRoadConditionsEvents(true);//True means enable and false means disable
  ```
- To enable/disable the Navigation Safety Events
  ```java
  MapplsNavigationHelper.getInstance().showSafetyEvents(true);//True means enable and false means disable
  ```
- To enable/disable the Navigation Traffic Events
  ```java
  MapplsNavigationHelper.getInstance().showTrafficEvents(true);//True means enable and false means disable
  ```

## [JunctionView in Mappls Navigation SDK](junction-view-in-mappls-navigation-sdk)

##### [1. option to enable/disable](option-to-enable-disable)

```java
 MapplsNavigationHelper.getInstance().setJunctionViewEnabled(true);
```

Its default value is set to false.

##### [2. Set junction view image size](set-junction-view-image-size)

By default junction view image size is "280X200". You can change it by using following function and passing width and height of the image.

```java
MapplsNavigationHelper.getInstance().setJunctionViewImageSize(280, 200);
```

##### [3. Set junction view mode](set-junction-view-mode)

```java
MapplsNavigationHelper.getInstance().setJunctionViewMode("night");
```

default value is day.

##### [4. Get list of junctions when navigation is active](get-list-of-junctions-when-navigation-is-active)

```java
 List<Junction> junctionViews = MapplsNavigationHelper.getInstance().getJunctionViews();
```

There is also a callback which inform the user if junctionView on the route changed during navigation.

```java
MapplsNavigationHelper.getInstance().setJunctionViewsLoadedListener(new JunctionViewsLoadedListener() {
@Override
public void onJunctionViewsLoaded(List<Junction> junctions) {
      //junctions contains list of junctions
 } });
```

##### [3. Callback method to inform a user about upcoming junction information.](callback-method-to-inform-a-user-about-upcoming-junction-information)

```java
 MapplsNavigationHelper.getInstance().setJunctionInfoChangedListener(new JunctionInfoChangedListener() {
@Override
public void junctionInfoChanged(Junction junction) {
      // junction is upcoming junction, null if there is no junction ahead
 } });
```

##### [4. Option to configure distance. Before how many meters a user will start getting a junction in callback. default will be 200 and a user can not set it less than 50.](option-to-configure-distance)

```java
 MapplsNavigationHelper.getInstance().setJunctionVisualPromptBefore(200);
```

Junction class has following data members

```java
public String id;
public String image;
public double longitude;
public double latitude;
public Bitmap bitmap;
```
### [Congestion delay events in Mappls Navigation SDK](congestion-events-in-mappls-navigation-sdk)

#### [To Enable/Disable Congestion Delay event](to-enable-congestion-delay-event)
~~~java
MapplsNavigationHelper.getInstance().setCongestionDelayEventEnabled();
~~~

#### [Get list of Congestion delays when navigation is active]()
~~~java

        MapplsNavigationHelper.getInstance().setCongestionInfoLoadedListener(new CongestionInfoLoadedListener() {
            @Override
            public void onCongestionInfoLoaded(List<CongestionInfo> list) {
                
            }
        });
~~~

#### [Callback method to inform a user about enter in Congestion information.]()
~~~java
CongestionInfoChangedListener callback = new CongestionInfoChangedListener() {
            @Override
            public void onCongestionInfoChanged(CongestionInfo congestionInfo) {
                
            }
};
MapplsNavigationHelper.getInstance().addCongestionInfoChangedListener(callback);//To add listener 
MapplsNavigationHelper.getInstance().removeCongestionInfoChangedListener(callback); //To remove listener
~~~

Congestion delay class has following data members:
~~~java
private double delayInfo;
private Point startPoint;
private Point endPoint;
private List<Point> congestionLocationPoints;
~~~

## [Connecting an Android device to demonstrate navigation](Connecting-an-Android-device-to-demonstrate-navigation)

The simplest way to see your app in action is to connect an Android device to your computer. Follow the [instructions](https://developer.android.com/studio/run/device.html) to enable developer options on your Android device and configure your application and system to detect the device.

Alternatively, you can use the Android Emulator to run your app. Use the [Android Virtual Device (AVD) Manager](https://developer.android.com/studio/run/managing-avds.html) to configure one or more virtual devices which you'll be able to use with the Android Emulator when you build and run your app. When choosing your emulator, ensure that you use Android 4.2.2 or higher, and be careful to pick an image that includes the Mappls APIs, or the application will not have the requisite runtime APIs in order to execute. Also, take note of the instructions for [configuring virtual machine acceleration](https://developer.android.com/studio/run/emulator-acceleration.html), which you should use with an **x86 target AVD** as described in the instructions. This will improve your experience with the emulator.

## [Build and run the sample app](build-and-run-the-sample-app)

In Android Studio, click the **Run** menu option (or the play button icon) to run your app.

When prompted to choose a device, choose one of the following options:

- Select the Android device that's connected to your computer.
- Alternatively, select the **Launch emulator** radio button and choose the virtual device that you've previously configured.

Click **OK**. Android Studio will invoke Gradle to build your app, and then display the results on the device or on the emulator. It could take a couple of minutes before the app opens.

## [Next steps](next-steps)

You may wish to look in detail into the [sample code](mailto:apisupport@mappls.com).

For any queries and support, please contact:

[<img src="https://about.mappls.com/images/mappls-logo.svg" height="40"/> </p>](https://about.mappls.com/api/)
Email us at [apisupport@mappls.com](mailto:apisupport@mappls.com)

![](https://www.mapmyindia.com/api/img/icons/support.png)
[Support](https://about.mappls.com/contact/)
Need support? contact us!

<br></br>
<br></br>

[<p align="center"> <img src="https://www.mapmyindia.com/api/img/icons/stack-overflow.png"/> ](https://stackoverflow.com/questions/tagged/mappls-api)[![](https://www.mapmyindia.com/api/img/icons/blog.png)](https://about.mappls.com/blog/)[![](https://www.mapmyindia.com/api/img/icons/gethub.png)](https://github.com/Mappls-api)[<img src="https://mmi-api-team.s3.ap-south-1.amazonaws.com/API-Team/npm-logo.one-third%5B1%5D.png" height="40"/> </p>](https://www.npmjs.com/org/mapmyindia)

[<p align="center"> <img src="https://www.mapmyindia.com/june-newsletter/icon4.png"/> ](https://www.facebook.com/Mapplsofficial)[![](https://www.mapmyindia.com/june-newsletter/icon2.png)](https://twitter.com/mappls)[![](https://www.mapmyindia.com/newsletter/2017/aug/llinkedin.png)](https://www.linkedin.com/company/mappls/)[![](https://www.mapmyindia.com/june-newsletter/icon3.png)](https://www.youtube.com/channel/UCAWvWsh-dZLLeUU7_J9HiOA)

<div align="center">@ Copyright 2022 CE Info Systems Ltd. All Rights Reserved.</div>

<div align="center"> <a href="https://about.mappls.com/api/terms-&-conditions">Terms & Conditions</a> | <a href="https://about.mappls.com/about/privacy-policy">Privacy Policy</a> | <a href="https://about.mappls.com/pdf/mapmyIndia-sustainability-policy-healt-labour-rules-supplir-sustainability.pdf">Supplier Sustainability Policy</a> | <a href="https://about.mappls.com/pdf/Health-Safety-Management.pdf">Health & Safety Policy</a> | <a href="https://about.mappls.com/pdf/Environment-Sustainability-Policy-CSR-Report.pdf">Environmental Policy & CSR Report</a>

<div align="center">Customer Care: +91-9999333223</div>
