package utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


/**
 * Created by bunny on 06/04/18.
 */

public class JsonParser {

    public ArrayList<News> parseCurrentAffairsList(JSONArray response) {


        ArrayList<News> newsArrayList = new ArrayList<>();

        try {

            News news;

            for (int i = 0; i < response.length(); i++) {

                news = new News();

                JSONObject jsonObject = response.getJSONObject(i);

                news.setDate(jsonObject.getString("date"));
                news.setNewsLink(jsonObject.getString("link"));

                news.setNewsTitle(jsonObject.getJSONObject("title").getString("rendered"));

                news.setNewsDescription(jsonObject.getJSONObject("content").getString("rendered"));

                news.setNewsID(jsonObject.getInt("id") + "");

                if (jsonObject.getJSONArray("categories").length() > 1) {
                    news.setCategoryIndex(jsonObject.getJSONArray("categories").getInt(1));
                } else {
                    news.setCategoryIndex(jsonObject.getJSONArray("categories").getInt(0));
                }

                if (jsonObject.getJSONArray("tags").length() != 0) {
                    news.setTagIndex(jsonObject.getJSONArray("tags").getInt(0));
                }


                try {
                    news.setNewsImageURL(jsonObject.getJSONObject("acf").getString("image_url"));
                    news.setNewsSourceLink(jsonObject.getJSONObject("acf").getString("source_url"));


                }catch (Exception e){
                    e.printStackTrace();
                }

                newsArrayList.add(news);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newsArrayList;

    }

    public ArrayList<News> parseCurrentAffairsList(String string) {


        ArrayList<News> newsArrayList = new ArrayList<>();

        try {

            JSONArray response = new JSONArray(string);


            News news;

            for (int i = 0; i < response.length(); i++) {

                news = new News();

                JSONObject jsonObject = response.getJSONObject(i);

                news.setDate(jsonObject.getString("date"));
                news.setNewsSourceLink(jsonObject.getString("link"));

                news.setNewsTitle(jsonObject.getJSONObject("title").getString("rendered"));

                news.setNewsDescription(jsonObject.getJSONObject("content").getString("rendered"));

                news.setId(jsonObject.getInt("id"));

                if (jsonObject.getJSONArray("categories").length() > 1) {
                    news.setCategoryIndex(jsonObject.getJSONArray("categories").getInt(1));
                } else {
                    news.setCategoryIndex(jsonObject.getJSONArray("categories").getInt(0));
                }


                newsArrayList.add(news);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newsArrayList;

    }

    public News parseCurrentAffairs(JSONObject jsonObject) {


        News news = null;

        try {


            news = new News();


            news.setDate(jsonObject.getString("date"));
            news.setNewsSourceLink(jsonObject.getString("link"));

            news.setNewsTitle(jsonObject.getJSONObject("title").getString("rendered"));

            news.setNewsDescription(jsonObject.getJSONObject("content").getString("rendered"));

            news.setId(jsonObject.getInt("id"));

            if (jsonObject.getJSONArray("categories").length() > 1) {
                news.setCategoryIndex(jsonObject.getJSONArray("categories").getInt(1));
            } else {
                news.setCategoryIndex(jsonObject.getJSONArray("categories").getInt(0));
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return news;

    }

}
