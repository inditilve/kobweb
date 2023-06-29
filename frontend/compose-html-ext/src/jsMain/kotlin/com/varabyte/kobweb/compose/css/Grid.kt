package com.varabyte.kobweb.compose.css

import org.jetbrains.compose.web.css.*

typealias CSSFlexValue = CSSSizeValue<out CSSUnitFlex>

/**
 * A common interface for a parameter that is either a single grid track size or a group of them.
 *
 * This allows a user to convert a CSS value like "1fr repeat(3, 100px) 1fr" into a list of [GridTrackSizeEntry]s.
 */
sealed interface GridTrackSizeEntry

/**
 * Represents all possible size values that can be used to configure a CSS grid track.
 *
 * A track is the space between two grid lines -- it can be used for rows or columns based on context.
 *
 * For example, "auto 100px minmax(0.px, 1fr)" can be represented in Kotlin as
 * `GridTrackSize.Auto, GridTrackSize(100.px), GridTrackSize.minmax(0.px, 1.fr)`.
 */
sealed class GridTrackSize private constructor(private val value: String) : StylePropertyValue {
    override fun toString() = value

    class Keyword private constructor(value: String) : GridTrackSize(value)

    /**
     * A numeric value (or sizing keyword) used for this track.
     *
     * This essentially excludes singleton keywords like "none", "inherit", etc.
     */
    open class TrackBreadth internal constructor(value: String) : GridTrackSize(value), GridTrackSizeEntry

    /** A size which tells the track to be as small as possible while still fitting all of its contents. */
    class FitContent internal constructor(value: Any) : TrackBreadth("fit-content($value)")

    abstract class Repeat protected constructor(value: Any, entries: Array<out GridTrackSizeEntry>) :
        GridTrackSize("repeat($value, ${entries.toTrackListString()})"), GridTrackSizeEntry {
        init {
            check(entries.none { it is Repeat }) { "Repeat calls cannot nest other repeat calls" }
        }
    }

    class TrackRepeat(count: Int, vararg entries: GridTrackSizeEntry) : Repeat(count, entries)
    class AutoRepeat(type: Type, vararg entries: GridTrackSizeEntry) : Repeat(type, entries) {
        enum class Type(private val value: String) {
            AutoFill("auto-fill"),
            AutoFit("auto-fit");

            override fun toString() = value
        }
    }

    /** A size which represents a range of values this track can be. */
    open class MinMax internal constructor(min: Any, max: Any) : TrackBreadth("minmax($min, $max)")

    /** Like [TrackBreadth] but excludes flex values (e.g. `1fr`) */
    open class InflexibleBreadth internal constructor(value: String) : TrackBreadth(value)

    // these are used for grid-template-*
    sealed interface FixedSize

    /** Represents a track size which is fixed, either a pixel or percentage value (e.g. `100px`, `40%`) */
    class FixedBreadth internal constructor(value: String) : InflexibleBreadth(value), FixedSize

    /** A special min-max value which excludes flex values (e.g. `1fr`) */
    class FixedMinMax internal constructor(min: Any, max: Any) : MinMax(min, max), FixedSize

    companion object {
        val Auto get() = InflexibleBreadth("auto")
        val MinContent get() = InflexibleBreadth("min-content")
        val MaxContent get() = InflexibleBreadth("max-content")

        operator fun invoke(value: CSSLengthOrPercentageValue) = FixedBreadth(value.toString())
        operator fun invoke(value: CSSFlexValue) = TrackBreadth(value.toString())

        fun minmax(min: InflexibleBreadth, max: TrackBreadth): GridTrackSizeEntry = MinMax(min, max)
        fun minmax(min: InflexibleBreadth, max: CSSFlexValue): GridTrackSizeEntry = minmax(min, invoke(max))
        fun minmax(min: FixedBreadth, max: TrackBreadth): FixedMinMax = FixedMinMax(min, max)
        fun minmax(min: InflexibleBreadth, max: CSSLengthOrPercentageValue): FixedMinMax = FixedMinMax(min, max)
        fun minmax(min: CSSLengthOrPercentageValue, max: TrackBreadth): FixedMinMax = minmax(invoke(min), max)
        fun minmax(min: CSSLengthOrPercentageValue, max: CSSLengthOrPercentageValue): FixedMinMax =
            minmax(min, invoke(max))

        fun minmax(min: CSSLengthOrPercentageValue, max: CSSFlexValue): FixedMinMax = minmax(min, invoke(max))

        fun fitContent(value: CSSLengthOrPercentageValue) = FitContent(value)

        fun repeat(count: Int, vararg entries: GridTrackSizeEntry) = TrackRepeat(count, *entries)
        fun repeat(type: AutoRepeat.Type, vararg entries: GridTrackSizeEntry) = AutoRepeat(type, *entries)
    }
}

// TODO: Runtime exception if person uses AutoRepeat and TrackRepeat withflexible sizes in the same list?
private fun Array<out GridTrackSizeEntry>.toTrackListString(): String = buildString {
    val names = mutableListOf<String>()
    val entries = this@toTrackListString

    fun appendWithLeadingSpace(value: Any) {
        if (isNotEmpty()) {
            append(' ')
        }
        append(value)
    }

    fun appendNamesIfAny() {
        if (names.isNotEmpty()) {
            appendWithLeadingSpace(names.joinToString(" ", prefix = "[", postfix = "]"))
            names.clear()
        }
    }

    fun appendSize(value: Any) {
        appendNamesIfAny()
        appendWithLeadingSpace(value)
    }

    for (entry in entries) {
        when (entry) {
            is NamedGridTrackSize -> {
                if (entry.startNames != null) {
                    names.addAll(entry.startNames)
                }
                appendSize(entry.size)
                if (entry.endNames != null) {
                    names.addAll(entry.endNames)
                }
            }

            is GridTrackSize -> appendSize(entry)
        }
    }
    appendNamesIfAny()
}

/**
 * A CSS grid track size tagged with names.
 */
class NamedGridTrackSize(
    val size: GridTrackSize,
    val startNames: List<String>? = null,
    val endNames: List<String>? = null
) : GridTrackSizeEntry {
    constructor(size: GridTrackSize, startName: String? = null, endName: String? = null) : this(
        size,
        startName?.let { listOf(it) },
        endName?.let { listOf(it) })
}

fun GridTrackSize.named(startNames: List<String>? = null, endNames: List<String>? = null) =
    NamedGridTrackSize(this, startNames, endNames)

fun GridTrackSize.named(startName: String? = null, endName: String? = null) =
    NamedGridTrackSize(this, startName, endName)

sealed class GridAuto private constructor(private val value: String) : StylePropertyValue {
    override fun toString() = value

    class Keyword internal constructor(value: String) : GridAuto(value)

    companion object {
        // Keywords
        val None get() = Keyword("none")

        // Global values
        val Inherit get() = Keyword("inherit")
        val Initial get() = Keyword("initial")
        val Revert get() = Keyword("revert")
        val Unset get() = Keyword("unset")
    }
}

fun StyleScope.gridAutoColumns(gridAutoColumns: GridAuto.Keyword) {
    gridAutoColumns(gridAutoColumns.toString())
}

fun StyleScope.gridAutoColumns(vararg gridAutoColumns: GridTrackSizeEntry) {
    gridAutoColumns(gridAutoColumns.toTrackListString())
}

fun StyleScope.gridAutoRows(gridAutoRows: GridAuto.Keyword) {
    gridAutoRows(gridAutoRows.toString())
}

fun StyleScope.gridAutoRows(vararg gridAutoRows: GridTrackSizeEntry) {
    gridAutoRows(gridAutoRows.toTrackListString())
}

/**
 * Represents all possible values that can be passed into a CSS grid property.
 *
 * Note: "masonry" purposely excluded as it is not supported in any major browsers
 * See also: https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_grid_layout/Masonry_layout
 *
 * See also: https://developer.mozilla.org/en-US/docs/Web/CSS/grid-template
 */
sealed class GridTemplate private constructor(private val value: String) : StylePropertyValue {
    override fun toString() = value

    class Keyword internal constructor(value: String) : GridTemplate(value)
    companion object {
        // Keywords
        val None get() = Keyword("none")

        // Global
        val Initial get() = Keyword("initial")
        val Inherit get() = Keyword("inherit")
        val Revert get() = Keyword("revert")
        val Unset get() = Keyword("unset")
    }
}

fun StyleScope.gridTemplateColumns(gridTemplateColumns: GridTemplate.Keyword) {
    gridTemplateColumns(gridTemplateColumns.toString())
}

fun StyleScope.gridTemplateColumns(vararg gridTemplateColumns: GridTrackSizeEntry) {
    gridTemplateColumns(gridTemplateColumns.toTrackListString())
}

fun StyleScope.gridTemplateRows(gridTemplateRows: GridTemplate.Keyword) {
    gridTemplateRows(gridTemplateRows.toString())
}

fun StyleScope.gridTemplateRows(vararg gridTemplateRows: GridTrackSizeEntry) {
    gridTemplateRows(gridTemplateRows.toTrackListString())
}
