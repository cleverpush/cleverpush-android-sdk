package com.cleverpush.mapper;

import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.ArrayList;

public class SubscriptionToListMapper implements Mapper<JSONArray,ArrayList> {

	@Override
	public JSONArray toKey(ArrayList value) {
		return new JSONArray(value);
	}

	@Override
	public ArrayList toValue(JSONArray key) {
		return new Gson().fromJson(key.toString(),ArrayList.class);
	}
}
