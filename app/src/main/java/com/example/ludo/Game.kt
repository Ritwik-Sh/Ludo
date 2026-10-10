package com.example.ludo

import androidx.compose.runtime.*
import kotlin.math.*
import kotlin.random.Random

private val SEG = listOf(
    intArrayOf(1, 6, 1, 0, 5), intArrayOf(6, 5, 0, -1, 6), intArrayOf(7, 0, 1, 0, 2), intArrayOf(8, 1, 0, 1, 5),
    intArrayOf(9, 6, 1, 0, 6), intArrayOf(14, 7, 0, 1, 2), intArrayOf(13, 8, -1, 0, 5), intArrayOf(8, 9, 0, 1, 6),
    intArrayOf(7, 14, -1, 0, 2), intArrayOf(6, 13, 0, -1, 5), intArrayOf(5, 8, -1, 0, 6), intArrayOf(0, 7, 0, -1, 2)
)
val TRACK: List<Pair<Int, Int>> =
    SEG.flatMap { s -> (0 until s[4]).map { k -> Pair(s[0] + s[2] * k, s[1] + s[3] * k) } }

fun homeCell(o: Int, s: Int): Pair<Int, Int> = when (o) {
    0 -> Pair(1 + s, 7); 1 -> Pair(7, 1 + s); 2 -> Pair(13 - s, 7); else -> Pair(7, 13 - s)
}

/** Board geometry. 4 players = classic cross (13 cells/arm); 5-6 players = circular ring (10 cells/arm). */
class Board(val n: Int) {
    val per = if (n == 4) 13 else 10
    val tl = n * per
    val last = tl - 2          // last shared-track progress value
    val home = last + 6        // progress value of the home triangle
    val radial = n != 4
    val tokR = if (n == 6) .28f else if (n == 5) .32f else .38f
    val safe: Set<Int> = (0 until n).flatMap { listOf(it * per, it * per + if (n == 4) 8 else 5) }.toSet()
    fun start(o: Int) = o * per
    private fun ang(k: Float) = Math.toRadians(-90.0 + 360.0 * k / tl)
    private fun polar(r: Float, a: Double) = Pair(7.5f + (r * cos(a)).toFloat(), 7.5f + (r * sin(a)).toFloat())
    fun colAng(o: Int) = ang(o * per - .5f)
    fun track(k: Int) = if (!radial) TRACK[k].let { Pair(it.first + .5f, it.second + .5f) } else polar(6.3f, ang(k + .5f))
    fun col(o: Int, s: Int) = if (!radial) homeCell(o, s).let { Pair(it.first + .5f, it.second + .5f) } else polar(5.3f - .8f * s, colAng(o))
    fun baseCenter(o: Int) = if (!radial) Pair(if (o == 1 || o == 2) 12f else 3f, if (o >= 2) 12f else 3f)
    else polar(3.7f, ang(o * per - .5f + per / 2f))
    fun base(o: Int, i: Int): Pair<Float, Float> {
        val c = baseCenter(o); val d = if (radial) .7f else 1.5f
        return Pair(c.first + (i % 2 - .5f) * d, c.second + (i / 2 - .5f) * d)
    }
    fun fin(o: Int) = if (!radial) listOf(Pair(6.8f, 7.5f), Pair(7.5f, 6.8f), Pair(8.2f, 7.5f), Pair(7.5f, 8.2f))[o] else polar(1.0f, colAng(o))
    private fun at(o: Int, i: Int, p: Int): Pair<Float, Float> = when {
        p < 0 -> base(o, i)
        p <= last -> track((start(o) + p) % tl)
        p <= last + 5 -> col(o, p - last - 1)
        else -> fin(o)
    }
    /** Position for a (possibly fractional) progress value, interpolating along the path. */
    fun xy(o: Int, i: Int, q: Float): Pair<Float, Float> {
        val a = floor(q).toInt(); val f = q - a
        val (x0, y0) = at(o, i, a)
        if (f < 1e-3f) return Pair(x0, y0)
        val (x1, y1) = at(o, i, a + 1)
        return Pair(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f)
    }
}

class Game(val n: Int, val team: Boolean, val cpu: List<Boolean>, val names: List<String>, val stackSafe: Boolean,
           val unlock1: Boolean, val first: Int, val autoOut: Boolean, val autoSingle: Boolean, val cancel6: Boolean, val pseudo: Boolean,
           val startOne: Boolean = false, val active: List<Int> = (0 until n).toList()) {
    val b = Board(n)
    val pos = mutableStateListOf<Int>().apply { repeat(4 * n) { add(-1) } }
    init { if (startOne) for (o in active) pos[o * 4] = 0 }
    var turn by mutableStateOf(if (first in active) first else active[Random.nextInt(active.size)]); private set
    var dice by mutableStateOf(0); private set
    var rolled by mutableStateOf(false); private set
    var tick by mutableStateOf(0); private set
    var msg by mutableStateOf(""); private set
    var over by mutableStateOf(false); private set
    var title by mutableStateOf(""); private set
    val order = mutableStateListOf<Int>()
    val lastRoll = mutableStateListOf<Int>().apply { repeat(n) { add(Random.nextInt(1, 7)) } }
    val rolls = mutableStateListOf<Int>().apply { repeat(n) { add(0) } }
    val kills = mutableStateListOf<Int>().apply { repeat(n) { add(0) } }
    private var sixes = 0

    private fun teamOf(o: Int) = if (team) o % 2 else o
    private fun partner(o: Int) = (o + 2) % 4
    fun done(o: Int) = (0..3).all { pos[o * 4 + it] == b.home }
    private fun canAct(p: Int) = p in active && (if (team) !(done(p) && done(partner(p))) else !done(p))

    /** Whose tokens the current player moves (partner's once all own tokens are home in Team Up). */
    fun mover(): Int = if (team && done(turn) && !done(partner(turn))) partner(turn) else turn

    private fun dest(idx: Int): Int {
        val p = pos[idx]
        if (p == -1) return if (dice == 6 || (unlock1 && dice == 1)) 0 else -2
        val np = p + dice
        return if (np > b.home) -2 else np
    }

    fun moves(): List<Int> {
        val o = mover()
        return (o * 4 until o * 4 + 4).filter { dest(it) != -2 }.distinctBy { if (pos[it] == -1) -1000 - it else pos[it] }
    }

    private fun victims(o: Int, np: Int): List<Int> {
        if (np !in 0..b.last) return emptyList()
        val a = (b.start(o) + np) % b.tl
        if (a in b.safe) return emptyList()
        fun on(q: Int, i: Int) = pos[i] in 0..b.last && (b.start(q) + pos[i]) % b.tl == a
        val hit = (0 until 4 * n).filter { teamOf(it / 4) != teamOf(o) && on(it / 4, it) }
        // Team Up: a defending stack of N falls if the arriving team has >= N pieces there (incl. the mover).
        val mine = 1 + (0 until 4 * n).count { teamOf(it / 4) == teamOf(o) && on(it / 4, it) }
        return hit.groupBy { teamOf(it / 4) }.values
            .filter { if (team) mine >= it.size else (it.size < 2 || !stackSafe || mine > it.size) }.flatten()
    }

    private val bags = Array(n) { mutableListOf<Int>() }

    /** Dice values that would let the current mover capture an opponent piece. */
    private fun captureValues(): Set<Int> {
        val o = mover()
        val res = mutableSetOf<Int>()
        for (v in 1..6) for (i in o * 4 until o * 4 + 4) {
            val p = pos[i]
            val np = if (p == -1) (if (v == 6 || (unlock1 && v == 1)) 0 else -2) else if (p + v <= b.home) p + v else -2
            if (np >= 0 && victims(o, np).isNotEmpty()) res += v
        }
        return res
    }

    /**
     * True random, or "balanced" mode: a shuffled bag holding every face twice, plus a capture boost:
     * when a capture is possible, a capturing number comes up 55% of the time.
     */
    private fun draw(p: Int, no6: Boolean): Int {
        val allowed = (1..6).filter { !(no6 && it == 6) }
        if (!pseudo) return allowed.random()
        val bag = bags[p]
        fun fromBag(ok: (Int) -> Boolean): Int? {
            if (bag.isEmpty()) { repeat(2) { bag.addAll(1..6) }; bag.shuffle() }
            val i = bag.indexOfLast(ok)
            return if (i >= 0) bag.removeAt(i) else null
        }
        val cap = captureValues().filter { it in allowed }
        if (cap.isNotEmpty()) {
            if (Random.nextFloat() < 0.55f) return cap.random()
            val rest = allowed.filter { it !in cap }
            return if (rest.isNotEmpty()) (fromBag { it in rest } ?: rest.random()) else cap.random()
        }
        return fromBag { it in allowed } ?: allowed.random()
    }

    fun fresh() = Game(n, team, cpu, names, stackSafe, unlock1, first, autoOut, autoSingle, cancel6, pseudo, startOne, active)

    fun roll() {
        if (rolled || over) return
        // "Cancel 3rd 6" off: the dice never lands a third 6 in a row
        val r = draw(turn, !cancel6 && sixes >= 2)
        dice = r; tick++; lastRoll[turn] = r; rolls[turn] = rolls[turn] + 1
        sixes = if (r == 6) sixes + 1 else 0
        if (sixes == 3) { next(); msg = "Three 6s - turn lost"; return }
        val m = moves()
        when {
            m.isEmpty() -> { next(); msg = "No move possible" }
            (autoSingle && m.size == 1) || (autoOut && m.all { pos[it] == -1 }) -> { rolled = true; move(m[0]) }
            else -> rolled = true
        }
    }

    fun move(idx: Int) {
        if (!rolled || over || idx !in moves()) return
        val o = idx / 4
        val np = dest(idx)
        val v = victims(o, np)
        v.forEach { pos[it] = -1 }
        kills[o] = kills[o] + v.size
        pos[idx] = np
        val bonus = dice == 6 || v.isNotEmpty() || np == b.home
        if (done(o) && o !in order) order.add(o)
        rolled = false
        if (checkOver()) return
        if (bonus && canAct(turn)) msg = "Roll again" else next()
    }

    private fun checkOver(): Boolean {
        if (team) {
            for (t in 0..1) if (done(t) && done(t + 2)) {
                over = true; title = "${names[t]} & ${names[t + 2]} win"
                if (t !in order) order.add(t)
                return true
            }
        } else if (order.size >= active.size - 1) {
            active.filter { it !in order }.forEach { order.add(it) }
            over = true; title = "${names[order[0]]} wins"
            return true
        }
        return false
    }

    private fun next() {
        sixes = 0; rolled = false; msg = ""
        do { turn = (turn + 1) % n } while (!canAct(turn))
    }

    fun aiPick(): Int = moves().maxBy {
        val np = dest(it)
        when {
            np == b.home -> 100
            victims(it / 4, np).isNotEmpty() -> 80
            pos[it] == -1 -> 50
            else -> np
        }
    }
}
