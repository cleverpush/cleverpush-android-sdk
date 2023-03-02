package com.cleverpush.listener;

public interface AddTagCompletedListener {
    void tagAdded(int currentPositionOfTagToAdd);
    void onFailure(Exception exception);
}
