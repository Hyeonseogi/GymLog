package com.example.gymlog; // 본인 패키지명 확인!

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private FirebaseFirestore fbDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendarView = findViewById(R.id.calendarView);
        fbDb = FirebaseFirestore.getInstance();

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String selectedDateStr = sdf.format(date.getDate());
            showWorkoutHistoryDialog(selectedDateStr);
        });

        FloatingActionButton fabAddWorkout = findViewById(R.id.fab_add_workout);
        fabAddWorkout.setOnClickListener(v -> showWorkoutSelectionDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWorkoutRecordsAndSyncUI();
    }

    private void loadWorkoutRecordsAndSyncUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentUid = currentUser.getUid();

        calendarView.removeDecorators();

        ArrayList<CalendarDay> chestDates = new ArrayList<>();
        ArrayList<CalendarDay> backDates = new ArrayList<>();
        ArrayList<CalendarDay> legsDates = new ArrayList<>();
        ArrayList<CalendarDay> shoulderDates = new ArrayList<>();
        ArrayList<CalendarDay> armsDates = new ArrayList<>();
        ArrayList<CalendarDay> absDates = new ArrayList<>();
        ArrayList<CalendarDay> cardioDates = new ArrayList<>();

        Map<String, Boolean> partCheckMap = new HashMap<>();
        String[] allParts = {"가슴", "등", "하체", "어깨", "팔", "복근", "유산소"};
        for (String p : allParts) partCheckMap.put(p, false);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        fbDb.collection("workout_records")
                .whereEqualTo("user_id", currentUid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String dateStr = document.getString("record_date");
                        String part = document.getString("body_part");

                        if (dateStr != null && part != null) {
                            partCheckMap.put(part, true);

                            try {
                                Date date = sdf.parse(dateStr);
                                if (date != null) {
                                    CalendarDay calDay = CalendarDay.from(date);

                                    if ("가슴".equals(part)) chestDates.add(calDay);
                                    else if ("등".equals(part)) backDates.add(calDay);
                                    else if ("하체".equals(part)) legsDates.add(calDay);
                                    else if ("어깨".equals(part)) shoulderDates.add(calDay);
                                    else if ("팔".equals(part)) armsDates.add(calDay);
                                    else if ("복근".equals(part)) absDates.add(calDay);
                                    else if ("유산소".equals(part)) cardioDates.add(calDay);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (!chestDates.isEmpty()) calendarView.addDecorator(new EventDecorator(Color.parseColor("#FF9800"), chestDates));
                    if (!backDates.isEmpty()) calendarView.addDecorator(new EventDecorator(Color.parseColor("#2196F3"), backDates));
                    if (!legsDates.isEmpty()) calendarView.addDecorator(new EventDecorator(Color.parseColor("#4CAF50"), legsDates));
                    if (!shoulderDates.isEmpty()) calendarView.addDecorator(new EventDecorator(Color.parseColor("#9C27B0"), shoulderDates));
                    if (!armsDates.isEmpty()) calendarView.addDecorator(new EventDecorator(Color.parseColor("#FFEB3B"), armsDates));
                    if (!absDates.isEmpty()) calendarView.addDecorator(new EventDecorator(Color.parseColor("#00BCD4"), absDates));
                    if (!cardioDates.isEmpty()) calendarView.addDecorator(new EventDecorator(Color.parseColor("#F44336"), cardioDates));

                    updateCoachMessageWithData(partCheckMap);
                });
    }

    private void updateCoachMessageWithData(Map<String, Boolean> partCheckMap) {
        TextView tvCoachMessage = findViewById(R.id.tv_coach_message);
        String[] allParts = {"가슴", "등", "하체", "어깨", "팔", "복근", "유산소"};
        String recommendedPart = "하체";
        boolean isAllWorkedOut = true;

        for (String part : allParts) {
            if (!partCheckMap.get(part)) {
                recommendedPart = part;
                isAllWorkedOut = false;
                break;
            }
        }

        if (isAllWorkedOut) {
            tvCoachMessage.setText("모든 부위를 골고루 단련하고 계시네요!\n오늘도 멋지게 오운완 해볼까요?🔥");
        } else {
            if ("유산소".equals(recommendedPart)) {
                tvCoachMessage.setText("요즘 웨이트만 하신 것 같아요!\n오늘은 심폐지구력을 위해 🏃‍♂️유산소 운동을 추천합니다.");
            } else {
                tvCoachMessage.setText("최근 " + recommendedPart + " 운동 기록이 부족해요!\n오늘은 " + recommendedPart + " 루틴을 강력 추천합니다💪");
            }
        }
    }

    /**
     * 🌟 [업그레이드 완료] 날짜 탭 시 부위별 총 볼륨 및 하위에 종목별 세부 세트 리스트까지 완벽 표출
     */
    private void showWorkoutHistoryDialog(final String dateStr) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentUid = currentUser.getUid();

        fbDb.collection("workout_records")
                .whereEqualTo("user_id", currentUid)
                .whereEqualTo("record_date", dateStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder workoutSummary = new StringBuilder();
                    ArrayList<String> partsToday = new ArrayList<>();
                    boolean hasRecords = false;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        hasRecords = true;
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String part = document.getString("body_part");
                            double volume = document.getDouble("total_volume");

                            // 🌟 서버에서 상세 문자열 필드 추출
                            String details = document.getString("workout_details");

                            partsToday.add(part);
                            workoutSummary.append("■ ").append(part);
                            if ("유산소".equals(part)) {
                                workoutSummary.append(" (심폐 지구력 훈련 완료 🏃‍♂️)\n");
                            } else {
                                workoutSummary.append(String.format(Locale.getDefault(), " (총 볼륨: %.1f kg 💪)\n", volume));
                            }

                            // 🌟 조립된 상세 종목/세트 정보가 있다면 하단에 줄바꿈 후 예쁘게 출력
                            if (details != null && !details.isEmpty()) {
                                workoutSummary.append(details).append("\n");
                            }
                            workoutSummary.append("----------------------------------\n");
                        }
                    } else {
                        workoutSummary.append("이날은 운동 기록이 없습니다.\n휴식도 훈련의 일부입니다! 🛋️");
                    }

                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(dateStr + " 운동 일지");
                    builder.setMessage(workoutSummary.toString());

                    builder.setPositiveButton("확인", (dialog, which) -> dialog.dismiss());

                    if (hasRecords) {
                        builder.setNegativeButton("오늘 기록 삭제", (dialog, which) -> {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                fbDb.collection("workout_records").document(document.getId()).delete();
                            }
                            Toast.makeText(MainActivity.this, "운동 기록이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            loadWorkoutRecordsAndSyncUI();
                        });

                        builder.setNeutralButton("볼륨 수정", (dialog, which) -> {
                            CharSequence[] items = partsToday.toArray(new CharSequence[0]);

                            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                    .setTitle("수정할 부위를 선택하세요")
                                    .setItems(items, (subDialog, index) -> {
                                        String selectedPart = partsToday.get(index);

                                        if ("유산소".equals(selectedPart)) {
                                            Toast.makeText(MainActivity.this, "유산소 운동은 볼륨을 수정할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        final EditText etNewVolume = new EditText(MainActivity.this);
                                        etNewVolume.setHint("새로운 총 볼륨(kg) 입력");
                                        etNewVolume.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

                                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                                .setTitle(selectedPart + " 볼륨 수정")
                                                .setView(etNewVolume)
                                                .setPositiveButton("변경", (editDialog, editWhich) -> {
                                                    String input = etNewVolume.getText().toString();
                                                    if (!input.isEmpty()) {
                                                        double newVolume = Double.parseDouble(input);

                                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                                            if (selectedPart.equals(document.getString("body_part"))) {
                                                                fbDb.collection("workout_records")
                                                                        .document(document.getId())
                                                                        .update("total_volume", newVolume)
                                                                        .addOnSuccessListener(aVoid -> {
                                                                            Toast.makeText(MainActivity.this, "볼륨이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                                                                            loadWorkoutRecordsAndSyncUI();
                                                                        });
                                                                break;
                                                            }
                                                        }
                                                    }
                                                })
                                                .setNegativeButton("취소", null)
                                                .show();
                                    })
                                    .show();
                        });
                    }
                    builder.show();
                });
    }

    private void showWorkoutSelectionDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        CheckBox cbChest = view.findViewById(R.id.cb_chest);
        CheckBox cbBack = view.findViewById(R.id.cb_back);
        CheckBox cbLegs = view.findViewById(R.id.cb_legs);
        CheckBox cbShoulder = view.findViewById(R.id.cb_shoulder);
        CheckBox cbArms = view.findViewById(R.id.cb_arms);
        CheckBox cbAbs = view.findViewById(R.id.cb_abs);
        CheckBox cbCardio = view.findViewById(R.id.cb_cardio);
        Button btnStartRecord = view.findViewById(R.id.btn_start_record);

        btnStartRecord.setOnClickListener(v -> {
            ArrayList<String> selectedParts = new ArrayList<>();
            if (cbChest.isChecked()) selectedParts.add("가슴");
            if (cbBack.isChecked()) selectedParts.add("등");
            if (cbLegs.isChecked()) selectedParts.add("하체");
            if (cbShoulder.isChecked()) selectedParts.add("어깨");
            if (cbArms.isChecked()) selectedParts.add("팔");
            if (cbAbs.isChecked()) selectedParts.add("복근");
            if (cbCardio.isChecked()) selectedParts.add("유산소");

            if (selectedParts.isEmpty()) {
                Toast.makeText(this, "운동할 부위를 1개 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(MainActivity.this, ExerciseSelectActivity.class);
            intent.putStringArrayListExtra("SELECTED_PARTS", selectedParts);
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }
}