package com.orlkuk.chathere.hmi;

import android.app.ActionBar;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.orlkuk.chathere.R;
import com.orlkuk.chathere.gcm.GcmUtil;
import com.orlkuk.chathere.gcm.ServerUtilities;
import com.orlkuk.chathere.model.Common;
import com.orlkuk.chathere.model.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class ChatActivity  extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, MessageDialogFragment.MessageDialogListener{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    private GoogleMap mMap;
    private String mContactName;
    private GcmUtil mGcmUtil;
    private String mContactEmail;
    private String mContactID;
    private String mContactHostID;
    private Map<String, Marker> currentsMarkers;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        mContactID = getIntent().getStringExtra(Common.PROFILE_ID);
        currentsMarkers = new HashMap<String, Marker>();


        Cursor c = getContentResolver().query(Uri.withAppendedPath(DataProvider.CONTENT_URI_PROFILE, mContactID), null, null, null, null);
        if (c.moveToFirst()) {
            mContactName = c.getString(c.getColumnIndex(DataProvider.COL_NAME));
            mContactEmail = c.getString(c.getColumnIndex(DataProvider.COL_EMAIL));
            mContactHostID = c.getString(c.getColumnIndex(DataProvider.COL_CONTACT_ID));
        }
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setCurrentContact(mContactEmail);

        setUpMapIfNeeded();

        mMap.setOnMapLongClickListener( new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {


                FragmentManager fm = getSupportFragmentManager();
                MessageDialogFragment messageDialogFragment = new MessageDialogFragment();
                messageDialogFragment.setPosition(latLng);
                messageDialogFragment.show(fm, "fragment_message_dialog");
            }
        });
        
        populateMarkers();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        mGcmUtil = new GcmUtil(getApplicationContext());
    }
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            SupportMapFragment frag = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            if (frag != null) {
                mMap = frag.getMap();
            }
        }
    }
    @Override
    public void onNavigationDrawerItemSelected(int position) {

        // Check if we were successful in obtaining the map.
        if(mNavigationDrawerFragment != null) {
            SimpleCursorAdapter adp = ((SimpleCursorAdapter) mNavigationDrawerFragment.getAdapter());
            if (adp != null) {
                Cursor c = adp.getCursor();
                c.moveToPosition(position);
                double lat = c.getDouble(c.getColumnIndex(DataProvider.COL_LAT));
                double lon = c.getDouble(c.getColumnIndex(DataProvider.COL_LON));
                currentsMarkers.values();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 20.0f));

            }
        }
    }

    public String getProfileEmail() {
        return mContactEmail;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions( ActionBar.DISPLAY_SHOW_CUSTOM );
        RoundedImageView imageView = new RoundedImageView(actionBar.getThemedContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setPadding(-10,-10,-10,-10);


        InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(),
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(mContactHostID)));

        if (inputStream != null) {
            Bitmap photo = BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(photo);
        }
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.LEFT
                | Gravity.CENTER_VERTICAL);
        imageView.setLayoutParams(layoutParams);
        actionBar.setCustomView(imageView);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setTitle(mContactName);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateMarkers()
    {
        if (mMap != null) {

            Cursor c = getContentResolver().query(Uri.withAppendedPath(DataProvider.CONTENT_URI_USER_MESSAGES, mContactEmail), null, null, null, null);
            for(Marker mark : currentsMarkers.values())
            {
                mark.setVisible(false);
            }
            while( c.moveToNext())
            {
                double lat = c.getDouble(c.getColumnIndex(DataProvider.COL_LAT));
                double lon = c.getDouble(c.getColumnIndex(DataProvider.COL_LON));
                String msg = c.getString(c.getColumnIndex(DataProvider.COL_MSG));
                String to = c.getString(c.getColumnIndex(DataProvider.COL_TO));
                String markerId = c.getString(c.getColumnIndex(DataProvider.COL_ID));

                if(currentsMarkers.containsKey(markerId)) {
                    currentsMarkers.get(markerId).setVisible(true);
                }
                else {
                    BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker();
                    if (to != null)
                    {
                        if (to.equals(mContactEmail)) {
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
                        }
                    }
                    currentsMarkers.put(markerId,
                            mMap.addMarker(new MarkerOptions().
                                    position(new LatLng(lat, lon)).
                                    title(msg).
                                    icon(icon)));
                }

            }
        }
    }

    public void onSendMessageClicked(final String inputText, final LatLng latLng)
    {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    ServerUtilities.send(latLng, inputText, mContactEmail);

                    ContentValues values = new ContentValues(4);
                    values.put(DataProvider.COL_LAT, latLng.latitude);
                    values.put(DataProvider.COL_LON, latLng.longitude);
                    values.put(DataProvider.COL_MSG, inputText);
                    values.put(DataProvider.COL_TO, mContactEmail);
                    getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);
                    return true;

                } catch (IOException ex) {
                    return false;
                }
            }
            @Override
            protected void onPostExecute(Boolean sended) {
                if(sended) {
                    mNavigationDrawerFragment.getLoaderManager().restartLoader(0, null, mNavigationDrawerFragment);
                    populateMarkers();
                    Toast.makeText(getApplicationContext(), "Message sent", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Unable to send the message", Toast.LENGTH_LONG).show();
                }
            }

            }.execute(null, null, null);
    }

}
