package gk.affairs.current.craftystudio.app.currentaffairs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
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


    News news;
    int count;

    private OnFragmentInteractionListener mListener;

    public NewsFragment() {
        // Required empty public constructor
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news, container, false);

        TextView titleTextView, descriptionTextView, sourceTextView, dateTextView;
        NetworkImageView imageView;
        ImageView shareImageView, bookmarkImageView;

        titleTextView = view.findViewById(R.id.newsFragment_title_textView);
        descriptionTextView = view.findViewById(R.id.newsFragment_description_textView);
        sourceTextView = view.findViewById(R.id.newsFragment_source_textView);
        dateTextView = view.findViewById(R.id.newsFragment_date_textView);
        imageView = view.findViewById(R.id.newsFragment_imageView);

        titleTextView.setText(news.getNewsTitle());

        sourceTextView.setText(news.getNewsSource());

        try {
            news.setNewsDescription(news.getNewsDescription().replaceAll("\n","<br>"));

            if (Build.VERSION.SDK_INT >= 24) {
                descriptionTextView.setText(Html.fromHtml(news.getNewsDescription(), Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH));
            } else {
                descriptionTextView.setText(Html.fromHtml(news.getNewsDescription()));
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyy  ");
            String myDate = dateFormat.format(new Date(news.getTimeInMillis()));
            dateTextView.setText(myDate);



        } catch (Exception e) {
            e.printStackTrace();
        }





        try {
            if (news.getNewsImageURL() != null && !news.getNewsImageURL().isEmpty()) {
                ImageLoader imageLoader = VolleyManager.getInstance().getImageLoader();


                imageView.setImageUrl(news.getNewsImageURL(), imageLoader);


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


        return view;
    }

    private void onBookMarkClick() {

        new SqlDatabaseHelper(getContext()).addSavedNews(news);
        Toast.makeText(getContext(), "Saved ", Toast.LENGTH_SHORT).show();

        try {
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
                .setLink(Uri.parse("https://goo.gl/Ae4Mhw?newsID=" + news.getNewsID()))
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
                        Toast.makeText(getContext(), "Connection Failed! Try again later", Toast.LENGTH_SHORT).show();
                        try {
                            pd.dismiss();
                        } catch (Exception exception) {
                            e.printStackTrace();
                        }
                    }
                });


    }

    private void openShareDialog(Uri shortLink) {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        //sharingIntent.putExtra(Intent.EXTRA_STREAM, newsMetaInfo.getNewsImageLocalPath());

        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Download the app and Start reading");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shortLink
                + "\n" + news.getNewsTitle()
                + "\n\nRead full News at Current affairs app");
        startActivity(Intent.createChooser(sharingIntent, "Share News via"));


        try {
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
