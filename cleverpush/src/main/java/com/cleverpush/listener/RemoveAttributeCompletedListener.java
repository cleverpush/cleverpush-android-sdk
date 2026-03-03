package com.cleverpush.listener;

public interface RemoveAttributeCompletedListener {
  void attributeRemoved(int currentPositionOfAttributeToRemove);

  void onFailure(Exception exception);
}
