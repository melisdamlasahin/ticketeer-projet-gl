package com.easyrail.app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class MyTicketsAdapter extends RecyclerView.Adapter<MyTicketsAdapter.TicketViewHolder> {

    private final Context context;
    private final List<TicketApiModel> tickets;

    public MyTicketsAdapter(Context context, List<TicketApiModel> tickets) {
        this.context = context;
        this.tickets = tickets;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        TicketApiModel ticket = tickets.get(position);

        String departure = ticket.getVilleDepartNom() != null ? ticket.getVilleDepartNom() : "-";
        String destination = ticket.getVilleArriveeNom() != null ? ticket.getVilleArriveeNom() : "-";
        String train = ticket.getTrainNom() != null ? ticket.getTrainNom() : "Train non défini";
        String date = buildScheduleLabel(ticket.getDateTrajet(), ticket.getHeureDepart());
        String status = ticket.getEtat() != null ? ticket.getEtat() : "-";
        String price = ticket.getPrixFinal() != null
                ? String.format(Locale.FRANCE, "%.2f €", ticket.getPrixFinal())
                : "-";
        boolean roundTrip = "ALLER_RETOUR".equals(ticket.getTypeTrajet());

        holder.tvRoute.setText(ticket.getTrajetResume() != null ? ticket.getTrajetResume() : departure + " → " + destination);
        holder.tvTrain.setText(roundTrip && ticket.getTrainRetourNom() != null
                ? train + " / " + ticket.getTrainRetourNom()
                : train);
        holder.tvDate.setText(roundTrip && ticket.getDateRetour() != null
                ? date + " • retour " + buildScheduleLabel(ticket.getDateRetour(), ticket.getHeureRetour())
                : date);
        holder.tvPrice.setText(price);
        holder.tvStatus.setText(roundTrip ? status + " • Aller-retour" : status);

        holder.cardTicket.setOnClickListener(v -> {
            Intent intent = new Intent(context, TicketActivity.class);
            intent.putExtra("billetId", ticket.getBilletId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        CardView cardTicket;
        TextView tvRoute, tvTrain, tvDate, tvPrice, tvStatus;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTicket = itemView.findViewById(R.id.cardTicket);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvTrain = itemView.findViewById(R.id.tvTrain);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }

    private String buildScheduleLabel(String date, String departureTime) {
        String base = date != null ? date : "Date non définie";
        if (departureTime == null || departureTime.trim().isEmpty()) {
            return base;
        }
        return base + " " + departureTime;
    }
}
