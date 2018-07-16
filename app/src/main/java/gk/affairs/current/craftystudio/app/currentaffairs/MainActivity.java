package gk.affairs.current.craftystudio.app.currentaffairs;

import android.app.DatePickerDialog;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.PurchaseState;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.crashlytics.android.answers.PurchaseEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.NativeAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONArray;
import org.json.JSONObject;

import io.fabric.sdk.android.Fabric;

import java.io.UnsupportedEncodingException;
import java.text.BreakIterator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import utils.AdsSubscriptionManager;
import utils.AppRater;
import utils.CurrentAffairsAdapter;
import utils.FirebaseHelper;
import utils.JsonParser;
import utils.News;
import utils.SettingManager;
import utils.SqlDatabaseHelper;
import utils.VolleyManager;
import utils.ZoomOutPageTransformer;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TextToSpeech.OnInitListener {

    public static final int articleCount = 15;

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    ArrayList<News> newsArrayList = new ArrayList<>();

    private TextToSpeech tts;
    int currentReadingArticle = 0;

    boolean isPushNotification, isShareArticle;
    private boolean isLoading = false;
    ProgressDialog pDialog;


    FragmentTransaction transaction;
    CurrentAffairsCardFragment currentAffairsCardFragment;
    CurrentAffairListFragment currentAffairListFragment;
    private int pageNumber = 2;


    BillingProcessor bp;
    final String SUBSCRIPTION_ID = "ads_free";


    boolean isCardView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        if (SettingManager.getNightMode(this)) {
            setTheme(R.style.DarkActivityTheme);
        }
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pDialog = new ProgressDialog(this);

        showLoadingDialog("Loading...");


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setCardFragment();

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        mPager = findViewById(R.id.mainActivity_viewpager);


        isCardView = !SettingManager.getMode(this);

        if (!SettingManager.getMode(this)) {

            /*Start card  fragment*/
            currentAffairsCardFragment = CurrentAffairsCardFragment.newInstance(-1, "");

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.mainActivity_frameLayout, currentAffairsCardFragment);

            transaction.commit();

            isCardView = true;

        } else {

            /*start List Fragment*/

            FrameLayout frameLayout = findViewById(R.id.mainActivity_frameLayout);

            CurrentAffairListFragment currentAffairListFragment = CurrentAffairListFragment.newInstance(-1);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.mainActivity_frameLayout, currentAffairListFragment);

            transaction.commit();

            isCardView = false;

        }


        initializeViewPager();


        openDynamicLink();

        tts = new TextToSpeech(this, this);

        /*For push notification*/
        FirebaseMessaging.getInstance().subscribeToTopic("subscribed");
        FirebaseMessaging.getInstance().subscribeToTopic("wordPressNews");

        try {
            AppRater.app_launched(this);

            if (AdsSubscriptionManager.getSubscription(this)) {
                getSupportActionBar().setSubtitle("Subscribed (Ads Free)");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            Intent intent = getIntent();
            News news = (News) intent.getSerializableExtra("news");

            if (news != null) {
                isPushNotification = intent.getBooleanExtra("pushNotification", false);

                if (news.getContentType() == 1) {
                    //fetchCurrentAffairs(news.getNewsID());
                    if (isCardView) {
                        if (currentAffairsCardFragment != null) {
                            currentAffairsCardFragment.openArticleById(news.getNewsID());
                        }
                    } else {
                        Intent intentCurrentFeed = new Intent(MainActivity.this, CurrentAffairsFeedActivity.class);
                        intentCurrentFeed.putExtra("news", news);
                        intentCurrentFeed.putExtra("isPushNotification", true);
                        startActivity(intentCurrentFeed);
                    }
                } else if (news.getContentType() == 0) {
                    //downloadNewsByID(news.getNewsID());
                    if (isCardView) {
                        if (currentAffairsCardFragment != null) {
                            currentAffairsCardFragment.openFirebaseArticleById(news.getNewsID());
                        }
                    } else {
                        Intent intentCurrentFeed = new Intent(MainActivity.this, CurrentAffairsFeedActivity.class);
                        intentCurrentFeed.putExtra("news", news);
                        intentCurrentFeed.putExtra("isPushNotification", true);
                        startActivity(intentCurrentFeed);

                    }
                }
                Toast.makeText(this, "Notification - " + news.getNewsTitle(), Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        MobileAds.initialize(getApplicationContext(), "ca-app-pub-8455191357100024~2021354499");


        initializeInAppBilling();


    }

    private void setCardFragment() {

        if (isCardView) {

            FrameLayout frameLayout = findViewById(R.id.mainActivity_frameLayout);

            CurrentAffairListFragment currentAffairListFragment = CurrentAffairListFragment.newInstance(-1);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.mainActivity_frameLayout, currentAffairListFragment);

            transaction.commit();

            isCardView = false;

            SettingManager.setMode(this, true);


        } else {
            FrameLayout frameLayout = findViewById(R.id.mainActivity_frameLayout);

            currentAffairsCardFragment = CurrentAffairsCardFragment.newInstance(-1, "");

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.mainActivity_frameLayout, currentAffairsCardFragment);
            transaction.commit();

            isCardView = true;

            SettingManager.setMode(this, true);

        }


    }

    private void openDynamicLink() {
        //downloadNews();

        fetchCurrentAffairs();
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                            Log.d("DeepLink", "onSuccess: " + deepLink);

                            String contentType = deepLink.getQueryParameter("contentType");
                            String articleID = "", newsID;

                            if (contentType != null) {

                                if (contentType.equalsIgnoreCase("1")) {

                                    articleID = deepLink.getQueryParameter("articleID");
                                    //fetchCurrentAffairs(articleID);
                                    if (isCardView) {
                                        if (currentAffairsCardFragment != null) {
                                            currentAffairsCardFragment.openArticleById(articleID);
                                        }
                                    } else {

                                        News currentAffairs = new News();
                                        currentAffairs.setNewsID(articleID);
                                        currentAffairs.setContentType(1);

                                        Intent intent = new Intent(MainActivity.this, CurrentAffairsFeedActivity.class);
                                        intent.putExtra("news", currentAffairs);
                                        intent.putExtra("isPushNotification", true);
                                        startActivity(intent);

                                    }

                                }


                            } else {

                                newsID = deepLink.getQueryParameter("newsID");

                                isShareArticle = true;
                                //downloadNewsByID(newsID);

                                if (isCardView) {
                                    if (currentAffairsCardFragment != null) {
                                        currentAffairsCardFragment.openFirebaseArticleById(newsID);
                                    }
                                } else {

                                    News currentAffairs = new News();
                                    currentAffairs.setNewsID(newsID);
                                    currentAffairs.setContentType(0);

                                    Intent intent = new Intent(MainActivity.this, CurrentAffairsFeedActivity.class);
                                    intent.putExtra("news", currentAffairs);
                                    intent.putExtra("isPushNotification", true);
                                    startActivity(intent);

                                }


                            }


                            try {
                                Answers.getInstance().logCustom(new CustomEvent("User via Dynamic link")
                                        .putCustomAttribute("News id", articleID)
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }


                        } else {
                            Log.d("editorial", "getInvitation: no deep link found.");


                        }

                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("editorial", "getInvitation: no deep link found.");


                    }
                });


    }

    private void initializeInAppBilling() {

        try {
            bp = new BillingProcessor(this,
                    "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApIC2hlPjxdNNsRDjL3P2HAxTFQyUJHcUIDYh2xxjTtCI1pQDGYrfZrjzVxzBrVlJG2KnzOtt2uVAq2btDrmsnEMNSIzUry8cydCNnJPhRBPnLsIFYZ4RlH2nQqPLmJxnrAUhXwddy5fW3wsvN2h04orAJxA1eqN/PIzN0IxzxDwwuW0ykc9s7op/Pk3Gi5rw+RZhAunRgAJU55Xic+lgPbLqTAwU+NqjVgNFnkR6iLjDpdDdsxkPHTDEhklwvQj/YHDENBBX7a9U8vBOua8YUJcXsCkbsOSf3t+zQkUDda/q42Pfm0OwzIEGsKYroQPROWbXKEmNEFy/vyuyn+zVxQIDAQAB",
                    new BillingProcessor.IBillingHandler() {
                        @Override
                        public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
                            //Toast.makeText(EditorialListWithNavActivity.this, "product purchased - " + productId, Toast.LENGTH_SHORT).show();
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Thank you for Subscription");
                            builder.setMessage("We appreciate your contribution by going ads free.\n\nAds will be removed when you open the app next time.");
                            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                            builder.show();

                            AdsSubscriptionManager.setSubscription(MainActivity.this, true);

                            Answers.getInstance().logPurchase(new PurchaseEvent().putItemType("Subscription").putSuccess(true));

                        }

                        @Override
                        public void onPurchaseHistoryRestored() {

                        }

                        @Override
                        public void onBillingError(int errorCode, @Nullable Throwable error) {

                        }

                        @Override
                        public void onBillingInitialized() {
                            bp.loadOwnedPurchasesFromGoogle();

                            try {
                                TransactionDetails transactionDetails = bp.getSubscriptionTransactionDetails(SUBSCRIPTION_ID);

                                if (transactionDetails == null) {
                                    AdsSubscriptionManager.setSubscription(MainActivity.this, false);
                                    return;
                                }

                                if (transactionDetails.purchaseInfo.purchaseData.autoRenewing) {
                                    if (!AdsSubscriptionManager.getSubscription(MainActivity.this)) {
                                        AdsSubscriptionManager.setSubscription(MainActivity.this, true);
                                        Answers.getInstance().logPurchase(new PurchaseEvent().putItemType("Subscription").putSuccess(true));
                                        recreate();
                                    }

                                    Answers.getInstance().logCustom(new CustomEvent("Subscribed user enter"));

                                } else {
                                    AdsSubscriptionManager.setSubscription(MainActivity.this, false);
                                }


                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void downloadNewsByID(String newsID) {
        new FirebaseHelper().fetchNewsByID(newsID, new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {
                if (newsArrayList == null) {
                    return;
                }

                if (newsArrayList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No News Found", Toast.LENGTH_SHORT).show();
                }
                MainActivity.this.newsArrayList.add(0, newsArrayList.get(0));


                mPagerAdapter.notifyDataSetChanged();
                mPager.setCurrentItem(0);
                mPager.setAdapter(mPagerAdapter);

                hideLoadingDialog();

                //downloadNews();

            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });
    }

    private void downloadNews() {

        isLoading = true;
        new FirebaseHelper().fetchNewsList(articleCount, new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {

                MainActivity.this.newsArrayList.addAll(newsArrayList);
                mPagerAdapter.notifyDataSetChanged();
                isLoading = false;
                if (isShareArticle || isPushNotification) {
                    checkRepeatedArticle();
                }

                hideLoadingDialog();


                addNativeAds();

            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });

    }

    private void downloadMoreNews() {

        isLoading = true;

        new FirebaseHelper().fetchNewsList(articleCount, newsArrayList.get(newsArrayList.size() - 1).getTimeInMillis(), new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {
                MainActivity.this.newsArrayList.addAll(newsArrayList);
                mPagerAdapter.notifyDataSetChanged();
                isLoading = false;
            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });

        try {
            Answers.getInstance().logCustom(new CustomEvent("More data is loaded")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void fetchCurrentAffairs() {


        final String url = "http://aspirantworld.in/wp-json/wp/v2/posts?categories=3,16";

       /*
       loadCache(url);*/


        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        Log.d(TAG, "onResponse: " + response);

                        ArrayList<News> arrayList;

                        arrayList = new JsonParser().parseCurrentAffairsList(response);


                        for (News news : arrayList) {

                            newsArrayList.add(news);
                        }

                        addNativeAds();
                        //addReadStatus();

                        mPagerAdapter.notifyDataSetChanged();


                        //showRefreshing(false);

                        hideLoadingDialog();


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.d(TAG, "onErrorResponse: " + error);
                        loadCache(url);

                    }
                });


        jsonArrayRequest.setShouldCache(true);

        VolleyManager.getInstance().addToRequestQueue(jsonArrayRequest, "Group request");

    }


    private void loadCache(String url) {

        Cache cache = VolleyManager.getInstance().getRequestQueue().getCache();

        Cache.Entry entry = cache.get(url);
        if (entry != null) {
            //Cache data available.
            try {

                String response = new String(entry.data, "UTF-8");


                ArrayList<News> arrayList;

                arrayList = new JsonParser().parseCurrentAffairsList(response);


                for (News news : arrayList) {

                    newsArrayList.add(news);
                }

                addNativeAds();
                //addReadStatus();

                mPagerAdapter.notifyDataSetChanged();


                //showRefreshing(false);

                hideLoadingDialog();

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            // Cache data not exist.
        }

    }


    public void fetchMoreCurrentAffairs() {


        String url = "http://aspirantworld.in/wp-json/wp/v2/posts?categories=3,16&page=" + pageNumber;

       /*
       loadCache(url);*/


        isLoading = true;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        Log.d(TAG, "onResponse: " + response);

                        ArrayList<News> arrayList;

                        arrayList = new JsonParser().parseCurrentAffairsList(response);

                        newsArrayList.addAll(arrayList);

                        addNativeAds();
                        //addReadStatus();

                        if (mPagerAdapter != null) {
                            mPagerAdapter.notifyDataSetChanged();
                        }


                        pageNumber++;

                        isLoading = false;


                        //showRefreshing(false);

                        hideLoadingDialog();


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.d(TAG, "onErrorResponse: " + error);
                        isLoading = false;

                    }
                });


        jsonArrayRequest.setShouldCache(true);

        VolleyManager.getInstance().addToRequestQueue(jsonArrayRequest, "Group request");

    }

    public void fetchCurrentAffairs(long sortDateMillis) {


        if (isCardView) {

            currentAffairsCardFragment = CurrentAffairsCardFragment.newInstance(sortDateMillis, "");

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.mainActivity_frameLayout, currentAffairsCardFragment);

            transaction.commit();

        } else {

            CurrentAffairListFragment currentAffairListFragment = CurrentAffairListFragment.newInstance(sortDateMillis);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.mainActivity_frameLayout, currentAffairListFragment);

            transaction.commit();

        }



/*

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        String afterDateString = dateFormat.format(sortDateMillis) + "T00:00:00";
        String beforeDateString = dateFormat.format((sortDateMillis + 86400000l)) + "T00:00:00";


        String url = "http://aspirantworld.in/wp-json/wp/v2/posts?categories=3,16&after=" + afterDateString + "&before=" + beforeDateString;

        isLoading = true;


        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {

                        Log.d(TAG, "onResponse: " + response);

                        ArrayList<News> arrayList;

                        arrayList = new JsonParser().parseCurrentAffairsList(response);

                        newsArrayList.clear();

                        newsArrayList.addAll(arrayList);

                        addNativeAds();
                        //addReadStatus();

                        mPagerAdapter.notifyDataSetChanged();

                        mPager.setAdapter(mPagerAdapter);

                        isLoading = false;

                        //showRefreshing(false);

                        hideLoadingDialog();


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.d(TAG, "onErrorResponse: " + error);

                    }
                });


        jsonArrayRequest.setShouldCache(true);

        VolleyManager.getInstance().addToRequestQueue(jsonArrayRequest, "Group request");
*/

    }


    private void fetchCurrentAffairs(String articleID) {

        String url = "http://aspirantworld.in/wp-json/wp/v2/posts/" + articleID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        News currentAffairs = new JsonParser().parseCurrentAffairs(response);

                        MainActivity.this.newsArrayList.add(0, currentAffairs);


                        mPagerAdapter.notifyDataSetChanged();
                        mPager.setCurrentItem(0);
                        mPager.setAdapter(mPagerAdapter);

                        hideLoadingDialog();


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        Log.d(TAG, "onErrorResponse: " + error);

                    }
                });


        jsonObjectRequest.setShouldCache(true);

        VolleyManager.getInstance().addToRequestQueue(jsonObjectRequest, "Group request");

    }


    private void checkRepeatedArticle() {
        if (!newsArrayList.isEmpty()) {
            News firstNews = newsArrayList.get(0);
            for (int i = 1; i < newsArrayList.size(); i++) {
                News news = newsArrayList.get(i);
                if (firstNews.getNewsID().equalsIgnoreCase(news.getNewsID())) {
                    newsArrayList.remove(i);
                    break;
                }

            }

            mPagerAdapter.notifyDataSetChanged();
        }
    }

    public void addNativeAds() {

        if (!AdsSubscriptionManager.checkShowAds(this)) {
            return;
        }

        for (int i = 1; i < newsArrayList.size(); i = i + NewsFragment.adsCount) {
            if (newsArrayList.get(i).getNativeAd() == null) {

                NativeAd nativeAd = new NativeAd(this, "1641103999319593_1641104795986180");
                nativeAd.setAdListener(new AdListener() {

                    @Override
                    public void onError(Ad ad, AdError adError) {
                        Log.d("TAG", "onError: " + adError.getErrorMessage());

                        try {
                            Answers.getInstance().logCustom(new CustomEvent("Ad failed").putCustomAttribute("message", adError.getErrorMessage()).putCustomAttribute("Placement", "banner"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }

                    @Override
                    public void onAdLoaded(Ad ad) {

                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }


                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }
                });

                // Initiate a request to load an ad.
                nativeAd.loadAd();

                newsArrayList.get(i).setNativeAd(nativeAd);

            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_date_selection) {
            onSortByDateClick();
            return true;
        } else if (id == R.id.action_date) {
            onSortByDateClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onSortByDateClick() {
        Calendar c = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                //Toast.makeText(EditorialListWithNavActivity.this, "Date selected +" + dayOfMonth + " - " + month, Toast.LENGTH_SHORT).show();

                String str_date = dayOfMonth + "-" + (month + 1) + "-" + year;
                DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
                try {
                    Date date = (Date) formatter.parse(str_date);

                    long sortDateMillis = date.getTime();

                    fetchCurrentAffairs(sortDateMillis);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.getDatePicker().setMinDate(1519151400000l);
        datePickerDialog.show();

    }

    private void fetchNewsByDate(long sortDateMillis) {

        showLoadingDialog("Loading...");

        new FirebaseHelper().fetchNewsList(sortDateMillis, (sortDateMillis + 86400000), new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {
                if (isSuccesful) {

                    MainActivity.this.newsArrayList.clear();

                    MainActivity.this.newsArrayList.addAll(newsArrayList);

                    mPagerAdapter.notifyDataSetChanged();
                    mPager.setAdapter(mPagerAdapter);
                    hideLoadingDialog();

                } else {
                    Toast.makeText(MainActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                    hideLoadingDialog();
                }
            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });
    }

    private void onPurchaseClick() {

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Go Ads Free");
            builder.setMessage("Access the app without any ads by going ads free. Going ads free will be cheaper than Rs. 1/day or Rs. 29 / month.\n\nPlease make a small contribution and go ads free. \n\n For any query contact us at acraftystudio@gmail.com");
            builder.setPositiveButton("Go Ads Free", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (bp != null) {
                        bp.subscribe(MainActivity.this, SUBSCRIPTION_ID);

                        Answers.getInstance().logCustom(new CustomEvent("Subscription Flow").putCustomAttribute("Selection", "yes"));

                    }
                }
            });
            builder.setNegativeButton("Maybe Later", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Answers.getInstance().logCustom(new CustomEvent("Subscription Flow").putCustomAttribute("Selection", "No"));

                }
            });

            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {

            case R.id.nav_bookmark:
                onBookmarkClick();
                break;

            case R.id.nav_day_mode:
                onDayModeClick();
                break;

            case R.id.nav_night_mode:
                onNightModeClick();
                break;

            case R.id.nav_share:
                onShareClick();
                break;

            case R.id.nav_suggestion:
                onSuggestionClick();
                break;

            case R.id.nav_rate_us:
                onRateUsClick();
                break;

            case R.id.nav_text_size:
                onTextSizeClick();
                break;

            case R.id.nav_adsFree:
                onPurchaseClick();
                break;

            case R.id.nav_archive:
                openArchiveActivity();
                break;

            case R.id.nav_card_mode:

                onCardMode();
                break;

            case R.id.nav_list_mode:

                onListMode();
                break;

        }



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void onListMode() {
        SettingManager.setMode(this, true);
        recreate();
    }

    private void onCardMode() {
        SettingManager.setMode(this, false);
        recreate();
    }

    private void openArchiveActivity() {
        Intent intent = new Intent(this, ArchiveActivity.class);
        startActivity(intent);
    }


    public void onTextSizeClick() {

        final CharSequence sources[] = new CharSequence[]{"Small", "Medium", "Large", "Extra Large"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Text Size");
        builder.setItems(sources, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


                int size = 14;

                if (which == 0) {
                    size = 14;
                } else if (which == 1) {
                    size = 16;
                } else if (which == 2) {
                    size = 18;
                } else if (which == 3) {
                    size = 20;
                }


                SettingManager.setTextSize(MainActivity.this, size);

                mPager.setAdapter(mPagerAdapter);

            }
        });

        builder.show();

    }

    private void onNightModeClick() {
        SettingManager.setNightMode(MainActivity.this, true);
        recreate();
    }

    private void onDayModeClick() {
        SettingManager.setNightMode(MainActivity.this, false);
        recreate();
    }

    private void onBookmarkClick() {
        Intent intent = new Intent(this, BookMarkActivity.class);
        startActivity(intent);
    }

    private void initializeViewPager() {

        // Instantiate a ViewPager and a PagerAdapter.

        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        //mPager.setVisibility(View.VISIBLE);

        //change to zoom
        //mPager.setPageTransformer(true, new ZoomOutPageTransformer());

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                if (!isLoading) {
                    if ((newsArrayList.size() - position) < 3) {
                        //downloadMoreNews();
                        fetchMoreCurrentAffairs();

                        Toast.makeText(MainActivity.this, "Loading more article", Toast.LENGTH_SHORT).show();
                    }
                }


            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, this.getResources().getDisplayMetrics());

        mPager.setClipToPadding(false);
        mPager.setPadding(16 * px, 8 * px, 16 * px, 8 * px);
        mPager.setPageMargin(8 * px);

    }

    public void onTTSReaderClick(View view) {


        if (tts.isSpeaking()) {
            speakOutWord("");
            return;
        }


        String articleText = "";
        News news;
        if (currentAffairsCardFragment != null) {
            news = currentAffairsCardFragment.getCurrentNews();
        } else {
            return;
        }

        String descriptionTextView = news.getNewsDescription();

        if (Build.VERSION.SDK_INT >= 24) {
            articleText = Html.fromHtml(descriptionTextView, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            articleText = Html.fromHtml(descriptionTextView).toString();
        }


        currentReadingArticle = mPager.getCurrentItem();
        String string = news.getNewsTitle() + ". " + articleText;

        speakOutWord(string);

        try {
            Answers.getInstance().logCustom(new CustomEvent("TTS Reader")
                    .putCustomAttribute("News id", news.getNewsTitle()));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void onSuggestionClick() {
        Intent intent = new Intent(Intent.ACTION_SEND);

        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"acraftystudio@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Suggestion for Current affairs app");
        intent.putExtra(Intent.EXTRA_TEXT, "Your suggestion here \n");

        intent.setType("message/rfc822");

        startActivity(Intent.createChooser(intent, "Select Email app"));

    }

    private void onRateUsClick() {
        try {
            String link = "https://play.google.com/store/apps/details?id=" + this.getPackageName();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        } catch (Exception e) {

        }
    }

    private void onShareClick() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String link = "https://play.google.com/store/apps/details?id=" + this.getPackageName();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Download Current affairs app");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            speakOutWord(".");
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            Locale locale = new Locale("en", "IN");
            int availability = tts.isLanguageAvailable(locale);
            int result = 0;
            switch (availability) {
                case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE: {
                    result = tts.setLanguage(locale);
                    tts.setPitch(0.9f);
                    tts.setSpeechRate(0.9f);
                    break;
                }
                case TextToSpeech.LANG_NOT_SUPPORTED:
                case TextToSpeech.LANG_MISSING_DATA:
                case TextToSpeech.LANG_AVAILABLE: {
                    result = tts.setLanguage(Locale.US);
                    tts.setPitch(0.9f);
                    tts.setSpeechRate(0.9f);
                }
            }


            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                // btnSpeak.setEnabled(true);
                speakOutWord("");
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }

    }

    private void speakOutWord(String speakWord) {

        try {

            tts.speak(speakWord, TextToSpeech.QUEUE_FLUSH, null);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void showLoadingDialog(String message) {
        pDialog.setMessage(message);
        pDialog.show();
    }

    public void hideLoadingDialog() {
        try {
            pDialog.hide();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            return NewsFragment.newInstance(newsArrayList.get(position), position + 1);
        }

        @Override
        public int getCount() {
            return newsArrayList.size();
        }
    }


}
