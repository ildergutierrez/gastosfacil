package com.example.gastofacil;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.HashMap;
import java.util.Map;

public class PieChartView extends View {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rectF = new RectF();
    private Map<String, Double> data = new HashMap<>();
    private Map<String, Integer> colors = new HashMap<>();

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Colores por defecto para las categorías
        colors.put("Comida", Color.parseColor("#FF9800"));
        colors.put("Transporte", Color.parseColor("#2196F3"));
        colors.put("Estudio", Color.parseColor("#9C27B0"));
        colors.put("Ocio", Color.parseColor("#E91E63"));
        colors.put("Otro", Color.parseColor("#9E9E9E"));
    }

    public void setData(Map<String, Double> newData) {
        this.data = newData;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || data.isEmpty()) {
            paint.setColor(Color.LTGRAY);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 2.5f, paint);
            return;
        }

        double total = 0;
        for (double val : data.values()) total += val;

        float startAngle = 0;
        rectF.set(10, 10, getWidth() - 10, getHeight() - 10);

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            float sweepAngle = (float) (360 * (entry.getValue() / total));
            
            Integer color = colors.get(entry.getKey());
            if (color == null) color = Color.GRAY;
            
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            
            startAngle += sweepAngle;
        }
        
        // Efecto Donut (opcional, dibuja un círculo blanco en el centro)
        paint.setColor(Color.WHITE);
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, getWidth() / 4f, paint);
    }
}
