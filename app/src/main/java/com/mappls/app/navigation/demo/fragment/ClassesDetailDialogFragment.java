package com.mappls.app.navigation.demo.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.mappls.app.navigation.demo.R;
import com.mappls.sdk.navigation.MapplsNavigationHelper;
import com.mappls.sdk.services.api.directions.models.RouteClasses;

import java.util.ArrayList;
import java.util.List;

public class ClassesDetailDialogFragment extends DialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.routeSummaryDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return inflater.inflate(R.layout.fragment_classes_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvRouteClassDetail = view.findViewById(R.id.tv_route_class_detail);

        if(MapplsNavigationHelper.getInstance().getCurrentRoute() != null && MapplsNavigationHelper.getInstance().getCurrentRoute().routeClasses() != null) {
            RouteClasses routeClasses = MapplsNavigationHelper.getInstance().getCurrentRoute().routeClasses();
            List<String> classesList = new ArrayList<>();

            if(routeClasses.toll() != null && routeClasses.toll() == 1) {
                classesList.add("Toll");
            }
            if(routeClasses.ferry() != null && routeClasses.ferry() == 1) {
                classesList.add("Ferry");
            }
            if(routeClasses.ferry() != null && routeClasses.ferry() == 1) {
                classesList.add("Tunnel");
            }
            if(routeClasses.motorway() != null && routeClasses.motorway() == 1) {
                classesList.add("Motorway");
            }
            if(routeClasses.restricted() != null && routeClasses.restricted() == 1) {
                classesList.add("Restricted");
            }

            if (classesList.size() > 0) {
                StringBuilder builder = new StringBuilder("This route contains ");
                if(classesList.size() == 1) {
                    builder.append(classesList.get(0));
                } else {
                    for(int i =0; i< classesList.size(); i++) {
                        if(i == classesList.size() - 1) {
                            builder.append(" & ");
                            builder.append(classesList.get(i));
                        } else if(i == 0) {
                            builder.append(classesList.get(i));
                        } else {
                            builder.append(", ");
                            builder.append(classesList.get(i));
                        }
                    }
                }
                tvRouteClassDetail.setText(builder.toString());
            } else {
                tvRouteClassDetail.setText("This route does not contains any classes");
            }
        } else {
            tvRouteClassDetail.setText("This route does not contains any classes");
        }


    }
}
