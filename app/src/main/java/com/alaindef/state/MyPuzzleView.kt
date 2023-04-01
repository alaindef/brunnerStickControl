package com.alaindef.puzzle

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.weather_app.R
import java.lang.Integer.min
import java.util.*
import kotlin.math.abs

/**
 * Created by alaindef on 28.07.15.
 */
class MyPuzzleView : FrameLayout {
    private val logTag = ">---MyPuzzleView----"
    private val mTiles = ArrayList<Tile>()
    private lateinit var state: IntArray
    private var cols = 3
    private var show = 0
    private var slide: ImageView? = null
    private var flatBackground: ImageView? = null

    internal constructor(context: Context?) : super(context!!)
    internal constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    internal constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!, attrs, defStyleAttr
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)
        val requestedWidthMode = MeasureSpec.getMode(widthMeasureSpec)
        var requestedHeight = MeasureSpec.getSize(heightMeasureSpec)
        val requestedHeightMode = MeasureSpec.getMode(heightMeasureSpec)

        val drawDinges = slide!!.drawable         //the slide we are going to hak
        val origBitmap = ((drawDinges ?: nullexception()) as BitmapDrawable).bitmap
        val slideWidth = origBitmap.width
        val slideHeight = origBitmap.height

        val desiredWidth: Int
        val desiredHeight: Int

        requestedHeight = min(1500, requestedHeight)

        if ((requestedWidth * slideHeight) < (requestedHeight * slideWidth)) {
            desiredWidth = requestedWidth
            desiredHeight = if (slideWidth == 0) 80
            else ((requestedWidth * slideHeight) / slideWidth)
        }
        else {
            desiredHeight = requestedHeight
            desiredWidth = if (slideHeight == 0) 80
            else ((requestedHeight * slideWidth) / slideHeight)
        }

        val newWidth = when (requestedWidthMode) {
            MeasureSpec.EXACTLY -> requestedWidth
            MeasureSpec.UNSPECIFIED -> requestedWidth
            MeasureSpec.AT_MOST -> desiredWidth
            else -> 40
        }

        val newHeight = when (requestedHeightMode) {
            MeasureSpec.EXACTLY -> requestedHeight
            MeasureSpec.UNSPECIFIED -> requestedHeight
            MeasureSpec.AT_MOST -> desiredHeight
            else -> 80
        }

        setMeasuredDimension(newWidth, newHeight)

//        Log.i(
//            logTag,
//            "request=($requestedWidth, $requestedHeight)  mode=($requestedWidthMode, $requestedHeightMode)   desired=($desiredWidth, $desiredHeight) slide=($slideWidth, $slideHeight)  puzzle=($newWidth, $newHeight)"
//        )
    }

    fun init(slideIn: ImageView?, fixedBackground: ImageView?) {
        flatBackground = fixedBackground //fixed background (green)
        slide = slideIn
        //16 tiles, for both 8 and 15 puzzle
        for (tileIndex in 0 until max_number_of_tiles) {
            val tile = Tile(context, tileIndex)
            mTiles.add(tile)
            if (tileIndex == 0) {
                tile.setOnClickListener { v ->
                    Main.oscar.send(FSM.EV_SOLVE_1, 0, 0, state)
                    Log.i(logTag, "--------" + (v as Tile).tileIndex)
                }
            } else {
                tile.setOnClickListener { v -> Main.oscar.send(FSM.EV_CLICK, 0, 0, v) }
            }
        }
        hak(slide)
        addTiles()
    }

    private fun addTiles() {
        val nTiles = cols * cols
        state = IntArray(nTiles)
        if (this.childCount > 0) this.removeAllViews()
        for (tileIndex in 0 until nTiles) {
            addView(mTiles[tileIndex])
            state[tileIndex] = tileIndex // initialize state
        }
    }

    fun resetAndSortTiles() {
        val nTiles = cols * cols
        val tileWidth = this.width / cols
        val tileHeight = this.height / cols
        for (tileIndex in 0 until nTiles) {
            val col = tileIndex % cols
            val row = tileIndex / cols
            val left = tileWidth * col
            val top = tileHeight * row
            val right = left + tileWidth
            val bottom = top + tileHeight
            val tile = mTiles[tileIndex]
            val layoutParams = LayoutParams(tileWidth, tileHeight)
            layoutParams.setMargins(left, top, right, bottom) // without animation
            tile.layoutParams = layoutParams
        }
    }

    private fun resetTiles() {
        val nTiles = cols * cols
        val tileWidth = this.width / cols
        val tileHeight = this.height / cols
        for (i in 0 until nTiles) {
            val tileIndex = state[i]
            val col = tileIndex % cols
            val row = tileIndex / cols
            val left = tileWidth * col
            val top = tileHeight * row
            val right = left + tileWidth
            val bottom = top + tileHeight
            val tile = mTiles[tileIndex]
            val layoutParams = LayoutParams(tileWidth, tileHeight)
            layoutParams.setMargins(left, top, right, bottom) // without animation
            tile.layoutParams = layoutParams
        }
    }

    private fun nullexception() {
//      221020   tried the elvis operator on origbitmap
        Log.wtf(logTag, "origBitmap = " + null + " !!!!")
    }

    private fun hak(slide: ImageView?) {
        val drawDinges = slide!!.drawable         //the slide we are going to hak
        val origBitmap = ((drawDinges ?: nullexception()) as BitmapDrawable).bitmap
        val newWidth = origBitmap.width / cols
        val newHeight = origBitmap.height / cols
        var x: Int
        var y: Int
        val nTiles = cols * cols
        for (tileIndex in 0 until nTiles) {
            x = newWidth * (tileIndex % cols)
            y = newHeight * (tileIndex / cols)
//            cut a rectangle out of origBitmap:
            val zeBitmap = Bitmap.createBitmap(origBitmap, x, y, newWidth, newHeight)
            val bmd = BitmapDrawable(this.resources, zeBitmap)
            mTiles[tileIndex].background = bmd
        }
        dim(Main.DIM)
    }

    fun setSlideAndHak(bm: BitmapDrawable?) {
        slide!!.setImageDrawable(bm)
        hak(slide)
        post{resetTiles()}
    }

    private fun positionOf(state: IntArray, tileIndex: Int): Int {
        for (i in state.indices) if (state[i] == tileIndex) return i
        return -1
    }

    //    adf 221024 goalreached does not belong here!
    @Suppress("BooleanMethodIsAlwaysInverted")
    private fun goalReached(): Boolean {
        val len = state.size
        for (i in 0 until len) if (state[i] != i) return false
        return true
    }

    private fun dim(dim: Float) {
        mTiles[0].alpha = dim
    }

    //rotate returns an array of tileindices
    private fun rotate(sliceOfGridPositions: IntArray, len: Int): IntArray {
        val targetslice = IntArray(len)
        if (state[sliceOfGridPositions[0]] == 0) {
            for (i in 1 until len) targetslice[i - 1] = state[sliceOfGridPositions[i]]
            targetslice[len - 1] = 0
        }
        if (state[sliceOfGridPositions[len - 1]] == 0) {
            for (i in 1 until len) targetslice[len - i] = state[sliceOfGridPositions[len - 1 - i]]
            targetslice[0] = 0
        }
        return targetslice
    }

    // returns the positions in the current state of the tiles to move.
    // position is the position in the current state of the tile clicked
    private fun slice(pos: Int): IntArray {
//        int pos0 = Arrays.asList(mState).indexOf(0);    //werkt natuurlijk nie
        val pos0 = positionOf(state, 0)
        val x0 = pos0 % cols
        val y0 = pos0 / cols
        val x = pos % cols
        val y = pos / cols
        val len = abs(abs(x0 - x) - abs(y0 - y))
        val res = IntArray(len + 1)
        Arrays.fill(res, -1)
        if (len == 0) return intArrayOf() //empty
        if (y == y0) {
            if (x0 < x) for (i in 0..len) res[i] = pos0 + i else for (i in 0..len) res[i] = pos + i
        } else if (x == x0) {
            if (y0 < y) for (i in 0..len) res[i] = pos0 + i * cols else for (i in 0..len) res[i] =
                pos + i * cols
        } else return intArrayOf() //empty}
        return res
    }

    fun swap0(tile: Tile, sound: Boolean) {
        swap0(positionOf(state, tile.tileIndex), sound)
    }

    private fun swap0(tilePos: Int, sound: Boolean) {

        // Tiles to swap, starting or ending with tile0
        val sliceOfPositions = slice(tilePos)
        val len = sliceOfPositions.size
        if (len == 0) {                                  //tile 0 not in same row or col
            Main.oscar.send(FSM.EV_MOVE_DONE)
            return
        }
        val targetSliceOfTileIndices = rotate(sliceOfPositions, len)

        //store target x's and y's
        val sliceOfX = ArrayList<Int>()
        val sliceOfY = ArrayList<Int>()
        for (pos in sliceOfPositions) {
            val tile = mTiles[state[pos]]
            sliceOfX.add(tile.left)
            sliceOfY.add(tile.top)
        }

        //mov slice
        for (i in 0 until len) {
            val tile = mTiles[targetSliceOfTileIndices[i]]
            if (Main.ANIMATION_LAG > 0) tile.mov(
                sliceOfX[i],
                sliceOfY[i],
                Main.ANIMATION_LAG
            ) else tile.movNow(
                sliceOfX[i], sliceOfY[i]
            )
        }
        dim(Main.DIM)

        //update mState
        for (i in 0 until len) state[sliceOfPositions[i]] = targetSliceOfTileIndices[i]
        if (goalReached()) dim(255f)
        if (sound) Main.mediaPlayer!!.start()
    }

    private fun setNewState(newState: IntArray, animationLag: Int) {

        //first copy mState
//        int[] oldState = new int[Main.SIZE];
//        for (int pos = 0; pos < Main.SIZE; pos++) oldState[pos] = mState[pos];
        val oldState = state.clone()
        val size = oldState.size
        if (size != newState.size) {
            Log.e(logTag, "!!! New state has wrong size !!!")
            return
        }

        //store target x's and y's
        val x = ArrayList<Int>()
        val y = ArrayList<Int>()
        for (anOldState in oldState) {
            val tile = mTiles[anOldState]
            x.add(tile.left)
            y.add(tile.top)
        }

        //mov slice
        for (i in 0 until size) {
            val tile = mTiles[newState[i]]
            tile.mov(x[i], y[i], animationLag)
            dim(Main.DIM)
        }

        //update mState
        System.arraycopy(newState, 0, state, 0, size)
        // TODO: 23.10.15 mState = newState or mState = newState.clone(); DOES NOT WORK - WHY?
        if (goalReached()) dim(255f)
    }

    fun reset() {
        val nTiles = cols * cols
        val start = IntArray(nTiles)
        for (tileIndex in 0 until nTiles) {
            start[tileIndex] = tileIndex // initialize state
        }
        setNewState(start, Main.ANIMATION_LAG)
        dim(Main.DIM)
    }


    fun togglePuzzleSize(v: TextView) {
        cols = if (cols == 3) 4 else 3
        hak(slide)
        addTiles()
        resetAndSortTiles()
        Log.i(logTag, "selected $cols cols")
        when (cols) {
            3 -> v.text = Main.mContext?.resources?.getString(R.string.puzzle8)
            4 -> v.text = Main.mContext?.resources?.getString(R.string.puzzle15)
        }
//        if ((show == 1) or (show == 2)) {
//            for (tileIndex in 0 until max_number_of_tiles) {
//                mTiles[tileIndex].showTileIndex("")
//                mTiles[tileIndex].showTileIndex(String.format("%d", tileIndex))
//            }
//        }
    }

    fun showTileIndex() {
        when (show) {
            0 -> {
                show = 2
                hak(slide)
                var tileIndex = 0
                while (tileIndex < max_number_of_tiles) {
                    mTiles[tileIndex].showTileIndex(String.format("%d", tileIndex))
                    tileIndex++
                }
            }
            1 -> {
                show = 0
                hak(slide)
                var tileIndex = 0
                while (tileIndex < max_number_of_tiles) {
                    mTiles[tileIndex].showTileIndex("")
                    tileIndex++
                }
            }
            2 -> {
                show = 1
                var tileIndex = 0
                while (tileIndex < max_number_of_tiles) {
                    mTiles[tileIndex].background = flatBackground!!.drawable
                    mTiles[tileIndex].showTileIndex(String.format("%d", tileIndex))
                    tileIndex++
                }
            }
        }
        Log.i(logTag, "show $show")
    }

    fun solve() {
        Main.oscar.send(FSM.EV_SOLVE_REQ, 0, 0, state)
    }


    companion object {
        private const val max_number_of_tiles = 16
    }
}