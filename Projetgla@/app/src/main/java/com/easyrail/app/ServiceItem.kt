package com.easyrail.app

data class ServiceItem(
    val serviceId: String,
    val dateTrajet: String,
    val prixBase: Double,
    val trainNom: String,
    val villeDepartNom: String,
    val villeArriveeNom: String
)