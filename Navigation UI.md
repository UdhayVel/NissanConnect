
[<img src="https://about.mappls.com/images/mappls-b-logo.svg" height="60"/> </p>](https://www.mapmyindia.com/api)

# [Mappls Navigation UI Widget](mappls-navigation-widget)


**Mappls Navigation UI Widget** allows apps to add or improve their experience by integrating Mappls Maps turn-by-turn navigation into their Map app. The highly customizable SDK allows you to adjust the user interface with their own look and feel, custom controls, and route information.

It offers ready to use navigation view to show Turn by Turn information after starting the Navigation featuring:

- Drop-In Navigation UI
- Choose between faster or shorter route
- Turn-By-Turn Voice Guidance
- Automatic Rerouting
- Navigation Map With Real Time Traffic
- Road and Safety Events
- Lane Assistance & Junction Views
- Road Safety Warnings
- Multiple Modes Of Travel - Driving , Biking
- Discover Places Along Your Route
<br>

**Preview Images:**

<img src="https://about.mappls.com/api/api_doc_assets/2.jpeg" height="500"/> | <img src="https://about.mappls.com/api/api_doc_assets/3.jpeg" height="500"/> |
<img src="https://about.mappls.com/api/api_doc_assets/1.jpeg" height="500"/>|
<img src="https://about.mappls.com/api/api_doc_assets/4.jpeg" height="500"/> |


This documentation below describes how to implement it in simple steps!


## [Setup your project](setup-your-project)
**For older Build versions (i.e, Before gradle v7.0.0)**

-   Add Mappls private repository in your project level  `build.gradle`
~~~groovy
allprojects {  
    repositories {  
  
        maven {  
            url 'https://maven.mappls.com/repository/mappls/'  
        }  
        maven{  
		  url "https://maven.mappls.com/repository/mappls-private/"  
		  authentication {  
			  basic(BasicAuthentication)  
		  }  
		  credentials {  
			  username = "<ENTER_USER_NAME>"  
			  password = "<ENTER_PASSWORD>"  
		  }  
	   }
    }  
}
~~~
**For Newer Build Versions (i.e, After gradle v7.0.0)**

-   Add Mappls repository in your  `settings.gradle`

~~~groovy
dependencyResolutionManagement {  
//   repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) 
  repositories {  
        google()  
        mavenCentral()  
        maven {  
            url 'https://maven.mappls.com/repository/mappls/'  
        } 
        maven{  
		  url "https://maven.mappls.com/repository/mappls-private/"  
		  authentication {  
			  basic(BasicAuthentication)  
		  }  
		  credentials {  
			  username = "<ENTER_USER_NAME>"  
			  password = "<ENTER_PASSWORD>"  
		  }  
	   } 
    }  
   }  
}
~~~

-   Add below dependency in your app-level  `build.gradle`
~~~groovy
implementation 'com.mappls.sdk:navigation-ui:1.2.1'
~~~

### Initialise Navigation View
Initialise Navigation View in your Application class 
~~~java
MapplsNavigationViewHelper.getInstance().init(this);
~~~

To  set start Location:
~~~java
MapplsNavigationViewHelper.getInstance().setStartLocation(startNavLocation);
~~~

To set destination location:
~~~java
MapplsNavigationViewHelper.getInstance().setDestination(eLocation);
~~~
To set waypoint list:
~~~java
MapplsNavigationViewHelper.getInstance().setWayPoints(waypoints);
~~~

## Add Navigation View to your application
You can add Navigation view in two ways

1.  Using XML
2.  Using Java

### Using XML
~~~xml
<com.mappls.sdk.navigation.ui.navigation.NavigationView  
  android:id="@+id/navigation_view"  
  android:layout_width="match_parent"  
  android:layout_height="match_parent" />
~~~

We can set the following properties:
1. **navigationLightTheme**: To set the day theme styles
2. **navigationDarkTheme**: To set the night theme styles
3. **showSearchDuringNavigationOption**: Show/Hide search during navigation
4. **isUsingInternalMap**: To add default map
5. **mapplsMapLightStyle**: To set style of map for day mode
6. **mapplsMapDarkStyle**: To set style of map for night mode
7. **showDayNightOption**: To show/hide day night toggle option
8. **showSettingsOption**: To show/hide setting option
9. **showTrafficOption**: To show hide traffic toggle option
10. **navigationTheme**: To set the navigation theme. **Below are the possible values:**
	- `standard`
	- `day`
	- `night`
11. **showNextInstructionBanner**: To show hide next instruction banner
12. **showCurrentSpeed**: To show hide Current speed

### Using Java
~~~java
//To create with default UI and styles
NavigationView navigationView = new NavigationView(requireContext());

					//OR
//To create with custom settings
NavigationView navigationView = new NavigationView(requireContext(), NavigationOptions.builder().isUsingInternalMap(false).build());
~~~
`NavigationOptions` has following methods to change the properties:
1. `navigationLightTheme(@StyleRes Integer)`: To set the day theme styles
2. `navigationDarkTheme(@StyleRes Integer)`: To set the night theme styles
3. `showSearchDuringNavigationOption(Boolean)`: Show/Hide search during navigation
4. `isUsingInternalMap(Boolean)`: To add default map
5. `mapplsMapLightStyle(String)`: To set style of map for day mode
6. `mapplsMapDarkStyle(Sring)`: To set style of map for night mode
7. `showDayNightOption(Boolean)`: To show/hide day night toggle option
8. `showNavigationSettingsOption(Boolean)`: To show/hide setting option
9. `showTrafficOption(Boolean)`: To show hide traffic toggle option
10. `navigationTheme(Integer)`: To set the navigation theme. **Below are the possible values:**
	- `NavigationOptions.THEME_DEFAULT`
	- `NavigationOptions.THEME_DAY`
	- `NavigationOptions.THEME_NIGHT`
11. `showNextInstructionBanner(Boolean)`: To show hide next instruction banner
12. `showCurrentSpeed(Boolean)`: To show hide Current speed

### Initialise Navigation View
~~~java
@Override  
  public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
		setContentView(R.layout.activity_navigation_new);  
		navigationView = findViewById(R.id.navigation_view);  

	   navigationView.onCreate(savedInstanceState);  
       navigationView.setNavigationViewCallback(this);  
       navigationView.setOnNavigationCallback(this);  
       getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {  
            @Override  
            public void handleOnBackPressed() {  
                if(navigationView != null) {  
                    navigationView.onBackPressed(true);  
                }  
            }  
        });  
  
  }  
  
    @Override  
  public void onLowMemory() {  
        super.onLowMemory();  
  navigationView.onLowMemory();  
  }  
  
    @Override  
  protected void onResume() {  
        super.onResume();  
  navigationView.onResume();  
  }  
  
    @Override  
  public void onStart() {  
        super.onStart();  
  navigationView.onStart();  
  }  
  
    @Override  
  public void onStop() {  
        super.onStop();  
  navigationView.onStop();  
  }  
  
    @Override  
  public void onDestroy() {  
        super.onDestroy();  
  navigationView.onDestroy();  //For Fragment call this in onDestroyView()
  }
~~~

#### Navigation View Callback getting from Navigation View
Implement from `NavigationViewCallback` :
1. `onNavigationMapReady(MapplsMap)`: When Map is loaded successfully
2. `searchAlongRoute()`: When user click on search icon

#### To set the custom MapView
If you don't want to add default MapView:
~~~java
navigationView.setMapView(mapView);
~~~

#### Handle Back Press
It's required to handle the back press:
~~~java
getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {  
    @Override  
  public void handleOnBackPressed() {  
        navigationView.onBackPressed(true);  
    }  
});
~~~



<br><br><br>

For more details, please contact [apisupport@mappls.com](mailto:apisupport@mappls.com).
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

