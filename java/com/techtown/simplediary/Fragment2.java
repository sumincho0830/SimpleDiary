package com.techtown.simplediary;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.github.channguyen.rsv.RangeSliderView;
import com.yanzhenjie.permission.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Fragment2 extends Fragment {
    private static final String TAG = "Fragment2";

    int mMode = AppConstants.MODE_INSERT;
    int _id = -1;
    int weatherIndex = 0;

    RangeSliderView moodSlider;
    int moodIndex = 2;

    Note item;

    Context context;
    OnTabItemSelectedListener listener;
    OnRequestListener requestListener;

    ImageView weatherIcon;
    TextView dateTextView;
    TextView locationTextView;

    EditText contentsInput;
    ImageView pictureImageView;

    boolean isPhotoCaptured;
    boolean isPhotoFileSaved;
    boolean isPhotoCanceled;

    int selectedPhotoMenu;

    Uri uri;
    File file;
    Bitmap resultPhotoBitmap;

    SimpleDateFormat todayDateFormat;
    String currentDateString;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        this.context = context;

        if(context instanceof OnTabItemSelectedListener){
            listener = (OnTabItemSelectedListener) context;
        }

        if(context instanceof OnRequestListener){
            requestListener = (OnRequestListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(context != null){
            context = null;
            listener = null;
            requestListener = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment2, container,false);
        initUI(rootView);

        if(requestListener != null){
            requestListener.onRequest("getCurrentLocation"); //커맨드..?
        }

        applyItem();

        return rootView;
    }

    private void initUI(ViewGroup rootView){
        weatherIcon = rootView.findViewById(R.id.weatherIcon);
        dateTextView = rootView.findViewById(R.id.dateTextView);
        locationTextView = rootView.findViewById(R.id.locationTextView);

        contentsInput = rootView.findViewById(R.id.contentsInput);
        pictureImageView = rootView.findViewById(R.id.pictureImageView);
        pictureImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPhotoCaptured || isPhotoFileSaved){
                    showDialog(AppConstants.CONTENT_PHOTO_EX);
                }else{
                    showDialog(AppConstants.CONTENT_PHOTO);
                }
            }
        });

        Button saveButton = rootView.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMode == AppConstants.MODE_INSERT){
                    saveNote(); //현재 입력된 사항 저장
                }else if(mMode == AppConstants.MODE_MODIFY){
                    modifyNote(); //현재 수정된 사항 저장
                }

                if(listener != null){
                    listener.onTabSelected(0); //저장 후 0번으로 이동
                }
            }
        });

        Button deleteButton = rootView.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNote();

                if(listener != null){
                    listener.onTabSelected(0);
                }
            }
        });

        Button closeButton = rootView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null){
                    listener.onTabSelected(0);
                }
                //입력된 데이터는 그대로 두는가? 화면이 넘어가면 초기화 해야 하나? 모르겠네
            }
        });

        moodSlider = rootView.findViewById(R.id.sliderView);
        final RangeSliderView.OnSlideListener listener = new RangeSliderView.OnSlideListener() {
            @Override
            public void onSlide(int index) {
                AppConstants.println("moodIndex changed to "+index);
                moodIndex = index;
            }
        };

        moodSlider.setOnSlideListener(listener);
        moodSlider.setInitialIndex(2);
    }

    public void setAddress(String data){
        locationTextView.setText(data);
    }
    public void setDateString(String dateString){
        dateTextView.setText(dateString);
    }

    public void setContents(String data){
        contentsInput.setText(data);
    }

    public void setPicture(String picturePath, int sampleSize){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        resultPhotoBitmap = BitmapFactory.decodeFile(picturePath, options);

        pictureImageView.setImageBitmap(resultPhotoBitmap);
    }

    public void setMood(String mood){
        try{
            moodIndex = Integer.parseInt(mood);
            moodSlider.setInitialIndex(moodIndex);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setItem(Note item){
        this.item = item;
    }


    public void applyItem(){
        AppConstants.println("applyItem called.");

        if(item != null){
            mMode = AppConstants.MODE_MODIFY;

            setWeatherIndex(Integer.parseInt(item.getWeather()));
            setAddress(item.getAddress());
            setDateString(item.getCreateDateStr());
            setContents(item.getContents());

            String picturePath = item.getPicture();
            AppConstants.println("picturePath : "+picturePath);

            if(picturePath == null || picturePath.equals("")){
                pictureImageView.setImageResource(R.drawable.noimagefound);
            }else{
                setPicture(item.getPicture(), 1);
            }

            setMood(item.getMood());
        }else{
            mMode = AppConstants.MODE_INSERT;

            setWeatherIndex(0);
            setAddress("");

            Date currentDate = new Date();
            if(todayDateFormat == null){
                todayDateFormat = new SimpleDateFormat("MM월 dd일");
            }
            currentDateString = todayDateFormat.format(currentDate);
            AppConstants.println("currentDateString : "+currentDateString);
            setDateString(currentDateString);

            contentsInput.setText("");
            pictureImageView.setImageResource(R.drawable.noimagefound);
            setMood("2");

        }
    }


    public void setWeather(String data){
        AppConstants.println("setWeather called: "+data);

        if(data != null){
            if(data.equals("맑음")){
                weatherIcon.setImageResource(R.drawable.weather_1);
                weatherIndex = 0;
            }else if(data.equals("구름 조금")){
                weatherIcon.setImageResource(R.drawable.weather_2);
                weatherIndex = 1;
            }else if(data.equals("구름 많음")){
                weatherIcon.setImageResource(R.drawable.weather_3);
                weatherIndex = 2;
            }else if(data.equals("흐림")){
                weatherIcon.setImageResource(R.drawable.weather_4);
                weatherIndex = 3;
            }else if(data.equals("비")){
                weatherIcon.setImageResource(R.drawable.weather_5);
                weatherIndex = 4;
            }else if(data.equals("눈/비")){
                weatherIcon.setImageResource(R.drawable.weather_6);
                weatherIndex = 5;
            }else if(data.equals("눈")){
                weatherIcon.setImageResource(R.drawable.weather_7);
                weatherIndex = 6;
            }else{
                Log.d(TAG, "Unkown weather string: "+data);
            }
        }
    }

    public void setWeatherIndex(int index){
        if(index == 0){
            weatherIcon.setImageResource(R.drawable.weather_1);
            weatherIndex = 0;
        }else if(index == 1){
            weatherIcon.setImageResource(R.drawable.weather_2);
            weatherIndex = 1;
        }else if(index == 2){
            weatherIcon.setImageResource(R.drawable.weather_3);
            weatherIndex = 2;
        }else if(index == 3){
            weatherIcon.setImageResource(R.drawable.weather_4);
            weatherIndex = 3;
        }else if(index == 4){
            weatherIcon.setImageResource(R.drawable.weather_5);
            weatherIndex = 4;
        }else if(index == 5){
            weatherIcon.setImageResource(R.drawable.weather_6);
            weatherIndex = 5;
        }else if(index == 6){
            weatherIcon.setImageResource(R.drawable.weather_7);
            weatherIndex = 6;
        }else{
            Log.d(TAG, "Unkown weather index: "+index);
        }
    }

    public void showDialog(int id){
        AlertDialog.Builder builder = null;
        //AlertDialog를 활용한다. 일단 만들어 놓는다.

        switch (id){
            case AppConstants.CONTENT_PHOTO:
                builder= new AlertDialog.Builder(context); //MainActivity의 context이다
                builder.setTitle("사진 메뉴 선택");
                builder.setSingleChoiceItems(R.array.array_photo, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedPhotoMenu = which; //어떤 선택지인지 저장한다
                    }
                });
                builder.setPositiveButton("선택", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(selectedPhotoMenu == 0){
                            showPhotoCaptureActivity();
                        }else if(selectedPhotoMenu == 1){
                            showPhotoSelectionActivity();
                        }
                    }
                });
                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //아무것도 하지 않는다.
                    }
                });

                break;

            case AppConstants.CONTENT_PHOTO_EX:
                builder = new AlertDialog.Builder(context);
                builder.setTitle("사진 메뉴 선택");
                builder.setSingleChoiceItems(R.array.array_photo_ex, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedPhotoMenu = which;
                    }
                });

                builder.setPositiveButton("선택", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(selectedPhotoMenu == 0){
                            showPhotoCaptureActivity();
                        }else if(selectedPhotoMenu == 1){
                            showPhotoSelectionActivity();
                        }else if(selectedPhotoMenu == 2){
                            isPhotoCanceled = true;
                            isPhotoCaptured = false;

                            pictureImageView.setImageResource(R.drawable.imagetoset);
                        }
                    }
                });

                builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                break;
            default:
                break;
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showPhotoCaptureActivity(){
        try{
            file = createFile();
            if(file.exists()){
                file.delete();//이미 위 경로에 파일이 존재한다면 파일 지우기
            }
            file.createNewFile();
        }catch (Exception e){
            e.printStackTrace();
        }

        if(Build.VERSION.SDK_INT >= 24){
            uri = FileProvider.getUriForFile(context, "com.techtown.simplediary", file); //그냥 직접 입력해두자
        }else{
            uri = Uri.fromFile(file);
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //이미지 캡쳐 인텐트 시작
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //읽기 허용?
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri); //디렉토리+파일명으로 구성된 경로를 uri로 만든 것

        startActivityForResult(intent, AppConstants.REQ_PHOTO_CAPTURE);
    }
    public void showPhotoSelectionActivity(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(intent, AppConstants.REQ_PHOTO_SELECTION);
    }

    private File createFile(){
        String filename = createFilename();
        File outFile = new File(context.getFilesDir(), filename); //이 앱이 접근할 수 있는 개별 디렉토리의 루트를 가져온다
        // 디렉토리 아래에 filename이라는 자식 파일의 경로를 생성한다
        Log.d(TAG, "File path : "+outFile.getAbsolutePath());

        return outFile;
    }

    private String createFilename(){
        Date curDate = new Date();//아무 인자 없이 Date객체 생성 시
        String curDateStr = String.valueOf(curDate.getTime());//밀리초 단위의 현재 시간을 반환한다
        return curDateStr;
    }

    /**
     * 다른 액티비티로부터의 응답 처리
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(intent != null){
            switch (requestCode){
                case AppConstants.REQ_PHOTO_CAPTURE:
                    Log.d(TAG, "onActivityResult() for REQ_PHOTO_CAPTURE.");

                    Log.d(TAG, "resultCode : "+resultCode);

                    // setPicture(file.getAbsolutePath(), 8)
                    resultPhotoBitmap = decodeSampleBitmapFromResource(file, pictureImageView.getWidth(), pictureImageView.getHeight());
                    pictureImageView.setImageBitmap(resultPhotoBitmap);
                    break;
                case AppConstants.REQ_PHOTO_SELECTION:
                    Log.d(TAG, "onActivityResult() for REQ_PHOTO_SELECTION");

                    Uri fileUri = intent.getData(); //파일 경로를 받음
                    Uri exUri = Uri.parse(intent.getStringExtra(MediaStore.EXTRA_OUTPUT));
                    Log.d("Uri", "intent.getStringExtra(MediaStore.EXTRA_OUTPUT) : "+ exUri);

                    ContentResolver resolver = context.getContentResolver();

                    try{
                        InputStream inputStream = resolver.openInputStream(fileUri);//파일 경로에 스트림을 열고
                        resultPhotoBitmap = BitmapFactory.decodeStream(inputStream); // 그 스트림에서 사진을 디코딩 해 온다
                        pictureImageView.setImageBitmap(resultPhotoBitmap);

                        inputStream.close();

                        isPhotoCaptured = true;
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    break;

            }
        }
    }

    public static Bitmap decodeSampleBitmapFromResource(File res, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds = true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(res.getAbsolutePath(), options); //파일의 경로에서, options를 기준으로 가져오기

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(res.getAbsolutePath(), options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if(height > reqHeight || width > reqWidth){
            final int halfHeight = height;
            final int halfWidth = width;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width

            while((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth){
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 데이터베이스 레코드 추가
     */

    private void saveNote(){
        String address = locationTextView.getText().toString();
        String contents = contentsInput.getText().toString();

        String picturePath = savePicture();

        String sql = "insert into "+NoteDatabase.TABLE_NOTE+"" +
                "(WEATHER, ADDRESS, LOCATION_X, LOCATION_Y, CONTENTS, MOOD, PICTURE) VALUES (" +
                "'"+weatherIndex+"'," +
                "'"+address+"'," +
                "'"+""+"', " +
                "'"+""+"', " +
                "'"+contents+"', " +
                "'"+moodIndex+"', " +
                "'"+picturePath+"')";

        Log.d(TAG, "sql : "+sql);
        NoteDatabase database = NoteDatabase.getInstance(context);
        database.execSQL(sql);
    }

    /**
     *   데이터베이스 레코드 수정
     */
    private void modifyNote(){
        if(item != null){
            String address = locationTextView.getText().toString();
            String contents = contentsInput.getText().toString();

            String picturePath = savePicture();

            String sql = "UPDATE "+NoteDatabase.TABLE_NOTE+"" +
                    "SET " +
                    "WEATHER = '"+weatherIndex+"', " +
                    "ADDRESS = '"+address+"'," +
                    "LOCATION_X = '" +""+"'," +
                    "LOCATION_Y = '" +""+"'," +
                    "CONTENTS = '"+contents+"',"+
                    "MOOD = '"+moodIndex+"',"+
                    "PICTURE = '"+picturePath+"'"+
                    "WHERE _id = "+item.get_id();

            Log.d(TAG, "sql : "+sql);
            NoteDatabase database = NoteDatabase.getInstance(context);
            database.execSQL(sql);
        }
    }

    /**
     * 레코드 삭제
     */

    private void deleteNote(){
        AppConstants.println("deleteNote called.");

        if(item != null){
            // delete note
            String sql = "DELETE FROM "+NoteDatabase.TABLE_NOTE+" " +
                    "WHERE _id = "+item.get_id();

            Log.d(TAG, "sql : "+sql);
            NoteDatabase database = NoteDatabase.getInstance(context);
            database.execSQL(sql);
        }
    }

    private String savePicture(){
        if(resultPhotoBitmap == null){
            AppConstants.println("No picture to be saved.");
            return "";
        }

        File photoFolder = new File(AppConstants.FOLDER_PHOTO);
        if(!photoFolder.exists()){
            Log.d(TAG, "creating photo folder : "+photoFolder);
            photoFolder.mkdirs();
        }

        String photoFilename = createFilename();
        String picturePath = photoFolder + File.separator + photoFilename;

        try{
            FileOutputStream outputStream = new FileOutputStream(picturePath);
            resultPhotoBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        return picturePath;
    }

}
