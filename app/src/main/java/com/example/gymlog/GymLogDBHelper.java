package com.example.gymlog; // 본인 패키지명 확인!

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GymLogDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gymlog_db";
    // 🌟 버전을 2로 올려서 구조 변경을 안드로이드에 알립니다.
    private static final int DATABASE_VERSION = 2;

    public GymLogDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 🌟 body_part TEXT 칸을 추가했습니다.
        String createTableQuery = "CREATE TABLE workout_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "record_date TEXT, " +
                "body_part TEXT, " +
                "total_volume REAL)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS workout_records");
        onCreate(db);
    }

    /**
     * 운동 부위까지 함께 저장하는 업그레이드된 삽입 메서드
     */
    public void insertRecord(String date, String bodyPart, double volume) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("record_date", date);
        values.put("body_part", bodyPart); // 🌟 추가
        values.put("total_volume", volume);

        db.insert("workout_records", null, values);
        db.close();
    }

    /**
     * 테스트용 로그 출력 메서드도 구조에 맞게 수정
     */
    public void printAllRecordsToLog() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM workout_records", null);

        Log.d("GymLogDB", "========== DB 저장 내역 확인 시작 ==========");
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String date = cursor.getString(1);
                String part = cursor.getString(2); // 🌟 추가
                double volume = cursor.getDouble(3);

                Log.d("GymLogDB", "ID: " + id + " | 날짜: " + date + " | 부위: " + part + " | 총 볼륨: " + volume + " kg");
            } while (cursor.moveToNext());
        } else {
            Log.d("GymLogDB", "DB에 저장된 데이터가 없습니다.");
        }
        Log.d("GymLogDB", "========== DB 저장 내역 확인 끝 ==========");
        cursor.close();
        db.close();
    }
}