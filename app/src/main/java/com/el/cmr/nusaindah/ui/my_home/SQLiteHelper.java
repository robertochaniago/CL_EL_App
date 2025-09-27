package com.el.cmr.nusaindah.ui.my_home;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.el.cmr.nusaindah.ui.my_home.page.model.HomeModel;
import com.el.cmr.nusaindah.ui.my_home.page.model.ImageModel;
import com.el.cmr.nusaindah.ui.my_home.page.model.MoreModel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class SQLiteHelper extends SQLiteOpenHelper {

    private SQLiteDatabase sqLiteDatabase;
    private final Context myContext;
    public String DB_PATH = null;
    public static final String DB_NAME = "brainrot_v1.db";

    public SQLiteHelper(@Nullable Context context) {
        super(context, DB_NAME, null, 20);
        this.myContext = context;
        this.DB_PATH = myContext.getDatabasePath(DB_NAME).getPath();
    }

    public void createParkourDB() throws IOException {
        boolean dbExist = checkDB();
        if (!dbExist) {
            try { copyDataBase();
            } catch (IOException e) { throw new Error("Error copying database"); }
            this.close();
        }
    }

    public boolean checkDB() {
        SQLiteDatabase checkDB = null;
        try { String myPath = DB_PATH;
            checkDB = SQLiteDatabase.openDatabase(myPath,null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
        }
        if (checkDB != null) { checkDB.close(); }
        return checkDB != null ? true : false;
    }

    private void copyDataBase() throws IOException {
        InputStream myInput = myContext.getAssets().open(DB_NAME);
        String outFileName = DB_PATH;
        OutputStream myOutput = new FileOutputStream(outFileName);
        byte[] buffer = new byte[10];
        int length;
        while ((length = myInput.read(buffer)) > 0) { myOutput.write(buffer, 0, length); }
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    public void openDB() throws SQLException {
        String myPath = DB_PATH;
        sqLiteDatabase = SQLiteDatabase.openDatabase(myPath,null, SQLiteDatabase.OPEN_READWRITE);
    }

    @Override
    public synchronized void close() {
        if (sqLiteDatabase != null)
            sqLiteDatabase.close();
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion)
            try { copyDataBase();
            } catch (IOException e) { e.printStackTrace(); }
    }

    public void closeDB() {
        if(sqLiteDatabase!=null) { sqLiteDatabase.close(); }
    }

    public List<HomeModel> getDataHomeActivity(int status){
        HomeModel homeModel = null;
        List<HomeModel> homeModelList = new ArrayList<>();

        openDB();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT b.*, a.download_url, a.name FROM mod_download a\n" +
                "INNER JOIN (SELECT a.mod_id, a.parkour_title, b.image_url FROM mod_master a \n" +
                "INNER JOIN (SELECT a._id, a.mod_id, a.image_url FROM mod_image a\n" +
                "INNER JOIN (SELECT _id, mod_id FROM ( SELECT _id, mod_id FROM mod_image ORDER BY RANDOM() ) GROUP by mod_id) b\n" +
                "ON a._id = b._id) b ON a.mod_id = b.mod_id AND a.primary_status =" + status + " ) b ON a.mod_id = b.mod_id ORDER BY RANDOM()" , null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            homeModel = new HomeModel(cursor.getInt(0),cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4));
            homeModelList.add(homeModel);
            cursor.moveToNext();
        }
        cursor.close();
        closeDB();
        return homeModelList;
    }

    public List<MoreModel> getDataMoreActivity(int mod_id){
        MoreModel moreModel = null;
        List<MoreModel> moreModelList = new ArrayList<>();

        openDB();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT b.*, a.download_url, a.name FROM mod_download a\n" +
                "INNER JOIN (SELECT a.mod_id, a.parkour_title, b.image_url FROM mod_master a \n" +
                "INNER JOIN (SELECT a._id, a.mod_id, a.image_url FROM mod_image a\n" +
                "INNER JOIN (SELECT _id, mod_id FROM ( SELECT _id, mod_id FROM mod_image ORDER BY RANDOM() ) GROUP by mod_id) b\n" +
                "ON a._id = b._id) b ON a.mod_id = b.mod_id AND a.primary_status = 0) b ON a.mod_id = b.mod_id WHERE a.mod_id != " + mod_id + " ORDER BY RANDOM()" , null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            moreModel = new MoreModel(cursor.getInt(0),cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4));
            moreModelList.add(moreModel);
            cursor.moveToNext();
        }
        cursor.close();
        closeDB();
        return moreModelList;
    }

    public List<ImageModel> getDataImageActivity(int mod_id){
        ImageModel imageModel = null;
        List<ImageModel> imageModelList = new ArrayList<>();

        openDB();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT mod_id, image_url FROM mod_image WHERE mod_id = " + mod_id, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            imageModel = new ImageModel(cursor.getInt(0),cursor.getString(1));
            imageModelList.add(imageModel);
            cursor.moveToNext();
        }
        cursor.close();
        closeDB();
        return imageModelList;
    }
}
