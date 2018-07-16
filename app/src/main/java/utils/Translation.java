package utils;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import static android.content.ContentValues.TAG;

/**
 * Created by gamef on 22-02-2017.
 */

public class Translation {
    public String word ;
    public String wordTranslation;
    TranslateListener translateListener;

    public Translation(String word) {
        this.word = word;
    }

    public Translation() {
    }


    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getWordTranslation() {
        return wordTranslation;
    }

    public void setWordTranslation(String wordTranslation) {
        this.wordTranslation = wordTranslation;
    }


    public void fetchTranslation(TranslateListener listener){


        //new Hinditranslation().execute();
        translateListener = listener;
        translate();

        Log.d("tag", "fetchTranslation: After fetching translation");

    }






    public void translate(){

// Tag used to cancel the request
        String tag_string_req = "translate_req";

        final String url = "https://translate.yandex.net/api/v1.5/tr.json/translate?key=trnsl.1.1.20180318T123355Z.89cee6c627b0f4c6.67a2e05de9d43e4d0f87f4b378758a46cb38aae9&text="+getWord().trim()+"&lang=hi&[format=html]&[options=1]";



        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());

                        try {
                            JSONArray jsonarray = response.getJSONArray("text");

                            if (response.getInt("code") == 200) {

                                wordTranslation = jsonarray.getString(0);

                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }finally {
                            translateListener.onTranslation(Translation.this);
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d(TAG, "Error: " + error.getMessage());
                // hide the progress dialog

            }
        });

        jsonObjReq.setShouldCache(true);
        // Adding request to request queue
        VolleyManager.getInstance().addToRequestQueue(jsonObjReq, tag_string_req);
    }


    public interface TranslateListener{

        void onTranslation(Translation translation);

    }


}
