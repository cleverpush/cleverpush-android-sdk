package com.cleverpush.listener;

public class AddMultipleSubscriptionTagsHelper implements AddTagCompletedListener {

	private String[] tagIds;

	public AddMultipleSubscriptionTagsHelper(String[] tagIds) {
		this.tagIds = tagIds;
	}

	@Override
	public void tagAdded(int currentPositionOfTagToAdd) {
		if (currentPositionOfTagToAdd != tagIds.length - 1) {
			currentPositionOfTagToAdd++;
			//addSubscriptionTag(tagIds[currentPositionOfTagToAdd], this,currentPositionOfTagToAdd);
		}
	}

}
