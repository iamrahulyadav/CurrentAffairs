package gk.affairs.current.craftystudio.app.currentaffairs;

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

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.NativeAd;

import java.util.ArrayList;

import utils.FirebaseHelper;
import utils.News;


public class CurrentAffairsCardFragment extends Fragment {

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    ArrayList<News> newsArrayList = new ArrayList<>();


    private boolean isLoading = false;


    public CurrentAffairsCardFragment() {
        // Required empty public constructor
    }


    public static CurrentAffairsCardFragment newInstance(String param1, String param2) {
        CurrentAffairsCardFragment fragment = new CurrentAffairsCardFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }

        downloadNews();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_current_affairs_card, container, false);

        mPager = view.findViewById(R.id.mainActivity_viewpager);

        initializeViewPager();

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
                        downloadMoreNews();
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


                //hideLoadingDialog();


                addNativeAds();

            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });

    }

    public void addNativeAds() {

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
