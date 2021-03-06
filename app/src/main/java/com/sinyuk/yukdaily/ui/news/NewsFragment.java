package com.sinyuk.yukdaily.ui.news;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.sinyuk.yukdaily.App;
import com.sinyuk.yukdaily.R;
import com.sinyuk.yukdaily.base.ListFragment;
import com.sinyuk.yukdaily.data.news.NewsRepository;
import com.sinyuk.yukdaily.data.news.NewsRepositoryModule;
import com.sinyuk.yukdaily.databinding.NewsHeaderLayoutBinding;
import com.sinyuk.yukdaily.entity.news.Stories;
import com.sinyuk.yukdaily.events.HomepageLoadingEvent;
import com.sinyuk.yukdaily.utils.cardviewpager.ShadowTransformer;
import com.sinyuk.yukdaily.utils.recyclerview.ListItemMarginDecoration;
import com.sinyuk.yukdaily.utils.recyclerview.SlideInUpAnimator;
import com.sinyuk.yukdaily.widgets.CircleIndicator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import javax.inject.Inject;

import rx.Observer;
import rx.functions.Action0;

/**
 * Created by Sinyuk on 2016/10/12.
 */

public class NewsFragment extends ListFragment {
    public static final String TAG = "NewsFragment";
    @Inject
    NewsRepository newsRepository;
    private NewsHeaderLayoutBinding headerBinding;
    private final Observer<Stories> refreshObserver = new Observer<Stories>() {
        @Override
        public void onCompleted() {
            if (binding.listLayout.recyclerView.getAdapter().getItemCount() <= 0) {
                assertEmpty(getString(R.string.no_news));
            } else {
                final int count = headerBinding.viewPager.getChildCount();
                if (count >= 3) {
                    headerBinding.viewPager.setCurrentItem((int) Math.floor(count / 2f));
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            assertEmpty(getString(R.string.network_error));
        }

        @Override
        public void onNext(Stories stories) {
            if (headerBinding != null) {
                headerBinding.getAdapter().setData(stories.getTopStories());
            }

            ((NewsAdapter) binding.listLayout.recyclerView.getAdapter()).setData(stories.getStories());
        }
    };
    //
    private int fromToday = 1;
    private final Observer<Stories> loadObserver = new Observer<Stories>() {
        @Override
        public void onCompleted() {
            fromToday++;
        }

        @Override
        public void onError(Throwable e) {
            assertError(e.getLocalizedMessage());
        }

        @Override
        public void onNext(Stories oldNews) {
            ((NewsAdapter) binding.listLayout.recyclerView.getAdapter()).appendData(oldNews.getStories());
        }
    };


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        App.get(context).getAppComponent().plus(new NewsRepositoryModule()).inject(this);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void refreshData() {
        addSubscription(newsRepository.getLatestNews()
                .doOnTerminate(() -> fromToday = 1)
                .doOnTerminate(this::stopRefreshing).subscribe(refreshObserver));
    }

    @Override
    protected void fetchData() {
        addSubscription(newsRepository.getNewsAt(fromToday).doOnTerminate(this::stopLoading).subscribe(loadObserver));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initListLayout();
        binding.listLayout.swipeRefreshLayout.setEnabled(false);
        initListView();
        initListData();

    }

    private void bindHeaderView() {
        headerBinding = DataBindingUtil.inflate(
                LayoutInflater.from(getContext()), R.layout.news_header_layout, binding.listLayout.recyclerView, false);

        headerBinding.setContext(getContext());

        headerBinding.setAdapter(new CardPagerAdapter());

    }


    private void initListView() {
        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setAutoMeasureEnabled(true);
        binding.listLayout.recyclerView.setLayoutManager(manager);
        binding.listLayout.recyclerView.setItemAnimator(new SlideInUpAnimator(new FastOutSlowInInterpolator()));
        binding.listLayout.recyclerView.setHasFixedSize(true);
        binding.listLayout.recyclerView.addItemDecoration(new ListItemMarginDecoration(R.dimen.content_space_8, false, getContext()));
        binding.listLayout.recyclerView.addOnScrollListener(getLoadMoreListener());
    }

    private void initListData() {
        final NewsAdapter newsAdapter = new NewsAdapter();
        newsAdapter.setHasStableIds(true);
        bindHeaderView();
        newsAdapter.addHeaderBinding(headerBinding);
        binding.listLayout.recyclerView.setAdapter(newsAdapter);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onStartLoading(HomepageLoadingEvent event) {
        refreshData();
    }
}
