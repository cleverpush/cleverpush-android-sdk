package com.cleverpush.mapper;

import com.google.gson.Gson;

import org.json.JSONArray;

import java.util.Collection;

public class SubscriptionToListMapper implements Mapper<JSONArray, Collection<String>> {

  @Override
  public JSONArray toKey(Collection<String> value) {
    return new JSONArray(value);
  }

  @Override
  public Collection<String> toValue(JSONArray key) {
    return new Gson().fromJson(key.toString(), Collection.class);
  }

}
