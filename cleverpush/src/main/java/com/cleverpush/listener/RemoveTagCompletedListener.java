package com.cleverpush.listener;

public interface RemoveTagCompletedListener {
  void tagRemoved(int currentPositionOfTagToRemove);

  void onFailure(Exception exception);
}
