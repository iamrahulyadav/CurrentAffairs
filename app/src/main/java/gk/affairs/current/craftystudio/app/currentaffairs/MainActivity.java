package gk.affairs.current.craftystudio.app.currentaffairs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.messaging.FirebaseMessaging;

import io.fabric.sdk.android.Fabric;
import java.util.ArrayList;
import java.util.Locale;

import utils.FirebaseHelper;
import utils.News;
import utils.SettingManager;
import utils.SqlDatabaseHelper;
import utils.ZoomOutPageTransformer;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, TextToSpeech.OnInitListener {

    private final int articleCount=15;

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    ArrayList<News> newsArrayList = new ArrayList<>();

    private TextToSpeech tts;
    int currentReadingArticle = 0;

    boolean isPushNotification, isShareArticle;
    private boolean isLoading = false;

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


              /*  News news = new News();
                news.setNewsTitle("Titlr njkfsnaunnfd asmnn  kn dbcsa  bjn biucbsdjh sdbkj mnds jn sdkd");
                news.setNewsDescription("bh bi b iuui iu bkjbiubiu fds iuuid fsid fi  uifsa gfIU GFIUCSD FICUD IUS IDU UIFIUS IFDSUASDU  HUIFHUFHU CHFUDH HFDSBJ DFD FUIF hfuhiuhiv huiu ih sdihsfhuhiushiuhs ufh uhiuhu iusdiuf uh iuhiuhfhshuifdh   uigiufdgfig gdiugiufdgiuhf iu hi hihfudhiufhiuhf");
                news.setNewsImageURL("http://www.assignmentpoint.com/wp-content/uploads/2017/05/About-Electric-Current.jpg");

                news.setTimeInMillis(System.currentTimeMillis());
                news.setNewsID(String.valueOf(news.getTimeInMillis()));
                news.setNewsSource("Source");
                news.setNewsSourceLink("Source link");
                news.setNewsTopic("Topic");

                new FirebaseHelper().insertNews(news, new FirebaseHelper.NewsListener() {
                    @Override
                    public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {

                    }

                    @Override
                    public void onNewsInsert(boolean isSuccessful) {

                        Toast.makeText(MainActivity.this, "Forebase inserted - " + isSuccessful, Toast.LENGTH_SHORT).show();
                    }
                });*/

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
        initializeViewPager();


        openDynamicLink();

        tts = new TextToSpeech(this, this);

        /*For push notification*/
        FirebaseMessaging.getInstance().subscribeToTopic("subscribed");


        try {
            Intent intent = getIntent();
            News news = (News) intent.getSerializableExtra("news");

            isPushNotification = intent.getBooleanExtra("pushNotification", false);

            downloadNewsByID(news.getNewsID());

            Toast.makeText(this, "Notification - " + news.getNewsTitle(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    private void openDynamicLink() {
        downloadNews();

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

                            String newsID = deepLink.getQueryParameter("newsID");

                            isShareArticle = true;
                            downloadNewsByID(newsID);

                            try {
                                Answers.getInstance().logCustom(new CustomEvent("User via Dynamic link")
                                        .putCustomAttribute("News id", newsID)
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

    private void downloadNewsByID(String newsID) {
        new FirebaseHelper().fetchNewsByID(newsID, new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {
                if (newsArrayList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No News Found", Toast.LENGTH_SHORT).show();
                }
                MainActivity.this.newsArrayList.add(0, newsArrayList.get(0));


                mPagerAdapter.notifyDataSetChanged();
                mPager.setCurrentItem(0);
                mPager.setAdapter(mPagerAdapter);


                downloadNews();
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

    private void checkRepeatedArticle() {
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
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_bookmark) {
            onBookmarkClick();
        } else if (id == R.id.nav_day_mode) {
            onDayModeClick();

        } else if (id == R.id.nav_night_mode) {
            onNightModeClick();
        } else if (id == R.id.nav_share) {
            onShareClick();
        } else if (id == R.id.nav_suggestion) {
            onSuggestionClick();
        } else if (id == R.id.nav_rate_us) {
            onRateUsClick();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                        downloadMoreNews();
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
        mPager.setPageMargin(10 * px);

    }


    public void onTTSReaderClick(View view) {

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {

            }

            @Override
            public void onDone(String s) {


            }

            @Override
            public void onError(String s) {

            }
        });

        if (tts.isSpeaking()) {
            speakOutWord(".");
            return;
        }

        currentReadingArticle = mPager.getCurrentItem();
        String string = newsArrayList.get(currentReadingArticle).getNewsTitle() + ". " + newsArrayList.get(currentReadingArticle).getNewsDescription().replaceAll("<br>",".");

        speakOutWord(string);

        try {
            Answers.getInstance().logCustom(new CustomEvent("TTS Reader")
                    .putCustomAttribute("News id", newsArrayList.get(currentReadingArticle).getNewsTitle()));
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
