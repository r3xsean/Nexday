package com.nexday.app.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nexday.app.ui.main.fragments.TodayFragment
import com.nexday.app.ui.main.fragments.TomorrowFragment
import com.nexday.app.ui.main.fragments.YesterdayFragment

/**
 * ViewPager2 adapter for the 3-tab interface
 * Manages Yesterday, Today, and Tomorrow fragments
 */
class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    companion object {
        const val YESTERDAY_POSITION = 0
        const val TODAY_POSITION = 1
        const val TOMORROW_POSITION = 2
        const val TOTAL_PAGES = 3
    }
    
    override fun getItemCount(): Int = TOTAL_PAGES
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            YESTERDAY_POSITION -> YesterdayFragment()
            TODAY_POSITION -> TodayFragment()
            TOMORROW_POSITION -> TomorrowFragment()
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
    
    /**
     * Get the page title for the given position
     */
    fun getPageTitle(position: Int): String {
        return when (position) {
            YESTERDAY_POSITION -> "Yesterday"
            TODAY_POSITION -> "Today"
            TOMORROW_POSITION -> "Tomorrow"
            else -> throw IllegalStateException("Unexpected position $position")
        }
    }
}