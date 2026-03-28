package com.ourhome.server.domain.menu

import com.ourhome.server.config.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/menus")
class MenuController(private val menuRepository: MenuRepository) {

    @GetMapping
    fun getMenus(@RequestParam date: String): ResponseEntity<List<MenuResponse>> =
        ResponseEntity.ok(menuRepository.findByDate(date).map { it.toResponse() })

    @PostMapping
    fun createMenu(@RequestBody request: CreateMenuRequest): ResponseEntity<MenuResponse> {
        val menu = DailyMenu(
            date = request.date,
            mealType = request.mealType,
            menuName = request.menuName,
            description = request.description,
            registeredBy = request.registeredBy
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(menuRepository.save(menu).toResponse())
    }

    @PutMapping("/{id}")
    fun updateMenu(
        @PathVariable id: String,
        @RequestBody request: UpdateMenuRequest
    ): ResponseEntity<MenuResponse> {
        val menu = menuRepository.findById(id).orElseThrow { NotFoundException("Menu not found: $id") }
        request.menuName?.let { menu.menuName = it }
        request.description?.let { menu.description = it }
        return ResponseEntity.ok(menuRepository.save(menu).toResponse())
    }

    @DeleteMapping("/{id}")
    fun deleteMenu(@PathVariable id: String): ResponseEntity<Void> {
        if (!menuRepository.existsById(id)) throw NotFoundException("Menu not found: $id")
        menuRepository.deleteById(id)
        return ResponseEntity.noContent().build()
    }
}
