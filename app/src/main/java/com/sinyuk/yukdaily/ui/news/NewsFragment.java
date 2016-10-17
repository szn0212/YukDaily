package com.sinyuk.yukdaily.ui.news;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sinyuk.myutils.system.ScreenUtils;
import com.sinyuk.yukdaily.App;
import com.sinyuk.yukdaily.R;
import com.sinyuk.yukdaily.api.NewsService;
import com.sinyuk.yukdaily.base.BaseFragment;
import com.sinyuk.yukdaily.databinding.NewsFragmentBinding;
import com.sinyuk.yukdaily.model.LatestNews;
import com.sinyuk.yukdaily.utils.cardviewpager.ShadowTransformer;

import javax.inject.Inject;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Sinyuk on 2016/10/12.
 */

public class NewsFragment extends BaseFragment {
    @Inject
    NewsService newsService;
    private NewsFragmentBinding binding;
    private NewsAdapter newsAdapter;
    private CardPagerAdapter headerAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        App.get(context).getAppComponent().inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.news_fragment, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initHeader();
        initListView();
        initListData();

        refresh();
    }

    private void initHeader() {
        headerAdapter = new CardPagerAdapter();
        binding.viewPager.setAdapter(headerAdapter);
        binding.viewPager.setOffscreenPageLimit(3);
        binding.viewPager.setPageMargin(ScreenUtils.dpToPxInt(getContext(), 16));

        final ShadowTransformer transformer = new ShadowTransformer(binding.viewPager, headerAdapter);
        binding.viewPager.setPageTransformer(false, transformer);
        transformer.enableScaling(true);

        headerAdapter.registerDataSetObserver(binding.indicator.getDataSetObserver());
    }

    private void refresh() {
        newsService.getLatestNews()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<LatestNews>() {
                    @Override
                    public void onCompleted() {
                        if (binding.viewPager.getChildCount() >= 3) {
                            binding.viewPager.setCurrentItem(1);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(LatestNews latestNews) {
                        headerAdapter.setData(latestNews.getTopStories());
                        newsAdapter.setData(latestNews.getStories());
                    }
                });
    }

    private void initListView() {
        final LinearLayoutManager manager = new LinearLayoutManager(getContext());
        manager.setAutoMeasureEnabled(true);
        binding.recyclerView.setLayoutManager(manager);
        binding.recyclerView.setHasFixedSize(true);
    }

    private void initListData() {
        newsAdapter = new NewsAdapter();
        newsAdapter.setHasStableIds(true);
        binding.recyclerView.setAdapter(newsAdapter);
    }
}
