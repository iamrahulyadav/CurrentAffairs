package utils;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.NativeAd;
import com.google.android.gms.ads.AdView;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by bunny on 14/02/18.
 */

public class News implements Serializable {
    private String newsTitle, newsDescription, newsImageURL, newsTopic, newsSource, newsSourceLink, newsID, date, category,tag, newsLink;
    private long timeInMillis;
    boolean pushNotification = false;

    private transient NativeAd nativeAd;

    int id, categoryIndex, tagIndex, contentType;



    public String getNewsTitle() {
        return newsTitle;
    }

    public void setNewsTitle(String newsTitle) {
        this.newsTitle = newsTitle;
    }

    public String getNewsDescription() {
        return newsDescription;
    }

    public void setNewsDescription(String newsDescription) {
        this.newsDescription = newsDescription;
    }

    public String getNewsImageURL() {
        return newsImageURL;
    }

    public void setNewsImageURL(String newsImageURL) {
        this.newsImageURL = newsImageURL;
    }

    public String getNewsTopic() {
        return newsTopic;
    }

    public void setNewsTopic(String newsTopic) {
        this.newsTopic = newsTopic;
    }

    public String getNewsSource() {
        return newsSource;
    }

    public void setNewsSource(String newsSource) {
        this.newsSource = newsSource;
    }

    public String getNewsSourceLink() {
        return newsSourceLink;
    }

    public void setNewsSourceLink(String newsSourceLink) {
        this.newsSourceLink = newsSourceLink;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public void setTimeInMillis(long timeInMillis) {
        this.timeInMillis = timeInMillis;
    }

    public String getNewsID() {
        return newsID;
    }

    public void setNewsID(String newsID) {
        this.newsID = newsID;
    }

    public boolean isPushNotification() {
        return pushNotification;
    }

    public void setPushNotification(boolean pushNotification) {
        this.pushNotification = pushNotification;
    }

    public NativeAd getNativeAd() {
        return nativeAd;
    }

    public void setNativeAd(NativeAd nativeAd) {
        this.nativeAd = nativeAd;

    }



    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;

        resolveDate();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
        newsSource= category;
    }

    public String getNewsLink() {
        return newsLink;
    }

    public void setNewsLink(String newsLink) {
        this.newsLink = newsLink;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryIndex() {
        return categoryIndex;
    }

    public void setCategoryIndex(int categoryIndex) {
        this.categoryIndex = categoryIndex;

        resolveCategory();
    }

    public int getTagIndex() {
        return tagIndex;
    }

    public void setTagIndex(int tagIndex) {
        this.tagIndex = tagIndex;

        resolveTag();
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    private void resolveDate() {
        int endIndex = date.indexOf("T");
        try {


            date = date.substring(0, endIndex);

            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date customDate = (Date) formatter.parse(getDate());
            System.out.println("Today is " + customDate.getTime());

            DateFormat dateformatter = new SimpleDateFormat("dd MMM, yyyy");

            setDate(dateformatter.format(customDate.getTime()));

        } catch (Exception e) {

            if (endIndex > 0) {
                date = date.substring(0, endIndex);
            }
            e.printStackTrace();

        }


    }



    private void resolveTag() {

        switch (tagIndex) {

            case 22:
                setTag("International Relation");
                break;
            case 21:
                setTag("Economics");
                break;
            case 23:
                setTag("Polity");
                break;
            case 24:
                setTag("Science & Tech");
                break;
            case 25:
                setTag("Environment");
                break;
            case 26:
                setTag("National");
                break;
            case 27:
                setTag("Awards");
                break;

            default:
                setCategory("");
                break;


        }
    }

    public void resolveCategory() {


        switch (categoryIndex) {

            case 2:
                setCategory("Editorial Analysis");
                break;
            case 3:
                setCategory("Current Affairs");
                break;
            case 5:
                setCategory("The Hindu");
                break;
            case 6:
                setCategory("Indian Express");

                break;
            case 7:
                setCategory("The Hindu");

                break;
            case 8:
                setCategory("Indian Express");
                break;
            case 9:
                setCategory("Live Mint");
                break;
            case 10:
                setCategory("PIB");
                break;
            case 11:
                setCategory("Times of India");
                break;
            case 12:
                setCategory("Economic Times");
                break;
            case 13:
                setCategory("Important Dates");
                break;
            case 14:
                setCategory("News");
                break;
            case 15:
                setCategory("Live Mint");
                break;
            case 33:
                setCategory("One Liners");
                break;

            default:
                setCategory("Others");
                break;


        }

    }




}
