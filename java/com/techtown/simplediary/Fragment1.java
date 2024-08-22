package com.techtown.simplediary;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import lib.kingja.switchbutton.SwitchMultiButton;

public class Fragment1 extends Fragment {
    private static final String TAG = "Fragment1";
    RecyclerView recyclerView;
    NoteAdapter adapter;

    Context context;
    OnTabItemSelectedListener listener;

    SimpleDateFormat todayDateFormat;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;

        if(context instanceof OnTabItemSelectedListener){
            listener = (OnTabItemSelectedListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(context != null){
            context = null;
            listener = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment1, container, false);

        initUI(viewGroup);

        // 데이터 로딩
        loadNoteListData();

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private void initUI(ViewGroup rootView){
        Button todayWriteButton = rootView.findViewById(R.id.todayWriteButton);
        todayWriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listener != null){
                    // MainActivity에서 Fragment2로 이동하도록 처리해준다
                    listener.onTabSelected(1);//1번 탭을 선택한 것과 같은 효과를 낸다
                }
            }
        });
        SwitchMultiButton switchButton = rootView.findViewById(R.id.switchButton);
        switchButton.setOnSwitchListener(new SwitchMultiButton.OnSwitchListener() {
            @Override
            public void onSwitch(int position, String tabText) {
                Toast.makeText(getContext(), tabText, Toast.LENGTH_SHORT).show();//선택된 탭 이름 출력

                adapter.switchLayout(position); //레이아웃 바꿔서 다시 그리기
                adapter.notifyDataSetChanged();
            }
        });

        recyclerView = rootView.findViewById(R.id.recyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        
        adapter = new NoteAdapter();
        
        adapter.addItem(new Note(0,"0", "강남구 삼성동", "", "", "오늘 너무 행복해!", "5", null,"2월 10일" ));
        adapter.addItem(new Note(1,"1", "강남구 삼성동", "", "", "친구와 재미있게 놀았어", "4", null,"2월 11일" ));
        adapter.addItem(new Note(2,"2", "강남구 역삼동", "", "", "집에 왔는데 너무 피곤해 ㅠㅠ", "2", null,"2월 12일" ));

        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new OnNoteItemClickListener() {
            @Override
            public void onItemClick(NoteAdapter.ViewHolder holder, View view, int position) {
                Note item = adapter.getItem(position);
                Log.d(TAG, "아이템 선택됨: "+item.get_id());

                if(listener != null){
                    listener.showFragment2(item); //Fragment2에 item의 정보를 적용한 뒤 보여준다
                }
            }
        });


    }

    /**
     * 리스트 데이터 로딩
     */
    private int loadNoteListData(){
        AppConstants.println("loadNoteListData called.");

        String sql = "SELECT _id, WEATHER, ADDRESS, LOCATION_X, LOCATION_Y, CONTENTS, MOOD, PICTURE, CREATE_DATE, MODIFY_DATE FROM "+NoteDatabase.TABLE_NOTE+" ORDER BY CREATE_DATE DESC";
        // Select문으로 전체 데이터 가져오기

        int recordCount = 0;
        NoteDatabase database = NoteDatabase.getInstance(context);
        if(database != null){
            Cursor outCursor = database.rawQuery(sql);

            recordCount = outCursor.getCount();
            AppConstants.println("record count : "+recordCount + "\n");

            ArrayList<Note> items = new ArrayList<>();

            for(int i = 0; i< recordCount; i++){
                //index는 0부터 recordCount-1까지
                outCursor.moveToNext(); //더미 노드가 하나 있다. 반환된 노드는 더미 노드를 가리키고 있다.
                int _id = outCursor.getInt(0);
                String weather = outCursor.getString(1);
                String address = outCursor.getString(2);
                String locationX = outCursor.getString(3);
                String locationY = outCursor.getString(4);
                String contents = outCursor.getString(5);
                String mood = outCursor.getString(6);
                String picture = outCursor.getString(7);
                String dateStr = outCursor.getString(8);
                String createDateStr = null;
                if(dateStr != null && !dateStr.equals("")){
                    try{
                        // TimeStamp 자료형은 "yyyy-MM-dd- HH:mm:ss"와 같이 저장된다
                        Date inDate = AppConstants.dateFormat4.parse(dateStr);
                        // 한 번 Date형식으로 변환한 뒤 다시 변환
                        if(todayDateFormat == null){
                            todayDateFormat = new SimpleDateFormat(getResources().getString(R.string.today_date_format));
                        }
                        
                        createDateStr = todayDateFormat.format(inDate);
                        AppConstants.println("currentDateString: "+createDateStr);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else{
                    createDateStr = "";
                }

                AppConstants.println("#" + i +" -> "+_id+", "+weather+", "+address+", "+locationX+", "
                        +locationY+", "+contents+", "+mood+", "+picture+", "+dateStr+", "+createDateStr);

                items.add(new Note(_id, weather, address, locationX, locationY, contents, mood, picture, createDateStr));
            }

            outCursor.close();

            adapter.setItems(items);
            adapter.notifyDataSetChanged();
        }

        //데이터베이스에서 데이터를 가져와 로딩

        return recordCount;
    }
}
