package gk.affairs.current.craftystudio.app.currentaffairs;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.NativeAd;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import utils.AdsSubscriptionManager;
import utils.FirebaseHelper;
import utils.News;

import static gk.affairs.current.craftystudio.app.currentaffairs.MainActivity.articleCount;

public class ArchiveActivity extends AppCompatActivity {

    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    ArrayList<News> newsArrayList = new ArrayList<>();
    private boolean isLoading=false;


    ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pDialog = new ProgressDialog(this);

        showLoadingDialog("Loading...");

        mPager = findViewById(R.id.mainActivity_viewpager);

        initializeViewPager();

        downloadNews();

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

                    fetchNewsByDate(sortDateMillis);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMinDate(1519151400000l);
        datePickerDialog.getDatePicker().setMaxDate(1527952468418l);
        datePickerDialog.show();

    }



    private void initializeViewPager() {

        // Instantiate a ViewPager and a PagerAdapter.

        mPagerAdapter = new ArchiveActivity.ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setVisibility(View.VISIBLE);

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


                        Toast.makeText(ArchiveActivity.this, "Loading more article", Toast.LENGTH_SHORT).show();
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



    private void downloadNewsByID(String newsID) {
        new FirebaseHelper().fetchNewsByID(newsID, new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {
                if (newsArrayList.isEmpty()) {
                    Toast.makeText(ArchiveActivity.this, "No News Found", Toast.LENGTH_SHORT).show();
                }
                ArchiveActivity.this.newsArrayList.add(0, newsArrayList.get(0));


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

                ArchiveActivity.this.newsArrayList.clear();


                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyy  ");
                for (int i=0; i<newsArrayList.size();i++){

                    News news = newsArrayList.get(i);

                    String myDate = dateFormat.format(new Date(news.getTimeInMillis()));
                    news.setDate(myDate);

                    ArchiveActivity.this.newsArrayList.add(news);



                }


                mPagerAdapter.notifyDataSetChanged();
                isLoading = false;


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

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyy  ");
                for (int i=0; i<newsArrayList.size();i++){

                    News news = newsArrayList.get(i);


                    String myDate = dateFormat.format(new Date(news.getTimeInMillis()));
                    news.setDate(myDate);

                    ArchiveActivity.this.newsArrayList.add(news);



                }

                mPagerAdapter.notifyDataSetChanged();
                isLoading = false;

                addNativeAds();
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

    private void fetchNewsByDate(long sortDateMillis) {

        showLoadingDialog("Loading...");

        new FirebaseHelper().fetchNewsList(sortDateMillis, (sortDateMillis + 86400000), new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {
                if (isSuccesful) {

                    ArchiveActivity.this.newsArrayList.clear();


                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyy  ");
                    for (int i=0; i<newsArrayList.size();i++){

                        News news = newsArrayList.get(i);

                        String myDate = dateFormat.format(new Date(news.getTimeInMillis()));
                        news.setDate(myDate);

                        ArchiveActivity.this.newsArrayList.add(news);



                    }

                    mPagerAdapter.notifyDataSetChanged();
                    mPager.setAdapter(mPagerAdapter);
                    hideLoadingDialog();

                } else {
                    Toast.makeText(ArchiveActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                    hideLoadingDialog();
                }
            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });
    }


    public void addNativeAds() {

        if (!AdsSubscriptionManager.checkShowAds(this)){
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
