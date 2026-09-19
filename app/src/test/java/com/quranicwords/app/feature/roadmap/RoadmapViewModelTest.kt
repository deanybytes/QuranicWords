package com.quranicwords.app.feature.roadmap

import com.quranicwords.app.core.data.local.entity.LessonStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadmapViewModelTest {

    @Test
    fun `a completed lesson is reachable`() {
        assertTrue(isRoadmapReachable(LessonStatus.COMPLETED))
    }

    @Test
    fun `an unlocked-but-not-completed lesson is reachable`() {
        assertTrue(isRoadmapReachable(LessonStatus.UNLOCKED))
    }

    @Test
    fun `a locked lesson is not reachable`() {
        assertFalse(isRoadmapReachable(LessonStatus.LOCKED))
    }
}
