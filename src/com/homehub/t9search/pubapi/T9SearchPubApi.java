
package com.homehub.t9search.pubapi;

import java.util.List;

import android.content.Context;

import com.homehub.t9search.mode.SearchResultItem;
import com.homehub.t9search.service.SearchService;

public class T9SearchPubApi {

    public static void updatePackage(Context cxt, String pkg) {
        SearchService.getInstance(cxt).updatePackage(pkg);
    }

    public static void removePackage(Context cxt, String pkg) {
        SearchService.getInstance(cxt).removePakcage(pkg);
    }
    
    public static List<SearchResultItem> getRecentlyInstalled6Apps(Context cxt){
        return SearchService.getInstance(cxt).getRecentlyInstalled6Apps();
    }
}
