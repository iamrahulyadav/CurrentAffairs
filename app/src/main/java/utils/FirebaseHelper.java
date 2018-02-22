package utils;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

/**
 * Created by bunny on 15/02/18.
 */

public class FirebaseHelper {
    FirebaseFirestore firestore;

    public FirebaseHelper() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void fetchNewsList(int limit, final NewsListener newsListener) {


        CollectionReference newsReference = firestore.collection("News");

        newsReference.orderBy("timeInMillis", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(limit).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    ArrayList<News> newsArrayList = new ArrayList<>();

                    for (DocumentSnapshot document : task.getResult()) {
                        News news = document.toObject(News.class);
                        news.setNewsID(document.getId());
                        newsArrayList.add(news);
                    }
                    newsListener.onNewsList(newsArrayList, true);

                } else {
                    newsListener.onNewsList(null, false);
                }
            }
        });

    }

    public void fetchNewsList(int limit, long lastTimeInMillis, final NewsListener newsListener) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference vocabReference = firestore.collection("News");

        vocabReference.orderBy("timeInMillis", com.google.firebase.firestore.Query.Direction.DESCENDING).whereLessThan("timeInMillis", lastTimeInMillis).limit(limit).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    ArrayList<News> newsArrayList = new ArrayList<>();

                    for (DocumentSnapshot document : task.getResult()) {
                        News news = document.toObject(News.class);
                        newsArrayList.add(news);
                    }
                    newsListener.onNewsList(newsArrayList, true);

                } else {
                    newsListener.onNewsList(null, false);
                }
            }
        });

    }

    public void fetchNewsList(long startTimeInMillis, long endTimeInMillis, final NewsListener newsListener) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference vocabReference = firestore.collection("News");

        vocabReference.orderBy("timeInMillis", com.google.firebase.firestore.Query.Direction.DESCENDING).whereGreaterThanOrEqualTo("timeInMillis", startTimeInMillis).whereLessThanOrEqualTo("timeInMillis", endTimeInMillis).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    ArrayList<News> newsArrayList = new ArrayList<>();

                    for (DocumentSnapshot document : task.getResult()) {
                        News news = document.toObject(News.class);
                        newsArrayList.add(news);
                    }
                    newsListener.onNewsList(newsArrayList, true);

                } else {
                    newsListener.onNewsList(null, false);
                }
            }
        });

    }

    public void insertNews(News news, final NewsListener newsListener) {

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("News").document(String.valueOf(news.getTimeInMillis())).set(news).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                newsListener.onNewsInsert(true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                newsListener.onNewsInsert(true);

            }
        });

    }

    public void fetchNewsByID(String newsID, final NewsListener newsListener){

        DocumentReference documentReference = firestore.collection("News").document(newsID);

        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                ArrayList<News> newsArrayList = new ArrayList<>();
                News news = task.getResult().toObject(News.class);
                news.setNewsID(task.getResult().getId());
                newsArrayList.add(news);

                newsListener.onNewsList(newsArrayList,true);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                newsListener.onNewsList(null,false);
            }
        });

    }


    public interface NewsListener {
        void onNewsList(ArrayList<News> newsArrayList, boolean isSuccesful);

        void onNewsInsert(boolean isSuccessful);

    }


}
