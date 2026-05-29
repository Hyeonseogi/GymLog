package com.example.gymlog;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.Collection;
import java.util.HashSet;

public class EventDecorator implements DayViewDecorator {

    private final int color;
    private final HashSet<CalendarDay> dates;

    public EventDecorator(int color, Collection<CalendarDay> dates) {
        this.color = color;
        this.dates = new HashSet<>(dates); // 점을 찍을 날짜들을 모아둡니다.
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        // 이 날짜가 점을 찍어야 하는 날짜 목록에 포함되어 있는지 확인
        return dates.contains(day);
    }

    @Override
    public void decorate(DayViewFacade view) {
        // 해당 날짜 밑에 설정한 색상의 점(Dot)을 그립니다. 숫자는 점의 크기입니다.
        view.addSpan(new DotSpan(8, color));
    }
}