package com.mappls.app.navigation.demo;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mappls.sdk.navigation.activities.MapActivity;

import timber.log.Timber;

public abstract class BaseActivity extends MapActivity {

    FragmentManager manager;
    private MaterialDialog mProgressDialog;
    private boolean safeToPerformFragmentTransactions = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void navigateTo(Fragment newFragment, boolean addToBackStack) {
        try {
            manager = getSupportFragmentManager();
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.fragment_container, newFragment, newFragment.getClass().getSimpleName());
            try {
                if (addToBackStack) {
                    String name = newFragment.getClass().getSimpleName();
                    ft.addToBackStack(name);
                }
                ft.commitAllowingStateLoss();
                manager.executePendingTransactions();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public void popToBackStack(Fragment fragment) {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack(fragment.getClass().getSimpleName(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public void addFragment(Fragment newFragment, boolean addToBackStack) {
        try {
            manager = getSupportFragmentManager();
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(R.id.fragment_container, newFragment, newFragment.getClass().getSimpleName());
            try {
                if (addToBackStack) {
                    String name = newFragment.getClass().getSimpleName();
                    ft.addToBackStack(name);
                }
                ft.commitAllowingStateLoss();
                manager.executePendingTransactions();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    public void showProgress() {
        if (!safeToPerformFragmentTransactions)
            return;

        if (isProgressVisible()) {
            return;
        }

        mProgressDialog = new MaterialDialog.Builder(this)
                .content("Loading...Please Wait")
                .progress(true, 0)
                .progressIndeterminateStyle(false)
                .build();

        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    public void hideProgress() {
        if (isProgressVisible()) {
            if (safeToPerformFragmentTransactions) {
                mProgressDialog.dismiss();
            }
        }
    }

    public boolean isProgressVisible() {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                return true;
            }
        }
        return false;
    }


}
