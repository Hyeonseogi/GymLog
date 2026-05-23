package com.example.gymlog; // 본인 패키지명 확인!

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ExerciseSelectActivity extends AppCompatActivity {

    private LinearLayout checkboxContainer;
    private ArrayList<CheckBox> checkBoxList = new ArrayList<>(); // 생성된 체크박스들을 담아둘 리스트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_select);

        TextView tvSelectedPart = findViewById(R.id.tv_selected_part);
        checkboxContainer = findViewById(R.id.layout_checkbox_container);
        Button btnStartRecord = findViewById(R.id.btn_start_record);

        // 1. 메인 화면에서 넘겨준 '부위' 데이터 받기
        Intent intent = getIntent();
        String bodyPart = intent.getStringExtra("BODY_PART");

        if (bodyPart != null) {
            tvSelectedPart.setText(bodyPart + " 운동 선택");
            loadExercises(bodyPart); // 해당 부위의 운동 리스트 불러오기
        }

        // 2. 기록 시작 버튼 클릭 이벤트
        btnStartRecord.setOnClickListener(v -> {
            ArrayList<String> selectedExercises = new ArrayList<>();

            // 화면에 있는 모든 체크박스를 검사해서 '체크된' 항목만 리스트에 담기
            for (CheckBox cb : checkBoxList) {
                if (cb.isChecked()) {
                    selectedExercises.add(cb.getText().toString());
                }
            }

            // 하나도 선택 안 했을 경우 예외 처리
            if (selectedExercises.isEmpty()) {
                Toast.makeText(this, "운동을 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🌟 선택된 운동 리스트를 들고 '기록 화면'으로 이동!
            Intent nextIntent = new Intent(ExerciseSelectActivity.this, WorkoutRecordActivity.class);
            // 배열 리스트 자체를 Intent에 담아서 보냅니다.
            nextIntent.putStringArrayListExtra("SELECTED_EXERCISES", selectedExercises);
            startActivity(nextIntent);

            // (옵션) 뒤로가기 눌렀을 때 다시 체크박스 화면이 안 나오게 하려면 finish(); 추가
            finish();
        });
    }

    /**
     * 부위별 기본 운동 리스트를 바탕으로 동적 체크박스를 생성하는 메서드
     */
    private void loadExercises(String bodyPart) {
        String[] exerciseArray = {};

        // 부위에 따라 임시 DB(배열) 세팅
        switch (bodyPart) {
            case "가슴":
                exerciseArray = new String[]{"벤치 프레스", "인클라인 덤벨 프레스", "펙덱 플라이", "딥스", "푸쉬업"};
                break;
            case "등":
                exerciseArray = new String[]{"랫풀다운", "바벨 로우", "시티드 로우", "풀업", "데드리프트"};
                break;
            case "하체":
                exerciseArray = new String[]{"스쿼트", "레그 프레스", "레그 익스텐션", "레그 컬", "런지"};
                break;
            case "어깨":
                exerciseArray = new String[]{"오버헤드 프레스", "덤벨 숄더 프레스", "사이드 레터럴 레이즈", "벤트오버 레터럴 레이즈", "업라이트 로우"};
                break;
        }

        // 배열에 있는 운동 개수만큼 체크박스를 무한 생성!
        for (String exercise : exerciseArray) {
            CheckBox cb = new CheckBox(this);
            cb.setText(exercise);
            cb.setTextSize(18f);
            cb.setPadding(0, 16, 0, 16); // 체크박스 위아래 간격 넓게

            checkboxContainer.addView(cb); // 화면에 추가
            checkBoxList.add(cb); // 나중에 검사하기 위해 리스트에 저장
        }
    }
}