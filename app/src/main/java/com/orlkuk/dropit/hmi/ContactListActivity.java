package com.orlkuk.dropit.hmi;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.orlkuk.dropit.R;
import com.orlkuk.dropit.model.Common;
import com.orlkuk.dropit.model.DataProvider;

import java.io.IOException;
import java.io.InputStream;

public class ContactListActivity extends Activity  implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {

    private SimpleCursorAdapter adapter;
    private ListView listView;
    private FloatingActionButton addFloatingButton;
    private final int PICK_CONTACT_ACTIVITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);

        listView = (ListView) findViewById(R.id.contactListView);
        addFloatingButton = (FloatingActionButton) findViewById(R.id.addFAB);
        addFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent contactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI);
                startActivityForResult(contactIntent, PICK_CONTACT_ACTIVITY);
            }
        });
        createAdapter();

        listView.setAdapter(adapter);
        listView.setOnItemClickListener( this );


        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Common.PROFILE_ID, String.valueOf(l));
        startActivity(intent);
    }

    public void createAdapter()
    {
        adapter = new SimpleCursorAdapter(this,
                R.layout.main_list_item,
                null,
                new String[]{DataProvider.COL_NAME, DataProvider.COL_CONTACT_ID},
                new int[]{R.id.text1, R.id.avatar},
                0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch(view.getId()) {
                    case R.id.avatar:
                        int contactID = cursor.getInt(columnIndex);
                        if (contactID > 0) {

                            try {
                                InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(),
                                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactID)));


                                RoundedImageView imageView = (RoundedImageView) view;
                                if (inputStream != null) {
                                    Bitmap photo = BitmapFactory.decodeStream(inputStream);
                                    if(photo != null) {
                                        imageView.setImageBitmap(photo);
                                    }
                                    inputStream.close();
                                }
                                else
                                {
                                    imageView.setImageResource(R.drawable.ic_contact_picture);
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    //----------------------------------------------------------------------------

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(this,
                DataProvider.CONTENT_URI_PROFILE,
                new String[]{DataProvider.COL_ID, DataProvider.COL_NAME, DataProvider.COL_CONTACT_ID},
                null,
                null,
                DataProvider.COL_ID + " DESC");
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT_ACTIVITY) :
                if (resultCode == Activity.RESULT_OK) {
                    Uri emailData = data.getData();
                    String name = null;
                    String uri = null;
                    String email = null;
                    String id = null;

                    // Find email and ID
                    Cursor emailCursor = getContentResolver().query(emailData, null, null, null, null);
                    if (emailCursor.moveToFirst()) {

                        id = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID));
                        email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                    }
                    emailCursor.close();


                    // Find contact name
                    Cursor nameCursor = getContentResolver().query(
                            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(id))
                            , null, null, null, null);
                    if (nameCursor.moveToFirst()) {
                        name = nameCursor.getString(nameCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    }
                    nameCursor.close();

                    if( name != null && email != null) {
                        try {
                            ContentValues values = new ContentValues(2);
                            values.put(DataProvider.COL_NAME, name);
                            values.put(DataProvider.COL_EMAIL, email);
                            values.put(DataProvider.COL_CONTACT_ID, id);
                            getContentResolver().insert(DataProvider.CONTENT_URI_PROFILE, values);
                        } catch (SQLException sqle) {
                            Log.e(getLocalClassName(), "Impossible to insert contact");
                        }
                    }
                }
                break;
        }
    }
}
