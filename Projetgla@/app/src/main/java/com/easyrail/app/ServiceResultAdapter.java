package com.easyrail.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ServiceResultAdapter extends RecyclerView.Adapter<ServiceResultAdapter.ResultViewHolder> {

    private final Context context;
    private final List<ServiceResult> results;
    private final boolean roundTripFlow;
    private final boolean selectingReturnTrip;
    private final String originalDeparture;
    private final String originalDestination;
    private final String originalDate;
    private final String returnDate;
    private final String outboundServiceId;
    private final String outboundTrainName;
    private final String outboundDepartureTime;
    private final String outboundPrice;

    public ServiceResultAdapter(Context context, List<ServiceResult> results) {
        this(context, results, false, false, null, null, null, null, null, null, null, null);
    }

    public ServiceResultAdapter(Context context,
                                List<ServiceResult> results,
                                boolean roundTripFlow,
                                boolean selectingReturnTrip,
                                String originalDeparture,
                                String originalDestination,
                                String originalDate,
                                String returnDate,
                                String outboundServiceId,
                                String outboundTrainName,
                                String outboundDepartureTime,
                                String outboundPrice) {
        this.context = context;
        this.results = results;
        this.roundTripFlow = roundTripFlow;
        this.selectingReturnTrip = selectingReturnTrip;
        this.originalDeparture = originalDeparture;
        this.originalDestination = originalDestination;
        this.originalDate = originalDate;
        this.returnDate = returnDate;
        this.outboundServiceId = outboundServiceId;
        this.outboundTrainName = outboundTrainName;
        this.outboundDepartureTime = outboundDepartureTime;
        this.outboundPrice = outboundPrice;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_service_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        ServiceResult result = results.get(position);

        holder.tvRoute.setText(result.getRoute());
        holder.tvTrainName.setText(result.getTrainName());
        holder.tvDate.setText(buildScheduleLabel(result.getDate(), result.getDepartureTime()));
        holder.tvPrice.setText(result.getPrice());
        holder.tvLiveInfo.setText(buildLiveInfo(result));

        holder.btnChoose.setOnClickListener(v -> {
            Intent intent;
            if (selectingReturnTrip) {
                intent = new Intent(context, ConfirmationActivity.class);
                intent.putExtra("serviceId", outboundServiceId);
                intent.putExtra("departure", originalDeparture);
                intent.putExtra("destination", originalDestination);
                intent.putExtra("trainName", outboundTrainName);
                intent.putExtra("date", originalDate);
                intent.putExtra("departureTime", outboundDepartureTime);
                intent.putExtra("price", outboundPrice);
                intent.putExtra("returnServiceId", result.getServiceId());
                intent.putExtra("returnDeparture", result.getDeparture());
                intent.putExtra("returnDestination", result.getDestination());
                intent.putExtra("returnTrainName", result.getTrainName());
                intent.putExtra("returnDate", result.getDate());
                intent.putExtra("returnDepartureTime", result.getDepartureTime());
                intent.putExtra("returnPrice", result.getPrice());
            } else if (roundTripFlow) {
                intent = new Intent(context, ResultsActivity.class);
                intent.putExtra("departure", originalDeparture != null ? originalDeparture : result.getDeparture());
                intent.putExtra("destination", originalDestination != null ? originalDestination : result.getDestination());
                intent.putExtra("date", originalDate != null ? originalDate : result.getDate());
                intent.putExtra("roundTrip", true);
                intent.putExtra("returnDate", returnDate);
                intent.putExtra("selectingReturnTrip", true);
                intent.putExtra("outboundServiceId", result.getServiceId());
                intent.putExtra("outboundDeparture", result.getDeparture());
                intent.putExtra("outboundDestination", result.getDestination());
                intent.putExtra("outboundTrainName", result.getTrainName());
                intent.putExtra("outboundDate", result.getDate());
                intent.putExtra("outboundDepartureTime", result.getDepartureTime());
                intent.putExtra("outboundPrice", result.getPrice());
            } else {
                intent = new Intent(context, ConfirmationActivity.class);
                intent.putExtra("serviceId", result.getServiceId());
                intent.putExtra("departure", result.getDeparture());
                intent.putExtra("destination", result.getDestination());
                intent.putExtra("trainName", result.getTrainName());
                intent.putExtra("date", result.getDate());
                intent.putExtra("departureTime", result.getDepartureTime());
                intent.putExtra("price", result.getPrice());
            }
            context.startActivity(intent);
        });
    }

    private String buildScheduleLabel(String date, String departureTime) {
        if (departureTime == null || departureTime.trim().isEmpty()) {
            return date;
        }
        return date + " • " + departureTime;
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {

        TextView tvRoute;
        TextView tvTrainName;
        TextView tvDate;
        TextView tvPrice;
        TextView tvLiveInfo;
        Button btnChoose;

        public ResultViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvTrainName = itemView.findViewById(R.id.tvTrainName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvLiveInfo = itemView.findViewById(R.id.tvLiveInfo);
            btnChoose = itemView.findViewById(R.id.btnChoose);
        }
    }

    private String buildLiveInfo(ServiceResult result) {
        String platform = result.getPlatform() != null ? result.getPlatform() : "à confirmer";
        int delayMinutes = result.getDelayMinutes() != null ? result.getDelayMinutes() : 0;
        return delayMinutes > 0
                ? "Voie " + platform + " • retard " + delayMinutes + " min"
                : "Voie " + platform + " • à l'heure";
    }
}
