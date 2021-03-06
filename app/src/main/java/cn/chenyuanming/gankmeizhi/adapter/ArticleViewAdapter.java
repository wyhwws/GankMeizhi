package cn.chenyuanming.gankmeizhi.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.chenyuanming.gankmeizhi.R;
import cn.chenyuanming.gankmeizhi.activity.ShowBigImageActivity;
import cn.chenyuanming.gankmeizhi.activity.WebViewActivity;
import cn.chenyuanming.gankmeizhi.beans.CommonGoodsBean;
import cn.chenyuanming.gankmeizhi.beans.db.FavoriteBean;
import cn.chenyuanming.gankmeizhi.beans.db.ReadArticles;
import cn.chenyuanming.gankmeizhi.utils.DbHelper;
import cn.chenyuanming.gankmeizhi.utils.ShareUtils;
import cn.chenyuanming.gankmeizhi.utils.TimeHelper;

/**
 * Created by Chen Yuanming on 2016/1/28.
 */
public class ArticleViewAdapter extends RecyclerView.Adapter<ArticleViewAdapter.ViewHolder> {

    private List<CommonGoodsBean.Results> mDatas = new ArrayList<>();

    FavoriteBean favorite = DbHelper.getHelper().getData(FavoriteBean.class).get(0);
    ReadArticles readArticles = DbHelper.getHelper().getData(ReadArticles.class).get(0);
    static Drawable defaultShare;
    Date currDate = new Date();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.iv_meizhi)
        ImageView iv_meizhi;
        @Bind(R.id.tv_type)
        TextView tv_type;
        @Bind(R.id.tv_title)
        TextView tv_title;
        @Bind(R.id.tv_time)
        TextView tv_time;
        @Bind(R.id.tv_author)
        TextView tv_author;
        @Bind(R.id.iv_share)
        ImageView iv_share;
        @Bind(R.id.iv_favorite)
        ImageView ivFavorite;
        @Bind(R.id.webView)
        WebView webView;
        View mainView;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mainView = view;
            defaultShare = iv_share.getDrawable();
            defaultShare.setColorFilter(Color.parseColor("#bfc8d6"), PorterDuff.Mode.SRC_IN);
            iv_share.setImageDrawable(defaultShare);
        }
    }

    public String getValueAt(int position) {
        return mDatas.get(position).desc;
    }

    Context context;
    int fragType;

    public ArticleViewAdapter(Context context, List<CommonGoodsBean.Results> items, int fragType) {
        this.context = context;
        if (items != null) {
            mDatas.addAll(items);
        }
        this.fragType = fragType;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_article, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        CommonGoodsBean.Results data = mDatas.get(position);

        holder.tv_title.setText(data.desc);

        try {
            Date publishDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(data.publishedAt);
            holder.tv_time.setText(TimeHelper.getTime(publishDate.getTime()));
        } catch (ParseException e) {
            holder.tv_time.setText(data.updatedAt.substring(0, data.updatedAt.indexOf("T")));
            e.printStackTrace();
        }

        holder.tv_author.setText("by @" + data.who);
        holder.tv_type.setText(data.type);
        if (readArticles.articles.contains(data.objectId)) {
            holder.tv_title.setTextColor(context.getResources().getColor(R.color.lightBlack));
        } else {
            holder.tv_title.setTextColor(context.getResources().getColor(R.color.black));
        }


        if (data.type.equals("福利") || data.type.equals("休息视频")) {
            if (data.type.equals("休息视频")) {
                holder.tv_title.setVisibility(View.GONE);
                holder.webView.setVisibility(View.VISIBLE);
                holder.iv_meizhi.setVisibility(View.GONE);
                initWebview(holder.webView);
                holder.webView.loadUrl(data.url);
                holder.webView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, WebViewActivity.class);
                    intent.putExtra("url", mDatas.get(position).url);
                    intent.putExtra("objectId", mDatas.get(position).objectId);
                    context.startActivity(intent);
                });
            } else {
                holder.tv_title.setVisibility(View.GONE);
                holder.webView.setVisibility(View.GONE);
                holder.webView.stopLoading();
                holder.iv_meizhi.setVisibility(View.VISIBLE);
                Glide.with(context).load(data.url).into(holder.iv_meizhi);
                holder.mainView.setOnClickListener((v) -> {
                    Intent intent = new Intent(context, ShowBigImageActivity.class);
                    intent.putExtra("data", mDatas.get(position));
                    context.startActivity(intent);
                });
            }
        } else {
            holder.tv_title.setVisibility(View.VISIBLE);
            holder.webView.setVisibility(View.GONE);
            holder.webView.stopLoading();
            holder.iv_meizhi.setVisibility(View.GONE);
            holder.mainView.setOnClickListener(v -> {
                holder.tv_title.setTextColor(context.getResources().getColor(R.color.lightBlack));
                Intent intent = new Intent(context, WebViewActivity.class);
                intent.putExtra("url", mDatas.get(position).url);
                intent.putExtra("objectId", mDatas.get(position).objectId);
                context.startActivity(intent);
                readArticles.articles.add(data.objectId);
                DbHelper.getHelper().getLiteOrm().save(readArticles);
            });
        }

        setFavoriteIcon(holder.ivFavorite, favorite.favorites, data.objectId);
        holder.ivFavorite.setOnClickListener(v -> {
            onFavoriteClicked(holder.ivFavorite, favorite.favorites, data.objectId);
            DbHelper.getHelper().getLiteOrm().save(favorite);
        });

        holder.iv_share.setOnClickListener(v -> ShareUtils.share(context, data.desc + data.url));

        switch (data.type) {
            case "Android":
                holder.tv_type.setBackgroundResource(R.drawable.shape_type_android);
                break;
            case "iOS":
                holder.tv_type.setBackgroundResource(R.drawable.shape_type_ios);
                break;
            case "拓展资源":
            case "App":
                holder.tv_type.setBackgroundResource(R.drawable.shape_type_extend);
                break;
            case "休息视频":
            default:
                holder.tv_type.setBackgroundResource(R.drawable.shape_type_relax);
                break;
        }
    }

    private void initWebview(WebView webView) {
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.getSettings().setAppCacheEnabled(true);

        webView.requestFocus();
        webView.getSettings().getAllowFileAccess();
    }

    private void onFavoriteClicked(ImageView ivFavorite, TreeSet<String> favorites, String objectId) {
        if (favorites.contains(objectId)) {
            favorites.remove(objectId);
        } else {
            favorites.add(objectId);
        }
        setFavoriteIcon(ivFavorite, favorites, objectId);
    }

    private void setFavoriteIcon(ImageView ivFavorite, TreeSet<String> favorites, String objectId) {
        Drawable drawable = ivFavorite.getDrawable();
        if (favorites.contains(objectId)) {
            drawable.setColorFilter(Color.parseColor("#ff0000"), PorterDuff.Mode.SRC_IN);
            ivFavorite.setImageDrawable(drawable);
        } else {
            drawable.setColorFilter(Color.parseColor("#bfc8d6"), PorterDuff.Mode.SRC_IN);
            ivFavorite.setImageDrawable(drawable);
        }
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public List<CommonGoodsBean.Results> getDatas() {
        return mDatas;
    }

}
