package gk.affairs.current.craftystudio.app.currentaffairs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

import java.text.SimpleDateFormat;
import java.util.Date;

import utils.News;
import utils.SettingManager;
import utils.SqlDatabaseHelper;
import utils.VolleyManager;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NewsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NewsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NewsFragment extends Fragment {

    TextView titleTextView, descriptionTextView, sourceTextView, dateTextView, tagTextView;

    News news;
    int count;

    private OnFragmentInteractionListener mListener;

    public static final int adsCount = 3;
    private String selectedWord = "";

    public NewsFragment() {

    }

    public static NewsFragment newInstance(News currentNews, int fragmentNumber) {
        NewsFragment fragment = new NewsFragment();
        Bundle args = new Bundle();

        args.putInt("count", fragmentNumber);
        args.putSerializable("news", currentNews);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.news = (News) getArguments().getSerializable("news");
            this.count = getArguments().getInt("count", 1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        NetworkImageView imageView;
        ImageView shareImageView, bookmarkImageView;

        titleTextView = view.findViewById(R.id.newsFragment_title_textView);
        descriptionTextView = view.findViewById(R.id.newsFragment_description_textView);
        sourceTextView = view.findViewById(R.id.newsFragment_source_textView);
        dateTextView = view.findViewById(R.id.newsFragment_date_textView);
        imageView = view.findViewById(R.id.newsFragment_imageView);
        tagTextView= view.findViewById(R.id.newsFragment_tag_textView);

        titleTextView.setText(news.getNewsTitle());

        sourceTextView.setText(news.getNewsSource());


        int textSize = SettingManager.getTextSize(getContext());

        descriptionTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize + 4);



        try {
            //news.setNewsDescription(news.getNewsDescription().replaceAll("\n", "<br>"));


            if (Build.VERSION.SDK_INT >= 24) {
                descriptionTextView.setText(Html.fromHtml(news.getNewsDescription(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                descriptionTextView.setText(Html.fromHtml(news.getNewsDescription()));
            }

            tagTextView.setText(news.getTag());

        /*    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyy  ");
            String myDate = dateFormat.format(new Date(news.getTimeInMillis()));
            dateTextView.setText(myDate);*/

            dateTextView.setText(news.getDate());


        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            if (news.getNewsImageURL() != null) {
                if (!news.getNewsImageURL().isEmpty()) {

                    ImageLoader imageLoader = VolleyManager.getInstance().getImageLoader();


                    imageView.setImageUrl(news.getNewsImageURL(), imageLoader);
                } else {
                    imageView.setVisibility(View.GONE);
                }


            } else {
                imageView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        shareImageView = view.findViewById(R.id.newsFragment_share_imageView);
        shareImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onShareClick();
            }
        });

        bookmarkImageView = view.findViewById(R.id.newsFragment_bookmark_imageView);
        bookmarkImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBookMarkClick();
            }
        });

        titleTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openNewsLink();
            }
        });


        if (news.getNativeAd() != null) {
            initializeNativeAd(view);
        }


        return view;
    }

    public void getSelectedWord(long timeDelay) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String string = descriptionTextView.getText().toString();
                try {
                    if (descriptionTextView.hasSelection()) {
                        selectedWord = string.substring(descriptionTextView.getSelectionStart(), descriptionTextView.getSelectionEnd()).trim();
                    }
                } catch (Exception e) {
                    selectedWord = "word";
                }

                Toast.makeText(getContext(), "Selected - " + selectedWord, Toast.LENGTH_SHORT).show();

                //loadWebview(selectedWord);

                //translationTextView.setText(selectedWord);

                descriptionTextView.clearFocus();


                /*new Translation(selectedWord).fetchTranslation(new Translation.TranslateListener() {
                    @Override
                    public void onTranslation(Translation translation) {

                        if (translation.getWord().equalsIgnoreCase(selectedWord.trim())) {
                            translationTextView.setText(translation.getWord() + " = " + translation.wordTranslation);
                        }

                    }
                });*/


                Answers.getInstance().logCustom(new CustomEvent("Word Meaning").putCustomAttribute("word", selectedWord));


            }
        }, timeDelay);

    }

    private void initializeNativeAd(final View view) {

        if (getContext()==null){
            return;
        }

        final NativeAd nativeAd = news.getNativeAd();
        if (nativeAd != null) {

            if (nativeAd.isAdLoaded()) {

                LinearLayout adContainer = view.findViewById(R.id.newsFragment_native_adContainer);
                adContainer.setVisibility(View.VISIBLE);

                adContainer.removeAllViews();

                View adView = NativeAdView.render(getContext(), nativeAd, NativeAdView.Type.HEIGHT_300);
                // Add the Native Ad View to your ad container
                adContainer.addView(adView);


            } else {


                nativeAd.setAdListener(new AdListener() {
                    @Override
                    public void onError(Ad ad, AdError adError) {

                        initializeNativeAd(view, true);

                    }

                    @Override
                    public void onAdLoaded(Ad ad) {

                        if (view != null) {

                            try {
                                LinearLayout adContainer = view.findViewById(R.id.newsFragment_native_adContainer);
                                adContainer.setVisibility(View.VISIBLE);


                                adContainer.removeAllViews();

                                View adView = NativeAdView.render(getContext(), nativeAd, NativeAdView.Type.HEIGHT_300);
                                // Add the Native Ad View to your ad container
                                adContainer.addView(adView);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }

                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }

                });

                View adContainer = view.findViewById(R.id.newsFragment_native_adContainer);
                adContainer.setVisibility(View.GONE);

                initializeNativeAd(view, true);

            }
        } else {
            View adContainer = view.findViewById(R.id.newsFragment_native_adContainer);
            adContainer.setVisibility(View.GONE);

        }


    }

    private void initializeNativeAd(final View view, boolean admob) {

        try {

            if (count == 2) {
                return;
            }

            final AdView adView = new AdView(getContext());
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId("ca-app-pub-8455191357100024/4907086877");


            AdRequest request = new AdRequest.Builder().build();
            adView.loadAd(request);

            adView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);


            adView.setAdListener(new com.google.android.gms.ads.AdListener() {

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

                    LinearLayout cardView = view.findViewById(R.id.newsFragment_banner_adContainer);
                    cardView.setVisibility(View.VISIBLE);

                    cardView.removeAllViews();
                    cardView.addView(adView);

                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void openNewsLink() {

       /* Intent intent = new Intent(getContext(),WebActivity.class);
        intent.putExtra("link",news.getNewsSourceLink());
        startActivity(intent);
*/

        try {

            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(news.getNewsSourceLink())));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void onBookMarkClick() {

        new SqlDatabaseHelper(getContext()).addSavedNews(news);

        try {
            Toast.makeText(getContext(), "Saved ", Toast.LENGTH_SHORT).show();

            Answers.getInstance().logCustom(new CustomEvent("Book mark click")
                    .putCustomAttribute("News id", news.getNewsTitle()));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void onShareClick() {

        String appCode = "wt6q7.app.goo.gl";
        String appName = getString(R.string.app_name);
        String packageName = getContext().getPackageName();
        String imageUrl = news.getNewsImageURL();


        String utmSource = "Android app";
        String utmCampaign = "app share";
        String utmMedium = "social";

        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setMessage("Creating link ...");
        pd.show();

        Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse("https://goo.gl/WDXMaQ?newsID=1527744728325&articleID=" + news.getNewsID() + "&contentType=1"))
                .setDynamicLinkDomain(appCode)
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder(packageName)
                                .build())
                .setSocialMetaTagParameters(
                        new DynamicLink.SocialMetaTagParameters.Builder()
                                .setTitle(news.getNewsTitle())
                                .setDescription(appName)
                                .setImageUrl(Uri.parse(news.getNewsImageURL()))
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
                            Toast.makeText(getContext(), "Connection Failed! Try again later", Toast.LENGTH_SHORT).show();

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
                    + "\n" + news.getNewsTitle()
                    + "\n\nRead full News at Current affairs app");
            startActivity(Intent.createChooser(sharingIntent, "Share News via"));


            Answers.getInstance().logCustom(new CustomEvent("share click")
                    .putCustomAttribute("News id", news.getNewsTitle()));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
