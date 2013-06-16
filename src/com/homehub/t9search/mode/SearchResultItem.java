
package com.homehub.t9search.mode;

public class SearchResultItem {
    public AppInfo appInfo;

    public String number;

    public boolean isContacts;

    public SearchResultItem() {
        super();
    }

    public SearchResultItem(AppInfo info, String number, boolean isContacts) {
        super();
        this.appInfo = info;
        this.number = number;
        this.isContacts = isContacts;
    }

}
