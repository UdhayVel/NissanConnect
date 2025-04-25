package com.mappls.app.navigation.demo.car.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mappls.app.navigation.demo.HomeActivity;
import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.car.model.PolylineCoordinates;
import com.mappls.sdk.maps.geometry.LatLng;

import java.util.ArrayList;

import timber.log.Timber;

public class LocationSharingActivity extends AppCompatActivity {

    LatLng sourceLatLng;
    LatLng destinationLatLng;
    ArrayList<PolylineCoordinates> wayPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_sharing);


        Button shareLocation = findViewById(R.id.shareLocation);
        TextView txt_back = findViewById(R.id.txt_back);


        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            sourceLatLng = (LatLng) intent.getExtras().get("sourceLatLng");
            destinationLatLng = (LatLng) intent.getExtras().get("destLatLng");
////            wayPoints = getIntent().getParcelableArrayListExtra("wayPoints");
        }

        txt_back.setOnClickListener(v -> {
            Intent intent1 = new Intent(this, HomeActivity.class);
            startActivity(intent1);
            finishAffinity();
        });

        shareLocation.setOnClickListener(v -> {
            if (sourceLatLng != null && destinationLatLng != null) {

                String origin = sourceLatLng.getLatitude() + "," + sourceLatLng.getLongitude(); // Bangalore
                String destination = destinationLatLng.getLatitude() + "," + destinationLatLng.getLongitude(); // Chennai

                Timber.tag("sourceLatLng").d(origin);
                Timber.tag("destinationLatLng").d(destination);

//            StringBuilder wayLists = new StringBuilder();
//            for (int i = 0; i < wayPoints.size(); i++) {
//                wayLists.append(wayPoints.get(i).getLat()).append(",").append(wayPoints.get(i).getLng()).append(
//                        "|");
//            }

                String uri = "https://www.google.com/maps/dir/?api=1"
                        + "&origin=" + origin
                        + "&destination=" + destination
//                    + "&waypoints=" + wayLists
                        + "&travelmode=driving";

                // Create the share intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out this route");
                shareIntent.putExtra(Intent.EXTRA_TEXT, uri);

                // Launch the share sheet
                startActivity(Intent.createChooser(shareIntent, "Share route via"));

            }
        });

    }
}
