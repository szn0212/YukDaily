package com.sinyuk.yukdaily.data.news;

import android.content.Context;
import android.util.Log;

import com.sinyuk.yukdaily.api.NewsService;
import com.sinyuk.yukdaily.entity.news.News;
import com.sinyuk.yukdaily.entity.news.NewsComment;
import com.sinyuk.yukdaily.entity.news.NewsCommentResponse;
import com.sinyuk.yukdaily.entity.news.NewsExtras;
import com.sinyuk.yukdaily.entity.news.Stories;
import com.sinyuk.yukdaily.entity.news.Theme;
import com.sinyuk.yukdaily.entity.news.ThemeData;
import com.sinyuk.yukdaily.entity.news.ThemeResponse;
import com.sinyuk.yukdaily.utils.rx.SchedulerTransformer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rx.Observable;
import rx.functions.Func1;

/**
 * Created by Sinyuk on 16.10.20.
 */

public class NewsRepository {
    public static final String TAG = "NewsRepository";
    private final SimpleDateFormat formatter;
    private final Context context;
    private final NewsService newsService;
    private Date currentDate;
    private Calendar calendar;
    private boolean hasPreloaded = false;
    private Stories preloadStories = null;
    private List<Theme> others = new ArrayList<>();
    private List<Theme> subscribed = new ArrayList<>();

    public NewsRepository(Context context, NewsService newsService) {
        this.context = context;
        this.newsService = newsService;
        calendar = Calendar.getInstance();
        currentDate = new Date(System.currentTimeMillis());
        formatter = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
    }

    public Observable<Stories> getLatestNews() {
        currentDate.setTime(System.currentTimeMillis()); // 只有在刷新的时候才重置时间

        if (!hasPreloaded && preloadStories != null) {
            return Observable.just(preloadStories).doOnTerminate(() -> {
                preloadStories = null;
                hasPreloaded = true;
            });
        }

        return newsService.getLatestNews()
                .doOnNext(stories -> {
                    if (!hasPreloaded) { preloadStories = stories; }
                })
                .compose(new SchedulerTransformer<>());
    }

    public Observable<Stories> getNewsAt(int whichDay2Today) {
        calendar.setTime(currentDate);
        calendar.add(Calendar.DATE, -whichDay2Today);
        Log.d(TAG, "getNewsAt: " + formatter.format(calendar.getTime()));
        return newsService.getNewsAt(formatter.format(calendar.getTime()))
                .compose(new SchedulerTransformer<>());
    }

    public Observable<News> getNews(int id) {
        return newsService.getNews(id)
                .compose(new SchedulerTransformer<>());
    }

    public Observable<NewsExtras> getNewsExtras(int id) {
        return newsService.getNewsExtras(id)
                .compose(new SchedulerTransformer<>());
    }

    private Observable<List<NewsComment>> getNewsLongComments(int id) {
        return newsService.getNewsLongComments(id)

                .flatMap(new Func1<NewsCommentResponse, Observable<List<NewsComment>>>() {
                    @Override
                    public Observable<List<NewsComment>> call(NewsCommentResponse response) {
                        return Observable.just(response.getComments());
                    }
                })
                .doOnError(throwable -> {
                    Log.d(TAG, "getNewsLongComments: ");
                    throwable.printStackTrace();
                })
                .compose(new SchedulerTransformer<>());
    }

    private Observable<List<NewsComment>> getNewsShortComments(int id) {
        return newsService.getNewsShortComments(id)
                .flatMap(new Func1<NewsCommentResponse, Observable<List<NewsComment>>>() {
                    @Override
                    public Observable<List<NewsComment>> call(NewsCommentResponse response) {
                        return Observable.just(response.getComments());
                    }
                })
                .doOnError(throwable -> {
                    Log.d(TAG, "getNewsShortComments: ");
                    throwable.printStackTrace();
                })
                .compose(new SchedulerTransformer<>());
    }


    public Observable<List<NewsComment>> getNewsAllComments(int id) {
        return Observable
                .zip(getNewsLongComments(id), getNewsShortComments(id), (comment1, comment2) -> {
                    comment1.addAll(comment2);
                    return comment1;
                })
                .flatMap(new Func1<List<NewsComment>, Observable<NewsComment>>() {
                    @Override
                    public Observable<NewsComment> call(List<NewsComment> newsComments) {
                        return Observable.from(newsComments);
                    }
                })
                .toSortedList(NewsComment::compareTo)
                .doOnError(throwable -> {
                    Log.d(TAG, "getNewsAllComments: ");
                    throwable.printStackTrace();
                })
                .compose(new SchedulerTransformer<>());
    }

    public Observable<List<Theme>> getOtherThemes() {
        if (!others.isEmpty()) {
            return Observable.just(others);
        }

        Log.d(TAG, "getThemes: from web");
        return getThemes()
                .flatMap(new Func1<ThemeResponse, Observable<List<Theme>>>() {
                    @Override
                    public Observable<List<Theme>> call(ThemeResponse themeResponse) {
                        return Observable.just(themeResponse.getThemes());
                    }
                })
                .doOnError(throwable -> others.clear())
                .compose(new SchedulerTransformer<>());
    }

    public Observable<List<Theme>> getSubscribed() {
        if (!subscribed.isEmpty()) { return Observable.just(subscribed); }
        Log.d(TAG, "getThemes: from web");
        return getThemes()
                .flatMap(new Func1<ThemeResponse, Observable<List<Theme>>>() {
                    @Override
                    public Observable<List<Theme>> call(ThemeResponse themeResponse) {
                        return Observable.just(themeResponse.getSubscribed());
                    }
                })
                .doOnError(throwable -> subscribed.clear())
                .compose(new SchedulerTransformer<>());
    }

    public Observable<ThemeResponse> getThemes() {
        return newsService.getThemes()
                .doOnNext(this::saveThemes)
                .doOnError(throwable -> clearThemeCache())
                .compose(new SchedulerTransformer<>());
    }

    private void clearThemeCache() {
        subscribed.clear();
        others.clear();
    }

    private void saveThemes(ThemeResponse themeResponse) {
        if (themeResponse.getSubscribed() != null) {
            subscribed.clear();
            subscribed.addAll(themeResponse.getSubscribed());
        }

        if (themeResponse.getThemes() != null) {
            others.clear();
            others.addAll(themeResponse.getThemes());
        }
    }

    public Observable<ThemeData> getThemeData(int index) {
        return newsService.getThemeData(index)
                .compose(new SchedulerTransformer<>());
    }
}
