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
class Board(val n: Int, val ppp: Int = 4) {
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
        val c = baseCenter(o)
        val cols = if (ppp > 4) 3 else 2
        val rows = (ppp + cols - 1) / cols
        val dx = if (radial) (if (ppp > 4) .62f else .7f) else (if (ppp > 4) 1.3f else 1.5f)
        val dy = if (radial) (if (ppp > 4) .62f else .7f) else 1.5f
        return Pair(c.first + (i % cols - (cols - 1) / 2f) * dx, c.second + (i / cols - (rows - 1) / 2f) * dy)
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
           val startOne: Boolean = false, val active: List<Int> = (0 until n).toList(),
           val killLock: Boolean = false, val removeWith1: Boolean = false, val ppp: Int = 4) {
    val b = Board(n, ppp)
    private val doneVal = if (removeWith1) b.home + 1 else b.home
    val pos = mutableStateListOf<Int>().apply { repeat(ppp * n) { add(-1) } }
    init { if (startOne) for (o in active) pos[o * ppp] = 0 }
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
    private var idlePasses = 0
    private var lockBroken = false

    private fun teamOf(o: Int) = if (team) o % 2 else o
    private fun partner(o: Int) = (o + 2) % 4
    /** Has this player (or, in Team Up, their team) captured at least once? Also true if the game got stuck. */
    private fun hasKill(o: Int) = lockBroken || (if (team) kills[o] + kills[partner(o)] else kills[o]) > 0
    fun homeLocked(o: Int) = killLock && !hasKill(o)
    fun gone(i: Int) = removeWith1 && pos[i] == b.home + 1
    fun done(o: Int) = (0 until ppp).all { pos[o * ppp + it] == doneVal }
    private fun canAct(p: Int) = p in active && (if (team) !(done(p) && done(partner(p))) else !done(p))

    /** Whose tokens the current player moves (partner's once all own tokens are home in Team Up). */
    fun mover(): Int = if (team && done(turn) && !done(partner(turn))) partner(turn) else turn

    private fun dest(idx: Int): Int {
        val p = pos[idx]
        if (p == -1) return if (dice == 6 || (unlock1 && dice == 1)) 0 else -2
        if (p == b.home) return if (removeWith1 && dice == 1) b.home + 1 else -2
        if (p > b.home) return -2
        val np = p + dice
        if (np > b.home) return -2
        if (killLock && np > b.last && !hasKill(idx / ppp)) return -2
        return np
    }

    fun moves(): List<Int> {
        val o = mover()
        return (o * ppp until o * ppp + ppp).filter { dest(it) != -2 }.distinctBy { if (pos[it] == -1) -1000 - it else pos[it] }
    }

    private fun tokensAt(a: Int): List<Int> = (0 until ppp * n).filter {
        it / ppp in active && pos[it] in 0..b.last && (b.start(it / ppp) + pos[it]) % b.tl == a
    }

    /** Team Up: two pieces of different colours on one square (same team) cannot be captured. */
    private fun protectedGroup(grp: List<Int>) = team && grp.map { it / ppp }.toSet().size >= 2

    /** Pieces an arriving piece captures: one piece of every unprotected opposing group (stacks of the same colour lose one). */
    private fun victims(o: Int, np: Int): List<Int> {
        if (np !in 0..b.last) return emptyList()
        val a = (b.start(o) + np) % b.tl
        if (a in b.safe) return emptyList()
        return tokensAt(a).filter { teamOf(it / ppp) != teamOf(o) }
            .groupBy { teamOf(it / ppp) }.values.filter { !protectedGroup(it) }.map { it.first() }
    }

    private val bags = Array(n) { mutableListOf<Int>() }

    /** Dice values that would let the current mover capture an opponent piece. */
    private fun captureValues(): Set<Int> {
        val o = mover()
        val res = mutableSetOf<Int>()
        for (v in 1..6) for (i in o * ppp until o * ppp + ppp) {
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

    fun fresh() = Game(n, team, cpu, names, stackSafe, unlock1, first, autoOut, autoSingle, cancel6, pseudo, startOne, active, killLock, removeWith1, ppp)

    fun roll() {
        if (rolled || over) return
        // "Cancel 3rd 6" off: the dice never lands a third 6 in a row
        val r = draw(turn, !cancel6 && sixes >= 2)
        dice = r; tick++; lastRoll[turn] = r; rolls[turn] = rolls[turn] + 1
        sixes = if (r == 6) sixes + 1 else 0
        if (sixes == 3) { next(); msg = "Three 6s - turn lost"; return }
        val m = moves()
        when {
            m.isEmpty() -> { idlePasses++; if (idlePasses >= active.size * 8) lockBroken = true; next(); msg = "No move possible" }
            (autoSingle && m.size == 1) || (autoOut && m.all { pos[it] == -1 }) -> { rolled = true; move(m[0]) }
            else -> rolled = true
        }
    }

    fun move(idx: Int) {
        if (!rolled || over || idx !in moves()) return
        idlePasses = 0
        val o = idx / ppp
        val np = dest(idx)
        val p0 = pos[idx]
        val v = victims(o, np)
        v.forEach { pos[it] = -1 }
        kills[o] = kills[o] + v.size
        pos[idx] = np
        // A protected Team Up pair was split: the piece left behind next to an enemy is captured instantly
        if (p0 in 0..b.last) {
            val a0 = (b.start(o) + p0) % b.tl
            if (a0 !in b.safe) {
                val here = tokensAt(a0)
                val mine = here.filter { teamOf(it / ppp) == teamOf(o) }
                val foes = here.filter { teamOf(it / ppp) != teamOf(o) }
                if (mine.isNotEmpty() && foes.isNotEmpty() && !protectedGroup(mine)) {
                    val k = min(foes.size, mine.size)
                    mine.take(k).forEach { pos[it] = -1 }
                    kills[foes.first() / ppp] = kills[foes.first() / ppp] + k
                }
            }
        }
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
            victims(it / ppp, np).isNotEmpty() -> 80
            pos[it] == -1 -> 50
            else -> np
        }
    }
}
