package com.dreams.chat.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.dreams.chat.R;
import com.dreams.chat.activities.StatusStoriesActivity;
import com.dreams.chat.models.StatusImageList;
import com.dreams.chat.models.StatusNew;
import com.squareup.picasso.Picasso;

import io.realm.RealmList;

public class StatusItemAdapter extends RecyclerView.Adapter<StatusItemAdapter.ViewHolder> {

    private Context context;
    private RealmList<StatusImageList> urlList;
    private StatusNew chatUser;

    public StatusItemAdapter(Context context, RealmList<StatusImageList> urlList, StatusNew chatUser) {
        this.context = context;
        this.urlList = urlList;
        this.chatUser = chatUser;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.adaper_stautus_grid, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {
        Picasso.get()
                .load(urlList.get(i).getUrl())
                .tag(this)
                .placeholder(R.drawable.ic_avatar)
                .into(viewHolder.statusImage);


        viewHolder.statusLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (urlList != null && urlList.size() > 0) {
                    Intent a = new Intent(context, StatusStoriesActivity.class);
                    a.putExtra(StatusStoriesActivity.STATUS_RESOURCES_KEY, new String[]{urlList.get(i).getUrl()});
                    a.putExtra(StatusStoriesActivity.STATUS_DURATION_KEY, 5000L);
                    a.putExtra(StatusStoriesActivity.IS_IMMERSIVE_KEY, true);
                    a.putExtra(StatusStoriesActivity.IS_CACHING_ENABLED_KEY, true);
                    a.putExtra(StatusStoriesActivity.IS_TEXT_PROGRESS_ENABLED_KEY, false);
                    a.putExtra(StatusStoriesActivity.USER_NAME, "My Status");
                    a.putExtra(StatusStoriesActivity.URL, chatUser.getUser().getImage());
                    StatusStoriesActivity.FROM = true;
                    a.putExtra(StatusStoriesActivity.RECIPIENT_ID, chatUser.getStatusImages().get(0).getSenderId());
                    context.startActivity(a);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return urlList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView statusImage;
        private LinearLayout statusLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusImage = itemView.findViewById(R.id.statusImage);
            statusLayout = itemView.findViewById(R.id.statusLayout);
        }
    }
}
