package com.fixmateai.utils

import java.util.Calendar

/**
 * Curated seasonal home-maintenance tips (no network needed). Surfaced as a card
 * on the dashboard and rotated by month so the advice stays timely.
 */
object MaintenanceTips {

    data class Season(val name: String, val tips: List<String>)

    private val spring = Season(
        "Spring",
        listOf(
            "Clean gutters and downspouts after winter debris.",
            "Check window/door seals for drafts and reseal.",
            "Service your AC before the summer heat arrives.",
            "Inspect the roof for loose or missing shingles."
        )
    )
    private val summer = Season(
        "Summer",
        listOf(
            "Clean or replace AC filters monthly.",
            "Check outdoor faucets and hoses for leaks.",
            "Reseal wooden decks and outdoor furniture.",
            "Test smoke and CO detectors."
        )
    )
    private val autumn = Season(
        "Autumn",
        listOf(
            "Service the heating system before winter.",
            "Clear gutters of falling leaves.",
            "Bleed radiators so they heat evenly.",
            "Insulate exposed pipes to prevent freezing."
        )
    )
    private val winter = Season(
        "Winter",
        listOf(
            "Keep faucets dripping in freezing weather to avoid burst pipes.",
            "Check for ice dams on the roof edge.",
            "Reverse ceiling fans to push warm air down.",
            "Inspect the water heater for leaks and pressure."
        )
    )

    fun currentSeason(): Season = when (Calendar.getInstance().get(Calendar.MONTH)) {
        in 2..4 -> spring
        in 5..7 -> summer
        in 8..10 -> autumn
        else -> winter
    }

    /** A single tip chosen by the day of the year, so it changes daily. */
    fun tipOfTheDay(): Pair<String, String> {
        val season = currentSeason()
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return season.name to season.tips[day % season.tips.size]
    }
}
