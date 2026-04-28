package com.easyrail.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ServiceAdapter(
    private val services: List<ServiceItem>,
    private val onItemClick: (ServiceItem) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder>() {

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val trainText: TextView = itemView.findViewById(R.id.trainText)
        val routeText: TextView = itemView.findViewById(R.id.routeText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val priceText: TextView = itemView.findViewById(R.id.priceText)
        val chooseButton: Button = itemView.findViewById(R.id.chooseButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = services[position]

        holder.trainText.text = service.trainNom
        holder.routeText.text = "${service.villeDepartNom} → ${service.villeArriveeNom}"
        holder.dateText.text = service.dateTrajet
        holder.priceText.text = "${service.prixBase} €"

        holder.itemView.setOnClickListener {
            onItemClick(service)
        }

        holder.chooseButton.setOnClickListener {
            onItemClick(service)
        }
    }

    override fun getItemCount(): Int = services.size
}