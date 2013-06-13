package com.homehub.t9search.dao;

import java.io.InputStream;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;

import com.homehub.t9search.R;

public class DBManager {
    private static final String CONTACTS_URI = "content://com.android.contacts/"
            + "data/phones/filter/";

    /**
     * @param strPhoneNumber The phone number you want to query.
     * @return if the query has result, it will be the people's photo; otherwise, it will be our default
     *         photo.
     */
    public static Drawable getPeoplePhotoByPhoneNumber(Context context, String strPhoneNumber) {
        Drawable peoplePhotoDrawable = null;
        Uri uriNumber2Contacts = Uri
                .parse(CONTACTS_URI + strPhoneNumber);
        Cursor cursorCantacts = context
                .getContentResolver().query(uriNumber2Contacts, null, null,
                        null, null);
        if (cursorCantacts != null && cursorCantacts.getCount() > 0) {
            // 若游标不为0则说明有头像,游标指向第一条记录
            cursorCantacts.moveToFirst();
            Long contactID = cursorCantacts.getLong(cursorCantacts
                    .getColumnIndex("contact_id"));
            Uri uri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI, contactID);
            InputStream input = ContactsContract.Contacts
                    .openContactPhotoInputStream(context
                            .getContentResolver(), uri);
            Bitmap peoplePhotoBitmap = BitmapFactory.decodeStream(input);
            peoplePhotoDrawable = new BitmapDrawable(context.getResources(), peoplePhotoBitmap);
        } else {
            peoplePhotoDrawable = context.getResources().getDrawable(R.drawable.ic_dialer);
        }
        return peoplePhotoDrawable;
    }
}
