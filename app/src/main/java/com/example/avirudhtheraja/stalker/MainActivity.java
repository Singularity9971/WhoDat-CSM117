package com.example.avirudhtheraja.stalker;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.params.Face;
import android.media.FaceDetector.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int MenuShootImage = 69;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private String pictureImagePath;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final static String APP_ID = "bd6499fe";
    private final static String APP_KEY = "7b5efae26a84635db56d8c66aa2cc39f";
    private final static String GALLERY_NAME = "galleryest1";
    private final static String postUrl = "https://api.kairos.com/enroll";
    private final static String postUrlReco = "https://api.kairos.com/recognize";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //new CustomAsync().execute("");    //remove after data set is done

        verifyStoragePermissions(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.camera));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = timeStamp + ".jpg";
                File storageDir = Environment.getExternalStorageDirectory();
                pictureImagePath = storageDir.getAbsolutePath() + "/" + imageFileName;
                File file = new File(pictureImagePath);
                Uri outputFileUri = Uri.fromFile(file);
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                startActivityForResult(cameraIntent, MenuShootImage);
        }});
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        if(requestCode==MenuShootImage && resultCode==RESULT_OK)
        {
            File imgFile = new  File(pictureImagePath);
            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                uploadPhoto(myBitmap);

            }
            imgFile.delete();
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void uploadPhoto(Bitmap myBitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
        new CustomAsync().execute(encoded);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class CustomAsync extends AsyncTask<String,Void,String>{

        private ProgressDialog pd = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            pd.show();
            pd.setMessage("Finding best match");
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            String photo = params[0];
            try {
                URL url = new URL(postUrlReco); //change url based on enroll or recognize
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.addRequestProperty("app_id",APP_ID);
                conn.addRequestProperty("app_key",APP_KEY);
                conn.setRequestProperty("Content-Type","application/json");
                conn.setRequestProperty("Host", "api.kairos.com");
                conn.connect();

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("image",photo);
                //jsonParam.put("subject_id","linkedin profile");
                jsonParam.put("gallery_name",GALLERY_NAME);
                jsonParam.put("threshold","0.6");
                DataOutputStream printout = new DataOutputStream(conn.getOutputStream ());
                printout.write(jsonParam.toString().getBytes("UTF-8"));
                printout.flush ();
                printout.close ();

                Log.d("Avi","Connection created, code is "+ conn.getResponseCode()+" "+conn.getResponseMessage());
                String check;
                String json = "";
                BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while((check = input.readLine()) != null) {
                    Log.d("Avi", check);
                    json += check;
                }
                return getLinkedinID(new JSONObject(json));

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected String getLinkedinID(JSONObject json) throws JSONException{
            JSONObject transaction = json.getJSONArray("images").getJSONObject(0).getJSONObject("transaction");
            if(!transaction.getString("status").equals("success"))
            {
                Log.d("Avi","Failed, no match found");
                return null;
            }
            Log.d("Avi","Best match is "+transaction.getString("subject"));
            return transaction.getString("subject");
        }

        @Override
        protected void onPostExecute(String s) {
            pd.hide();
            if(s == null){
                Toast.makeText(MainActivity.this,"Sorry, we couldn't find a match",Toast.LENGTH_LONG).show();
                return;
            }
            else if(s.equals("Avirudh"))        //one hardcoded case since I have 2 profiles in the dataset
                s = "avirudh-theraja-084159111";
            Intent intent = new Intent(MainActivity.this,DisplayPageActivity.class);
            intent.putExtra("id",s);
            startActivity(intent);
        }
    }

}
