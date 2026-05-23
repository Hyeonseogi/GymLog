// src/main/java/com/hyeonseok/gymlog/WorkoutRecordActivity.java
package com.example.gymlog; // 본인의 패키지명으로 변경하세요

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WorkoutRecordActivity extends AppCompatActivity {

    private LinearLayout setsContainer;
    private TextView tvTotalVolume;
    private int setCounter = 1; // 현재 세트 번호 추적

    private Dialog timerDialog;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private final long START_TIME_IN_MILLIS = 60000; // 60초 (1분)
    private long timeLeftInMillis = START_TIME_IN_MILLIS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_record);

        setsContainer = findViewById(R.id.layout_sets_container);
        tvTotalVolume = findViewById(R.id.tv_total_volume);

        Button btnAddSet = findViewById(R.id.btn_add_set);
        Button btnDeleteSet = findViewById(R.id.btn_delete_set);
        Button btnCalculate = findViewById(R.id.btn_calculate);

        TextView tvExerciseTitle = findViewById(R.id.tv_exercise_title);
        // ✨ 이전 화면(MainActivity)에서 보낸 데이터 받아오기
        Intent intent = getIntent();
        String exerciseName = intent.getStringExtra("EXERCISE_NAME");

        // 받아온 종목 이름이 비어있지 않다면 화면 상단 텍스트 변경!
        if (exerciseName != null) {
            tvExerciseTitle.setText(exerciseName);
        }

        // 앱 실행 시 기본 1세트 세팅
        addSetRow();

        // 1. 세트 추가 버튼 이벤트 (동적 뷰 생성)
        btnAddSet.setOnClickListener(v -> addSetRow());

        // 2. 세트 삭제 버튼 이벤트 (동적 뷰 제거)
        btnDeleteSet.setOnClickListener(v -> removeLastSetRow());

        // 3. 완료 및 볼륨 합산 버튼 이벤트
        btnCalculate.setOnClickListener(v -> calculateTotalVolume());

        // 🌟 타이머 버튼 뷰 찾기 및 클릭 이벤트
        Button btnShowTimer = findViewById(R.id.btn_show_timer);
        btnShowTimer.setOnClickListener(v -> showTimerDialog());
    }

    /**
     * item_workout_set.xml을 불러와서(Inflate) 화면에 추가하는 메서드
     */
    private void addSetRow() {
        LayoutInflater inflater = LayoutInflater.from(this);
        // item_workout_set.xml을 객체화하여 가져옴
        View setRowView = inflater.inflate(R.layout.item_workout_set, setsContainer, false);

        // 세트 번호 부여
        TextView tvSetNumber = setRowView.findViewById(R.id.tv_set_number);
        tvSetNumber.setText(String.valueOf(setCounter));

        // 컨테이너에 완성된 한 줄 추가
        setsContainer.addView(setRowView);
        setCounter++; // 다음 세트 번호를 위해 증가
    }

    /**
     * 가장 마지막에 추가된 세트를 삭제하는 메서드
     */
    private void removeLastSetRow() {
        int childCount = setsContainer.getChildCount();
        if (childCount > 1) { // 무조건 1세트는 남겨두기 위함
            setsContainer.removeViewAt(childCount - 1);
            setCounter--; // 세트 번호 감소
        } else {
            Toast.makeText(this, "최소 1개의 세트는 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 생성된 모든 세트의 무게와 횟수를 읽어와 총 볼륨을 계산하는 메서드
     */
    private void calculateTotalVolume() {
        double totalVolume = 0;
        int childCount = setsContainer.getChildCount();

        for (int i = 0; i < childCount; i++) {
            // 컨테이너 안에 있는 i번째 줄(Row)을 가져옴
            View setRowView = setsContainer.getChildAt(i);

            EditText etWeight = setRowView.findViewById(R.id.et_weight);
            EditText etReps = setRowView.findViewById(R.id.et_reps);

            String weightStr = etWeight.getText().toString();
            String repsStr = etReps.getText().toString();

            // 무게와 횟수 칸이 비어있지 않을 때만 계산 (예외 처리)
            if (!weightStr.isEmpty() && !repsStr.isEmpty()) {
                double weight = Double.parseDouble(weightStr);
                int reps = Integer.parseInt(repsStr);
                totalVolume += (weight * reps);
            }
        }



        // 결과를 텍스트뷰에 출력 (소수점 1자리까지 표시)
        tvTotalVolume.setText(String.format("%.1f kg", totalVolume));
        Toast.makeText(this, "볼륨이 합산되었습니다.", Toast.LENGTH_SHORT).show();
    }

    /**
     * 🌟 팝업 타이머를 화면에 띄우고 제어하는 메서드
     */
    private void showTimerDialog() {
        if (timerDialog == null) {
            timerDialog = new Dialog(this);
            timerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            timerDialog.setContentView(R.layout.dialog_timer);
            timerDialog.setCancelable(false); // 바깥 화면 터치로 안 꺼지게 설정

            TextView tvCountdown = timerDialog.findViewById(R.id.tv_countdown);
            Button btnAction = timerDialog.findViewById(R.id.btn_timer_action);
            Button btnClose = timerDialog.findViewById(R.id.btn_timer_close);

            // 타이머 시작/초기화 버튼 동작
            btnAction.setOnClickListener(v -> {
                if (isTimerRunning) {
                    resetTimer(tvCountdown, btnAction);
                } else {
                    startTimer(tvCountdown, btnAction);
                }
            });

            // 닫기 버튼 동작
            btnClose.setOnClickListener(v -> {
                timerDialog.dismiss();
                // 원한다면 팝업을 닫을 때 타이머를 강제 초기화할 수도 있습니다.
            });
        }

        timerDialog.show();
    }

    private void startTimer(TextView tvCountdown, Button btnAction) {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText(tvCountdown);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnAction.setText("1분 다시 시작");
                btnAction.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"));
                timeLeftInMillis = START_TIME_IN_MILLIS; // 시간 초기화
                Toast.makeText(WorkoutRecordActivity.this, "휴식 끝! 다음 세트를 준비하세요.", Toast.LENGTH_SHORT).show();
            }
        }.start();

        isTimerRunning = true;
        btnAction.setText("초기화 (정지)");
        btnAction.setBackgroundColor(android.graphics.Color.parseColor("#F44336")); // 빨간색으로 변경
    }

    private void resetTimer(TextView tvCountdown, Button btnAction) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        timeLeftInMillis = START_TIME_IN_MILLIS;
        updateCountDownText(tvCountdown);
        btnAction.setText("1분 시작");
        btnAction.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")); // 초록색 복구
    }

    private void updateCountDownText(TextView tvCountdown) {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        tvCountdown.setText(timeLeftFormatted);
    }
}
