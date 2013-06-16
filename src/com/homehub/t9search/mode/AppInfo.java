
package com.homehub.t9search.mode;

public class AppInfo {
    public String name;

    public String pkg;

    public String pingyin;

    public String activity;

    public long installedDate;

    public AppInfo(String name, String pkg, String pingyin, String activity, long installedDate) {
        super();
        this.name = name;
        this.pkg = pkg;
        this.pingyin = pingyin;
        this.activity = activity;
        this.installedDate = installedDate;
    }

    public AppInfo() {
        super();
    }

}
