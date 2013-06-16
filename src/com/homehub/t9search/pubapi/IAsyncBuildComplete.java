
package com.homehub.t9search.pubapi;

import java.util.List;

import com.homehub.t9search.mode.SearchResultItem;

public interface IAsyncBuildComplete {
    void onAsyncBuildComplete(List<SearchResultItem> result);
}
