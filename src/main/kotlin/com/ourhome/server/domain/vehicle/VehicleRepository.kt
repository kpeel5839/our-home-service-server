package com.ourhome.server.domain.vehicle

import org.springframework.data.jpa.repository.JpaRepository

interface VehicleRepository : JpaRepository<Vehicle, String>

interface VehicleReservationRepository : JpaRepository<VehicleReservation, String> {
    fun findByVehicleId(vehicleId: String): List<VehicleReservation>
    fun findByVehicleIdAndStartTimeLessThanAndEndTimeGreaterThan(
        vehicleId: String, endTime: String, startTime: String
    ): List<VehicleReservation>
}

interface FuelRecordRepository : JpaRepository<FuelRecord, String> {
    fun findByVehicleId(vehicleId: String): List<FuelRecord>
}

interface ParkingRecordRepository : JpaRepository<ParkingRecord, String> {
    fun findTopByVehicleIdOrderByRecordedAtDesc(vehicleId: String): ParkingRecord?
}
