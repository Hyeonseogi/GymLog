package com.example.gymlog; // 본인 패키지명 확인!

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 홈 화면 연결

        // 버튼 뷰 찾기
        Button btnChest = findViewById(R.id.btn_chest);
        Button btnBack = findViewById(R.id.btn_back);
        Button btnLegs = findViewById(R.id.btn_legs);
        Button btnShoulder = findViewById(R.id.btn_shoulder);

        // 가슴 버튼 클릭 시 -> 벤치프레스로 세팅해서 이동
        btnChest.setOnClickListener(v -> moveToRecord("가슴"));

        // 등 버튼 클릭 시 -> 랫풀다운으로 세팅해서 이동
        btnBack.setOnClickListener(v -> moveToRecord("등"));

        // 하체 버튼 클릭 시 -> 스쿼트로 세팅해서 이동
        btnLegs.setOnClickListener(v -> moveToRecord("하체"));

        // 어깨 버튼 클릭 시 -> 숄더프레스로 세팅해서 이동
        btnShoulder.setOnClickListener(v -> moveToRecord("어깨"));
    }

    /**
     * 기록 화면으로 이동하면서 부위와 종목 데이터를 전달하는 메서드
     */
    private void moveToRecord(String bodyPart) {
        // 목적지를 ExerciseSelectActivity 로 변경!
        Intent intent = new Intent(MainActivity.this, ExerciseSelectActivity.class);
        intent.putExtra("BODY_PART", bodyPart);
        startActivity(intent);
    }
}