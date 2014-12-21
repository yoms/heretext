package com.orlkuk.chathere.hmi;

import android.app.ActionBar;
import android.content.ContentValues;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
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
import java.util.HashMap;
import java.util.Map;


public class ChatActivity  extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, MessageDialogFragment.MessageDialogListener{

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private GoogleMap mMap;
    private String mProfileName;
    private GcmUtil mGcmUtil;
    private String mProfileEmail;
    private String profileId;
    private Map<String, Marker> currentsMarkers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        profileId = getIntent().getStringExtra(Common.PROFILE_ID);
        currentsMarkers = new HashMap<String, Marker>();


        Cursor c = getContentResolver().query(Uri.withAppendedPath(DataProvider.CONTENT_URI_PROFILE, profileId), null, null, null, null);
        if (c.moveToFirst()) {
            mProfileName = c.getString(c.getColumnIndex(DataProvider.COL_NAME));
            mProfileEmail = c.getString(c.getColumnIndex(DataProvider.COL_EMAIL));
        }
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        mNavigationDrawerFragment.setCurrentContact(mProfileEmail);

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

        mTitle = getTitle();

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
        return mProfileEmail;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat, menu);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions( ActionBar.DISPLAY_SHOW_CUSTOM );
        RoundedImageView imageView = new RoundedImageView(actionBar.getThemedContext());
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setImageResource(R.drawable.ic_contact_picture);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.LEFT
                | Gravity.CENTER_VERTICAL);
        layoutParams.rightMargin = 40;
        layoutParams.topMargin = 10;
        layoutParams.bottomMargin = 10;
        imageView.setLayoutParams(layoutParams);
        actionBar.setCustomView(imageView);
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

            Cursor c = getContentResolver().query(Uri.withAppendedPath(DataProvider.CONTENT_URI_USER_MESSAGES, mProfileEmail), null, null, null, null);
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
                    BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                    if (to != null)
                    {
                        if (to.equals(mProfileEmail)) {
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
                    ServerUtilities.send(latLng, inputText, mProfileEmail);

                    ContentValues values = new ContentValues(4);
                    values.put(DataProvider.COL_LAT, latLng.latitude);
                    values.put(DataProvider.COL_LON, latLng.longitude);
                    values.put(DataProvider.COL_MSG, inputText);
                    values.put(DataProvider.COL_TO, mProfileEmail);
                    getContentResolver().insert(DataProvider.CONTENT_URI_MESSAGES, values);
                    return true;

                } catch (IOException ex) {
                    return false;
                }
            }
            @Override
            protected void onPostExecute(Boolean sended) {
                if(sended) {
                    mNavigationDrawerFragment.getAdapter().notifyDataSetChanged();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Unable to send the message", Toast.LENGTH_LONG).show();
                }
            }

            }.execute(null, null, null);
    }

}
