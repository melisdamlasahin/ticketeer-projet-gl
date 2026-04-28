package com.easyrail.app;

public class ServiceResult {

    private final String serviceId;
    private final String departure;
    private final String destination;
    private final String trainName;
    private final String date;
    private final String departureTime;
    private final String price;
    private final String platform;
    private final Integer delayMinutes;
    private final String returnServiceId;
    private final String returnDeparture;
    private final String returnDestination;
    private final String returnTrainName;
    private final String returnDate;
    private final String returnDepartureTime;
    private final String returnPrice;

    public ServiceResult(String serviceId, String departure, String destination, String trainName, String date, String price) {
        this(serviceId, departure, destination, trainName, date, null, price, null, null, null, null, null, null, null, null, null);
    }

    public ServiceResult(String serviceId,
                         String departure,
                         String destination,
                         String trainName,
                         String date,
                         String departureTime,
                         String price,
                         String platform,
                         Integer delayMinutes) {
        this(serviceId, departure, destination, trainName, date, departureTime, price, platform, delayMinutes, null, null, null, null, null, null, null);
    }

    public ServiceResult(String serviceId,
                         String departure,
                         String destination,
                         String trainName,
                         String date,
                         String departureTime,
                         String price,
                         String platform,
                         Integer delayMinutes,
                         String returnServiceId,
                         String returnDeparture,
                         String returnDestination,
                         String returnTrainName,
                         String returnDate,
                         String returnDepartureTime,
                         String returnPrice) {
        this.serviceId = serviceId;
        this.departure = departure;
        this.destination = destination;
        this.trainName = trainName;
        this.date = date;
        this.departureTime = departureTime;
        this.price = price;
        this.platform = platform;
        this.delayMinutes = delayMinutes;
        this.returnServiceId = returnServiceId;
        this.returnDeparture = returnDeparture;
        this.returnDestination = returnDestination;
        this.returnTrainName = returnTrainName;
        this.returnDate = returnDate;
        this.returnDepartureTime = returnDepartureTime;
        this.returnPrice = returnPrice;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getDeparture() {
        return departure;
    }

    public String getDestination() {
        return destination;
    }

    public String getRoute() {
        return departure + " → " + destination;
    }

    public String getTrainName() {
        return trainName;
    }

    public String getDate() {
        return date;
    }

    public String getPrice() {
        return price;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getPlatform() {
        return platform;
    }

    public Integer getDelayMinutes() {
        return delayMinutes;
    }

    public boolean isRoundTrip() {
        return returnServiceId != null && !returnServiceId.trim().isEmpty();
    }

    public String getReturnServiceId() {
        return returnServiceId;
    }

    public String getReturnDeparture() {
        return returnDeparture;
    }

    public String getReturnDestination() {
        return returnDestination;
    }

    public String getReturnTrainName() {
        return returnTrainName;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public String getReturnDepartureTime() {
        return returnDepartureTime;
    }

    public String getReturnPrice() {
        return returnPrice;
    }
}
