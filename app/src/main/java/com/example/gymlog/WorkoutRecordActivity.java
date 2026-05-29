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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class WorkoutRecordActivity extends AppCompatActivity {

    private LinearLayout mainCardsContainer;
    private TextView tvTotalVolume;

    private ArrayList<String> selectedPartsList;

    private Dialog timerDialog;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private final long START_TIME_IN_MILLIS = 60000;
    private long timeLeftInMillis = START_TIME_IN_MILLIS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_record);

        mainCardsContainer = findViewById(R.id.layout_main_cards_container);
        tvTotalVolume = findViewById(R.id.tv_total_volume);

        Button btnCalculate = findViewById(R.id.btn_calculate);
        Button btnShowTimer = findViewById(R.id.btn_show_timer);
        Button btnAddExtraExercise = findViewById(R.id.btn_add_extra_exercise);

        btnAddExtraExercise.setOnClickListener(v -> showAddExtraExerciseDialog());

        Intent intent = getIntent();
        ArrayList<String> selectedExercises = intent.getStringArrayListExtra("SELECTED_EXERCISES");

        selectedPartsList = intent.getStringArrayListExtra("SELECTED_PARTS");
        if (selectedPartsList == null) {
            selectedPartsList = new ArrayList<>();
            selectedPartsList.add("기본");
        }

        if (selectedExercises != null && !selectedExercises.isEmpty()) {
            for (String exerciseName : selectedExercises) {
                addExerciseCard(exerciseName);
            }
        } else {
            addExerciseCard("기본 운동");
        }

        btnCalculate.setOnClickListener(v -> calculateTotalVolumeAndSave());
        btnShowTimer.setOnClickListener(v -> showTimerDialog());
    }

    private void addExerciseCard(String exerciseName) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.item_exercise_card, mainCardsContainer, false);

        TextView tvCardTitle = cardView.findViewById(R.id.tv_card_exercise_title);
        LinearLayout cardSetsContainer = cardView.findViewById(R.id.layout_card_sets_container);
        Button btnCardAddSet = cardView.findViewById(R.id.btn_card_add_set);
        Button btnCardDeleteSet = cardView.findViewById(R.id.btn_card_delete_set);
        Button btnDeleteCard = cardView.findViewById(R.id.btn_delete_card);

        tvCardTitle.setText(exerciseName);

        boolean isCardio = exerciseName.contains("러닝") || exerciseName.contains("사이클") ||
                exerciseName.contains("계단") || exerciseName.contains("로잉");

        addSetRowToCard(cardSetsContainer, isCardio);

        btnCardAddSet.setOnClickListener(v -> addSetRowToCard(cardSetsContainer, isCardio));
        btnCardDeleteSet.setOnClickListener(v -> removeLastSetRowFromCard(cardSetsContainer));

        btnDeleteCard.setOnClickListener(v -> {
            mainCardsContainer.removeView(cardView);
            Toast.makeText(this, exerciseName + " 종목이 리스트에서 제외되었습니다.", Toast.LENGTH_SHORT).show();
        });

        mainCardsContainer.addView(cardView);
    }

    private void addSetRowToCard(LinearLayout targetContainer, boolean isCardio) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View setRowView;

        if (isCardio) {
            setRowView = inflater.inflate(R.layout.item_cardio_set, targetContainer, false);
        } else {
            setRowView = inflater.inflate(R.layout.item_workout_set, targetContainer, false);
        }

        TextView tvSetNumber = setRowView.findViewById(R.id.tv_set_number);
        int currentSetNumber = targetContainer.getChildCount() + 1;
        tvSetNumber.setText(String.valueOf(currentSetNumber));

        targetContainer.addView(setRowView);
    }

    private void removeLastSetRowFromCard(LinearLayout targetContainer) {
        int childCount = targetContainer.getChildCount();
        if (childCount > 1) {
            targetContainer.removeViewAt(childCount - 1);
        } else {
            Toast.makeText(this, "최소 1개의 세트는 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 🌟 [구조 고도화] 종목별 상세 텍스트 조립 및 부위별 매칭 데이터 분기 저장
     */
    private void calculateTotalVolumeAndSave() {
        int cardCount = mainCardsContainer.getChildCount();

        // 부위별 데이터를 분리해서 임시 보관할 맵 세팅
        Map<String, Double> partVolumeMap = new HashMap<>();
        Map<String, StringBuilder> partDetailsMap = new HashMap<>();
        Map<String, Boolean> partHasCardioMap = new HashMap<>();

        for (String part : selectedPartsList) {
            partVolumeMap.put(part, 0.0);
            partDetailsMap.put(part, new StringBuilder());
            partHasCardioMap.put(part, false);
        }

        try {
            for (int i = 0; i < cardCount; i++) {
                View cardView = mainCardsContainer.getChildAt(i);
                if (cardView == null) continue;

                LinearLayout cardSetsContainer = cardView.findViewById(R.id.layout_card_sets_container);
                if (cardSetsContainer == null) continue;

                TextView tvCardTitle = cardView.findViewById(R.id.tv_card_exercise_title);
                String exerciseName = tvCardTitle != null ? tvCardTitle.getText().toString() : "기본 운동";

                // 🌟 종목 이름을 분석해 해당 운동이 어떤 부위인지 콕 집어냅니다.
                String itemPart = getBodyPartByExercise(exerciseName);

                // 사용자가 바텀시트에서 선택하지 않은 엉뚱한 부위라면 첫 번째 선택 부위로 보정
                if (!partVolumeMap.containsKey(itemPart)) {
                    if (!selectedPartsList.isEmpty()) itemPart = selectedPartsList.get(0);
                    else continue;
                }

                int setCount = cardSetsContainer.getChildCount();
                if (setCount == 0) continue;

                View firstSetView = cardSetsContainer.getChildAt(0);
                boolean isCardioView = (firstSetView != null && firstSetView.findViewById(R.id.et_time) != null);

                StringBuilder detailsBuilder = partDetailsMap.get(itemPart);
                detailsBuilder.append(" ▶ ").append(exerciseName).append("\n");

                double cardVolume = 0;

                for (int j = 0; j < setCount; j++) {
                    View setRowView = cardSetsContainer.getChildAt(j);
                    if (setRowView == null) continue;

                    int setNum = j + 1;

                    if (isCardioView) {
                        EditText etTime = setRowView.findViewById(R.id.et_time);
                        if (etTime != null) {
                            String timeStr = etTime.getText().toString().trim();
                            if (!timeStr.isEmpty()) {
                                detailsBuilder.append("     └ ").append(setNum).append("세트: ").append(timeStr).append("분\n");
                                partHasCardioMap.put(itemPart, true);
                            }
                        }
                    } else {
                        EditText etWeight = setRowView.findViewById(R.id.et_weight);
                        EditText etReps = setRowView.findViewById(R.id.et_reps);

                        if (etWeight != null && etReps != null) {
                            String weightStr = etWeight.getText().toString().trim();
                            String repsStr = etReps.getText().toString().trim();

                            if (!weightStr.isEmpty() && !repsStr.isEmpty()) {
                                try {
                                    double weight = Double.parseDouble(weightStr);
                                    double reps = Double.parseDouble(repsStr);
                                    cardVolume += (weight * reps);

                                    // 🌟 [핵심 수집] 1세트: 60kg x 10회 형태로 텍스트를 한 줄 한 줄 조립합니다.
                                    detailsBuilder.append("     └ ").append(setNum).append("세트: ").append(weightStr).append("kg x ").append(repsStr).append("회\n");
                                } catch (NumberFormatException e) {
                                    // 예외 발생 시 스킵
                                }
                            }
                        }
                    }
                }
                detailsBuilder.append("\n");
                partVolumeMap.put(itemPart, partVolumeMap.get(itemPart) + cardVolume);
            }
        } catch (Exception e) {
            Toast.makeText(this, "데이터 계산 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // UI에 보여줄 오늘의 전체 합산 볼륨 계산
        double totalVolumeAll = 0;
        boolean hasAnyRecord = false;
        for (String part : selectedPartsList) {
            totalVolumeAll += partVolumeMap.get(part);
            if (partVolumeMap.get(part) > 0 || partHasCardioMap.get(part)) {
                hasAnyRecord = true;
            }
        }

        tvTotalVolume.setText(String.format(Locale.getDefault(), "오늘의 총 볼륨: %.1f kg", totalVolumeAll));

        if (!hasAnyRecord) {
            Toast.makeText(this, "세트의 무게와 횟수(또는 시간)를 1개 이상 입력해주세요!", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인 세션이 만료되었습니다. 앱을 재실행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUid = currentUser.getUid();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        FirebaseFirestore fbDb = FirebaseFirestore.getInstance();

        int totalSaves = selectedPartsList.size();
        final int[] successCount = {0}; // 배열 형태로 선언하여 내부에서 수정 가능하게 함

        for (String part : selectedPartsList) {
            double partVolume = partVolumeMap.get(part);
            String partDetails = partDetailsMap.get(part).toString().trim();
            boolean partHasCardio = partHasCardioMap.get(part);

            // 해당 부위에 아무 기록을 적지 않았다면 문서 저장 생략
            if (partVolume == 0 && !partHasCardio) {
                totalSaves--;
                if (totalSaves == 0 || successCount[0] == totalSaves) {
                    Toast.makeText(WorkoutRecordActivity.this, "클라우드 저장 완료!🔥", Toast.LENGTH_SHORT).show();
                    finish();
                }
                continue;
            }

            Map<String, Object> record = new HashMap<>();
            record.put("user_id", currentUid);
            record.put("record_date", todayDate);
            record.put("body_part", part);
            record.put("total_volume", partVolume); // 🌟 해당 부위 고유의 순수 볼륨값만 할당
            record.put("workout_details", partDetails); // 🌟 조립 완료된 상세 세트 내역 텍스트 추가!
            record.put("timestamp", com.google.firebase.Timestamp.now());

            fbDb.collection("workout_records")
                    .add(record)
                    .addOnSuccessListener(documentReference -> {
                        successCount[0]++;
                        if (successCount[0] == totalSaves) {
                            Toast.makeText(WorkoutRecordActivity.this, "클라우드 저장 완료!🔥", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(WorkoutRecordActivity.this, "DB 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    /**
     * 🌟 종목명을 통해 부위를 지능적으로 실시간 판별해 주는 알고리즘 헬퍼 함수
     */
    private String getBodyPartByExercise(String exerciseName) {
        if (exerciseName.contains("벤치") || exerciseName.contains("프레스") || exerciseName.contains("플라이") || exerciseName.contains("케이블") || exerciseName.contains("딥스") || exerciseName.contains("푸쉬업")) {
            if (exerciseName.contains("숄더") || exerciseName.contains("오버헤드")) return "어깨";
            return "가슴";
        }
        if (exerciseName.contains("랫풀") || exerciseName.contains("로우") || exerciseName.contains("풀업") || exerciseName.contains("턱걸이") || exerciseName.contains("데드리프트") || exerciseName.contains("암풀")) return "등";
        if (exerciseName.contains("스쿼트") || exerciseName.contains("레그") || exerciseName.contains("런지") || exerciseName.contains("카프")) return "하체";
        if (exerciseName.contains("레이즈") || exerciseName.contains("페이스")) return "어깨";
        if (exerciseName.contains("컬") || exerciseName.contains("익스텐션") || exerciseName.contains("푸시다운") || exerciseName.contains("킥백")) return "팔";
        if (exerciseName.contains("크런치") || exerciseName.contains("레이즈") || exerciseName.contains("플랭크") || exerciseName.contains("트위스트")) return "복근";
        if (exerciseName.contains("러닝") || exerciseName.contains("사이클") || exerciseName.contains("계단") || exerciseName.contains("로잉") || exerciseName.contains("일립티컬")) return "유산소";
        return "기본";
    }

    // =========================================================================
    // 아래 팝업 타이머 및 종목 추가 로직은 기존과 동일
    // =========================================================================

    private void showTimerDialog() {
        if (timerDialog == null) {
            timerDialog = new Dialog(this);
            timerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            timerDialog.setContentView(R.layout.dialog_timer);
            timerDialog.setCancelable(false);

            TextView tvCountdown = timerDialog.findViewById(R.id.tv_countdown);
            Button btnAction = timerDialog.findViewById(R.id.btn_timer_action);
            Button btnClose = timerDialog.findViewById(R.id.btn_timer_close);

            btnAction.setOnClickListener(v -> {
                if (isTimerRunning) {
                    resetTimer(tvCountdown, btnAction);
                } else {
                    startTimer(tvCountdown, btnAction);
                }
            });

            btnClose.setOnClickListener(v -> timerDialog.dismiss());
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
                timeLeftInMillis = START_TIME_IN_MILLIS;
                Toast.makeText(WorkoutRecordActivity.this, "휴식 끝! 다음 세트를 준비하세요.", Toast.LENGTH_SHORT).show();
            }
        }.start();

        isTimerRunning = true;
        btnAction.setText("초기화 (정지)");
        btnAction.setBackgroundColor(android.graphics.Color.parseColor("#F44336"));
    }

    private void resetTimer(TextView tvCountdown, Button btnAction) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = false;
        timeLeftInMillis = START_TIME_IN_MILLIS;
        updateCountDownText(tvCountdown);
        btnAction.setText("1분 시작");
        btnAction.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"));
    }

    private void updateCountDownText(TextView tvCountdown) {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvCountdown.setText(timeLeftFormatted);
    }

    private void showAddExtraExerciseDialog() {
        ArrayList<String> availableExercises = new ArrayList<>();

        for (String part : selectedPartsList) {
            switch (part) {
                case "가슴":
                    availableExercises.add("벤치 프레스"); availableExercises.add("인클라인 벤치 프레스");
                    availableExercises.add("덤벨 프레스"); availableExercises.add("펙덱 플라이");
                    availableExercises.add("케이블 크로스오버"); availableExercises.add("딥스"); availableExercises.add("푸쉬업");
                    break;
                case "등":
                    availableExercises.add("랫풀다운"); availableExercises.add("바벨 로우");
                    availableExercises.add("시티드 로우"); availableExercises.add("풀업 (턱걸이)");
                    availableExercises.add("데드리프트"); availableExercises.add("암풀다운"); availableExercises.add("티바 로우");
                    break;
                case "하체":
                    availableExercises.add("스쿼트"); availableExercises.add("레그 프레스");
                    availableExercises.add("레그 익스텐션"); availableExercises.add("레그 컬");
                    availableExercises.add("런지"); availableExercises.add("카프 레이즈"); availableExercises.add("브이 스쿼트");
                    break;
                case "어깨":
                    availableExercises.add("오버헤드 프레스"); availableExercises.add("덤벨 숄더 프레스");
                    availableExercises.add("사이드 레터럴 레이즈"); availableExercises.add("프론트 레이즈");
                    availableExercises.add("페이스 풀"); availableExercises.add("리버스 펙덱 플라이");
                    break;
                case "팔":
                    availableExercises.add("바벨 컬"); availableExercises.add("덤벨 컬");
                    availableExercises.add("해머 컬"); availableExercises.add("트라이셉스 익스텐션");
                    availableExercises.add("케이블 푸시다운"); availableExercises.add("킥백");
                    break;
                case "복근":
                    availableExercises.add("크런치"); availableExercises.add("레그 레이즈");
                    availableExercises.add("플랭크"); availableExercises.add("행잉 레그 레이즈");
                    availableExercises.add("러시안 트위스트"); availableExercises.add("AB 슬라이드");
                    break;
                case "유산소":
                    availableExercises.add("러닝머신 (트레드밀)"); availableExercises.add("실내 사이클");
                    availableExercises.add("천국의 계단 (스텝밀)"); availableExercises.add("로잉머신"); availableExercises.add("일립티컬");
                    break;
            }
        }

        CharSequence[] items = availableExercises.toArray(new CharSequence[0]);

        new android.app.AlertDialog.Builder(this)
                .setTitle("추가할 종목을 선택하세요")
                .setItems(items, (dialog, which) -> {
                    String selectedNewExercise = availableExercises.get(which);
                    addExerciseCard(selectedNewExercise);
                    Toast.makeText(WorkoutRecordActivity.this, selectedNewExercise + " 카드가 추가되었습니다!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("닫기", null)
                .show();
    }
}