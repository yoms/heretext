package com.orlkuk.dropit.hmi;

import android.app.ActionBar;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.orlkuk.dropit.R;
import com.orlkuk.dropit.gcm.GcmUtil;
import com.orlkuk.dropit.gcm.ServerUtilities;
import com.orlkuk.dropit.model.Common;
import com.orlkuk.dropit.model.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class ChatActivity  extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks{

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
    private LatLng mLastLatLng;
    private Marker mCurrentMessageMarker;
    private Map<String, Marker> currentsMarkers;
    private ImageButton mSendButton;
    private EditText mMsgEdit;

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
        mSendButton = (ImageButton) findViewById(R.id.sendBtn);
        mMsgEdit = (EditText) findViewById(R.id.msgEdit);


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

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {

                mLastLatLng = latLng;
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLastLatLng, 20.0f));
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.sendLayout);
                linearLayout.setVisibility(View.VISIBLE);
                mSendButton.requestFocus();

                if (mCurrentMessageMarker != null) {
                    mCurrentMessageMarker.setVisible(false);
                    mCurrentMessageMarker.remove();
                }

                BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                mCurrentMessageMarker = mMap.addMarker(new MarkerOptions().position(mLastLatLng).icon(icon));
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                clearMessageEdition();
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

                Location location = Common.getCurrentLocation();
                if(location != null)
                {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17.0f));
                }

                mMap.setMyLocationEnabled(true);

                UiSettings settings = mMap.getUiSettings();
                settings.setAllGesturesEnabled(true);
                settings.setMyLocationButtonEnabled(true);
                settings.setZoomControlsEnabled(false);
            }
        }
    }

    public void clearMessageEdition()
    {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.sendLayout);
        linearLayout.setVisibility(View.GONE);
        mMsgEdit.setText("");
        if (mCurrentMessageMarker != null) {
            mCurrentMessageMarker.setVisible(false);
            mCurrentMessageMarker.remove();
            mCurrentMessageMarker = null;
        }
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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

    public void onSendMessageClicked(View view)
    {
        String text = mMsgEdit.getText().toString();
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                try {
                    ServerUtilities.send(mLastLatLng, params[0], mContactEmail);

                    ContentValues values = new ContentValues(4);
                    values.put(DataProvider.COL_LAT, mLastLatLng.latitude);
                    values.put(DataProvider.COL_LON, mLastLatLng.longitude);
                    values.put(DataProvider.COL_MSG, params[0]);
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
                    clearMessageEdition();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Unable to send the message", Toast.LENGTH_LONG).show();
                }
            }

            }.execute(text, null, null);
    }

}
