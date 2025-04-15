package com.mappls.app.navigation.demo.car.screens.models;

public class UiState {
    private boolean loading;
    private String error;

    public UiState() {
        this.loading = false;
        this.error = null;
    }

    public UiState(boolean loading, String error) {
        this.loading = loading;
        this.error = error;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}