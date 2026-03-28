package com.ourhome.server.config

import com.ourhome.server.domain.member.FamilyMember
import com.ourhome.server.domain.member.MemberRepository
import com.ourhome.server.domain.member.MemberRole
import com.ourhome.server.domain.trash.ApartmentTrashRule
import com.ourhome.server.domain.trash.ApartmentTrashRuleRepository
import com.ourhome.server.domain.trash.TrashType
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val memberRepository: MemberRepository,
    private val trashRuleRepository: ApartmentTrashRuleRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (memberRepository.count() == 0L) {
            memberRepository.saveAll(listOf(
                FamilyMember(id = "m1", name = "아빠", role = MemberRole.FATHER, avatarColor = "#4A90E2"),
                FamilyMember(id = "m2", name = "엄마", role = MemberRole.MOTHER, avatarColor = "#E24A90"),
                FamilyMember(id = "m3", name = "아들", role = MemberRole.SON,    avatarColor = "#4AE290"),
                FamilyMember(id = "m4", name = "딸",   role = MemberRole.DAUGHTER, avatarColor = "#E2C44A")
            ))
        }

        if (trashRuleRepository.count() == 0L) {
            trashRuleRepository.saveAll(listOf(
                ApartmentTrashRule(dayOfWeek = 1, trashTypes = mutableListOf(TrashType.GENERAL, TrashType.RECYCLE)), // 월
                ApartmentTrashRule(dayOfWeek = 3, trashTypes = mutableListOf(TrashType.FOOD)),                       // 수
                ApartmentTrashRule(dayOfWeek = 5, trashTypes = mutableListOf(TrashType.GENERAL, TrashType.RECYCLE)), // 금
                ApartmentTrashRule(dayOfWeek = 6, trashTypes = mutableListOf(TrashType.LARGE))                       // 토
            ))
        }
    }
}
