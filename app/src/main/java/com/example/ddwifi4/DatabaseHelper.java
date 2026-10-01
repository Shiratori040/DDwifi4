package com.example.ddwifi4;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bulletin_board.db";
    private static final int DATABASE_VERSION = 5;
    public static final String TABLE_NAME = "posts";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_EXPIRY_TIMESTAMP = "expiry_timestamp";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CONTENT + " TEXT NOT NULL, "
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + COLUMN_EXPIRY_TIMESTAMP + " INTEGER, "
                + COLUMN_LATITUDE + " REAL, "
                + COLUMN_LONGITUDE + " REAL" + ");";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public String getDatabaseAsJson() {
        List<Post> posts = getAllPosts();
        JSONArray jsonArray = new JSONArray();
        for (Post post : posts) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", post.getId());
                jsonObject.put("content", post.getContent());
                jsonObject.put("timestamp", post.getTimestamp());
                jsonObject.put("expiryTimestamp", post.getExpiryTimestamp());
                jsonObject.put("latitude", post.getLatitude());
                jsonObject.put("longitude", post.getLongitude());
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return jsonArray.toString();
    }

    // 投稿を追加し、掲載期限と位置情報を設定
    public void addPost(String content, long expiryTimestamp, double latitude, double longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_EXPIRY_TIMESTAMP, expiryTimestamp);
        values.put(COLUMN_LATITUDE, latitude);
        values.put(COLUMN_LONGITUDE, longitude);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // 投稿を削除
    public void deletePost(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // 全ての投稿を取得し、掲載期限が過ぎたものは除外
    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_ID, COLUMN_CONTENT, COLUMN_TIMESTAMP, COLUMN_EXPIRY_TIMESTAMP, COLUMN_LATITUDE, COLUMN_LONGITUDE},
                COLUMN_EXPIRY_TIMESTAMP + " > ? OR " + COLUMN_EXPIRY_TIMESTAMP + " IS NULL",
                new String[]{String.valueOf(currentTime)},
                null, null, COLUMN_ID + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                long expiryTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPIRY_TIMESTAMP));
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE));
                posts.add(new Post(id, content, timestamp, expiryTimestamp, latitude, longitude));
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return posts;
    }

    // 最新の投稿を取得
    public Post getLatestPost() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_ID, COLUMN_CONTENT, COLUMN_TIMESTAMP, COLUMN_EXPIRY_TIMESTAMP, COLUMN_LATITUDE, COLUMN_LONGITUDE},
                null, null, null, null, COLUMN_ID + " DESC", "1");

        Post post = null;
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
            long expiryTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPIRY_TIMESTAMP));
            double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE));
            double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE));
            post = new Post(id, content, timestamp, expiryTimestamp, latitude, longitude);
            cursor.close();
        }
        db.close();
        return post;
    }

    // JSONからデータベースにデータを挿入
    public void insertJsonToDatabase(String jsonData) {
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String content = jsonObject.getString("content");
                long expiryTimestamp = jsonObject.getLong("expiryTimestamp");
                double latitude = jsonObject.getDouble("latitude");
                double longitude = jsonObject.getDouble("longitude");
                addPost(content, expiryTimestamp, latitude, longitude);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
