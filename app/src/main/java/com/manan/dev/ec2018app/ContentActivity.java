package com.manan.dev.ec2018app;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.manan.dev.ec2018app.Adapters.DashboardCategoryScrollerAdapter;
import com.manan.dev.ec2018app.Adapters.DashboardSlideAdapter;
import com.manan.dev.ec2018app.DatabaseHandler.DatabaseController;
import com.manan.dev.ec2018app.Models.CategoryItemModel;
import com.manan.dev.ec2018app.Models.Coordinators;
import com.manan.dev.ec2018app.Models.EventDetails;
import com.manan.dev.ec2018app.Xunbao.XunbaoActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ContentActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ViewPager viewPager;
    private TextView[] dots;
    private DashboardSlideAdapter myViewPagerAdapter;
    private LinearLayout dotsLayout;
    private ArrayList<CategoryItemModel> allSampleData = new ArrayList<CategoryItemModel>();
    private ArrayList<EventDetails> allEvents;
    TextView categoriesHeadingTextView;
    private DrawerLayout drawer;
    private NavigationView nav_view;
    private DatabaseController mDatabaseController;
    private String phoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navbar_content);

        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        allEvents = new ArrayList<>();
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                ContentActivity.this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        SharedPreferences prefs = getSharedPreferences(getResources().getString(R.string.sharedPrefName), MODE_PRIVATE);
        phoneNumber = prefs.getString("Phone", null);


        nav_view = (NavigationView) findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener((NavigationView.OnNavigationItemSelectedListener) this);
        nav_view.setCheckedItem(R.id.nav_home);

        if (phoneNumber == null) {
            Menu menu = nav_view.getMenu();
            menu.findItem(R.id.nav_logout).setVisible(false);
            menu.findItem(R.id.nav_tickets).setVisible(false);
        }
        try {
            mDatabaseController = new DatabaseController(getApplicationContext());
        } catch (Exception e) {
            Log.d("DBChecker", e.getMessage());
        }

        categoriesHeadingTextView = findViewById(R.id.text_viewcategories);
        viewPager = (ViewPager) findViewById(R.id.slliderview_pager);
        myViewPagerAdapter = new DashboardSlideAdapter(getSupportFragmentManager());
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);
        ImageView img = findViewById(R.id.drawerTogglebtn);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });

        addBottomDots(0);

        addData();
        retreiveEvents();
        RecyclerView categoryRecycleview = (RecyclerView) findViewById(R.id.category_recycler_view);

        categoryRecycleview.setHasFixedSize(true);

        DashboardCategoryScrollerAdapter adapter = new DashboardCategoryScrollerAdapter(ContentActivity.this, allSampleData);

        categoryRecycleview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        categoryRecycleview.setAdapter(adapter);

    }

    private void retreiveEvents() {
        if (isNetworkAvailable()) {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = getResources().getString(R.string.get_all_events_api);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject object = new JSONObject(response);
                        JSONArray eventArray = object.getJSONArray("data");
                        EventDetails event = new EventDetails();
                        for (int i = 0; i < eventArray.length(); i++) {
                            JSONObject currEvent = eventArray.getJSONObject(i);
                            if (currEvent.has("timing"))
                                event.setmName(currEvent.getString("title"));
                            if (currEvent.has("fee"))
                                event.setmFees(currEvent.getLong("fee"));
                            if (currEvent.has("timing")) {
                                JSONObject timing = currEvent.getJSONObject("timing");
                                if (timing.has("from"))
                                    event.setmStartTime(timing.getLong("from"));
                                if (timing.has("to"))
                                    event.setmEndTime(timing.getLong("to"));
                            }
                            if (currEvent.has("clubname"))
                                event.setmClubname(currEvent.getString("clubname"));
                            if (currEvent.has("category"))
                                event.setmCategory(currEvent.getString("category"));
                            if (currEvent.has("desc"))
                                event.setmDesc(currEvent.getString("desc"));
                            if (currEvent.has("venue"))
                                event.setmVenue(currEvent.getString("venue"));
                            if (currEvent.has("rules"))
                                event.setmRules(currEvent.getString("rules"));
                            if (currEvent.has("photolink")) {
                                event.setmPhotoUrl(currEvent.getString("photolink"));
                            } else {
                                event.setmPhotoUrl(null);
                            }
                            event.setmCoordinators(new ArrayList<Coordinators>());
                            if (currEvent.has("coordinators")) {
                                JSONArray coordinators = currEvent.getJSONArray("coordinators");
                                for (int j = 0; j < coordinators.length(); j++) {
                                    JSONObject coordinatorsDetail = coordinators.getJSONObject(j);
                                    Coordinators coord = new Coordinators();
                                    if (coordinatorsDetail.has("_id"))
                                        coord.setmCoordId(coordinatorsDetail.getString("_id"));
                                    if (coordinatorsDetail.has("phone"))
                                        coord.setmCoordPhone(coordinatorsDetail.getLong("phone"));
                                    if (coordinatorsDetail.has("name"))
                                        coord.setmCoordName(coordinatorsDetail.getString("name"));
                                    event.getmCoordinators().add(coord);
                                }
                            }
                            event.setmPrizes(new ArrayList<String>());
                            if (currEvent.has("prizes")) {
                                JSONObject prize = currEvent.getJSONObject("prizes");
                                if (prize.has("prize1"))
                                    event.getmPrizes().add(prize.getString("prize1"));
                                if (prize.has("prize2"))
                                    event.getmPrizes().add(prize.getString("prize2"));
                                if (prize.has("prize3"))
                                    event.getmPrizes().add(prize.getString("prize3"));
                            } else {
                                event.getmPrizes().add(null);
                                event.getmPrizes().add(null);
                                event.getmPrizes().add(null);
                            }
                            if (currEvent.has("_id"))
                                event.setmEventId(currEvent.getString("_id"));
                            if (currEvent.has("eventtype")) {
                                event.setmEventTeamSize(currEvent.getString("eventtype"));
                                Log.d("DBChecker", currEvent.getString("eventtype") + " " + event.getmEventTeamSize());
                                //Toast.makeText(ContentActivity.this, currEvent.getString("eventtype"), Toast.LENGTH_SHORT).show();
                            }
                            //Toast.makeText(ContentActivity.this, event.getmEventId() + " " + event.getmPrizes().toString(), Toast.LENGTH_LONG).show();
                            allEvents.add(event);
                            updateDatabase();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ContentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d("DBChecker", e.getMessage());
                        e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
            queue.add(stringRequest);
        }

    }

    private void updateDatabase() {
        if (allEvents.size() > mDatabaseController.getCount()) {
            for (EventDetails eventDetails : allEvents) {
                mDatabaseController.addEntryToDb(eventDetails);
            }
        } else {
            for (EventDetails eventDetails : allEvents) {
                mDatabaseController.updateDb(eventDetails);
            }
        }
        Log.d("DBChecker", Integer.toString(mDatabaseController.getCount()));
//        if (mDatabaseController.retreiveCategory("Vividha").get(0).getmEventTeamSize() != null)
//            Log.d("DBChecker", mDatabaseController.retreiveCategory("Vividha").get(0).getmEventTeamSize());
    }

    private void addData() {

        CategoryItemModel manan = new CategoryItemModel();
        manan.setClubName("Manan");
        manan.setDisplayName("Coding");
        manan.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.manan));
        allSampleData.add(manan);

        CategoryItemModel ananya = new CategoryItemModel();
        ananya.setClubName("Ananya");
        ananya.setDisplayName("Literature");
        ananya.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.ananya));
        allSampleData.add(ananya);

        CategoryItemModel vividha = new CategoryItemModel();
        vividha.setClubName("Vividha");
        vividha.setDisplayName("Drama");
        vividha.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.vividha));
        allSampleData.add(vividha);

        CategoryItemModel jhalak = new CategoryItemModel();
        jhalak.setClubName("Jhalak");
        jhalak.setDisplayName("Photography & Designing");
        jhalak.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.jhalak));
        allSampleData.add(jhalak);

        CategoryItemModel eklavya = new CategoryItemModel();
        eklavya.setClubName("Eklavya");
        eklavya.setDisplayName("Fun Events");
        eklavya.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.eklavya));
        allSampleData.add(eklavya);

        CategoryItemModel ieee = new CategoryItemModel();
        ieee.setClubName("IEEE");
        ieee.setDisplayName("Techno Fun");
        ieee.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.ieee));
        allSampleData.add(ieee);

        CategoryItemModel mechnext = new CategoryItemModel();
        mechnext.setClubName("Mechnext");
        mechnext.setDisplayName("Mechanical");
        mechnext.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.mechnext));
        allSampleData.add(mechnext);

        CategoryItemModel microbird = new CategoryItemModel();
        microbird.setClubName("Microbird");
        microbird.setDisplayName("Electronics");
        microbird.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.micobird));
        allSampleData.add(microbird);

        CategoryItemModel natraja = new CategoryItemModel();
        natraja.setClubName("Natraja");
        natraja.setDisplayName("Dance");
        natraja.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.natraja));
        allSampleData.add(natraja);

        CategoryItemModel sae = new CategoryItemModel();
        sae.setClubName("SAE/BAJA");
        sae.setDisplayName("Automobiles");
        sae.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.sae));
        allSampleData.add(sae);

        CategoryItemModel samarpan = new CategoryItemModel();
        samarpan.setClubName("Samarpan");
        samarpan.setDisplayName("Electrical");
        samarpan.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.samarpan));
        allSampleData.add(samarpan);

        CategoryItemModel srijan = new CategoryItemModel();
        srijan.setClubName("Srijan");
        srijan.setDisplayName("Arts");
        srijan.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.srijan));
        allSampleData.add(srijan);

        CategoryItemModel tarannum = new CategoryItemModel();
        tarannum.setClubName("Taranuum");
        tarannum.setDisplayName("Music");
        tarannum.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.tarannum));
        allSampleData.add(tarannum);

        CategoryItemModel vivekanand = new CategoryItemModel();
        vivekanand.setClubName("Vivekanand Manch");
        vivekanand.setDisplayName("Vivekanand Manch");
        vivekanand.setImage(BitmapFactory.decodeResource(ContentActivity.this.getResources(), R.raw.vivekanand));
        allSampleData.add(vivekanand);
    }

    private void addBottomDots(int currentPage) {
        dots = new TextView[3];

        int[] colorsActive = getResources().getIntArray(R.array.array_dot_active);
        int[] colorsInactive = getResources().getIntArray(R.array.array_dot_inactive);


        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInactive[currentPage]);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorsActive[currentPage]);
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }


    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            Toast.makeText(this, "hi", Toast.LENGTH_SHORT).show();
            drawer.closeDrawer(GravityCompat.START, true);
        } else {
            if (phoneNumber != null) {
                startActivity(new Intent(ContentActivity.this, UserLoginActivity.class)
                        .putExtra("closeApp", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            } else {
                startActivity(new Intent(ContentActivity.this, UserLoginActivity.class)
                        .putExtra("logout", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }

        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        item.setChecked(false);

        switch (id) {
            case R.id.nav_home:
                //handle home case
                break;
            case R.id.nav_profile:
                SharedPreferences prefs = getSharedPreferences(getResources().getString(R.string.sharedPrefName), MODE_PRIVATE);
                String restoredText = prefs.getString("Phone", null);
                if (restoredText == null) {
                    startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                } else {
                    startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                }
                break;
            case R.id.nav_tickets:
                startActivity(new Intent(ContentActivity.this, Tickets.class));
                break;
            case R.id.nav_xunbao:
                //TODO
                //pass intent to activity with tab layout with 2 tabs
                //first for trending among all users using firebase analytics
                //second for trending among facebook friends
                //currently xunbao.. remove it later
                startActivity(new Intent(ContentActivity.this, XunbaoActivity.class));
                break;
            case R.id.nav_culmyca:
                //TODO
                //Culmyca times here
                break;
            case R.id.nav_about:
                //TODO
                //display about fest and college
                break;
            case R.id.nav_logout:
                SharedPreferences preferences = getSharedPreferences(getResources().getString(R.string.sharedPrefName), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();
                if (AccessToken.getCurrentAccessToken() != null) {


                    new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/permissions/", null, HttpMethod.DELETE, new GraphRequest
                            .Callback() {
                        @Override
                        public void onCompleted(GraphResponse graphResponse) {

                            LoginManager.getInstance().logOut();
                            Toast.makeText(getApplicationContext(), "fb logout ho gya", Toast.LENGTH_SHORT).show();

                        }
                    }).executeAsync();
                }
                startActivity(new Intent(getApplicationContext(), UserLoginActivity.class)
                        .putExtra("logout", true)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
                break;
            case R.id.nav_sponsors:
                //TODO
                //add sponsors
                break;
            case R.id.nav_share:
                String msg = "Install the elements culmyca app to stay updated about the latest events. Follow the link: ";
                shareTextMessage(msg);
                break;
            case R.id.nav_bug:
                String to = "manantechnosurge@gmail.com";
                String subject = "Bug Found";
                String messg = "I found a bug!\n";
                sendEmailBug(to, subject, messg);
                break;
            case R.id.nav_dev:
                //TODO
                //show developers
                break;
            case R.id.nav_location:
                startActivity(new Intent(ContentActivity.this, MapsActivity.class));
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void shareTextMessage(String msg) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, msg);
        startActivity(i);
    }

    private void sendEmailBug(String to, String subject, String msg) {

        Uri uri = Uri.parse("mailto:")
                .buildUpon()
                .appendQueryParameter("subject", subject)
                .appendQueryParameter("body", msg)
                .build();

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
        startActivity(Intent.createChooser(emailIntent, "Choose an Email client :"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(nav_view != null){
            nav_view.setCheckedItem(R.id.nav_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(nav_view != null){
            nav_view.setCheckedItem(R.id.nav_home);
        }
    }
}
