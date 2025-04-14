package com.mappls.app.navigation.demo.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;

import com.mappls.app.navigation.demo.NavApplication;
import com.mappls.app.navigation.demo.R;
import com.mappls.app.navigation.demo.model.Stop;
import com.mappls.sdk.navigation.NavigationFormatter;
import com.mappls.sdk.navigation.routing.NavigationStep;
import com.mappls.sdk.navigation.ui.views.turnlane.TurnLaneAdapter;
import com.mappls.sdk.plugin.directions.view.ManeuverView;
import com.mappls.sdk.services.api.directions.models.LegStep;

import java.util.List;

public class NavigationPagerAdapter extends PagerAdapter {

    private List<NavigationStep> mAdvises;
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private int selectedPosition = 0;
    private long distance = 0;
    private Stop endPoint;



    public NavigationPagerAdapter(Context context, List<NavigationStep> mAdvises, Stop endPoint) {
        this.mContext = context;
        this.mAdvises = mAdvises;
        this.endPoint = endPoint;
        this.mLayoutInflater = LayoutInflater.from(mContext);
    }


    @Override
    public int getCount() {
        return mAdvises == null ? 0 : mAdvises.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.navigation_strip_item, container, false);

        container.addView(itemView);

        MyViewHolder holder = new MyViewHolder(itemView);

        NavigationStep trip = mAdvises.get(position);

        holder.directionPreviewText.setText(trip.getDescriptionRoutePartHTML());
        holder.directionShortText.setText(trip.getShortInstruction());
        if (selectedPosition == position) {
            int meters = (distance > 0 ? 10 * (Math.round(distance / 10)) : 0);
            holder.directionPreviewDist.setText(NavigationFormatter.getFormattedDistance(meters, (NavApplication) mContext.getApplicationContext()));

        } else {
            if (position > 0) {
                NavigationStep _trip = mAdvises.get(position - 1);


                holder.directionPreviewDist.setText(NavigationFormatter.getFormattedDistance(_trip.distance, (NavApplication) mContext.getApplicationContext()));
            } else {
                holder.directionPreviewDist.setText("");
            }

        }
        holder.container.setBackgroundColor((position == selectedPosition) ? mContext.getResources().getColor(R.color.app_blue) : mContext.getResources().getColor(R.color.colorGray700));
        holder.repeatCurrentInstructionsLayout.setTag(position);
        holder.repeatCurrentInstructionsLayout.setOnClickListener(view -> {


        });
        holder.directionPreviewDist.setVisibility((mAdvises.size() - 1 == position) ? View.GONE : View.VISIBLE);

        if ((mAdvises.size() - 1 == position) && trip.getManeuverID() == 8) {
            holder.directionPreviewText.setText(endPoint.getName());
        }

        if (trip.getExtraInfo() instanceof LegStep) {
            LegStep legStep = (LegStep) trip.getExtraInfo();
            String type = legStep.maneuver().type();
            String modifier = legStep.maneuver().modifier();


            holder.maneuverViewImageView.setManeuverTypeAndModifier(type, modifier);
            if (legStep.maneuver().degree() != null) {
                holder.maneuverViewImageView.setRoundaboutAngle(legStep.maneuver().degree().floatValue());
            } else {
                holder.maneuverViewImageView.setRoundaboutAngle(180f);
            }


            if (legStep.intersections() != null && legStep.intersections().size() > 0 && legStep.intersections().get(0).lanes() != null
                    && !TextUtils.isEmpty(legStep.maneuver().modifier())) {
                holder.rvTurnLanes.setBackgroundColor((position == selectedPosition) ? mContext.getResources().getColor(R.color.app_blue) : mContext.getResources().getColor(R.color.colorGray700));

                holder.turnLaneAdapter.addTurnLanes(legStep.intersections().get(0).lanes(), legStep.maneuver().modifier());
                holder.laneGuidanceContainer.setVisibility(View.VISIBLE);
            } else {
                holder.laneGuidanceContainer.setVisibility(View.GONE);

            }
        }

        return itemView;
    }



    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
        notifyDataSetChanged();
    }

    public void setDistance(long distance) {
        if (distance > 0) {
            this.distance = 10 * (Math.round(distance / 10));
        } else {
            this.distance = distance;
        }

    }

    public NavApplication getMyApplication() {
        return ((NavApplication) mContext.getApplicationContext());
    }

    int getDrawableResId(int maneuverId) {
        return mContext.getResources().getIdentifier("ic_step_" + maneuverId, "drawable", mContext.getPackageName());
    }

    class MyViewHolder {
        private final RecyclerView rvTurnLanes;
        private ManeuverView maneuverViewImageView;

        private TextView directionPreviewText;
        private TextView directionShortText;
        private TextView directionPreviewDist;
        private View container;
        private LinearLayout repeatCurrentInstructionsLayout;
        private TurnLaneAdapter turnLaneAdapter;
        private View laneGuidanceContainer;

        public MyViewHolder(View view) {

            maneuverViewImageView = view.findViewById(R.id.maneuver_image_view);
            directionPreviewText = view.findViewById(R.id.navigation_strip_text);
            directionShortText = view.findViewById(R.id.navigation_strip_short_text);
            directionPreviewDist = view.findViewById(R.id.navigation_strip_dist);
            container = view.findViewById(R.id.strip_item_container);
            repeatCurrentInstructionsLayout = view.findViewById(R.id.repeat_current_instructions_layout);
            rvTurnLanes = view.findViewById(R.id.rvTurnLanes);
            laneGuidanceContainer = view.findViewById(R.id.lane_guidance_container);
            turnLaneAdapter = new TurnLaneAdapter();
            rvTurnLanes.setAdapter(turnLaneAdapter);
            rvTurnLanes.setHasFixedSize(true);
            rvTurnLanes.setLayoutManager(new LinearLayoutManager(view.getContext(),
                    LinearLayoutManager.HORIZONTAL, false));
        }
    }
}