package net.bradball.android.sandbox.model;

import org.joda.time.LocalDateTime;

public class Review implements Comparable<Review> {

    /*
        {
            "review_id": "13355",
            "reviewbody": "Scary, sad note.\n\nThank you Jerry.",
            "reviewtitle": "Last Show..",
            "reviewer": "digdogdig",
            "reviewdate": "2004-05-19 08:34:19",
            "stars": "5"
        }
    */


    private long mID;

    private long mRecordingID;
    private long mReviewID;
    private String mTitle;
    private LocalDateTime mDate;
    private String mReview;
    private String mReviewer;
    private int mStars;


    //region GETTERS-SETTERS
    public long getID() {
        return mID;
    }

    public void setID(long ID) {
        mID = ID;
    }

    public long getRecordingID() {
        return mRecordingID;
    }

    public void setRecordingID(long recordingID) {
        mRecordingID = recordingID;
    }

    public LocalDateTime getDate() {
        return mDate;
    }

    public void setDate(LocalDateTime date) {
        mDate = date;
    }

    public String getReview() {
        return mReview;
    }

    public void setReview(String review) {
        mReview = review;
    }

    public String getReviewer() {
        return mReviewer;
    }

    public void setReviewer(String reviewer) {
        mReviewer = reviewer;
    }

    public long getReviewID() {
        return mReviewID;
    }

    public void setReviewID(long reviewID) {
        mReviewID = reviewID;
    }

    public int getStars() {
        return mStars;
    }

    public void setStars(int stars) {
        mStars = stars;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }
    //endregion

    public Review(long reviewID) {
        setReviewID(reviewID);
    }

    @Override
    public int compareTo(Review another) {
        /* Sorts with most recent reviews first */

        if (getDate().isAfter(another.getDate()))
            return -1;

        if (getDate().isBefore(another.getDate()))
            return 1;

        return 0;
    }
}
