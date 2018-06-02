package gk.affairs.current.craftystudio.app.currentaffairs;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import java.util.ArrayList;

import utils.CurrentAffairsAdapter;
import utils.FirebaseHelper;
import utils.News;


public class CurrentAffairListFragment extends Fragment {

    RecyclerView recyclerView;
    CurrentAffairsAdapter currentAffairsAdapter;
    ArrayList<Object> newsArrayList = new ArrayList<>();

    private boolean isLoading = false;


    public CurrentAffairListFragment() {
        // Required empty public constructor
    }


    public static CurrentAffairListFragment newInstance(String param1, String param2) {
        CurrentAffairListFragment fragment = new CurrentAffairListFragment();
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
        View view = inflater.inflate(R.layout.fragment_current_affair_list, container, false);

        recyclerView = view.findViewById(R.id.currentAffairsFragment_recyclerView);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());



        currentAffairsAdapter = new CurrentAffairsAdapter(newsArrayList, getContext());


        recyclerView.setAdapter(currentAffairsAdapter);

        return view;

    }


    private void downloadNews() {

        isLoading = true;
        new FirebaseHelper().fetchNewsList(MainActivity.articleCount, new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {

                CurrentAffairListFragment.this.newsArrayList.addAll(newsArrayList);

                isLoading = false;

                if (currentAffairsAdapter!=null){
                    currentAffairsAdapter.notifyDataSetChanged();
                }


                //hideLoadingDialog();


                //addNativeAds();

            }

            @Override
            public void onNewsInsert(boolean isSuccessful) {

            }
        });

    }

    private void downloadMoreNews() {

        isLoading = true;

        new FirebaseHelper().fetchNewsList(MainActivity.articleCount, ((News)newsArrayList.get(newsArrayList.size() - 1)).getTimeInMillis(), new FirebaseHelper.NewsListener() {
            @Override
            public void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful) {
                CurrentAffairListFragment.this.newsArrayList.addAll(newsArrayList);

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
}
