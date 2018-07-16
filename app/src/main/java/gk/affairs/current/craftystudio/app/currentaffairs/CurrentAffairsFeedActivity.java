package gk.affairs.current.craftystudio.app.currentaffairs;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdView;
import com.facebook.ads.NativeAdViewAttributes;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import utils.AdsSubscriptionManager;
import utils.FirebaseHelper;
import utils.JsonParser;
import utils.News;
import utils.SettingManager;
import utils.SqlDatabaseHelper;
import utils.Translation;
import utils.VolleyManager;

import static android.content.ContentValues.TAG;

public class CurrentAffairsFeedActivity extends AppCompatActivity implements
        TextToSpeech.OnInitListener {


    TextView headingTextView, dateTextView, sourceTextView, contentTextView, sourceLinkTextView, articleTypeTextView, tagTextView;

    NetworkImageView imageView;

    News currentAffairs;

    TextView translateText;
    private BottomSheetBehavior mBottomSheetBehavior;
    public String selectedWord = "null";
    WebView meaningWebView;

    private boolean muteVoice;


    private boolean isPushNotification;

    boolean firebaseDatabaseArticle;

    private TextToSpeech textToSpeech;
    int voiceReaderChunk = 0;
    private boolean isShareArticle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SettingManager.getNightMode(this)) {
            setTheme(R.style.DarkActivityTheme);
        }
        setContentView(R.layout.activity_current_affairs_feed);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


            }
        });


        currentAffairs = (News) getIntent().getSerializableExtra("news");
        isPushNotification = getIntent().getBooleanExtra("isPushNotification", false);


        headingTextView = findViewById(R.id.currentAffairsFeed_heading_textview);
        dateTextView = findViewById(R.id.currentAffairsFeed_date_textview);
        sourceTextView = findViewById(R.id.currentAffairsFeed_source_textview);
        contentTextView = findViewById(R.id.currentAffairsFeed_content_textview);
        sourceLinkTextView = findViewById(R.id.currentAffairsFeed_sourceLink_textView);
        tagTextView = findViewById(R.id.currentAffairsFeed_tag_textview);

        translateText = (TextView) findViewById(R.id.editorial_feed_cardview_textview);
        imageView = findViewById(R.id.currentAffairsFeed_imageView);


        initializeTTS();

        initializeBottomSheet();
        initializeMeaningWebView();


        if (isPushNotification) {
            if (currentAffairs.getContentType() == 1) {
                fetchCurrentAffairs(currentAffairs.getNewsID());
            } else {
                downloadNewsByID(currentAffairs.getNewsID());
            }
        } else {
            initializeActivity();
        }


        contentTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                getSelectedWord(250);
                return false;
            }
        });




        textToSpeech = new TextToSpeech(this, this);
        muteVoice = SettingManager.getMuteVoice(this);

        /*Mute button text*/
        Button button = (Button) findViewById(R.id.editorial_bottomsheet_audio_button);
        if (muteVoice) {
            button.setBackgroundResource(R.drawable.ic_action_mute_off);
        } else {
            button.setBackgroundResource(R.drawable.ic_action_mute_on);
        }


        if (AdsSubscriptionManager.checkShowAds(this)) {
            initializeNativeAds(true);
            //initializeTopNativeAds(true);
            initializeTopBannerAds(true);

            initializeBottomSheetAd(true);
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_currentaffair_feed, menu);

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_bookmark) {
            onBookMarkClick();
            return true;
        } else if (id == R.id.action_share) {
            onShareClick();
            return true;
        } else if (id == R.id.action_tts_reader) {
            onTTSReaderClick();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onBackPressed() {


        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {

            super.onBackPressed();

        }


    }


    private void fetchCurrentAffairs(String articleID) {

        String url = "http://aspirantworld.in/wp-json/wp/v2/posts/" + articleID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        currentAffairs = new JsonParser().parseCurrentAffairs(response);

                        initializeActivity();

                        //hideLoadingDialog();

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

    private void downloadNewsByID(String newsID) {
        new FirebaseHelper().fetchNewsByID(newsID, new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {

                try {
                    if (newsArrayList == null) {
                        return;
                    }

                    if (newsArrayList.isEmpty()) {
                        Toast.makeText(CurrentAffairsFeedActivity.this, "No News Found", Toast.LENGTH_SHORT).show();
                    }

                    currentAffairs = newsArrayList.get(0);

                    initializeActivity();


                    //hideLoadingDialog();

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });
    }


    private void initializeActivity() {


        headingTextView.setText(currentAffairs.getNewsTitle());
        dateTextView.setText("• " + currentAffairs.getDate());
        sourceTextView.setText(currentAffairs.getCategory());

        sourceLinkTextView.setText("Website Link : " + currentAffairs.getNewsSourceLink());


        tagTextView.setText(currentAffairs.getTag());

        int textSize = SettingManager.getTextSize(this);

        contentTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize+2);
        headingTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize + 4);


        if (Build.VERSION.SDK_INT >= 24) {
            contentTextView.setText(Html.fromHtml(currentAffairs.getNewsDescription(), Html.FROM_HTML_MODE_LEGACY));
        } else {
            contentTextView.setText(Html.fromHtml(currentAffairs.getNewsDescription()));
        }


        try {
            if (currentAffairs.getNewsImageURL() != null) {
                if (!currentAffairs.getNewsImageURL().isEmpty()) {

                    ImageLoader imageLoader = VolleyManager.getInstance().getImageLoader();


                    imageView.setImageUrl(currentAffairs.getNewsImageURL(), imageLoader);
                } else {
                    imageView.setVisibility(View.GONE);
                }

            } else {
                imageView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        headingTextView.requestFocus();


    }


    private void initializeTTS() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {

                    Locale locale = new Locale("en", "IN");
                    int availability = textToSpeech.isLanguageAvailable(locale);
                    int result = 0;
                    switch (availability) {
                        case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                        case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE: {
                            result = textToSpeech.setLanguage(locale);
                            textToSpeech.setPitch(0.9f);
                            textToSpeech.setSpeechRate(SettingManager.getVoiceReaderSpeed(CurrentAffairsFeedActivity.this));
                            break;
                        }
                        case TextToSpeech.LANG_NOT_SUPPORTED:
                        case TextToSpeech.LANG_MISSING_DATA:
                        case TextToSpeech.LANG_AVAILABLE: {
                            result = textToSpeech.setLanguage(Locale.US);
                            textToSpeech.setPitch(0.9f);
                            textToSpeech.setSpeechRate(SettingManager.getVoiceReaderSpeed(CurrentAffairsFeedActivity.this));
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
        });

    }

    private void speakOutWord(String speakWord) {

        try {
            if (!muteVoice) {
                if (Build.VERSION.SDK_INT > 18) {
                }

                textToSpeech.speak(speakWord, TextToSpeech.QUEUE_FLUSH, null);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initializeBottomSheet() {

        View bottomSheet = findViewById(R.id.editorial_activity_bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mBottomSheetBehavior.setHideable(false);
        Button button = (Button) findViewById(R.id.editorial_bottomsheet_audio_button);

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback()

        {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {


                    // loadWebview(selectedWord);

                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });


        translateText = findViewById(R.id.editorial_feed_cardview_textview);

    }


    public void initializeMeaningWebView() {
        meaningWebView = findViewById(R.id.editorial_bottomSheet_webview);

        meaningWebView.getSettings().setLoadsImagesAutomatically(false);

        meaningWebView.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                return shouldOverrideUrlLoading(url);
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return shouldOverrideUrlLoading(uri.toString());
            }

            private boolean shouldOverrideUrlLoading(final String url) {
                // Log.i(TAG, "shouldOverrideUrlLoading() URL : " + url);

                // Here put your code
                meaningWebView.loadUrl(url);

                return true; // Returning True means that application wants to leave the current WebView and handle the url itself, otherwise return false.
            }
        });

        meaningWebView.getSettings().setAppCacheEnabled(true);
        meaningWebView.getSettings().setAppCachePath(this.getCacheDir().getPath());
        meaningWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);


    }


    public void getSelectedWord(long timeDelay) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String string = contentTextView.getText().toString();
                try {
                    if (contentTextView.hasSelection()) {
                        selectedWord = string.substring(contentTextView.getSelectionStart(), contentTextView.getSelectionEnd()).trim();
                    }

                } catch (Exception e) {
                    selectedWord = "";
                }

                contentTextView.clearFocus();

                if (selectedWord == null) {
                    return;
                } else {
                    if (selectedWord.isEmpty()) {
                        return;
                    }
                }

                //Toast.makeText(NewsDescriptionActivity.this, "Selected - " + selectedWord, Toast.LENGTH_SHORT).show();

                loadWebview(selectedWord);

                translateText.setText(selectedWord);



                new Translation(selectedWord).fetchTranslation(new Translation.TranslateListener() {
                    @Override
                    public void onTranslation(Translation translation) {

                        if (translation.getWord().equalsIgnoreCase(selectedWord.trim())) {
                            translateText.setText(translation.getWord() + " = " + translation.wordTranslation);
                        }

                    }
                });


                speakOutWord(selectedWord);

                Answers.getInstance().logCustom(new CustomEvent("Word Meaning").putCustomAttribute("word", selectedWord));


            }
        }, timeDelay);

    }


    public void loadWebview(String mWord) {
        meaningWebView.loadUrl("http://www.dictionary.com/browse/" + mWord);
    }


    public void onMuteClick(View view) {

        if (muteVoice) {
            muteVoice = false;
            Toast.makeText(this, "Voice enabled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Voice disabled", Toast.LENGTH_SHORT).show();
            muteVoice = true;
        }
        SettingManager.setMuteVoice(CurrentAffairsFeedActivity.this, muteVoice);
        Button button = (Button) findViewById(R.id.editorial_bottomsheet_audio_button);
        if (muteVoice) {
            button.setBackgroundResource(R.drawable.ic_action_mute_off);
        } else {
            button.setBackgroundResource(R.drawable.ic_action_mute_on);
        }

    }


    private void onBookMarkClick() {

        new SqlDatabaseHelper(this).addSavedNews(currentAffairs);

        try {
            Toast.makeText(this, "Saved ", Toast.LENGTH_SHORT).show();

            Answers.getInstance().logCustom(new CustomEvent("Book mark click")
                    .putCustomAttribute("News id", currentAffairs.getNewsTitle()));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void onShareClick() {

        String appCode = "wt6q7.app.goo.gl";
        String appName = getString(R.string.app_name);
        String packageName = this.getPackageName();
        String imageUrl = currentAffairs.getNewsImageURL();


        String utmSource = "Android app";
        String utmCampaign = "app share";
        String utmMedium = "social";

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Creating link ...");
        pd.show();

        Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://goo.gl/WDXMaQ?newsID=1527744728325&articleID=" + currentAffairs.getNewsID() + "&contentType=1"))
                .setDynamicLinkDomain(appCode)
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder(packageName)
                                .build())
                .setSocialMetaTagParameters(
                        new DynamicLink.SocialMetaTagParameters.Builder()
                                .setTitle(currentAffairs.getNewsTitle())
                                .setDescription(appName)
                                .setImageUrl(Uri.parse(currentAffairs.getNewsImageURL()))
                                .build())
                .setGoogleAnalyticsParameters(
                        new DynamicLink.GoogleAnalyticsParameters.Builder()
                                .setSource(utmSource)
                                .setMedium(utmMedium)
                                .setCampaign(utmCampaign)
                                .build())
                .buildShortDynamicLink()
                .addOnCompleteListener(new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if (task.isSuccessful()) {
                            Uri shortLink = task.getResult().getShortLink();
                            if (pd.isShowing()) {
                                try {
                                    pd.dismiss();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            openShareDialog(shortLink);

                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        try {
                            Toast.makeText(CurrentAffairsFeedActivity.this, "Connection Failed! Try again later", Toast.LENGTH_SHORT).show();

                            pd.dismiss();
                        } catch (Exception exception) {
                            e.printStackTrace();
                        }
                    }
                });


    }

    private void openShareDialog(Uri shortLink) {
        try {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");

            //sharingIntent.putExtra(Intent.EXTRA_STREAM, newsMetaInfo.getNewsImageLocalPath());

            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Download the app and Start reading");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shortLink
                    + "\n" + currentAffairs.getNewsTitle()
                    + "\n\nRead full News at Current affairs app");
            startActivity(Intent.createChooser(sharingIntent, "Share News via"));


            Answers.getInstance().logCustom(new CustomEvent("share click")
                    .putCustomAttribute("News id", currentAffairs.getNewsTitle()));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onTTSReaderClick() {


        if (textToSpeech.isSpeaking()) {
            speakOutWord("");
            return;
        }


        String articleText = "";
        News news = currentAffairs;

        String descriptionTextView = news.getNewsDescription();

        if (Build.VERSION.SDK_INT >= 24) {
            articleText = Html.fromHtml(descriptionTextView, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            articleText = Html.fromHtml(descriptionTextView).toString();
        }


        String string = news.getNewsTitle() + ". " + articleText;

        speakOutWord(string);

        try {
            Answers.getInstance().logCustom(new CustomEvent("TTS Reader")
                    .putCustomAttribute("News id", news.getNewsTitle()));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void initializeNativeAds() {


        try {
            final AdView adView = new AdView(this);
            adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
            adView.setAdUnitId("ca-app-pub-8455191357100024/5329906436");


            AdRequest request = new AdRequest.Builder().build();
            adView.loadAd(request);

            adView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);


            adView.setAdListener(new AdListener() {

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);

                    try {
                        Answers.getInstance().logCustom(new CustomEvent("Ad failed to load")
                                .putCustomAttribute("Placement", "Feed native bottom").putCustomAttribute("errorType", i));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();

                    CardView cardView = findViewById(R.id.currentAffairsFeed_admob_cardView);
                    cardView.setVisibility(View.VISIBLE);

                    cardView.removeAllViews();
                    cardView.addView(adView);

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void initializeNativeAds(boolean isFacebook) {

        final NativeAd nativeAd = new NativeAd(this, "1641103999319593_1690643811032278");
        nativeAd.setAdListener(new com.facebook.ads.AdListener() {

            @Override
            public void onError(Ad ad, AdError error) {
                // Ad error callback
                initializeNativeAds();

                try {
                    Answers.getInstance().logCustom(new CustomEvent("Ad failed to load")
                            .putCustomAttribute("Placement", "Feed native bottom CA").putCustomAttribute("errorType", error.getErrorMessage()).putCustomAttribute("Source", "Facebook"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Ad loaded callback

                CardView linearLayout = (CardView) findViewById(R.id.currentAffairsFeed_facebook_cardView);
                linearLayout.setVisibility(View.VISIBLE);
                NativeAdViewAttributes viewAttributes = new NativeAdViewAttributes()
                        .setBackgroundColor(Color.parseColor("#28292e"))
                        .setTitleTextColor(Color.WHITE)
                        .setButtonTextColor(Color.WHITE)
                        .setDescriptionTextColor(Color.WHITE)
                        .setButtonColor(Color.parseColor("#F44336"));

                View adView = NativeAdView.render(CurrentAffairsFeedActivity.this, nativeAd, NativeAdView.Type.HEIGHT_400, viewAttributes);

                linearLayout.removeAllViews();
                linearLayout.addView(adView);

            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Ad impression logged callback
            }
        });

        // Request an ad
        nativeAd.loadAd();


    }

    public void initializeBottomSheetAd() {
        AdView mAdView = (AdView) findViewById(R.id.editorialFeed_bottomSheet_bannerAdview);
        mAdView.setVisibility(View.VISIBLE);

        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mAdView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(int i) {
                super.onAdFailedToLoad(i);
                try {
                    Answers.getInstance().logCustom(new CustomEvent("Ad failed to load")
                            .putCustomAttribute("Placement", "Bottom sheet").putCustomAttribute("errorType", i));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public void initializeBottomSheetAd(boolean isFacebook) {


        com.facebook.ads.AdView adView = new com.facebook.ads.AdView(this, "1641103999319593_1690651231031536", com.facebook.ads.AdSize.BANNER_HEIGHT_50);

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.editorialFeed_bottomSheet_adcontainer);
        linearLayout.setVisibility(View.VISIBLE);

        linearLayout.addView(adView);

        adView.loadAd();

        adView.setAdListener(new com.facebook.ads.AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                initializeBottomSheetAd();
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





    }

    private void initializeTopNativeAds() {

        try {
            final AdView adView = new AdView(this);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId("ca-app-pub-8455191357100024/7680737603");


            AdRequest request = new AdRequest.Builder().build();
            adView.loadAd(request);

            adView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);


            adView.setAdListener(new AdListener() {

                @Override
                public void onAdFailedToLoad(int i) {
                    super.onAdFailedToLoad(i);

                    try {
                        Answers.getInstance().logCustom(new CustomEvent("Ad failed to load")
                                .putCustomAttribute("Placement", "Feed native bottom").putCustomAttribute("errorType", i));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();

                    CardView cardView = findViewById(R.id.editorialfeed_top_admob_cardView);
                    cardView.setVisibility(View.VISIBLE);

                    cardView.removeAllViews();
                    cardView.addView(adView);

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void initializeTopNativeAds(boolean isFacebook) {


        final NativeAd nativeAd = new NativeAd(this, "1641103999319593_1690644271032232");
        nativeAd.setAdListener(new com.facebook.ads.AdListener() {

            @Override
            public void onError(Ad ad, AdError error) {
                // Ad error callback
                initializeTopNativeAds();


                try {
                    Answers.getInstance().logCustom(new CustomEvent("Ad failed to load").putCustomAttribute("Placement", "Feed native top").putCustomAttribute("errorType", error.getErrorMessage()).putCustomAttribute("Source", "Facebook"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAdLoaded(Ad ad) {
                // Ad loaded callback

                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.currentAffairsFeed_top_adContainer);
                linearLayout.setVisibility(View.VISIBLE);
                NativeAdViewAttributes viewAttributes;
                if (SettingManager.getNightMode(CurrentAffairsFeedActivity.this)) {

                    viewAttributes = new NativeAdViewAttributes()
                            .setBackgroundColor(Color.parseColor("#28292e"))
                            .setTitleTextColor(Color.WHITE)
                            .setButtonTextColor(Color.WHITE)
                            .setDescriptionTextColor(Color.WHITE)
                            .setButtonColor(Color.parseColor("#F44336"));

                } else {


                    viewAttributes = new NativeAdViewAttributes()
                            .setBackgroundColor(Color.LTGRAY)
                            .setButtonTextColor(Color.WHITE)
                            .setButtonColor(Color.parseColor("#F44336"));

                }

                View adView = NativeAdView.render(CurrentAffairsFeedActivity.this, nativeAd, NativeAdView.Type.HEIGHT_120, viewAttributes);

                adView.setFocusable(false);

                linearLayout.removeAllViews();
                linearLayout.addView(adView);

            }

            @Override
            public void onAdClicked(Ad ad) {
                // Ad clicked callback
            }

            @Override
            public void onLoggingImpression(Ad ad) {
                // Ad impression logged callback
            }
        });


        // Request an ad
        nativeAd.loadAd();
    }

    private void initializeTopBannerAds(boolean isFacebook) {
        com.facebook.ads.AdView adView = new com.facebook.ads.AdView(this, "1641103999319593_1715265158570143", com.facebook.ads.AdSize.BANNER_HEIGHT_50);

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.currentAffairsFeed_top_adContainer);
        linearLayout.setVisibility(View.VISIBLE);

        linearLayout.addView(adView);

        adView.loadAd();

        adView.setAdListener(new com.facebook.ads.AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                initializeTopNativeAds();
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

    }


    public void onDictionaryClick(View view) {
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

    }


    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            Locale locale = new Locale("en", "IN");
            int availability = textToSpeech.isLanguageAvailable(locale);
            int result = 0;
            switch (availability) {
                case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE: {
                    result = textToSpeech.setLanguage(locale);
                    break;
                }
                case TextToSpeech.LANG_NOT_SUPPORTED:
                case TextToSpeech.LANG_MISSING_DATA:
                case TextToSpeech.LANG_AVAILABLE: {
                    result = textToSpeech.setLanguage(Locale.US);
                    textToSpeech.setPitch(0.9f);
                    textToSpeech.setSpeechRate(0.9f);
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


}
