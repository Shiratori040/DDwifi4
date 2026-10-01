package com.example.ddwifi4;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    private List<Post> mPosts;
    private Context mContext;
    private String searchText = "";
    private double myLatitude;
    private double myLongitude;

    public PostAdapter(Context context, List<Post> posts, double myLatitude, double myLongitude) {
        this.mContext = context;
        this.mPosts = posts;
        this.myLatitude = myLatitude;
        this.myLongitude = myLongitude;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText.toLowerCase();
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView contentTextView;
        public TextView timestampTextView;
        public TextView expiryTextView;
        public TextView locationTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.text_view_content);
            timestampTextView = itemView.findViewById(R.id.text_view_timestamp);
            expiryTextView = itemView.findViewById(R.id.text_view_expiry);
            locationTextView = itemView.findViewById(R.id.text_view_location);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Post clickedPost = mPosts.get(position);
                showDeleteConfirmationDialog(position, clickedPost);
            }
        }
    }

    private void showDeleteConfirmationDialog(int position, Post post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("データの削除");
        builder.setMessage("このデータを削除しますか？");
        builder.setPositiveButton("はい", (dialog, which) -> {
            DatabaseHelper dbHelper = new DatabaseHelper(mContext);
            dbHelper.deletePost(post.getId());  // データベースから削除
            mPosts.remove(position);             // リストから削除
            notifyItemRemoved(position);         // リスト更新
        });
        builder.setNegativeButton("いいえ", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @NonNull
    @Override
    public PostAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostAdapter.ViewHolder holder, int position) {
        Post post = mPosts.get(position);

        SpannableString spannableContent = new SpannableString(post.getContent());
        int startIndex = post.getContent().toLowerCase().indexOf(searchText);
        if (startIndex != -1 && !searchText.isEmpty()) {
            int endIndex = startIndex + searchText.length();
            spannableContent.setSpan(new ForegroundColorSpan(Color.RED), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.contentTextView.setText(spannableContent);

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date parsedDate = inputFormat.parse(post.getTimestamp());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN);
            outputFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
            holder.timestampTextView.setText("投稿日時: " + outputFormat.format(parsedDate));
        } catch (Exception e) {
            holder.timestampTextView.setText("日時取得エラー");
        }

        SimpleDateFormat expiryFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN);
        expiryFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        holder.expiryTextView.setText("有効期限: " + expiryFormat.format(new Date(post.getExpiryTimestamp())));

        // 距離と方向を計算して表示
        String distanceAndDirection = getDistanceAndDirection(myLatitude, myLongitude, post.getLatitude(), post.getLongitude());
        holder.locationTextView.setText(distanceAndDirection);

        // locationTextViewにクリックリスナーを設定してダイアログを表示
        holder.locationTextView.setOnClickListener(v -> showLocationDialog(post.getLatitude(), post.getLongitude()));
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    // 自分と相手の緯度経度から距離と方向を計算するメソッド
    private String getDistanceAndDirection(double myLatitude, double myLongitude, double otherLatitude, double otherLongitude) {
        Location myLocation = new Location("myLocation");
        myLocation.setLatitude(myLatitude);
        myLocation.setLongitude(myLongitude);

        Location otherLocation = new Location("otherLocation");
        otherLocation.setLatitude(otherLatitude);
        otherLocation.setLongitude(otherLongitude);

        // 距離の計算
        float distance = myLocation.distanceTo(otherLocation);
        String distanceStr = distance >= 1000 ? String.format("%.2f km", distance / 1000) : String.format("%.2f m", distance);

        // 方角の計算
        float bearing = myLocation.bearingTo(otherLocation);
        String directionStr = getDirectionString(bearing);

        return distanceStr + " " + directionStr;
    }

    // 方角を8方位に変換するメソッド
    private String getDirectionString(float bearing) {
        if (bearing < 0) bearing += 360;

        if (bearing >= 337.5 || bearing < 22.5) return "北";
        else if (bearing >= 22.5 && bearing < 67.5) return "北東";
        else if (bearing >= 67.5 && bearing < 112.5) return "東";
        else if (bearing >= 112.5 && bearing < 157.5) return "南東";
        else if (bearing >= 157.5 && bearing < 202.5) return "南";
        else if (bearing >= 202.5 && bearing < 247.5) return "南西";
        else if (bearing >= 247.5 && bearing < 292.5) return "西";
        else return "北西";
    }

    // 緯度経度をダイアログに表示するメソッド
    private void showLocationDialog(double latitude, double longitude) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("投稿場所の情報");
        builder.setMessage("緯度: " + latitude + "\n経度: " + longitude);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}
