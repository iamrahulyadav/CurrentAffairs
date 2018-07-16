package gk.affairs.current.craftystudio.app.currentaffairs;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.NativeAd;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import utils.FirebaseHelper;
import utils.JsonParser;
import utils.News;
import utils.VolleyManager;

import static android.content.ContentValues.TAG;


public class CurrentAffairsCardFragment extends Fragment {

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    ArrayList<News> newsArrayList = new ArrayList<>();


    private boolean isLoading = false;

    private ProgressDialog pDialog;

    private int pageNumber = 2;


    long sortDateMillis;
    String articleID = "";


    public CurrentAffairsCardFragment() {
        // Required empty public constructor
    }


    public static CurrentAffairsCardFragment newInstance(long timeinmillis, String articleID) {
        CurrentAffairsCardFragment fragment = new CurrentAffairsCardFragment();
        Bundle args = new Bundle();
        args.putLong("time", timeinmillis);
        args.putString("article", articleID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sortDateMillis = getArguments().getLong("time");
            articleID = getArguments().getString("article");
        }

        if (!articleID.isEmpty()) {
            fetchCurrentAffairs(articleID);
        } else if (sortDateMillis > 0) {
            fetchCurrentAffairs(sortDateMillis);
        } else {

            fetchCurrentAffairs();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_current_affairs_card, container, false);

        mPager = view.findViewById(R.id.mainActivity_viewpager);

        initializeViewPager();


        pDialog = new ProgressDialog(getContext());
        showLoadingDialog("Loading...");

        return view;

    }


    private void initializeViewPager() {

        // Instantiate a ViewPager and a PagerAdapter.

        mPagerAdapter = new CurrentAffairsCardFragment.ScreenSlidePagerAdapter(this.getChildFragmentManager());
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
                        fetchMoreCurrentAffairs();
                        Toast.makeText(getContext(), "Loading more article", Toast.LENGTH_SHORT).show();
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


    private void downloadNews() {

        isLoading = true;
        new FirebaseHelper().fetchNewsList(MainActivity.articleCount, new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {

                CurrentAffairsCardFragment.this.newsArrayList.addAll(newsArrayList);
                if (mPagerAdapter != null) {
                    mPagerAdapter.notifyDataSetChanged();
                    mPager.setAdapter(mPagerAdapter);
                }

                isLoading = false;


                hideLoadingDialog();


                addNativeAds();

            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });

    }

    public void addNativeAds() {

        if (getContext()==null){
            return;
        }

        for (int i = 1; i < newsArrayList.size(); i = i + NewsFragment.adsCount) {
            if (newsArrayList.get(i).getNativeAd() == null) {

                NativeAd nativeAd = new NativeAd(getContext(), "1641103999319593_1641104795986180");
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


    private void downloadMoreNews() {

        isLoading = true;

        new FirebaseHelper().fetchNewsList(MainActivity.articleCount, newsArrayList.get(newsArrayList.size() - 1).getTimeInMillis(), new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {
                CurrentAffairsCardFragment.this.newsArrayList.addAll(newsArrayList);
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

    }


    private void fetchCurrentAffairs(String articleID) {

        String url = "http://aspirantworld.in/wp-json/wp/v2/posts/" + articleID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        News currentAffairs = new JsonParser().parseCurrentAffairs(response);

                        newsArrayList.add(0, currentAffairs);


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

    private void downloadNewsByID(String newsID) {
        new FirebaseHelper().fetchNewsByID(newsID, new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {

                try {
                    if (newsArrayList == null) {
                        return;
                    }

                    if (newsArrayList.isEmpty()) {
                        Toast.makeText(getContext(), "No News Found", Toast.LENGTH_SHORT).show();
                    }
                    CurrentAffairsCardFragment.this.newsArrayList.add(0, newsArrayList.get(0));


                    mPagerAdapter.notifyDataSetChanged();
                    mPager.setCurrentItem(0);
                    mPager.setAdapter(mPagerAdapter);

                    hideLoadingDialog();

                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });
    }


    /*this function is called by main activity to get the current displaying article for tts reader*/
    public News getCurrentNews() {

        return newsArrayList.get(mPager.getCurrentItem());


    }


    /*Called by main activity to send notification / share id and this will trigger article opening function*/
    public void openArticleById(String articleID) {

        fetchCurrentAffairs(articleID);

    }

    /*Called by main activity to send notification / share id and this will trigger older article opening function by firebase*/
    public void openFirebaseArticleById(String articleID) {

        downloadNewsByID(articleID);

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


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
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
