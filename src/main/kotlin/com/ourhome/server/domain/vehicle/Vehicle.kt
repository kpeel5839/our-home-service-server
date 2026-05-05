package com.ourhome.server.domain.vehicle

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "vehicles")
class Vehicle(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var plateNumber: String
)

@Entity
@Table(name = "vehicle_reservations")
class VehicleReservation(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val vehicleId: String,

    @Column(nullable = false)
    val memberId: String,

    @Column(nullable = false)
    val startTime: String,

    @Column(nullable = false)
    val endTime: String,

    var purpose: String? = null,

    @Column(nullable = false)
    var approved: Boolean = false
)

@Entity
@Table(name = "fuel_records")
class FuelRecord(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val vehicleId: String,

    @Column(nullable = false)
    val memberId: String,

    @Column(nullable = false)
    val date: String,

    @Column(nullable = false)
    val liters: Double,

    @Column(nullable = false)
    val amount: Int,

    val stationName: String? = null
)

@Entity
@Table(name = "parking_records")
class ParkingRecord(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(nullable = false)
    val vehicleId: String,

    @Column(nullable = false)
    val memberId: String,

    @Column(nullable = false)
    val floor: String,

    val zone: String? = null,
    val memo: String? = null,

    @Column(nullable = false)
    val recordedAt: String
)

data class VehicleResponse(val id: String, val name: String, val plateNumber: String)
data class ReservationResponse(val id: String, val vehicleId: String, val memberId: String,
    val startTime: String, val endTime: String, val purpose: String?, val approved: Boolean)
data class FuelRecordResponse(val id: String, val vehicleId: String, val memberId: String,
    val date: String, val liters: Double, val amount: Int, val stationName: String?)
data class ParkingRecordResponse(val id: String, val vehicleId: String, val memberId: String,
    val floor: String, val zone: String?, val memo: String?, val recordedAt: String)

data class CreateVehicleRequest(val name: String, val plateNumber: String)
data class CreateReservationRequest(val startTime: String, val endTime: String, val purpose: String? = null)
data class CreateFuelRecordRequest(val date: String, val liters: Double, val amount: Int, val stationName: String? = null)
data class CreateParkingRecordRequest(val floor: String, val zone: String? = null, val memo: String? = null)

fun Vehicle.toResponse() = VehicleResponse(id, name, plateNumber)
fun VehicleReservation.toResponse() = ReservationResponse(id, vehicleId, memberId, startTime, endTime, purpose, approved)
fun FuelRecord.toResponse() = FuelRecordResponse(id, vehicleId, memberId, date, liters, amount, stationName)
fun ParkingRecord.toResponse() = ParkingRecordResponse(id, vehicleId, memberId, floor, zone, memo, recordedAt)
