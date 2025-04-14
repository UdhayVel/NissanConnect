[<img src="https://about.mappls.com/images/mappls-b-logo.svg" height="60"/> </p>](https://www.mapmyindia.com/api)

# Advance Navigation UI Widget Controls
To change the colors and icons in navigation view.

**For Override styles for Light mode**
~~~xml
<style name="CustomNavigationLightView" parent="NavigationViewLight">  
	 <item name="navigationViewPrimary">@color/colorPrimary</item>  
</style>
~~~
**For Override styles for Dark mode**
~~~xml
<style name="CustomNavigationViewDark" parent="NavigationViewDark">  
	 <item name="navigationViewPrimary">@color/colorPrimary</item>  
</style>
~~~
To set these styling :
~~~xml
<com.mappls.sdk.navigation.ui.navigation.NavigationView  
  android:id="@+id/navigation_view"  
  android:layout_width="match_parent"  
  android:layout_height="match_parent"  
  app:navigationLightTheme="@style/CustomNavigationView"  
  app:navigationDarkTheme="@style/CustomNavigationViewDark" />
~~~

#### Attributes for Navigation View Styles
1. **navigationViewPrimary**: To set the background color of Direction List, Setting screen, and bottom info panel
2. **navigationTextColorPrimary**: To set the text color Direction List title text color, Setting title text color and recenter text color
3. **navigationTextColorSecondary**: Bottom sheet item text color, total distance left and destination text color
4. **navigationTextColorTertiary**: To change direction list distance text color
5. **navigationRecenterBackgroundDrawable**: To change recenter background drawable
6. **navigationViewRecenterDrawable**: To change recenter drawable
7. **navigationViewSoundBackground**: To change mute/unmute button background drawable
8. **navigationViewSoundOn**: To change unmute drawable
9. **navigationViewSoundOff**: To change mute drawable
10. **navigationViewBannerBackgroundSelected**: To set navigation top instruction banner background drawable for current step
11. **navigationViewBannerBackgroundUnSelected**: To set  navigation top instruction banner background drawable for other than current step
12. **navigationViewBannerManeuverPrimary**: To set navigation top banner turn icon primary color
13. **navigationViewBannerManeuverSecondary**: To set navigation top banner turn icon secondary color (for round about, fork etc., icons)
14. **navigationViewBannerPrimaryText**: Top banner instruction text color and short instruction text color
15. **navigationViewBannerSecondaryText**: Top banner instruction distance text color
16. **navigationBearingIcon**: Navigation current location drawable

<br><br><br>

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

