package com.homehub.t9search.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.homehub.t9search.BuildConfig;
import com.homehub.t9search.R;
import com.homehub.t9search.service.SearchService;

public class T9SearchResultAdapter extends BaseAdapter {
    private static final String TAG = "T9SearchResultAdapter";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private List<Map<String, Object>> mResults = new ArrayList<Map<String, Object>>();
    private Context mContext;

    public T9SearchResultAdapter(Context context) {
        mContext = context;
    }

    @Override
    public synchronized int getCount() {
        return mResults.size();
    }

    public synchronized void updateData(List<Map<String, Object>> results) {
        this.mResults = results;
    }

    @Override
    public Object getItem(int position) {
        return mResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            rowView = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent,
                    false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.name = (TextView) rowView.findViewById(R.id.name);
            viewHolder.number = (TextView) rowView.findViewById(R.id.number);
            viewHolder.icon = (ImageView) rowView.findViewById(R.id.icon);
            rowView.setTag(viewHolder);
        }
        ViewHolder holder = (ViewHolder) rowView.getTag();
        holder.name.setText(getNameStr(position));
        holder.icon.setVisibility(View.VISIBLE);
        holder.icon.setImageDrawable(getAppIcon(position));
        holder.number.setVisibility(View.GONE);
        return rowView;
    }

    protected Spanned getNameStr(int position) {
        Map<String, Object> searchRes = mResults.get(position);
        StringBuilder nameBuilder = new StringBuilder();
        if (searchRes.containsKey(SearchService.FIELD_NAME)) {
            nameBuilder.append(searchRes.get(SearchService.FIELD_NAME).toString());
        } else {
            nameBuilder.append("Not contains*****");
        }
        nameBuilder.append(' ');
        if (searchRes.containsKey(SearchService.FIELD_PINYIN)) {
            nameBuilder.append(searchRes.get(SearchService.FIELD_PINYIN).toString());
        }
        return Html.fromHtml(nameBuilder.toString());
    }

    protected Spanned getNumberStr(int position) {
        Map<String, Object> searchRes = mResults.get(position);
        StringBuilder numberBuilder = new StringBuilder();
        if (searchRes.containsKey(SearchService.FIELD_HIGHLIGHTED_NUMBER)) {
            numberBuilder.append(searchRes.get(SearchService.FIELD_HIGHLIGHTED_NUMBER)
                    .toString());
        } else if (searchRes.containsKey(SearchService.FIELD_NUMBER)) {
            numberBuilder.append(searchRes.get(SearchService.FIELD_NUMBER));
        }
        return Html.fromHtml(numberBuilder.toString());
    }

    protected boolean checkIsApp(int position) {
        Map<String, Object> searchRes = mResults.get(position);
        boolean ret = false;
        if (searchRes.containsKey(SearchService.FIELD_PKG)
                && searchRes.containsKey(SearchService.FIELD_ACTIVITY)) {
            ret = true;
        }
        if (DEBUG) {
            Log.e(TAG, "checkIsApp:" + ret);
        }
        return ret;
    }

    protected Drawable getAppIcon(int position) {
        Map<String, Object> searchRes = mResults.get(position);
        Drawable icon = null;
        try {
            PackageManager pm = mContext.getPackageManager();
            String pkg = searchRes.get(SearchService.FIELD_PKG).toString();
            if (DEBUG) {
                Log.e(TAG, "getAppIcon: pkg:" + pkg);
            }
            icon = pm.getApplicationIcon(pkg);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return icon;
    }

    protected void naviToApp(int position) {
        Map<String, Object> searchRes = mResults.get(position);
        String pkg = searchRes.get(SearchService.FIELD_PKG).toString();
        String activity = searchRes.get(SearchService.FIELD_ACTIVITY).toString();
        if (DEBUG) {
            Log.e(TAG, "naviToApp: pkg:" + pkg + "; activity:" + activity);
        }
        ComponentName name = new ComponentName(pkg, activity);
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        i.setComponent(name);
        mContext.startActivity(i);
    }

    protected void naviTocall(int position) {
        Map<String, Object> searchRes = mResults.get(position);
        String number = searchRes.get(SearchService.FIELD_NUMBER).toString();
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.fromParts("tel", number, null));
        mContext.startActivity(intent);
    }

    public void handleClick(int position) {
        if (checkIsApp(position)) {
            naviToApp(position);
        } else {
            naviTocall(position);
        }
    }

    public void resetData() {
        mResults.clear();
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        public ImageView icon;
        public TextView name;
        public TextView number;
    }
}
