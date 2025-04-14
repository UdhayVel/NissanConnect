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
import com.mappls.sdk.navigation.NavigationFormatter;

public class RouteSummaryDialogFragment extends DialogFragment {

    private TextView tvTotalDistance;
    private TextView tvTimeTaken;
    private TextView tvAverageSpeed;

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
        return inflater.inflate(R.layout.fragment_route_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCancelable(false);
        tvTotalDistance = view.findViewById(R.id.tv_total_distance);
        tvTimeTaken = view.findViewById(R.id.tv_total_time_taken);
        tvAverageSpeed = view.findViewById(R.id.tv_avg_speed);

        tvTotalDistance.setText(NavigationFormatter.getFormattedDistance(MapplsNavigationHelper.getInstance().getNavigationSummary().getTotalDistance(), getContext()));
        tvTimeTaken.setText(NavigationFormatter.getFormattedDuration((int) MapplsNavigationHelper.getInstance().getNavigationSummary().getTotalTimeTaken(), getContext()));
        tvAverageSpeed.setText(NavigationFormatter.getFormattedSpeed(MapplsNavigationHelper.getInstance().getNavigationSummary().getAverageSpeed(), getContext()));

        view.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().finish();
            }
        });
    }
}
