import kotlinx.datetime.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.*

val AOC_TZ = TimeZone.of("America/New_York")
const val AOC_YEAR = 2022

@Serializable
data class Leaderboard(
    val members: Map<String, Member>,
    @SerialName("owner_id")
    val ownerId: Long,
    val event: String,
)

@Serializable
data class Member(
    @SerialName("local_score")
    var localScore: Int,
    val name: String?,
    val id: Int,
    @SerialName("global_score")
    val globalScore: Int,
    val stars: Int,
    @SerialName("last_star_ts")
    val lastStarTs: Long,
    @SerialName("completion_day_level")
    val completionDayLevel: Map<String, Map<String, Completion>>
) {
    fun completion(d: Int, l: Int): Completion? = completionDayLevel[d.toString()]?.get(l.toString())
}

@Serializable
data class Completion(
    @SerialName("get_star_ts")
    val starTs: Long,
    @SerialName("star_index")
    val starIndex: Int
) {
    var place = 0
}

const val placeLen = 3
const val scoreLen = 5
const val nameLen = 20
const val gapLen = 4
const val days = 25

val files: List<File> = File("json").listFiles { _, name -> name.endsWith(".json") }!!.toList()

data class MemberCompletion(val m: Member, val c: Completion)

fun main() {
    // Collect all non-empty members from leaderboards
    val members = HashMap<String, Member>()
    for (file in files) {
        val text = file.readText()
        val leaderboard = Json.decodeFromString<Leaderboard>(text)
        members += leaderboard.members.filterValues { it.completionDayLevel.isNotEmpty() }
    }
    // Recompute scores
    for (member in members.values) {
        member.localScore = 0
    }
    val best = HashMap<Int, HashMap<Int, Long>>()
    for (d in 1..25) {
        for (l in 1..2) {
            val order = members.values
                .mapNotNull { m -> m.completion(d, l)?.let { MemberCompletion(m, it) } }
                .sortedBy { it.c.starTs }
            order.firstOrNull()?.c?.starTs?.let { ts ->
                best.getOrPut(d) { HashMap() }[l] = ts
            }
            var ts = 0L
            var place = 0
            for ((i, mc) in order.withIndex()) {
                if (mc.c.starTs > ts) {
                    ts = mc.c.starTs
                    place = i + 1
                }
                mc.m.localScore += (members.size.coerceAtMost(100) - place + 1).coerceAtLeast(0)
                mc.c.place = place
            }
        }
    }
    // Output overall table
    fun header(title: String = "", dayBlock: (Int) -> Unit) {
        print(title.padStart(placeLen + scoreLen + nameLen + scoreLen + 4))
        for (d in 1..days) dayBlock(d)
        println()
    }
    header("Day:") { d ->
        print(" ".repeat(placeLen + gapLen))
        print(d.toString().padEnd(2 + placeLen + gapLen))
    }
    header("Best time:") { d ->
        val dayStart = LocalDate(AOC_YEAR, 12, d).atStartOfDayIn(AOC_TZ).epochSeconds
        for (l in 1..2) {
            if (l == 2) print("/")
            val bt = best[d]?.get(l)
            if (bt == null) {
                print(" ".repeat(placeLen + gapLen))
            } else {
                val dt = bt - dayStart
                print("${dt / 60}:${(dt % 60).toString().padStart(2, '0')}".padStart(placeLen + gapLen))
            }
        }
        print(" ")
    }
    header {
        print("-".repeat(1 + 2 * (placeLen + gapLen)))
        print(" ")
    }
    fun Long.gap(d: Int, l: Int): Long? {
        val bt = best[d]?.get(l) ?: return null
        return this - bt
    }
    for ((index, m) in members.values.sortedByDescending { it.localScore }.withIndex()) {
        print((index + 1).toString().padStart(placeLen))
        print(")")
        print(m.localScore.toString().padStart(scoreLen))
        print(" ")
        print(m.cleanName().padEnd(nameLen))
        if (m.globalScore == 0) {
            print(" ".repeat(scoreLen + 2))
        } else {
            print("[${m.globalScore.toString().padStart(scoreLen)}]")
        }
        for (d in 1..25) {
            for (l in 1..2) {
                if (l == 2) print("/")
                val c = m.completion(d, l)
                if (c == null) {
                    print(" ".repeat(placeLen + gapLen))
                } else {
                    val ps = if (c.place <= 10) Style.BOLD else Style.NORMAL
                    printStyled(ps, c.place.toString().padStart(placeLen))
                    val gap = c.starTs.gap(d, l)
                    val gs = if (gap != null && gap <= 60) Style.BOLD_CYAN else Style.DIM_GRAY
                    printStyled(gs, gap.fmtGap())
                }
            }
            print(" ")
        }
        println()
    }
}

fun Member.cleanName(): String = buildString {
    val s = name ?: return "(anonymous #$id)"
    for (x in s.codePoints()) {
        when (x) {
            // NEGATIVE SQUARED LATIN CAPITAL LETTER
            in 0x1F170..0x1F189 -> appendCodePoint(x - 0x1F170 + 'A'.code)
            else -> appendCodePoint(x)
        }
    }
}

fun Long?.fmtGap(): String {
    var dt = this ?: return " ".repeat(gapLen)
    if (dt <= 0) return "*".repeat(gapLen)
    if (dt <= 59) return "+${dt.toString().padStart(2, '0')}s"
    dt /= 60
    if (dt <= 99) return "+${dt.toString().padStart(2, '0')}m"
//        dt /= 60
//        if (dt <= 23) return "+${dt.toString().padStart(2, '0')}h"
//        dt /= 24
//        return "+${dt.toString().padStart(2, '0')}d"
    return " ".repeat(gapLen)
}

enum class Style { NORMAL, BOLD, BOLD_CYAN, DIM_GRAY }

fun printStyled(style: Style, text: String) {
    val sgr = when (style) {
        Style.BOLD -> "1"
        Style.BOLD_CYAN -> "1;36"
        Style.DIM_GRAY -> "2;90"
        else -> ""
    }
    printSGR(sgr)
    print(text)
    printSGR("")
}

const val ESC = 0x1B.toChar()

fun printSGR(sgr: String) {
    print("${ESC}[${sgr}m")
}