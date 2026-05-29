package com.example.gymlog; // 본인 패키지명 확인!

import android.content.Intent;
import android.graphics.Color;
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
    private ArrayList<CheckBox> checkBoxList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_select);

        TextView tvSelectedPart = findViewById(R.id.tv_selected_part);
        checkboxContainer = findViewById(R.id.layout_checkbox_container);
        Button btnStartRecord = findViewById(R.id.btn_start_record);

        // 🌟 1. 메인 화면에서 넘겨준 '다중 부위' 배열 데이터 받기
        Intent intent = getIntent();
        ArrayList<String> selectedPartsList = intent.getStringArrayListExtra("SELECTED_PARTS");

        if (selectedPartsList != null && !selectedPartsList.isEmpty()) {
            // "가슴, 어깨, 유산소 운동 선택" 처럼 쉼표로 예쁘게 묶어서 보여주기
            String titleText = String.join(", ", selectedPartsList) + " 운동 선택";
            tvSelectedPart.setText(titleText);

            // 해당 부위들의 운동 리스트 불러오기
            loadExercises(selectedPartsList);
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

            if (selectedExercises.isEmpty()) {
                Toast.makeText(this, "운동을 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent nextIntent = new Intent(ExerciseSelectActivity.this, WorkoutRecordActivity.class);
            // 🌟 선택된 운동(종목) 리스트 넘기기
            nextIntent.putStringArrayListExtra("SELECTED_EXERCISES", selectedExercises);

            // 🌟 [수정됨] 메인에서 받아온 다중 부위(selectedPartsList)를 기록 화면으로 그대로 토스!
            nextIntent.putStringArrayListExtra("SELECTED_PARTS", selectedPartsList);

            startActivity(nextIntent);
            finish();
        });
    }

    /**
     * 🌟 배열로 넘어온 부위들을 하나씩 꺼내서 운동 체크박스를 동적으로 생성하는 메서드
     */
    private void loadExercises(ArrayList<String> bodyParts) {

        for (String part : bodyParts) {
            String[] exerciseArray = {};

            // 🌟 팔, 복근, 유산소 종목 추가
            switch (part) {
                case "가슴":
                    exerciseArray = new String[]{"벤치 프레스", "인클라인 벤치 프레스", "덤벨 프레스", "펙덱 플라이", "케이블 크로스오버", "딥스", "푸쉬업"};
                    break;
                case "등":
                    exerciseArray = new String[]{"랫풀다운", "바벨 로우", "시티드 로우", "풀업 (턱걸이)", "데드리프트", "암풀다운", "티바 로우"};
                    break;
                case "하체":
                    exerciseArray = new String[]{"스쿼트", "레그 프레스", "레그 익스텐션", "레그 컬", "런지", "카프 레이즈", "브이 스쿼트"};
                    break;
                case "어깨":
                    exerciseArray = new String[]{"오버헤드 프레스", "덤벨 숄더 프레스", "사이드 레터럴 레이즈", "프론트 레이즈", "페이스 풀", "리버스 펙덱 플라이"};
                    break;
                case "팔":
                    exerciseArray = new String[]{"바벨 컬", "덤벨 컬", "해머 컬", "트라이셉스 익스텐션", "케이블 푸시다운", "킥백"};
                    break;
                case "복근":
                    exerciseArray = new String[]{"크런치", "레그 레이즈", "플랭크", "행잉 레그 레이즈", "러시안 트위스트", "AB 슬라이드"};
                    break;
                case "유산소":
                    exerciseArray = new String[]{"러닝머신 (트레드밀)", "실내 사이클", "천국의 계단 (스텝밀)", "로잉머신", "일립티컬"};
                    break;
            }

            // 💡 UX 디테일: 부위별로 작은 소제목을 달아주면 섞였을 때 보기 편합니다.
            TextView tvPartTitle = new TextView(this);
            tvPartTitle.setText("■ " + part);
            tvPartTitle.setTextSize(16f);
            tvPartTitle.setTextColor(Color.parseColor("#1976D2"));
            tvPartTitle.setPadding(0, 32, 0, 8);
            checkboxContainer.addView(tvPartTitle);

            // 배열에 있는 운동 개수만큼 체크박스를 무한 생성!
            for (String exercise : exerciseArray) {
                CheckBox cb = new CheckBox(this);
                cb.setText(exercise);
                cb.setTextSize(18f);
                cb.setPadding(0, 16, 0, 16);

                checkboxContainer.addView(cb);
                checkBoxList.add(cb);
            }
        }
    }
}