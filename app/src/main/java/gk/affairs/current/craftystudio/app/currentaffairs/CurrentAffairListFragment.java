package gk.affairs.current.craftystudio.app.currentaffairs;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import utils.AdsSubscriptionManager;
import utils.CurrentAffairsAdapter;
import utils.FirebaseHelper;
import utils.JsonParser;
import utils.News;
import utils.VolleyManager;

import static android.content.ContentValues.TAG;


public class CurrentAffairListFragment extends Fragment {

    RecyclerView recyclerView;
    CurrentAffairsAdapter currentAffairsAdapter;
    ArrayList<Object> newsArrayList = new ArrayList<>();

    private boolean isLoading = false;
    private ProgressDialog pDialog;

    private int pageNumber = 2;

    long sortDateMillis;


    public CurrentAffairListFragment() {
        // Required empty public constructor
    }


    public static CurrentAffairListFragment newInstance(long timeinmillis) {
        CurrentAffairListFragment fragment = new CurrentAffairListFragment();
        Bundle args = new Bundle();
        args.putLong("time", timeinmillis);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sortDateMillis = getArguments().getLong("time", -1);
        }

        if (sortDateMillis > 0) {
            fetchCurrentAffairs(sortDateMillis);
        } else {
            fetchCurrentAffairs();
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_affair_list, container, false);

        recyclerView = view.findViewById(R.id.currentAffairsFragment_recyclerView);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());


        currentAffairsAdapter = new CurrentAffairsAdapter(newsArrayList, getContext());


        recyclerView.setAdapter(currentAffairsAdapter);


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {

                    if (!isLoading) {


                        fetchMoreCurrentAffairs();
                        Toast.makeText(getContext(), "Loading", Toast.LENGTH_SHORT).show();

                    }

                }
            }
        });

        setAdapterListener();


        pDialog = new ProgressDialog(getContext());
        showLoadingDialog("Loading...");


        return view;

    }


    private void setAdapterListener() {
        currentAffairsAdapter.setClickListener(new CurrentAffairsAdapter.ClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                if (position < 0) {

                    return;
                }


                News currentAffairs = (News) newsArrayList.get(position);

                Intent intent = new Intent(getContext(), CurrentAffairsFeedActivity.class);
                intent.putExtra("news", currentAffairs);
                startActivity(intent);


                /*Read Status

                try {
                    currentAffairs.setReadStatus(true);
                    currentAffairsAdapter.notifyDataSetChanged();

                    new DatabaseHandlerRead(getContext()).addReadNews(currentAffairs);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                */

            }
        });
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

                        CurrentAffairListFragment.this.newsArrayList.addAll(arrayList);


                        isLoading = false;

                        if (currentAffairsAdapter != null) {
                            currentAffairsAdapter.notifyDataSetChanged();
                        }


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


                CurrentAffairListFragment.this.newsArrayList.addAll(arrayList);


                isLoading = false;

                if (currentAffairsAdapter != null) {
                    currentAffairsAdapter.notifyDataSetChanged();
                }


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

                        isLoading = false;

                        if (currentAffairsAdapter != null) {
                            currentAffairsAdapter.notifyDataSetChanged();
                        }


                        hideLoadingDialog();

                        pageNumber++;


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

                        //addNativeAds();
                        //addReadStatus();


                        //showRefreshing(false);

                        if (currentAffairsAdapter != null) {
                            currentAffairsAdapter.notifyDataSetChanged();
                        }


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
}
