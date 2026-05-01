package com.ourhome.server.domain.vehicle

import com.ourhome.server.config.ConflictException
import com.ourhome.server.config.NotFoundException
import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.notification.DiscordNotificationService
import com.ourhome.server.domain.member.MemberRole
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
    private val parkingRepository: ParkingRecordRepository,
    private val memberRepository: MemberRepository,
    private val discordNotificationService: DiscordNotificationService
) {

    private fun memberName(memberId: String): String =
        memberRepository.findById(memberId).map { it.name }.orElse("알 수 없음")

    private fun formatTime(isoTime: String): String =
        if (isoTime.length >= 16) isoTime.substring(11, 16) else isoTime

    private fun formatDate(dateStr: String): String {
        val d = if (dateStr.length >= 10) dateStr.substring(0, 10) else dateStr
        val parts = d.split("-")
        return if (parts.size == 3) "${parts[0]}년 ${parts[1].trimStart('0')}월 ${parts[2].trimStart('0')}일" else d
    }

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
        val saved = reservationRepository.save(reservation)
        val name = memberName(request.memberId)
        val dateStr = formatDate(request.startTime)
        val timeStr = "${formatTime(request.startTime)}~${formatTime(request.endTime)}"
        val purposePart = if (!request.purpose.isNullOrBlank()) "\n목적: ${request.purpose}" else ""
        discordNotificationService.sendToRole("FATHER",
            "🚗 ${name}님이 차량 예약을 요청했어요\n$dateStr $timeStr$purposePart\n\n✅ 승인하려면 아래 API를 호출해주세요:\nPATCH /api/vehicles/$vehicleId/reservations/${saved.id}/approve\n{\"approverId\": \"m1\"}"
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @DeleteMapping("/{vehicleId}/reservations/{id}")
    fun deleteReservation(
        @PathVariable vehicleId: String,
        @PathVariable id: String,
        @RequestParam memberId: String
    ): ResponseEntity<Void> {
        val reservation = reservationRepository.findById(id).orElseThrow { NotFoundException("Reservation not found: $id") }
        if (reservation.vehicleId != vehicleId) throw NotFoundException("Reservation not found for this vehicle")
        if (reservation.memberId != memberId) throw ForbiddenException("본인이 만든 예약만 삭제할 수 있습니다")
        reservationRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{vehicleId}/reservations/{id}/approve")
    fun approveReservation(
        @PathVariable vehicleId: String,
        @PathVariable id: String,
        @RequestBody request: ApproveReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val approver = memberRepository.findById(request.approverId).orElseThrow { NotFoundException("Member not found: ${request.approverId}") }
        if (approver.role != MemberRole.FATHER) throw ForbiddenException("아빠만 차량 예약을 승인할 수 있습니다")
        val reservation = reservationRepository.findById(id).orElseThrow { NotFoundException("Reservation not found: $id") }
        if (reservation.vehicleId != vehicleId) throw NotFoundException("Reservation not found for this vehicle")
        reservation.approved = true
        val saved = reservationRepository.save(reservation)
        val requesterName = memberName(reservation.memberId)
        val dateStr = formatDate(reservation.startTime)
        val timeStr = "${formatTime(reservation.startTime)}~${formatTime(reservation.endTime)}"
        discordNotificationService.sendToAllMembers("✅ ${requesterName}님의 차량 예약이 승인되었어요\n$dateStr $timeStr")
        return ResponseEntity.ok(saved.toResponse())
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
        val savedFuel = fuelRepository.save(record)
        val fuelName = memberName(request.memberId)
        val stationPart = if (!request.stationName.isNullOrBlank()) "\n주유소: ${request.stationName}" else ""
        val amountFormatted = "%,d".format(request.amount)
        discordNotificationService.sendToAllMembers("⛽ ${fuelName}님이 주유를 기록했어요\n${request.liters}L / ${amountFormatted}원$stationPart")
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFuel.toResponse())
    }

    @DeleteMapping("/{vehicleId}/fuel/{id}")
    fun deleteFuelRecord(
        @PathVariable vehicleId: String,
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val record = fuelRepository.findById(id).orElseThrow { NotFoundException("Fuel record not found: $id") }
        if (record.vehicleId != vehicleId) throw NotFoundException("Fuel record not found for this vehicle")
        fuelRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }

    // ─── Parking Records ─────────────────────────────────────────────────────

    @GetMapping("/{vehicleId}/parking/latest")
    fun getLatestParking(@PathVariable vehicleId: String): ResponseEntity<ParkingRecordResponse> {
        if (!vehicleRepository.existsById(vehicleId)) throw NotFoundException("Vehicle not found: $vehicleId")
        val record = parkingRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId)
            ?: throw NotFoundException("No parking record found for vehicle: $vehicleId")
        return ResponseEntity.ok(record.toResponse())
    }

    @DeleteMapping("/{vehicleId}/parking/{id}")
    fun deleteParkingRecord(
        @PathVariable vehicleId: String,
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val record = parkingRepository.findById(id).orElseThrow { NotFoundException("Parking record not found: $id") }
        if (record.vehicleId != vehicleId) throw NotFoundException("Parking record not found for this vehicle")
        parkingRepository.deleteById(id)
        return ResponseEntity.noContent().build()
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
        val savedParking = parkingRepository.save(record)
        val parkName = memberName(request.memberId)
        val zonePart = if (!request.zone.isNullOrBlank()) " ${request.zone}구역" else ""
        val memoPart = if (!request.memo.isNullOrBlank()) "\n메모: ${request.memo}" else ""
        discordNotificationService.sendToAllMembers("🅿️ ${parkName}님이 주차 위치를 기록했어요\n${request.floor}$zonePart$memoPart")
        return ResponseEntity.status(HttpStatus.CREATED).body(savedParking.toResponse())
    }
}
