package com.homehub.t9search.pubapi;

import java.util.List;

import com.homehub.t9search.mode.SearchResultItem;

public interface ISearchComplet {
    // runs in background
    void onSearchResult(String query, long hits, List<SearchResultItem> result);
}
