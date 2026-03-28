package com.ourhome.server.domain.vehicle

import com.ourhome.server.config.ConflictException
import com.ourhome.server.config.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/vehicles")
class VehicleController(
    private val vehicleRepository: VehicleRepository,
    private val reservationRepository: VehicleReservationRepository,
    private val fuelRepository: FuelRecordRepository,
    private val parkingRepository: ParkingRecordRepository
) {

    @GetMapping
    fun getVehicles(): ResponseEntity<List<VehicleResponse>> =
        ResponseEntity.ok(vehicleRepository.findAll().map { it.toResponse() })

    @PostMapping
    fun createVehicle(@RequestBody request: CreateVehicleRequest): ResponseEntity<VehicleResponse> {
        val vehicle = Vehicle(name = request.name, plateNumber = request.plateNumber)
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleRepository.save(vehicle).toResponse())
    }

    // ─── Reservations ───────────────────────────────────────────────────────

    @GetMapping("/{vehicleId}/reservations")
    fun getReservations(@PathVariable vehicleId: String): ResponseEntity<List<ReservationResponse>> {
        if (!vehicleRepository.existsById(vehicleId)) throw NotFoundException("Vehicle not found: $vehicleId")
        return ResponseEntity.ok(reservationRepository.findByVehicleId(vehicleId).map { it.toResponse() })
    }

    @PostMapping("/{vehicleId}/reservations")
    fun createReservation(
        @PathVariable vehicleId: String,
        @RequestBody request: CreateReservationRequest
    ): ResponseEntity<ReservationResponse> {
        if (!vehicleRepository.existsById(vehicleId)) throw NotFoundException("Vehicle not found: $vehicleId")
        val conflicts = reservationRepository.findByVehicleIdAndStartTimeLessThanAndEndTimeGreaterThan(
            vehicleId, request.endTime, request.startTime
        )
        if (conflicts.isNotEmpty()) throw ConflictException("Reservation time conflicts with existing reservation")
        val reservation = VehicleReservation(
            vehicleId = vehicleId,
            memberId = request.memberId,
            startTime = request.startTime,
            endTime = request.endTime,
            purpose = request.purpose
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationRepository.save(reservation).toResponse())
    }

    @DeleteMapping("/{vehicleId}/reservations/{id}")
    fun deleteReservation(
        @PathVariable vehicleId: String,
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val reservation = reservationRepository.findById(id).orElseThrow { NotFoundException("Reservation not found: $id") }
        if (reservation.vehicleId != vehicleId) throw NotFoundException("Reservation not found for this vehicle")
        reservationRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    // ─── Fuel Records ────────────────────────────────────────────────────────

    @GetMapping("/{vehicleId}/fuel")
    fun getFuelRecords(@PathVariable vehicleId: String): ResponseEntity<List<FuelRecordResponse>> {
        if (!vehicleRepository.existsById(vehicleId)) throw NotFoundException("Vehicle not found: $vehicleId")
        return ResponseEntity.ok(fuelRepository.findByVehicleId(vehicleId).map { it.toResponse() })
    }

    @PostMapping("/{vehicleId}/fuel")
    fun createFuelRecord(
        @PathVariable vehicleId: String,
        @RequestBody request: CreateFuelRecordRequest
    ): ResponseEntity<FuelRecordResponse> {
        if (!vehicleRepository.existsById(vehicleId)) throw NotFoundException("Vehicle not found: $vehicleId")
        val record = FuelRecord(
            vehicleId = vehicleId,
            memberId = request.memberId,
            date = request.date,
            liters = request.liters,
            amount = request.amount,
            stationName = request.stationName
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(fuelRepository.save(record).toResponse())
    }

    // ─── Parking Records ─────────────────────────────────────────────────────

    @GetMapping("/{vehicleId}/parking/latest")
    fun getLatestParking(@PathVariable vehicleId: String): ResponseEntity<ParkingRecordResponse> {
        if (!vehicleRepository.existsById(vehicleId)) throw NotFoundException("Vehicle not found: $vehicleId")
        val record = parkingRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId)
            ?: throw NotFoundException("No parking record found for vehicle: $vehicleId")
        return ResponseEntity.ok(record.toResponse())
    }

    @PostMapping("/{vehicleId}/parking")
    fun createParkingRecord(
        @PathVariable vehicleId: String,
        @RequestBody request: CreateParkingRecordRequest
    ): ResponseEntity<ParkingRecordResponse> {
        if (!vehicleRepository.existsById(vehicleId)) throw NotFoundException("Vehicle not found: $vehicleId")
        val record = ParkingRecord(
            vehicleId = vehicleId,
            memberId = request.memberId,
            floor = request.floor,
            zone = request.zone,
            memo = request.memo,
            recordedAt = Instant.now().toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingRepository.save(record).toResponse())
    }
}
