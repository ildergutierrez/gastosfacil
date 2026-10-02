package com.example.gastofacil;

import android.graphics.Color;
import android.net.Uri;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<HistoryItem> items;
    private OnTransactionClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", new Locale("es", "ES"));

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionModel transaction);
    }

    public TransactionAdapter(List<HistoryItem> items, OnTransactionClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == HistoryItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryItem item = items.get(position);

        if (holder.getItemViewType() == HistoryItem.TYPE_HEADER) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.tvHeaderTitle.setText(item.getHeaderTitle());
        } else {
            TransactionViewHolder txHolder = (TransactionViewHolder) holder;
            TransactionModel transaction = item.getTransaction();

            txHolder.tvName.setText(transaction.getCategoria());
            
            String fechaFormateada;
            if (transaction.getTimestamp() > 0) {
                fechaFormateada = dateFormat.format(new Date(transaction.getTimestamp()));
            } else {
                fechaFormateada = transaction.getFecha();
                if (fechaFormateada != null && fechaFormateada.contains("Hoy, ")) {
                    fechaFormateada = fechaFormateada.replace("Hoy, ", "");
                }
            }
            
            String descripcion = transaction.getDescripcion();
            if (descripcion != null && descripcion.length() > 10) {
                descripcion = descripcion.substring(0, 10) + "...";
            }
            
            txHolder.tvDesc.setText(String.format("%s · %s", fechaFormateada, descripcion));

            if (transaction.getLatitude() != null && transaction.getLongitude() != null) {
                txHolder.ivLocation.setVisibility(View.VISIBLE);
                txHolder.ivLocation.setOnClickListener(v -> {
                    try {
                        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(Ubicación)", 
                                transaction.getLatitude(), transaction.getLongitude(), 
                                transaction.getLatitude(), transaction.getLongitude());
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setPackage("com.google.android.apps.maps");
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        try {
                            String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f", 
                                    transaction.getLatitude(), transaction.getLongitude(), 
                                    transaction.getLatitude(), transaction.getLongitude());
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                            v.getContext().startActivity(intent);
                        } catch (Exception ex) {
                            Toast.makeText(v.getContext(), "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                txHolder.ivLocation.setVisibility(View.GONE);
            }

            double monto = Double.parseDouble(transaction.getMonto());
            String montoFormateado = CurrencyUtils.formatShort(monto);

            if ("INGRESO".equals(transaction.getTipo())) {
                txHolder.tvValue.setText("+" + montoFormateado);
                txHolder.tvValue.setTextColor(Color.parseColor("#4CAF50"));
                txHolder.ivIcon.setImageResource(R.drawable.ic_trend_up);
            } else {
                txHolder.tvValue.setText("-" + montoFormateado);
                txHolder.tvValue.setTextColor(Color.RED);
                txHolder.ivIcon.setImageResource(R.drawable.ic_trend_down);
            }

            txHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderTitle;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderTitle = itemView.findViewById(R.id.tvHeaderTitle);
        }
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvValue;
        ImageView ivIcon, ivLocation;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTransName);
            tvDesc = itemView.findViewById(R.id.tvTransDesc);
            tvValue = itemView.findViewById(R.id.tvTransValue);
            ivIcon = itemView.findViewById(R.id.ivTransIcon);
            ivLocation = itemView.findViewById(R.id.ivLocationIcon);
        }
    }
}
