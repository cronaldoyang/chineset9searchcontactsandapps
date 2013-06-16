
package com.homehub.t9search.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.homehub.t9search.pubapi.T9SearchPubApi;

public class PackageReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context cxt, Intent intent) {
        if (cxt != null && intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Uri uri = intent.getData();
            String pkg = (uri != null) ? uri.getSchemeSpecificPart().toString() : null;
            if (pkg != null && !pkg.equals("")) {
                if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                    T9SearchPubApi.updatePackage(cxt, pkg);
                } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                    T9SearchPubApi.updatePackage(cxt, pkg);
                } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    T9SearchPubApi.removePackage(cxt, pkg);
                }
            }
        }
    }

}
